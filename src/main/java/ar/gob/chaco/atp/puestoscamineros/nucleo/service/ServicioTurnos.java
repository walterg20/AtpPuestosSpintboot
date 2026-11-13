package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Turno;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.TurnoId;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.ComisionRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.TurnoRepositorio;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.Formulario2276Repositorio;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Formulario2276;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.TotalesFormulario2276;import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.TurnoDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.turno.ActaView;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.turno.BitacoraEvento;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.turno.TurnoDetalleView;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.turno.TurnoFilaView;
import ar.gob.chaco.atp.puestoscamineros.shared.time.FechaVacia;
import ar.gob.chaco.atp.puestoscamineros.shared.error.NotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de turnos del módulo {@code 06-turnos} (spec §3.1, §3.2 y §3.3; T-TUR-B1, T-TUR-B2).
 *
 * <p>Es la fachada del módulo: el listado, el alta y el cierre. Las reglas viven <strong>aquí</strong>
 * (o en los servicios especializados {@link ServicioEstadoDeForm}, {@link ServicioCierreTurno} y
 * {@link ServicioGeneradorSn}); los adaptadores web (MVC y REST) no llevan reglas propias.
 *
 * <p><strong>Contexto de sesión</strong>: el servicio recibe el {@link ContextoSesion} y de ahí
 * toma la sucursal ({@code Context.Sucursal} → {@code SucNroId}), el tipo ({@code Context.Tipo} →
 * {@code SucTipo}), la comisión ({@code Context.Comision} → {@code TurCPCId}) y el usuario
 * ({@code Context.Usuario} → {@code TurUsuario}).
 *
 * <p><strong>Actualización del contexto</strong>: el alta y el cierre cambian
 * {@code Context.Turno} (R-TUR-009 y R-TUR-012). El servicio <strong>no</strong> escribe en la
 * sesión: devuelve el número de turno nuevo (o informa el cierre) y es el <strong>adaptador</strong>
 * —el controller— quien actualiza el contexto y lo persiste, porque el servicio de dominio no
 * depende de la capa de presentación.
 */
@Service
public class ServicioTurnos {

    private static final Logger log = LoggerFactory.getLogger(ServicioTurnos.class);
    /** Filas por página de la grilla (paridad con el {@code MaxRows="10"} del legacy, R-TUR-003). */
    public static final int TAMANIO_PAGINA = 10;

    /** Tipo de sucursal de un puesto caminerero. */
    public static final String TIPO_PUE = "PUE";

    /** Tipo de sucursal de una receptoría. */
    public static final String TIPO_REC = "REC";

    /** Estado de turno abierto. */
    public static final String ESTADO_ABIERTO = "A";

    /** Estado de turno cerrado. */
    public static final String ESTADO_CERRADO = "C";

    /** Estado de comisión cerrada: impide abrir turnos nuevos (R-TUR-010). */
    public static final String ESTADO_COMISION_CERRADA = "C";

    private final TurnoRepositorio turnoRepositorio;
    private final ComisionRepository comisionRepository;
    private final ServicioEstadoDeForm servicioEstadoDeForm;
    private final ServicioCierreTurno servicioCierreTurno;
    private final Formulario2276Repositorio formularioRepositorio;

