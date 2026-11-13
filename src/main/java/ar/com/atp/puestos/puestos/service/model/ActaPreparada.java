package ar.gob.chaco.atp.puestoscamineros.service.model;

import java.time.LocalDate;

/**
 * Estado del formulario de alta antes de renderizarlo (spec {@code 09-formularios-acta} §3.3,
 * plan §3.3, D-ACTA-22).
 *
 * @param bloqueado {@code true} ⇒ hay un lote o un turno que bloquea el alta (R-ACTA-041,
 *        R-ACTA-042)
 * @param mensajeBloqueo motivo <strong>verbatim</strong> del bloqueo, o {@code null} si no hay
 *        bloqueo
 * @param turnoEstado estado del turno de la sesión ({@code 'A'} abierto, {@code 'C'} cerrado) o
 *        {@code null} si el turno no existe
 * @param f276IdSugerido próximo número de Acta (informativo; el real lo asigna el servicio al
 *        guardar) o {@code null}
 * @param fechaMovimiento fecha por defecto de la alta
 */
public record ActaPreparada(boolean bloqueado, String mensajeBloqueo, String turnoEstado,
        Integer f276IdSugerido, LocalDate fechaMovimiento) {
}
