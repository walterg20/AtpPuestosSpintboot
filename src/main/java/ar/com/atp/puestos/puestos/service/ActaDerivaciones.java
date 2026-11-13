package ar.gob.chaco.atp.puestoscamineros.service;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Calle;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Localidad;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Producto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.ProductoVigencia;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Provincia;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Turno;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.TurnoId;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.CalleRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.LocalidadRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.ProductoRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.ProductoVigenciaRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.TurnoRepositorio;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.TurnoMapper;
import ar.gob.chaco.atp.puestoscamineros.domain.ActaProducto;
import ar.gob.chaco.atp.puestoscamineros.domain.Contribuyente;
import ar.gob.chaco.atp.puestoscamineros.domain.RangoManualFormulario;
import ar.gob.chaco.atp.puestoscamineros.domain.VersionSistema;
import ar.gob.chaco.atp.puestoscamineros.repository.ContribuyenteRepository;
import ar.gob.chaco.atp.puestoscamineros.repository.RangoManualFormularioRepositorio;
import ar.gob.chaco.atp.puestoscamineros.repository.VersionSistemaRepositorio;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaAlta;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaAltaProducto;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaProductoDisponible;
import ar.gob.chaco.atp.puestoscamineros.service.model.DatosParte;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derivaciones del módulo de Actas: los procedimientos del legacy que el
 * {@link ActaService} consume (spec {@code 09-formularios-acta} §3.12, plan §3.4).
 *
 * <p>Se separan del servicio orquestador para respetar el límite de 500 líneas por archivo
 * (G4): la <strong>API pública</strong> sigue estando en {@link ActaService}, que delega aquí.
 *
 * <p>Procedimientos replicados: {@code F2276_BusProTarifa}, {@code F2276_BusProTarCM},
 * {@code Nuc_CalculoImpIB}, {@code F2276_DevRazSocial}, {@code F2276_BusDom},
 * {@code F2276_BusLoc}, {@code BuscarVersionActual}, {@code ActaDeterminaTipoFormulario},
 * {@code PPrim_TurnoEstado} y las fórmulas del detalle ({@code GridToBC} + {@code Sub 'Formulas'}).
 */
@Component
@Transactional(readOnly = true)
public class ActaDerivaciones {

    /** Aplicación de {@code VERSIONE} de Puestos Camineros ({@code VerApliId = 'SWP'}). */
    public static final String APLICACION_VERSION = "SWP";

    private final ProductoRepository productoRepository;
    private final ProductoVigenciaRepository vigenciaRepository;
    private final ContribuyenteRepository contribuyenteRepository;
    private final CalleRepository calleRepository;
    private final LocalidadRepository localidadRepository;
    private final TurnoRepositorio turnoRepositorio;
    private final VersionSistemaRepositorio versionSistemaRepositorio;
    private final RangoManualFormularioRepositorio rangoManualRepositorio;

    public ActaDerivaciones(ProductoRepository productoRepository,
            ProductoVigenciaRepository vigenciaRepository,
            ContribuyenteRepository contribuyenteRepository, CalleRepository calleRepository,
            LocalidadRepository localidadRepository, TurnoRepositorio turnoRepositorio,
            VersionSistemaRepositorio versionSistemaRepositorio,
            RangoManualFormularioRepositorio rangoManualRepositorio) {
        this.productoRepository = productoRepository;
        this.vigenciaRepository = vigenciaRepository;
        this.contribuyenteRepository = contribuyenteRepository;
        this.calleRepository = calleRepository;
        this.localidadRepository = localidadRepository;
        this.turnoRepositorio = turnoRepositorio;
        this.versionSistemaRepositorio = versionSistemaRepositorio;
        this.rangoManualRepositorio = rangoManualRepositorio;
    }

    /**
     * Catálogo de productos elegibles en la grilla (R-ACTA-020, R-ACTA-022).
     *
     * <p>Sólo los productos <strong>no anulados</strong> ({@code ProdAnula = 0}).
     *
     * @return los productos ordenados por descripción
     */
    public List<ActaProductoDisponible> productosDisponibles() {
        LocalDate hoy = LocalDate.now();
        List<ActaProductoDisponible> resultado = new ArrayList<>();
        for (Producto producto : productoRepository.findByProdAnulaOrderByProdDescAsc(0)) {
            resultado.add(new ActaProductoDisponible(producto.getProdCod(),
                    TurnoMapper.trim(producto.getProdDesc()), producto.getProdTipCal(),
                    TurnoMapper.trim(producto.getProdAcop()),
                    precioProducto(producto, "N", hoy, false),
                    precioProducto(producto, "N", hoy, true)));
        }
        return resultado;
    }