    public ServicioTurnos(TurnoRepositorio turnoRepositorio, ComisionRepository comisionRepository,
            ServicioEstadoDeForm servicioEstadoDeForm, ServicioCierreTurno servicioCierreTurno) {
        this(turnoRepositorio, comisionRepository, servicioEstadoDeForm, servicioCierreTurno, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ServicioTurnos(TurnoRepositorio turnoRepositorio, ComisionRepository comisionRepository,
            ServicioEstadoDeForm servicioEstadoDeForm, ServicioCierreTurno servicioCierreTurno,
            Formulario2276Repositorio formularioRepositorio) {
        this.turnoRepositorio = turnoRepositorio;
        this.comisionRepository = comisionRepository;
        this.servicioEstadoDeForm = servicioEstadoDeForm;
        this.servicioCierreTurno = servicioCierreTurno;
        this.formularioRepositorio = formularioRepositorio;
    }

    // ------------------------------------------------------------------ listado (R-TUR-001..003)

    /**
     * Listado de los turnos de la sucursal del usuario, primera página (10 filas).
     *
     * @param contexto contexto de sesión
     * @return la página de turnos
     */
    @Transactional(readOnly = true)
    public Page<TurnoDto> listar(ContextoSesion contexto) {
        return listar(contexto, PageRequest.of(0, TAMANIO_PAGINA));
    }

    /**
     * Listado de los turnos de la sucursal del usuario.
     *
     * <p>La sucursal y el tipo salen del contexto ({@code Context.Sucursal} y {@code Context.Tipo});
     * el orden es el de la clave primaria y la paginación la del adaptador (R-TUR-001, R-TUR-003).
     *
     * @param contexto contexto de sesión
     * @param pageable paginación pedida por la vista
     * @return la página de turnos
     */
    @Transactional(readOnly = true)
    public Page<TurnoDto> listar(ContextoSesion contexto, Pageable pageable) {
        return listar(contexto, FiltrosTurno.vacio(), pageable);
    }

    /**
     * Listado de los turnos de la sucursal del usuario con filtros opcionales (Fase 1).
     *
     * @param contexto contexto de sesion
     * @param filtros  filtros opcionales (puede ser {@code null})
     * @param pageable paginacion pedida por la vista
     * @return la pagina de turnos
     */
    @Transactional(readOnly = true)
    public Page<TurnoDto> listar(ContextoSesion contexto, FiltrosTurno filtros, Pageable pageable) {
        Integer sucursal = contexto.sucursal();
        String tipo = TurnoMapper.trim(contexto.tipo());
        FiltrosTurno f = (filtros == null) ? FiltrosTurno.vacio() : filtros;
        String estado = TurnoMapper.trim(f.estado());
        return turnoRepositorio.listarPorSucursalConFiltros(
                sucursal, tipo, f.busqueda(), f.desde(), f.hasta(), estado, pageable)
                .map(TurnoMapper::toDto);
    }

    // ------------------------------------------------------------------ alta (R-TUR-006..011)

    /**
     * Abre un turno nuevo en la sucursal del usuario.
     *
     * <p>Reproduce el evento {@code 'NuevoTurno'} del legacy y las reglas del WorkWith
     * {@code PPrim_TurnosNue}:
     *
     * <ol>
     *   <li>si el tipo es {@code PUE}, se consulta la comisión de
     *       {@code Context.Sucursal}/{@code Context.Comision} y, si su {@code CPCEstado} es
     *       {@code 'C'}, el alta se rechaza (R-TUR-010). Para {@code REC} <strong>no</strong> se
     *       consulta la comisión (R-TUR-011);</li>
     *   <li>el número de turno es el <strong>máximo</strong> {@code TurNroId} de la sucursal/tipo
     *       más uno, o 1 si no hay turnos (R-TUR-006);</li>
     *   <li>la fila se escribe con {@code TurUsuario} = usuario de la sesión, {@code TurFchApe} =
     *       {@code serverdate()}, {@code TurHraApe} = {@code servernow()}, {@code TurEstado} =
     *       {@code 'A'} y {@code TurCPCId} = {@code Context.Comision} (0 si está vacía)
     *       (R-TUR-007);</li>
     *   <li>{@code TurFchCie} y {@code TurHraCie} se escriben con la sentinela
     *       {@code 1753-01-01T00:00:00} y {@code TurFchPago} en {@code NULL} (R-TUR-008).</li>
     * </ol>
     *
     * <p>El adaptador es quien actualiza {@code Context.Turno} con el número devuelto y persiste
     * el contexto (R-TUR-009).
     *
     * @param contexto contexto de sesión
     * @return el resultado: con {@code numeroTurno} si el alta se realizó, o el mensaje verbatim
     *         del rechazo
     */
    @Transactional
    public ResultadoTurno nuevoTurno(ContextoSesion contexto) {
        Integer sucursal = contexto.sucursal();
        String tipo = TurnoMapper.trim(contexto.tipo());

        if (TIPO_PUE.equals(tipo)) {
            Optional<String> estado = comisionRepository.estadoComision(sucursal, contexto.comision());
            // Si la comisión no existe o su estado es NULL, el alta se permite (D-TUR-002).
            if (estado.isPresent() && ESTADO_COMISION_CERRADA.equals(TurnoMapper.trim(estado.get()))) {
                return ResultadoTurno.error(ResultadoTurno.COMISION_CERRADA,
                        MensajesTurno.COMISION_CERRADA);
            }
        }

        Integer maximo = turnoRepositorio.maximoTurNroId(sucursal, tipo);
        int numero = (maximo == null ? 0 : maximo) + 1;

        LocalDate hoy = LocalDate.now();
        TurnoDto dto = new TurnoDto(
                sucursal,
                tipo,
                numero,
                TurnoMapper.trim(contexto.usuario()),
                contexto.comision() == null ? 0 : contexto.comision(),
                hoy.atStartOfDay(),
                FechaVacia.FECHA_VACIA,
                LocalDateTime.now(),
                FechaVacia.FECHA_VACIA,
                ESTADO_ABIERTO,
                null);
        turnoRepositorio.save(TurnoMapper.toEntidad(dto));

        return ResultadoTurno.creado(numero);
    }

    // ------------------------------------------------------------------ cierre (R-TUR-012..014)

    /**
     * Cierra el turno seleccionado de la sucursal del usuario.
     *
     * <p>El turno se identifica por su número: la sucursal y el tipo salen del contexto, igual que
     * en el listado. El orden exacto de las comprobaciones vive en
     * {@link ServicioCierreTurno#cerrar(TurnoId)}; este método sólo traduce el fallo de la
     * generación del SN (si lo hay) al mensaje del legacy.
     *
     * <p>El adaptador es quien pone {@code Context.Turno = 0} y persiste el contexto (R-TUR-012).
     *
     * @param contexto contexto de sesión
     * @param numeroTurno número de turno a cerrar
     * @return el resultado con el mensaje verbatim correspondiente
     */
    public ResultadoTurno cerrarTurno(ContextoSesion contexto, Integer numeroTurno) {
        TurnoId turno = new TurnoId(contexto.sucursal(), TurnoMapper.trim(contexto.tipo()),
                numeroTurno);
        return cerrarTurno(turno);
    }

    /**
     * Cierra el turno identificado por su clave primaria compuesta.
     *
     * @param turno clave primaria del turno a cerrar
     * @return el resultado con el mensaje verbatim correspondiente
     */
    public ResultadoTurno cerrarTurno(TurnoId turno) {
        try {
            return servicioCierreTurno.cerrar(turno);
        } catch (RuntimeException error) {
            // El SN del volante de pago no se pudo generar: el cierre se revierte y el turno
            // queda abierto (R-TUR-014).
            log.error("No se puede cerrar el turno {}: {}", turno, error.getMessage(), error);
            return ResultadoTurno.error(ResultadoTurno.ERROR_CIERRE, MensajesTurno.ERROR_CIERRE);
        }
    }

    /**
     * ¿El turno tiene formularios abiertos? (expuesto para la vista, que lo usa para deshabilitar
     * la acción de cierre).
     *
     * @param turno clave primaria del turno
     * @return {@code true} si el turno tiene formularios abiertos
     */
    @Transactional(readOnly = true)
    public boolean hayFormularioAbierto(TurnoId turno) {
        if (turno == null) {
            return false;
        }
        return servicioEstadoDeForm.hayFormularioAbierto(turno.getSucNroId(), turno.getSucTipo(),
                turno.getTurNroId());
    }

    // ------------------------------------------------------------------ detalle (Fase 2)

    /**
     * Detalle de un turno: fila del turno, actas, total, percepción estimada y bitácora.
     *
     * @param contexto contexto de sesión
     * @param turNroId número de turno
     * @return el detalle del turno
     * @throws NotFoundException si el turno no existe
     */
    @Transactional(readOnly = true)
    public TurnoDetalleView detalleTurno(ContextoSesion contexto, Integer turNroId) {
        Integer sucursal = contexto.sucursal();
        String tipo = TurnoMapper.trim(contexto.tipo());
        Turno turno = turnoRepositorio.findById(new TurnoId(sucursal, tipo, turNroId))
                .orElseThrow(() -> new NotFoundException("NO_ENCONTRADO",
                        "El turno no existe"));
        TurnoFilaView turnoView = TurnoFilaView.de(TurnoMapper.toDto(turno));

        List<Formulario2276> formularios = (formularioRepositorio == null)
                ? List.of()
                : formularioRepositorio.findBySucNroIdAndSucTipoAndTurNroIdOrderByF276IdAsc(
                        sucursal, tipo, turNroId);

        List<ActaView> actas = new ArrayList<>();
        List<BitacoraEvento> bitacora = new ArrayList<>();
        for (Formulario2276 f : formularios) {
            BigDecimal importe = nz(f.getF276ImpuFl()).add(nz(f.getF276AdicFl()))
                    .add(nz(f.getF276AdIBTo())).add(nz(f.getF276AdCMTo()));
            String estado = f.getF276Estad() == null ? "" : f.getF276Estad().trim();
            actas.add(new ActaView(f.getF276Id(), f.getF276FchMov(), importe, estado));
            bitacora.add(new BitacoraEvento(f.getF276FchMov(),
                    "Acta N° " + f.getF276Id() + " emitida"));
        }

        int totalActas = actas.size();

        BigDecimal percepcionEstimada;
        if (formularioRepositorio == null) {
            percepcionEstimada = sumarImportes(actas);
        } else {
            List<Object[]> filas = formularioRepositorio.totalizarTurno(sucursal, tipo, turNroId);
            if (filas == null || filas.isEmpty()) {
                percepcionEstimada = sumarImportes(actas);
            } else {
                TotalesFormulario2276 totales = TotalesFormulario2276.deFila(filas.get(0));
                percepcionEstimada = totales.totFletes().add(totales.totalAd());
            }
        }

        return new TurnoDetalleView(turnoView, actas, totalActas, percepcionEstimada, bitacora);
    }

    // ------------------------------------------------------------------ listado actas (Fase 3)

    /**
     * Listado paginado de las actas (formularios 2276) de un turno.
     *
     * <p>Incluye <strong>todas</strong> las actas del turno (incluso las anuladas, cuyo estado se
     * muestra en la columna {@code estado}); el importe de cada acta es la suma de
     * {@code F276ImpuFl + F276AdicFl + F276AdIBTo + F276AdCMTo} (nulos → 0). El orden es por
     * {@code F276Id} ascendente (paridad con el {@code For Each} del legacy).
     *
     * @param contexto contexto de sesión
     * @param turNroId número de turno
     * @param pageable paginación pedida por la vista
     * @return la página de actas
     */
    @Transactional(readOnly = true)
    public Page<ActaView> listarActas(ContextoSesion contexto, Integer turNroId, Pageable pageable) {
        Integer sucursal = contexto.sucursal();
        String tipo = TurnoMapper.trim(contexto.tipo());
        Pageable p = (pageable == null) ? PageRequest.of(0, TAMANIO_PAGINA) : pageable;
        if (formularioRepositorio == null) {
            return Page.empty(p);
        }
        return formularioRepositorio
                .findBySucNroIdAndSucTipoAndTurNroIdOrderByF276IdAsc(sucursal, tipo, turNroId, p)
                .map(f -> {
                    BigDecimal importe = nz(f.getF276ImpuFl()).add(nz(f.getF276AdicFl()))
                            .add(nz(f.getF276AdIBTo())).add(nz(f.getF276AdCMTo()));
                    String estado = f.getF276Estad() == null ? "" : f.getF276Estad().trim();
                    return new ActaView(f.getF276Id(), f.getF276FchMov(), importe, estado);
                });
    }

    private static BigDecimal sumarImportes(List<ActaView> actas) {
        BigDecimal total = BigDecimal.ZERO;
        for (ActaView a : actas) {
            if (a.importe() != null) {
                total = total.add(a.importe());
            }
        }
        return total;
    }

    private static BigDecimal nz(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
