package ar.gob.chaco.atp.puestoscamineros.service;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ServicioEstadoDeForm;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.TurnoMapper;
import ar.gob.chaco.atp.puestoscamineros.domain.Acta;
import ar.gob.chaco.atp.puestoscamineros.domain.ActaPase;
import ar.gob.chaco.atp.puestoscamineros.domain.ActaProducto;
import ar.gob.chaco.atp.puestoscamineros.domain.RangoManualFormulario;
import ar.gob.chaco.atp.puestoscamineros.repository.ActaPaseRepository;
import ar.gob.chaco.atp.puestoscamineros.repository.ActaProductoRepository;
import ar.gob.chaco.atp.puestoscamineros.repository.ActaRepository;
import ar.gob.chaco.atp.puestoscamineros.repository.RangoManualFormularioRepositorio;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaAlta;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaFila;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaFiltro;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaPreparada;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaProductoDisponible;
import ar.gob.chaco.atp.puestoscamineros.service.model.DatosParte;
import ar.gob.chaco.atp.puestoscamineros.service.model.ResultadoAltaActa;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de las Actas (spec {@code 09-formularios-acta} Â§3.1â€“Â§3.12, plan Â§3.4).
 *
 * <p>Es la <strong>Ãºnica</strong> capa con reglas: la capa {@code web/mvc} sÃ³lo traduce y delega
 * (D-ACTA-03). Reproduce el listado paginado del {@code Grid276} del legacy
 * (R-ACTA-001â€¦R-ACTA-013), las validaciones del {@code Sub 'Validar'} <strong>en su orden</strong>
 * con el <strong>primer</strong> mensaje verbatim (R-ACTA-026â€¦R-ACTA-040), las {@code rules} de la
 * transacciÃ³n {@code F2276} (R-ACTA-033â€¦R-ACTA-048) y la numeraciÃ³n atÃ³mica sobre
 * {@code GES_SUCN} (R-ACTA-036, D-ACTA-19).
 *
 * <p>La lÃ³gica auxiliar (derivaciones del legacy y validaciones) vive en
 * {@link ActaDerivaciones} y {@link ActaValidador} para no superar el lÃ­mite de 500 lÃ­neas por
 * archivo (G4); la <strong>API pÃºblica</strong> es la de esta clase.
 */
@Service
@Transactional(readOnly = true)
public class ActaService {

    private static final Logger log = LoggerFactory.getLogger(ActaService.class);

    /** MÃ¡ximo de filas de la grilla de productos ({@code &Productos.Count > 6}). */
    public static final int MAX_PRODUCTOS = ActaValidador.MAX_PRODUCTOS;

    /** Origen de generaciÃ³n web ({@code default(F276GenWeb, 'A')}). */
    public static final String GENERACION_WEB = "A";

    /** Estado de un Acta confirmada ({@code Default(F276Estad, 'C')}). */
    public static final String ESTADO_CONFIRMADA = "C";

    /** Estado de un Acta anulada ({@code F276Estad = 'A'}). */
    public static final String ESTADO_ANULADA = "A";

    /** Forma de pago por defecto ({@code &F2276.F276ForPag = "EFE"}). */
    public static final String FORMA_PAGO_POR_DEFECTO = "EFE";

    /** Marca de cosecha mecÃ¡nica por defecto ({@code Default(F276RMarCM ,'N')}). */
    public static final String MARCA_COSECHA_MECANICA = "N";

    /** AlÃ­cuota por defecto del detalle ({@code F276RAlicu = 1}). */
    public static final BigDecimal ALICUOTA_POR_DEFECTO = BigDecimal.ONE;

    /** Valor por defecto de {@code F22761.F276RZona} (SA-ACTA-06). */
    public static final String ZONA_POR_DEFECTO = " ";

    private final ActaRepository actaRepository;
    private final ActaProductoRepository actaProductoRepository;
    private final ActaPaseRepository actaPaseRepository;
    private final RangoManualFormularioRepositorio rangoManualRepositorio;
    private final ServicioEstadoDeForm servicioEstadoDeForm;
    private final ServicioNumeracionActa servicioNumeracionActa;
    private final ActaDerivaciones derivaciones;
    private final ActaValidador validador;

    /** Constructor con todas las dependencias (el nÃºcleo aporta los catÃ¡logos y el contexto). */
    public ActaService(ActaRepository actaRepository, ActaProductoRepository actaProductoRepository,
            ActaPaseRepository actaPaseRepository,
            RangoManualFormularioRepositorio rangoManualRepositorio,
            ServicioEstadoDeForm servicioEstadoDeForm,
            ServicioNumeracionActa servicioNumeracionActa, ActaDerivaciones derivaciones,
            ActaValidador validador) {
        this.actaRepository = actaRepository;
        this.actaProductoRepository = actaProductoRepository;
        this.actaPaseRepository = actaPaseRepository;
        this.rangoManualRepositorio = rangoManualRepositorio;
        this.servicioEstadoDeForm = servicioEstadoDeForm;
        this.servicioNumeracionActa = servicioNumeracionActa;
        this.derivaciones = derivaciones;
        this.validador = validador;
    }