    /**
     * Precio del producto (R-ACTA-022): {@code F2276_BusProTarifa} / {@code F2276_BusProTarCM}.
     *
     * <p>Toma la última vigencia de {@code PRODUCT1} con {@code ProdFchVig <= fecha}: con
     * {@code clasif = 'S'} usa {@code ProdImpu1}/{@code ProdImpu2} y en caso contrario
     * {@code ProdImpu3}/{@code ProdImpu4}. Sin vigencia ⇒ {@code 0}.
     *
     * @param prodCod código de producto
     * @param clasif tipo de movimiento ({@code 'S'} = salida)
     * @param fecha fecha del Acta
     * @param marCM {@code true} ⇒ precio de cosecha mecánica; {@code false} ⇒ precio fijo
     * @return el precio
     */
    public BigDecimal precioProducto(Integer prodCod, String clasif, LocalDate fecha,
            boolean marCM) {
        if (prodCod == null) {
            return BigDecimal.ZERO;
        }
        LocalDate dia = fecha == null ? LocalDate.now() : fecha;
        ProductoVigencia vigente = vigenciaRepository
                .findFirstById_ProdCodAndId_ProdFchVigLessThanEqualOrderById_ProdFchVigDesc(
                        prodCod, dia.atTime(LocalTime.MAX))
                .orElse(null);
        return precioDe(vigente, clasif, marCM);
    }

    /** Precio de un producto con sus vigencias ya cargadas (evita el N+1 del catálogo). */
    private BigDecimal precioProducto(Producto producto, String clasif, LocalDate fecha,
            boolean marCM) {
        return precioDe(vigenciaVigente(producto, fecha), clasif, marCM);
    }

    /** Vigencia aplicable a la fecha (la más reciente con {@code ProdFchVig <= fecha}). */
    private ProductoVigencia vigenciaVigente(Producto producto, LocalDate fecha) {
        LocalDateTime corte = fecha.atTime(LocalTime.MAX);
        ProductoVigencia vigente = null;
        for (ProductoVigencia vigencia : producto.getVigencias()) {
            LocalDateTime desde = vigencia.getProdFchVig();
            if (desde != null && !desde.isAfter(corte)
                    && (vigente == null || desde.isAfter(vigente.getProdFchVig()))) {
                vigente = vigencia;
            }
        }
        return vigente;
    }

    /** Aplica la selección de columna de la KB ({@code ProdImpu1..4}) según la clasificación. */
    private BigDecimal precioDe(ProductoVigencia vigencia, String clasif, boolean marCM) {
        if (vigencia == null) {
            return BigDecimal.ZERO;
        }
        boolean salida = "S".equals(TurnoMapper.trim(clasif));
        if (marCM) {
            return nz(salida ? vigencia.getProdImpu2() : vigencia.getProdImpu4());
        }
        return nz(salida ? vigencia.getProdImpu1() : vigencia.getProdImpu3());
    }

