package ar.com.atp.puestos.nucleo.service;

import ar.com.atp.puestos.nucleo.domain.Turno;
import ar.com.atp.puestos.nucleo.domain.TurnoId;
import ar.com.atp.puestos.nucleo.repository.ComisionRepository;
import ar.com.atp.puestos.nucleo.repository.TurnoRepositorio;
import ar.com.atp.puestos.nucleo.web.dto.TurnoDto;
import ar.com.atp.puestos.shared.time.FechaVacia;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public ServicioTurnos(TurnoRepositorio turnoRepositorio, ComisionRepository comisionRepository,
            ServicioEstadoDeForm servicioEstadoDeForm, ServicioCierreTurno servicioCierreTurno) {
        this.turnoRepositorio = turnoRepositorio;
        this.comisionRepository = comisionRepository;
        this.servicioEstadoDeForm = servicioEstadoDeForm;
        this.servicioCierreTurno = servicioCierreTurno;
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
        Integer sucursal = contexto.sucursal();
        String tipo = TurnoMapper.trim(contexto.tipo());
        return turnoRepositorio.listarPorSucursal(sucursal, tipo, pageable).map(TurnoMapper::toDto);
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
}
