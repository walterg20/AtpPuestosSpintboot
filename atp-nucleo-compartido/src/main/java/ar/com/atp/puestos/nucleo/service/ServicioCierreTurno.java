package ar.com.atp.puestos.nucleo.service;

import ar.com.atp.puestos.nucleo.domain.Turno;
import ar.com.atp.puestos.nucleo.domain.TurnoId;
import ar.com.atp.puestos.nucleo.repository.TurnoRepositorio;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierre de un turno (spec {@code 06-turnos} R-TUR-012, R-TUR-014, T-TUR-B4).
 *
 * <p>Reproduce el evento {@code 'CerrarTurno'} del legacy ({@code TurnosListado}) y el
 * procedimiento {@code PPrim_CerrarTurno}, <strong>en este orden exacto</strong>:
 *
 * <ol>
 *   <li>si {@code TurEstado <> 'A'} ⇒ {@code El turno se encuentra cerrado};</li>
 *   <li>si el turno tiene formularios abiertos ⇒ {@code Existen Formularios Abiertos};</li>
 *   <li>si el procedimiento de cierre falla ⇒ {@code No se puede cerrar el turno};</li>
 *   <li>si tiene éxito ⇒ {@code TurFchCie = serverdate()}, {@code TurHraCie = servernow()},
 *       {@code TurEstado = 'C'} y {@code El turno seleccionado ha sido cerrado}.</li>
 * </ol>
 *
 * <p><strong>Transacción</strong>: todo el cierre (las comprobaciones, la escritura de la fila y,
 * para receptoría, la generación del SN del volante de pago) ocurre en <strong>una</strong>
 * transacción. Si la generación del SN falla, la excepción revierte la escritura y el turno
 * <strong>queda abierto</strong> (R-TUR-014).
 *
 * <p>El mensaje de éxito no se devuelve como código de error: el cierre es una operación de
 * negocio que informa, no un fallo de la API.
 */
@Service
public class ServicioCierreTurno {

    private final TurnoRepositorio turnoRepositorio;
    private final ServicioEstadoDeForm servicioEstadoDeForm;
    private final ServicioGeneradorSn servicioGeneradorSn;

    public ServicioCierreTurno(TurnoRepositorio turnoRepositorio,
            ServicioEstadoDeForm servicioEstadoDeForm, ServicioGeneradorSn servicioGeneradorSn) {
        this.turnoRepositorio = turnoRepositorio;
        this.servicioEstadoDeForm = servicioEstadoDeForm;
        this.servicioGeneradorSn = servicioGeneradorSn;
    }

    /**
     * Cierra el turno aplicando las comprobaciones del legacy en orden.
     *
     * @param turnoId clave primaria del turno a cerrar
     * @return el resultado con el mensaje verbatim correspondiente
     */
    @Transactional
    public ResultadoTurno cerrar(TurnoId turnoId) {
        Turno turno = turnoRepositorio.findById(turnoId).orElse(null);
        if (turno == null) {
            // El legacy, en el "when none" de su For Each, deja &Error = true.
            return ResultadoTurno.error(ResultadoTurno.ERROR_CIERRE, MensajesTurno.ERROR_CIERRE);
        }

        // (1) El turno ya está cerrado.
        if (!"A".equals(TurnoMapper.trim(turno.getTurEstado()))) {
            return ResultadoTurno.error(ResultadoTurno.TURNO_YA_CERRADO,
                    MensajesTurno.TURNO_YA_CERRADO);
        }

        // (2) El turno tiene formularios abiertos.
        if (servicioEstadoDeForm.hayFormularioAbierto(turno.getSucNroId(), turno.getSucTipo(),
                turno.getTurNroId())) {
            return ResultadoTurno.error(ResultadoTurno.FORMULARIOS_ABIERTOS,
                    MensajesTurno.FORMULARIOS_ABIERTOS);
        }

        // (3) y (4): escritura del cierre.
        LocalDate hoy = LocalDate.now();
        turno.setTurFchCie(hoy.atStartOfDay());
        turno.setTurHraCie(LocalDateTime.now());
        turno.setTurEstado("C");
        turnoRepositorio.save(turno);

        // Sólo la receptoría genera el volante de pago al cerrar el turno.
        if ("REC".equals(TurnoMapper.trim(turno.getSucTipo()))) {
            // Si la generación falla, la excepción revierte toda la operación de cierre.
            servicioGeneradorSn.generar(turno.getSucNroId(), turno.getSucTipo(), turno.getTurNroId());
        }

        return ResultadoTurno.cerrado();
    }
}