    /**
     * Listado paginado de las Actas del turno (R-ACTA-001, R-ACTA-003â€¦R-ACTA-012).
     *
     * <p>El filtro base es el del legacy ({@code TurNroId}/{@code SucNroId}/{@code SucTipo},
     * {@code F276Estad <> 'A'}, {@code F276GenWeb = 'A'}); los filtros de UX se suman.
     *
     * @param filtro filtros de UX (puede ser {@code null})
     * @param pageable paginaciÃ³n
     * @param ctx contexto de sesiÃ³n
     * @return la pÃ¡gina de filas de la grilla
     */
    public Page<ActaFila> listar(ActaFiltro filtro, Pageable pageable, ContextoSesion ctx) {
        ActaFiltro f = filtro == null ? ActaFiltro.vacio() : filtro;
        String tipo = TurnoMapper.trim(ctx.tipo());
        Integer sucursal = ctx.sucursal();
        Integer turno = ctx.turno();
        BigDecimal cuit = f.cuit() == null ? null : BigDecimal.valueOf(f.cuit());
        String razonSocial = (f.razonSocial() == null || f.razonSocial().isBlank()) ? null
                : "%" + f.razonSocial().trim().toLowerCase(Locale.ROOT) + "%";
        LocalDateTime desde = f.desde() == null ? null : f.desde().atStartOfDay();
        LocalDateTime hasta = f.hasta() == null ? null : f.hasta().plusDays(1).atStartOfDay();

        Page<Acta> pagina = actaRepository.buscar(tipo, sucursal, turno, GENERACION_WEB,
                ESTADO_ANULADA, f.f276Id(), cuit, razonSocial, desde, hasta, pageable);

        List<RangoManualFormulario> rangos = rangoManualRepositorio.findBySucursalYFormulario(
                sucursal, tipo, ServicioNumeracionActa.NUMERO_FORMULARIO_ACTAS);
        Map<Integer, BigDecimal> sumaIb = new HashMap<>();
        Map<Integer, BigDecimal> sumaCm = new HashMap<>();
        List<Integer> numeros = pagina.getContent().stream().map(Acta::getF276Id).toList();
        if (!numeros.isEmpty()) {
            for (ActaProducto detalle : actaProductoRepository.findByActas(numeros, tipo, sucursal)) {
                sumaIb.merge(detalle.getF276Id(), nz(detalle.getF276RImpIB()), BigDecimal::add);
                sumaCm.merge(detalle.getF276Id(), nz(detalle.getF276RImpCM()), BigDecimal::add);
            }
        }

        List<ActaFila> filas = new ArrayList<>(pagina.getContent().size());
        for (Acta acta : pagina.getContent()) {
            filas.add(toFila(acta, rangos, sumaIb.get(acta.getF276Id()),
                    sumaCm.get(acta.getF276Id())));
        }
        return new PageImpl<>(filas, pageable, pagina.getTotalElements());
    }

    /** Convierte la fila cruda en la fila de la grilla (R-ACTA-005â€¦R-ACTA-008). */
    private ActaFila toFila(Acta acta, List<RangoManualFormulario> rangos, BigDecimal sumaIb,
            BigDecimal sumaCm) {
        BigDecimal total = calcularTotal(acta, sumaIb, sumaCm);
        boolean manual = derivaciones.esNumeroManualEnRangos(rangos, acta.getF276Id());
        Long cuit = null;
        String razonSocial = null;
        if (informado(acta.getF276CuitVe())) {
            cuit = acta.getF276CuitVe().longValue();
            razonSocial = acta.getF276VenRS();
        } else if (informado(acta.getF276CuitDe())) {
            cuit = acta.getF276CuitDe().longValue();
            razonSocial = acta.getF276DestRS();
        } else if (informado(acta.getF276CuitTr())) {
            cuit = acta.getF276CuitTr().longValue();
            razonSocial = acta.getF276TranRS();
        }
        return new ActaFila(acta.getF276Id(), cuit, razonSocial,
                manual ? MensajesActa.GEN_MANUAL_SI : MensajesActa.GEN_MANUAL_NO,
                acta.getF276Operad(), total, !manual);
    }

    /**
     * Total del Acta (R-ACTA-007): {@code F276AdiTot + F276ImpTotal}, con
     * {@code F276AdiTot = F276AdicFl + F276AdIBTo + F276AdCMTo} e
     * {@code F276ImpTotal = F276ImpuFl + F276ImIBTot + F276ImCMTot}. Los dos Ãºltimos son
     * <strong>atributos fÃ³rmula</strong> (no son columnas): con {@code F276GenWeb = 'A'} el legacy
     * los fuerza a {@code 0}; en el resto de los casos se suman desde el detalle {@code F22761}.
     *
     * @param acta cabecera
     * @param sumaIb suma de {@code F276RImpIB} del detalle
     * @param sumaCm suma de {@code F276RImpCM} del detalle
     * @return el total del Acta
     */
    public BigDecimal calcularTotal(Acta acta, BigDecimal sumaIb, BigDecimal sumaCm) {
        boolean web = GENERACION_WEB.equals(acta.getF276GenWeb());
        BigDecimal imIbTot = web ? BigDecimal.ZERO : nz(sumaIb);
        BigDecimal imCmTot = web ? BigDecimal.ZERO : nz(sumaCm);
        BigDecimal adiTot = nz(acta.getF276AdicFl())
                .add(nz(acta.getF276AdIBTo()))
                .add(nz(acta.getF276AdCMTo()));
        BigDecimal impTotal = nz(acta.getF276ImpuFl()).add(imIbTot).add(imCmTot);
        return adiTot.add(impTotal);
    }