    /**
     * Impuesto a los ingresos brutos del detalle: {@code Nuc_CalculoImpIB} (R-ACTA-047).
     *
     * <p>{@code TipCal = 1} ("PorTonelada") y {@code TipCal = 2} ("PorUnidad") ⇒
     * {@code Calculo1 = Round(cant * fijo, 2)}; {@code TipCal = 3} ("PorMontoFijo") ⇒
     * {@code Calculo1 = fijo}; en ambos casos {@code Calculo2 = Round(mImpo * alicu / 100, 2)} y el
     * resultado es {@code MAX(Calculo1, Calculo2)}. {@code TipCal} fuera de {1, 2, 3} ⇒ {@code 0}.
     *
     * @param prodTipCal tipo de cálculo del producto
     * @param cant cantidad
     * @param mImpo importe de la línea
     * @param fijo precio fijo
     * @param alicu alícuota
     * @return el impuesto calculado
     */
    public BigDecimal impuestoIb(Integer prodTipCal, BigDecimal cant, BigDecimal mImpo,
            BigDecimal fijo, BigDecimal alicu) {
        if (prodTipCal == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal calculo1 = switch (prodTipCal) {
            case 1, 2 -> nz(cant).multiply(nz(fijo)).setScale(2, RoundingMode.HALF_UP);
            case 3 -> nz(fijo);
            default -> null;
        };
        if (calculo1 == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal calculo2 = nz(mImpo).multiply(nz(alicu))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return calculo1.max(calculo2);
    }

    /**
     * Datos de una parte derivados del CUIT (R-ACTA-017, R-ACTA-018).
     *
     * <p>Reproduce {@code F2276_DevRazSocial} (razón social), {@code F2276_BusDom} (domicilio =
     * {@code CallNom + ' ' + manzana}) y {@code F2276_BusLoc} ({@code LocCodR}). Si el
     * contribuyente no existe devuelve valores vacíos (paridad del {@code when none} del legacy).
     *
     * @param cuit CUIT de la parte
     * @return los datos derivados
     */
    public DatosParte derivarPorCuit(Long cuit) {
        if (cuit == null) {
            return DatosParte.vacio();
        }
        Contribuyente contribuyente = contribuyenteRepository.findByConCuit(BigDecimal.valueOf(cuit));
        if (contribuyente == null) {
            return DatosParte.vacio();
        }        Integer codigoLocalidad = contribuyente.getLocCodR();
        return new DatosParte(TurnoMapper.trim(contribuyente.getConRazSoc()),
                domicilio(contribuyente), codigoLocalidad, provincia(codigoLocalidad));
    }

    /** {@code F2276_BusDom}: calle + manzana del contribuyente. */
    private String domicilio(Contribuyente contribuyente) {
        String calle = "";
        if (contribuyente.getCallCodr() != null) {
            Calle entidad = calleRepository.findById(contribuyente.getCallCodr()).orElse(null);
            if (entidad != null && entidad.getCallNom() != null) {
                calle = entidad.getCallNom();
            }
        }
        String manzana = contribuyente.getConNMzanaR() == null ? ""
                : String.valueOf(contribuyente.getConNMzanaR());
        return (calle + " " + manzana).trim();
    }

    /** Nombre de la provincia de la localidad (no se persiste: no es columna, D-ACTA-17). */
    private String provincia(Integer codigoLocalidad) {
        if (codigoLocalidad == null) {
            return "";
        }
        Localidad localidad = localidadRepository.findByIdConProvincia(codigoLocalidad).orElse(null);
        if (localidad == null) {
            return "";
        }
        if (localidad.getLocNomProv() != null && !localidad.getLocNomProv().isBlank()) {
            return localidad.getLocNomProv();
        }
        Provincia provincia = localidad.getProvincia();
        return provincia == null || provincia.getProvNombre() == null ? ""
                : provincia.getProvNombre();
    }

    /**
     * Versión del sistema ({@code BuscarVersionActual}, R-ACTA-045).
     *
     * <p>Toma la fila de {@code VERSIONE} con {@code VerApliId = 'SWP'} y devuelve
     * {@code VerVersion + "." + VerRevisio + "." + VerBuild}. Sin fila ⇒ cadena vacía (paridad:
     * {@code &Version} queda sin asignar).
     *
     * @return la versión del sistema
     */
    public String versionSistema() {
        List<VersionSistema> versiones = versionSistemaRepositorio.buscarPorAplicacion(
                APLICACION_VERSION);
        if (versiones.isEmpty()) {
            return "";
        }
        VersionSistema version = versiones.get(0);
        String build = version.getVerBuild() == null ? "0"
                : version.getVerBuild().stripTrailingZeros().toPlainString();
        return version.getVerVersion() + "." + version.getVerRevisio() + "." + build;
    }

    /**
     * Tipo de formulario ({@code ActaDeterminaTipoFormulario}, R-ACTA-046).
     *
     * <p>{@code 1} si {@code F276Clasif = 'E'} y el contribuyente no está en infracción,
     * {@code 2} si {@code 'E'} y está en infracción, {@code 3} si {@code 'S'}. Está en infracción
     * cuando el contribuyente existe y su {@code ConEstado} no es {@code 12} ni {@code 14}.
     *
     * @param cuit CUIT de la parte
     * @param clasif tipo de movimiento
     * @return el tipo de formulario, o {@code null} si la clasificación no es {@code 'E'} ni
     *         {@code 'S'}
     */
    public Integer tipoFormulario(Long cuit, String clasif) {
        String c = TurnoMapper.trim(clasif);
        if ("E".equals(c)) {
            return estaEnInfraccion(cuit) ? 2 : 1;
        }
        if ("S".equals(c)) {
            return 3;
        }
        return null;
    }

    /** ¿El contribuyente está en infracción? (paridad de {@code VerContribuEstado}). */
    private boolean estaEnInfraccion(Long cuit) {
        if (cuit == null) {
            return false;
        }
        Contribuyente contribuyente = contribuyenteRepository.findByConCuit(BigDecimal.valueOf(cuit));
        if (contribuyente == null) {
            return false;
        }
        Short estado = contribuyente.getConEstado();
        return estado == null || (estado != 12 && estado != 14);
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
        if (sucNroId == null || turNroId == null || sucTipo == null) {
            return null;
        }
        Turno turno = turnoRepositorio
                .findById(new TurnoId(sucNroId, TurnoMapper.trim(sucTipo), turNroId)).orElse(null);
        return turno == null ? null : turno.getTurEstado();
    }

    /**
     * Detalle calculado de un Acta: un renglón por producto con las fórmulas del legacy
     * (R-ACTA-047).
     *
     * <p>{@code F276RFijo} = {@code F2276_BusProTarifa}; {@code F276RPreCM} = {@code 0} (la marca
     * de cosecha mecánica es siempre {@code 'N'}: {@code F2276_BusProTarCM} devuelve {@code 0});
     * {@code F276RImpIB} = {@code Nuc_CalculoImpIB}; {@code F276RImpCM = F276RCant * F276RPreCM};
     * {@code F276RSPre} informada o el precio fijo registrado cuando viene en cero (R-ACTA-037).
     *
     * @param alta comando de alta
     * @return los renglones del detalle (sin la cabecera)
     */
    public List<ActaProducto> calcularDetalle(ActaAlta alta) {
        LocalDate fecha = alta.f276FchMov() == null ? LocalDate.now() : alta.f276FchMov();
        List<ActaProducto> detalle = new ArrayList<>();
        for (ActaAltaProducto linea : alta.productos()) {
            Integer prodCod = linea.prodCod();
            BigDecimal cant = nz(linea.f276RCant());
            BigDecimal informado = linea.f276RSPre() == null ? BigDecimal.ZERO : linea.f276RSPre();
            BigDecimal fijo = precioProducto(prodCod, alta.f276Clasif(), fecha, false);
            BigDecimal precio = informado.signum() == 0 ? fijo : informado;
            Integer prodTipCal = productoRepository.findById(prodCod)
                    .map(Producto::getProdTipCal).orElse(null);
            BigDecimal monto = precio.multiply(cant);

            ActaProducto fila = new ActaProducto();
            fila.setProdCod(prodCod);
            fila.setF276RZona(ActaService.ZONA_POR_DEFECTO);
            fila.setF276RCant(cant);
            fila.setF276RMarCM(ActaService.MARCA_COSECHA_MECANICA);
            fila.setF276RMImpo(monto);
            fila.setF276RFijo(fijo);
            fila.setF276RPreCM(BigDecimal.ZERO);
            fila.setF276RAlicu(ActaService.ALICUOTA_POR_DEFECTO);
            fila.setF276RImpIB(impuestoIb(prodTipCal, cant, monto, fijo,
                    ActaService.ALICUOTA_POR_DEFECTO));
            fila.setF276RImpCM(cant.multiply(BigDecimal.ZERO));
            fila.setF276RSPre(precio);
            detalle.add(fila);
        }
        return detalle;
    }

    private static BigDecimal nz(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    // ------------------------------------------------------- números manuales

    /**
     * ¿El número cae dentro de un rango manual de {@code FORMSUCU}? (R-ACTA-008, R-ACTA-036).
     *
     * <p>Reproduce {@code ProPri_VerManual} / {@code PPrim_VerificaGenMan}: el rango es
     * {@code [ForRecIni, ForRecIni + ForRecCant)}.
     *
     * @param f276Id número a verificar
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @return {@code true} si es un número manual
     */
    public boolean esNumeroManual(Integer f276Id, Integer sucNroId, String sucTipo) {
        if (f276Id == null) {
            return false;
        }
        return rangoManualRepositorio.contarSiEsManual(sucNroId, TurnoMapper.trim(sucTipo),
                ServicioNumeracionActa.NUMERO_FORMULARIO_ACTAS, f276Id) > 0;
    }

    /** ¿El número cae en alguno de los rangos ya cargados? */
    public boolean esNumeroManualEnRangos(List<RangoManualFormulario> rangos, Integer f276Id) {
        if (rangos == null || rangos.isEmpty() || f276Id == null) {
            return false;
        }
        for (RangoManualFormulario rango : rangos) {
            if (rango.getForRecIni() != null && rango.getForRecCant() != null
                    && rango.getForRecIni() <= f276Id
                    && rango.getForRecIni() + rango.getForRecCant() > f276Id) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validación del número de Acta informado (R-ACTA-036): si se informa un {@code F276Id} que
     * <strong>no</strong> cae en un rango manual de {@code FORMSUCU}, el alta se rechaza.
     *
     * @param f276Id número informado ({@code 0} o {@code null} ⇒ no se informa)
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @return el mensaje verbatim, o {@code null} si el número es válido
     */
    public String mensajeNumeroNoGenerado(Integer f276Id, Integer sucNroId, String sucTipo) {
        if (f276Id == null || f276Id == 0) {
            return null;
        }
        if (esNumeroManual(f276Id, sucNroId, sucTipo)) {
            return null;
        }
        return MensajesActa.MSG_NRO_FORMULARIO_GENERADO;
    }
}