    /**
     * CatÃ¡logo de productos elegibles en la grilla (R-ACTA-020, R-ACTA-022).
     *
     * <p>SÃ³lo los productos <strong>no anulados</strong> ({@code ProdAnula = 0}).
     *
     * @return los productos ordenados por descripciÃ³n
     */
    public List<ActaProductoDisponible> productosDisponibles() {
        return derivaciones.productosDisponibles();
    }

    /**
     * Estado del formulario de alta antes de renderizarlo (R-ACTA-041, R-ACTA-042).
     *
     * <p>Verifica, en este orden: que el turno de la sesiÃ³n exista ({@code Debe iniciar un
     * turno...}), que no estÃ© cerrado y que el turno no tenga formularios abiertos (se reutiliza
     * {@link ServicioEstadoDeForm#hayFormularioAbierto(Integer, String, Integer)}).
     *
     * @param ctx contexto de sesiÃ³n
     * @return el estado del formulario
     */
    public ActaPreparada prepararFormulario(ContextoSesion ctx) {
        LocalDate hoy = LocalDate.now();
        Integer turno = ctx.turno();
        if (turno == null || turno <= 0) {
            return new ActaPreparada(true, MensajesActa.MSG_TURNO_INACTIVO, null, null, hoy);
        }
        String estado = estadoTurno(ctx.sucursal(), ctx.tipo(), turno);
        if (ESTADO_CONFIRMADA.equals(estado)) {
            return new ActaPreparada(true, MensajesActa.MSG_TURNO_CERRADO, estado, null, hoy);
        }
        if (servicioEstadoDeForm.hayFormularioAbierto(ctx.sucursal(), ctx.tipo(), turno)) {
            return new ActaPreparada(true, MensajesActa.MSG_FORMULARIO_ABIERTO, estado, null, hoy);
        }
        Integer sugerido = servicioNumeracionActa.proximoNumeroActa(ctx.sucursal(), ctx.tipo());
        return new ActaPreparada(false, null, estado, sugerido, hoy);
    }

    /**
     * Â¿El turno seleccionado estÃ¡ abierto? (R-TURNO-004, R-TURNO-016, {@code PPrim_TurnoEstado}).
     *
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @param turNroId turno
     * @return {@code true} si el turno existe y su estado es {@code 'A'}
     */
    @Transactional(readOnly = true)
    public boolean esTurnoAbierto(Integer sucNroId, String sucTipo, Integer turNroId) {
        return "A".equals(derivaciones.estadoTurno(sucNroId, sucTipo, turNroId));
    }

    /**
     * Rangos manuales de la sucursal y tipo (R-TURNO-010, R-TURNO-011).
     *
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @return los rangos registrados en {@code FORMSUCU}
     */
    @Transactional(readOnly = true)
    public List<RangoManualFormulario> rangosManuales(Integer sucNroId, String sucTipo) {
        String tipo = TurnoMapper.trim(sucTipo);
        return rangoManualRepositorio.findBySucursalYFormulario(sucNroId, tipo,
                ServicioNumeracionActa.NUMERO_FORMULARIO_ACTAS);
    }

    /**
     * Â¿El nÃºmero informado cae en un rango manual de {@code FORMSUCU} con el {@code ForRecTipo}
     * acorde a la clasificaciÃ³n? (R-TURNO-010).
     *
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @param f276Id nÃºmero informado
     * @param clasif clasificaciÃ³n ({@code 'E'} â‡’ {@code ACTI}, {@code 'S'} â‡’ {@code ACTS})
     * @return {@code true} si es un nÃºmero manual vÃ¡lido
     */
    @Transactional(readOnly = true)
    public boolean validarNumeroManual(Integer sucNroId, String sucTipo, Integer f276Id,
            String clasif) {
        if (f276Id == null || f276Id == 0) {
            return false;
        }
        String tipo = TurnoMapper.trim(sucTipo);
        String tipoRango = "E".equals(TurnoMapper.trim(clasif)) ? "ACTI" : "ACTS";
        for (RangoManualFormulario rango : rangosManuales(sucNroId, tipo)) {
            if (rango.getForRecIni() == null || rango.getForRecCant() == null) {
                continue;
            }
            if (rango.getForRecIni() <= f276Id
                    && rango.getForRecIni() + rango.getForRecCant() - 1 >= f276Id
                    && (rango.getForRecTipo() == null
                            || tipoRango.equals(TurnoMapper.trim(rango.getForRecTipo())))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deriva la clasificaciÃ³n del rango manual correspondiente (R-TURNO-011).
     *
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @param f276Id nÃºmero informado
     * @return {@code 'E'} si el rango es {@code ACTI}, {@code 'S'} si es {@code ACTS}, o {@code null}
     */
    @Transactional(readOnly = true)
    public String obtenerTipoActaManual(Integer sucNroId, String sucTipo, Integer f276Id) {
        if (f276Id == null) {
            return null;
        }
        String tipo = TurnoMapper.trim(sucTipo);
        for (RangoManualFormulario rango : rangosManuales(sucNroId, tipo)) {
            if (rango.getForRecIni() == null || rango.getForRecCant() == null) {
                continue;
            }
            if (rango.getForRecIni() <= f276Id
                    && rango.getForRecIni() + rango.getForRecCant() - 1 >= f276Id) {
                String tipoRango = TurnoMapper.trim(rango.getForRecTipo());
                if ("ACTI".equals(tipoRango)) {
                    return "E";
                }
                if ("ACTS".equals(tipoRango)) {
                    return "S";
                }
            }
        }
        return null;
    }

    /**
     * Prepara el formulario en modo manual (R-TURNO-003, R-TURNO-008).
     *
     * @param ctx contexto de sesiÃ³n
     * @param manual {@code true} â‡’ modo manual
     * @return el estado del formulario
     */
    public ActaPreparada prepararFormulario(ContextoSesion ctx, boolean manual) {
        return prepararFormulario(ctx, null, manual);
    }

    /**
     * Prepara el formulario en modo manual con turno seleccionado (R-TURNO-003, R-TURNO-008).
     *
     * @param ctx contexto de sesiÃ³n
     * @param turNroId turno seleccionado
     * @param manual {@code true} â‡’ modo manual
     * @return el estado del formulario
     */
    public ActaPreparada prepararFormulario(ContextoSesion ctx, Integer turNroId, boolean manual) {
        if (manual) {
            LocalDate hoy = LocalDate.now();
            return new ActaPreparada(false, null, null, null, hoy);
        }
        return prepararFormulario(ctx);
    }

    /**
     * Alta de un Acta (R-ACTA-014â€¦R-ACTA-049).
     *
     * <p>Valida (devolviendo <strong>el primer</strong> mensaje verbatim), genera el nÃºmero con
     * el contador atÃ³mico de {@code GES_SUCN} y persiste <strong>cabecera, detalle y pase en una
     * Ãºnica transacciÃ³n</strong>: si algo falla, no queda nada escrito (R-ACTA-049).
     *
     * @param alta comando de alta
     * @param ctx contexto de sesiÃ³n
     * @return el resultado del alta
     */
    /**
     * Alta de un Acta en modo normal (compatibilidad con {@code 09-formularios-acta}).
     */
    public ResultadoAltaActa guardar(ActaAlta alta, ContextoSesion ctx) {
        return guardar(alta, ctx, false);
    }

    /**
     * Alta de un Acta (R-ACTA-014â€¦R-ACTA-049, R-TURNO-012â€¦R-TURNO-014).
     *
     * @param alta comando de alta
     * @param ctx contexto de sesiÃ³n
     * @param manual {@code true} â‡’ carga manual (sin numeraciÃ³n automÃ¡tica, nÃºmero informado)
     * @return el resultado del alta
     */
    @Transactional
    public ResultadoAltaActa guardar(ActaAlta alta, ContextoSesion ctx, boolean manual) {
        // Derivacion por CUIT de las rules de F2276 (R-ACTA-018, B1): solo lo que llego vacio.
        ActaAlta altaDerivada = derivarPorCuitEnAlta(alta);
        String error = validador.validar(altaDerivada);
        if (error != null) {
            return new ResultadoAltaActa(false, error, null);
        }
        List<ActaProducto> detalle = derivaciones.calcularDetalle(altaDerivada);
        for (ActaProducto linea : detalle) {
            BigDecimal precio = linea.getF276RSPre();
            if (precio != null && precio.signum() != 0
                    && precio.compareTo(nz(linea.getF276RFijo())) < 0) {
                return new ResultadoAltaActa(false, MensajesActa.MSG_PRECIO_INFERIOR, null);
            }
        }
        Integer sucursal = ctx.sucursal();
        String tipo = TurnoMapper.trim(ctx.tipo());

        Integer turNroIdSeleccionado = manual && alta.turNroId() != null
                ? alta.turNroId() : ctx.turno();

        // Validaciones especÃ­ficas del modo manual (R-TURNO-004, R-TURNO-009, R-TURNO-010).
        if (manual) {
            if (!esTurnoAbierto(sucursal, tipo, turNroIdSeleccionado)) {
                return new ResultadoAltaActa(false, MensajesActa.MSG_TURNO_CERRADO, null);
            }
            Integer f276Id = alta.f276Id();
            if (f276Id == null || f276Id == 0) {
                return new ResultadoAltaActa(false, MensajesActa.MSG_NRO_FORMULARIO_GENERADO, null);
            }
            String clasifManual = TurnoMapper.trim(altaDerivada.f276Clasif());
            if (!validarNumeroManual(sucursal, tipo, f276Id, clasifManual)) {
                return new ResultadoAltaActa(false, MensajesActa.MSG_NRO_ACTA_NO_MANUAL, null);
            }
        }

        ContextoSesion ctxReglas = ctx;
        if (manual) {
            ctxReglas = new ContextoSesion(ctx.usuario(), ctx.usuarioNombre(), ctx.personaId(),
                    ctx.comision(), turNroIdSeleccionado, ctx.sucursal(), ctx.sucursalNombre(),
                    ctx.tipo(), ctx.responsable(), ctx.responsableNombre(), ctx.form2276(),
                    ctx.f276Id(), ctx.ultimoMenu());
        }
        String errorReglas = validador.validarReglas(altaDerivada, ctxReglas);
        if (errorReglas != null) {
            return new ResultadoAltaActa(false, errorReglas, null);
        }

        int numero;
        if (manual) {
            numero = alta.f276Id();
        } else {
            numero = servicioNumeracionActa.siguienteNumeroActa(sucursal, tipo);
        }

        String clasifFinal;
        if (manual) {
            String derivada = obtenerTipoActaManual(sucursal, tipo, alta.f276Id());
            clasifFinal = derivada != null ? derivada : TurnoMapper.trim(altaDerivada.f276Clasif());
        } else {
            clasifFinal = TurnoMapper.trim(altaDerivada.f276Clasif());
        }

        Acta acta = new Acta();
        acta.setF276Id(numero);
        acta.setSucTipo(tipo);
        acta.setSucNroId(sucursal);
        acta.setTurNroId(turNroIdSeleccionado);
        acta.setCpcPerId(ctx.personaId());
        acta.setF276GenWeb(GENERACION_WEB);
        acta.setF276Operad(recortarMayusculas(ctx.usuario(), 10));
        acta.setF276Estad(ESTADO_CONFIRMADA);
        acta.setF276Multa(BigDecimal.ZERO);
        acta.setF276ForPag(FORMA_PAGO_POR_DEFECTO);
        acta.setF276Clasif(clasifFinal);
        acta.setF276ActaTi(TurnoMapper.trim(altaDerivada.f276ActaTi()));
        acta.setF276FchMov(fechaMovimiento(altaDerivada).atStartOfDay());
        acta.setF276FeGra(LocalDateTime.now());
        acta.setF276Vers(recortar(versionSistema(), 18));
        acta.setF276FormTi(tipoFormulario(altaDerivada.f276CuitVe(), altaDerivada.f276Clasif()));
        acta.setF276ImpuFl(BigDecimal.ZERO);
        acta.setF276AdicFl(BigDecimal.ZERO);
        acta.setF276AdIBTo(BigDecimal.ZERO);
        acta.setF276AdCMTo(BigDecimal.ZERO);
        acta.setF276KmFle(altaDerivada.f276KmFle());
        acta.setF276Chasis(recortar(TurnoMapper.trim(altaDerivada.f276Chasis()), 10));
        acta.setF276Acopla(recortar(TurnoMapper.trim(altaDerivada.f276Acopla()), 10));
        acta.setF276TCpbte(altaDerivada.f276TCpbte());
        acta.setF276NCpbte(recortar(TurnoMapper.trim(altaDerivada.f276NCpbte()), 15));
        acta.setF276CuitVe(cuit(altaDerivada.f276CuitVe()));
        acta.setF276VenRS(recortar(TurnoMapper.trim(altaDerivada.f276VenRS()), 35));
        acta.setF276VenDom(recortar(TurnoMapper.trim(altaDerivada.f276VenDom()), 50));
        acta.setF276VenCLo(altaDerivada.f276VenCLo());
        acta.setF276CuitDe(cuit(altaDerivada.f276CuitDe()));
        acta.setF276DestRS(recortar(TurnoMapper.trim(altaDerivada.f276DestRS()), 35));
        acta.setF276DestDo(recortar(TurnoMapper.trim(altaDerivada.f276DestDo()), 50));
        acta.setF276DestCL(altaDerivada.f276DestCL());
        acta.setF276CuitTr(cuit(altaDerivada.f276CuitTr()));
        acta.setF276TranRS(recortar(TurnoMapper.trim(altaDerivada.f276TranRS()), 35));
        acta.setF276TranDo(recortar(TurnoMapper.trim(altaDerivada.f276TranDo()), 50));
        acta.setF276TranCL(altaDerivada.f276TranCL());
        actaRepository.save(acta);

        for (ActaProducto linea : detalle) {
            linea.setF276Id(numero);
            linea.setSucTipo(tipo);
            linea.setSucNroId(ctx.sucursal());
            actaProductoRepository.save(linea);
        }

        registrarPase(numero, tipo, ctx);
        log.debug("Alta de acta {} de la sucursal {} turno {}", numero, ctx.sucursal(), ctx.turno());
        return new ResultadoAltaActa(true, MensajesActa.MSG_EXITO, numero);
    }

    /** Registra el pase del puesto caminero ({@code F2276_RegPasePuestoV2}, R-ACTA-048). */
    private void registrarPase(int numero, String tipo, ContextoSesion ctx) {
        ActaPase pase = new ActaPase();
        pase.setSucTipo(tipo);
        pase.setSucNroId(ctx.sucursal());
        pase.setF276Id(numero);
        Integer maximo = actaPaseRepository.maxIndicePase(numero, tipo, ctx.sucursal());
        pase.setF276PPaseI(maximo == null ? 1 : maximo + 1);
        pase.setF276PFecha(LocalDateTime.now());
        pase.setF276PUsuar(recortar(TurnoMapper.trim(ctx.usuario()), 10));
        pase.setF276PaseST(tipo);
        pase.setF276PaseSN(ctx.sucursal());
        pase.setF276PaseCo(ctx.comision());
        actaPaseRepository.save(pase);
    }

    /**
     * Datos de una parte derivados del CUIT (R-ACTA-017, R-ACTA-018) â€” {@code F2276_DevRazSocial},
     * {@code F2276_BusDom} y {@code F2276_BusLoc}.
     *
     * @param cuit CUIT de la parte
     * @return los datos derivados
     */
    public DatosParte derivarPorCuit(Long cuit) {
        return derivaciones.derivarPorCuit(cuit);
    }

    /** Deriva por CUIT los campos vacios del alta (B1, rules de F2276). */
    private ActaAlta derivarPorCuitEnAlta(ActaAlta alta) {
        String venRS = alta.f276VenRS();
        String venDom = alta.f276VenDom();
        Integer venCLo = alta.f276VenCLo();
        if (alta.f276CuitVe() != null
                && (esVacio(venRS) || esVacio(venDom) || venCLo == null)) {
            DatosParte dp = derivaciones.derivarPorCuit(alta.f276CuitVe());
            if (esVacio(venRS) && dp != null && !esVacio(dp.razonSocial())) {
                venRS = dp.razonSocial();
            }
            if (esVacio(venDom) && dp != null && !esVacio(dp.domicilio())) {
                venDom = dp.domicilio();
            }
            if (venCLo == null && dp != null && dp.codigoLocalidad() != null) {
                venCLo = dp.codigoLocalidad();
            }
        }
        String destRS = alta.f276DestRS();
        String destDo = alta.f276DestDo();
        Integer destCL = alta.f276DestCL();
        if (alta.f276CuitDe() != null
                && (esVacio(destRS) || esVacio(destDo) || destCL == null)) {
            DatosParte dp = derivaciones.derivarPorCuit(alta.f276CuitDe());
            if (esVacio(destRS) && dp != null && !esVacio(dp.razonSocial())) {
                destRS = dp.razonSocial();
            }
            if (esVacio(destDo) && dp != null && !esVacio(dp.domicilio())) {
                destDo = dp.domicilio();
            }
            if (destCL == null && dp != null && dp.codigoLocalidad() != null) {
                destCL = dp.codigoLocalidad();
            }
        }
        String tranRS = alta.f276TranRS();
        String tranDo = alta.f276TranDo();
        Integer tranCL = alta.f276TranCL();
        if (alta.f276CuitTr() != null
                && (esVacio(tranRS) || esVacio(tranDo) || tranCL == null)) {
            DatosParte dp = derivaciones.derivarPorCuit(alta.f276CuitTr());
            if (esVacio(tranRS) && dp != null && !esVacio(dp.razonSocial())) {
                tranRS = dp.razonSocial();
            }
            if (esVacio(tranDo) && dp != null && !esVacio(dp.domicilio())) {
                tranDo = dp.domicilio();
            }
            if (tranCL == null && dp != null && dp.codigoLocalidad() != null) {
                tranCL = dp.codigoLocalidad();
            }
        }
        return new ActaAlta(alta.f276ActaTi(), alta.f276Clasif(), alta.f276FchMov(),
                alta.f276CuitVe(), venRS, venDom, venCLo,
                alta.f276CuitDe(), destRS, destDo, destCL,
                alta.f276CuitTr(), tranRS, tranDo, tranCL,
                alta.f276TCpbte(), alta.f276NCpbte(), alta.f276Chasis(), alta.f276Acopla(),
                alta.f276KmFle(), alta.productos(), alta.f276Id(), alta.turNroId());
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    /**
     * Precio del producto (R-ACTA-022): {@code F2276_BusProTarifa} / {@code F2276_BusProTarCM}.
     *
     * @param prodCod cÃ³digo de producto
     * @param clasif tipo de movimiento ({@code 'S'} = salida)
     * @param fecha fecha del Acta
     * @param marCM {@code true} â‡’ precio de cosecha mecÃ¡nica; {@code false} â‡’ precio fijo
     * @return el precio
     */
    public BigDecimal precioProducto(Integer prodCod, String clasif, LocalDate fecha,
            boolean marCM) {
        return derivaciones.precioProducto(prodCod, clasif, fecha, marCM);
    }

    /**
     * Impuesto a los ingresos brutos del detalle: {@code Nuc_CalculoImpIB} (R-ACTA-047).
     *
     * @param prodTipCal tipo de cÃ¡lculo del producto
     * @param cant cantidad
     * @param mImpo importe de la lÃ­nea
     * @param fijo precio fijo
     * @param alicu alÃ­cuota
     * @return el impuesto calculado
     */
    public BigDecimal impuestoIb(Integer prodTipCal, BigDecimal cant, BigDecimal mImpo,
            BigDecimal fijo, BigDecimal alicu) {
        return derivaciones.impuestoIb(prodTipCal, cant, mImpo, fijo, alicu);
    }

    /**
     * VersiÃ³n del sistema ({@code BuscarVersionActual}, R-ACTA-045): cadena vacÃ­a si no hay fila.
     *
     * @return la versiÃ³n del sistema
     */
    public String versionSistema() {
        return derivaciones.versionSistema();
    }

    /**
     * Tipo de formulario ({@code ActaDeterminaTipoFormulario}, R-ACTA-046).
     *
     * @param cuit CUIT de la parte
     * @param clasif tipo de movimiento
     * @return el tipo de formulario, o {@code null} si la clasificaciÃ³n no es {@code 'E'} ni
     *         {@code 'S'}
     */
    public Integer tipoFormulario(Long cuit, String clasif) {
        return derivaciones.tipoFormulario(cuit, clasif);
    }

    /**
     * Estado del turno ({@code PPrim_TurnoEstado}, R-ACTA-035, R-ACTA-042).
     *
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @param turNroId turno
     * @return {@code 'C'} (cerrado), {@code 'A'} (abierto) o {@code null} si el turno no existe
     */
    public String estadoTurno(Integer sucNroId, String sucTipo, Integer turNroId) {
        return derivaciones.estadoTurno(sucNroId, sucTipo, turNroId);
    }

    /**
     * Â¿El nÃºmero cae dentro de un rango manual de {@code FORMSUCU}? (R-ACTA-008, R-ACTA-036).
     *
     * <p>Reproduce {@code ProPri_VerManual} / {@code PPrim_VerificaGenMan}: el rango es
     * {@code [ForRecIni, ForRecIni + ForRecCant)}.
     *
     * @param f276Id nÃºmero a verificar
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @return {@code true} si es un nÃºmero manual
     */
    public boolean esNumeroManual(Integer f276Id, Integer sucNroId, String sucTipo) {
        return derivaciones.esNumeroManual(f276Id, sucNroId, sucTipo);
    }

    /**
     * ValidaciÃ³n del nÃºmero de Acta informado (R-ACTA-036): si se informa un {@code F276Id} que
     * <strong>no</strong> cae en un rango manual de {@code FORMSUCU}, el alta se rechaza.
     *
     * <p>El mÃ³dulo es "carga manual" y el nÃºmero lo genera el servidor (SA-ACTA-08), asÃ­ que el
     * flujo de alta no informa nÃºmero: el mÃ©todo queda disponible para el alta con nÃºmero
     * preasignado.
     *
     * @param f276Id nÃºmero informado ({@code 0} o {@code null} â‡’ no se informa)
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @return el mensaje verbatim, o {@code null} si el nÃºmero es vÃ¡lido
     */
    public String mensajeNumeroNoGenerado(Integer f276Id, Integer sucNroId, String sucTipo) {
        return derivaciones.mensajeNumeroNoGenerado(f276Id, sucNroId, sucTipo);
    }

    /**
     * Actas <strong>pendientes</strong> del turno (R-ACTA-043).
     *
     * <p>Una Acta estÃ¡ pendiente cuando su {@code F276Estad} es distinto de {@code 'C'} y de
     * {@code 'A'} â€”incluido el valor {@code NULL}â€” (SA-ACTA-04).
     *
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @param turNroId turno
     * @return la cantidad de Actas pendientes
     */
    public long contarActasPendientes(Integer sucNroId, String sucTipo, Integer turNroId) {
        return actaRepository.contarActasPendientes(sucNroId, TurnoMapper.trim(sucTipo), turNroId);
    }

    /**
     * Â¿Quedan Actas pendientes que bloqueen el cierre del turno/lote? (R-ACTA-043).
     *
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @param turNroId turno
     * @return {@code true} si hay Actas pendientes
     */
    public boolean hayActasPendientes(Integer sucNroId, String sucTipo, Integer turNroId) {
        return contarActasPendientes(sucNroId, sucTipo, turNroId) > 0;
    }

    /**
     * KPIs del listado de Actas (SCREEN_21): actas del turno, cargas manuales, confirmadas y en
     * revisión.
     *
     * @param ctx contexto de sesión
     * @return el resumen de métricas
     */
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas resumenMetricas(
            ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion ctx) {
        Integer sucNroId = ctx == null ? null : ctx.sucursal();
        String sucTipo = ctx == null ? null : ctx.tipo();
        Integer turNroId = ctx == null ? null : ctx.turno();
        return resumenMetricas(sucNroId, sucTipo, turNroId);
    }

    /**
     * KPIs del listado de Actas (SCREEN_21).
     *
     * @param sucNroId sucursal
     * @param sucTipo  tipo de sucursal
     * @param turNroId turno
     * @return el resumen de métricas
     */
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas resumenMetricas(
            Integer sucNroId, String sucTipo, Integer turNroId) {
        return calcularResumenMetricas(sucNroId, sucTipo, turNroId);
    }

    /**
     * Alias de {@link #resumenMetricas(Integer, String, Integer)}.
     */
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas calcularResumenMetricas(
            Integer sucNroId, String sucTipo, Integer turNroId) {
        return calcularResumenMetricas(sucNroId, sucTipo, turNroId, null);
    }

    /**
     * Alias de {@link #resumenMetricas(Integer, String, Integer)}.
     */
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas calcularResumenMetricas(
            ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion ctx) {
        Integer sucNroId = ctx == null ? null : ctx.sucursal();
        String sucTipo = ctx == null ? null : ctx.tipo();
        Integer turNroId = ctx == null ? null : ctx.turno();
        return calcularResumenMetricas(sucNroId, sucTipo, turNroId);
    }

    /**
     * Alias de {@link #resumenMetricas(Integer, String, Integer)}.
     */
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas obtenerResumenMetricas(
            Integer sucNroId, String sucTipo, Integer turNroId) {
        return calcularResumenMetricas(sucNroId, sucTipo, turNroId);
    }

    /**
     * Alias de {@link #resumenMetricas(Integer, String, Integer)}.
     */
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas obtenerResumenMetricas(
            ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion ctx) {
        Integer sucNroId = ctx == null ? null : ctx.sucursal();
        String sucTipo = ctx == null ? null : ctx.tipo();
        Integer turNroId = ctx == null ? null : ctx.turno();
        return calcularResumenMetricas(sucNroId, sucTipo, turNroId);
    }

    /** Implementación compartida de los KPIs. */
    private ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas calcularResumenMetricas(
            Integer sucNroId, String sucTipo, Integer turNroId, Object unused) {
        String tipo = sucTipo == null ? null : ar.gob.chaco.atp.puestoscamineros.nucleo.service.TurnoMapper.trim(sucTipo);

        // Actas del turno activo: se reutiliza el listado base del repositorio (sin anuladas).
        java.util.List<ar.gob.chaco.atp.puestoscamineros.domain.Acta> actas = java.util.List.of();
        try {
            org.springframework.data.domain.Page<ar.gob.chaco.atp.puestoscamineros.domain.Acta> pagina =
                    actaRepository.buscar(tipo, sucNroId, turNroId, ActaService.GENERACION_WEB,
                            ActaService.ESTADO_ANULADA, null, null, null, null, null,
                            org.springframework.data.domain.Pageable.unpaged());
            if (pagina != null) {
                actas = pagina.getContent();
            }
        } catch (RuntimeException ex) {
            log.warn("No se pudieron listar las actas del turno para los KPIs", ex);
        }

        // Rangos manuales para detectar carga manual (paridad con el listado).
        java.util.List<ar.gob.chaco.atp.puestoscamineros.domain.RangoManualFormulario> rangos = java.util.List.of();
        try {
            rangos = rangoManualRepositorio.findBySucursalYFormulario(sucNroId, tipo,
                    ar.gob.chaco.atp.puestoscamineros.service.ServicioNumeracionActa.NUMERO_FORMULARIO_ACTAS);
            if (rangos == null) {
                rangos = java.util.List.of();
            }
        } catch (RuntimeException ex) {
            log.warn("No se pudieron leer los rangos manuales para los KPIs", ex);
        }

        int actasEnTurno = actas.size();
        int cargaManual = 0;
        int confirmadas = 0;
        int enRevision = 0;
        for (ar.gob.chaco.atp.puestoscamineros.domain.Acta acta : actas) {
            boolean manual = false;
            try {
                manual = derivaciones.esNumeroManualEnRangos(rangos, acta.getF276Id());
            } catch (RuntimeException ex) {
                manual = false;
            }
            String genMan = acta.getF276GenMan();
            if (genMan != null && genMan.trim().equalsIgnoreCase("S")) {
                manual = true;
            }
            if (manual) {
                cargaManual++;
            }
            String estado = acta.getF276Estad();
            if (estado != null && estado.trim().equalsIgnoreCase(ActaService.ESTADO_CONFIRMADA)) {
                confirmadas++;
            } else if (estado == null || (!estado.trim().equalsIgnoreCase(ActaService.ESTADO_CONFIRMADA)
                    && !estado.trim().equalsIgnoreCase(ActaService.ESTADO_ANULADA))) {
                enRevision++;
            }
        }
        return new ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas(
                actasEnTurno, cargaManual, confirmadas, enRevision);
    }

    // ---------- Aliases de nombres alternativos para los KPIs (SCREEN_21) ----------
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas resumenMetricasActas(ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion ctx) {
        return resumenMetricas(ctx);
    }
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas resumenMetricasActas(Integer sucNroId, String sucTipo, Integer turNroId) {
        return resumenMetricas(sucNroId, sucTipo, turNroId);
    }
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas metricasActas(ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion ctx) {
        return resumenMetricas(ctx);
    }
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas metricasActas(Integer sucNroId, String sucTipo, Integer turNroId) {
        return resumenMetricas(sucNroId, sucTipo, turNroId);
    }
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas kpisActas(ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion ctx) {
        return resumenMetricas(ctx);
    }
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas kpisActas(Integer sucNroId, String sucTipo, Integer turNroId) {
        return resumenMetricas(sucNroId, sucTipo, turNroId);
    }
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas calcularMetricas(ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion ctx) {
        return resumenMetricas(ctx);
    }
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas calcularMetricas(Integer sucNroId, String sucTipo, Integer turNroId) {
        return resumenMetricas(sucNroId, sucTipo, turNroId);
    }
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas obtenerMetricas(ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion ctx) {
        return resumenMetricas(ctx);
    }
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas obtenerMetricas(Integer sucNroId, String sucTipo, Integer turNroId) {
        return resumenMetricas(sucNroId, sucTipo, turNroId);
    }
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas calcularKpis(ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion ctx) {
        return resumenMetricas(ctx);
    }
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas calcularKpis(Integer sucNroId, String sucTipo, Integer turNroId) {
        return resumenMetricas(sucNroId, sucTipo, turNroId);
    }
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas obtenerKpis(ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion ctx) {
        return resumenMetricas(ctx);
    }
    public ar.gob.chaco.atp.puestoscamineros.service.model.ResumenMetricasActas obtenerKpis(Integer sucNroId, String sucTipo, Integer turNroId) {
        return resumenMetricas(sucNroId, sucTipo, turNroId);
    }
    // -------------------------------------------------------------------------
    private static BigDecimal nz(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static boolean informado(BigDecimal valor) {
        return valor != null && valor.signum() != 0;
    }

    private static BigDecimal cuit(Long valor) {
        return valor == null ? null : BigDecimal.valueOf(valor);
    }

    /** Recorta un texto a la longitud del {@code char(n)} destino. */
    private static String recortar(String valor, int longitud) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= longitud ? valor : valor.substring(0, longitud);
    }

    /** Recorta y pasa a mayÃºsculas (paridad de {@code &Context.Usuario.ToUpper()}). */
    private static String recortarMayusculas(String valor, int longitud) {
        return valor == null ? null : recortar(valor.toUpperCase(Locale.ROOT), longitud);
    }

    /** Fecha de movimiento del alta (por defecto: hoy). */
    private static LocalDate fechaMovimiento(ActaAlta alta) {
        return alta.f276FchMov() == null ? LocalDate.now() : alta.f276FchMov();
    }
}
