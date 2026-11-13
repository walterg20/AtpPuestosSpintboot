package ar.gob.chaco.atp.puestoscamineros.service.model;

/**
 * Resultado del alta de un Acta (spec {@code 09-formularios-acta} §3.3, plan §3.3, D-ACTA-22).
 *
 * <p>Es un {@code record} inmutable: {@code ok}, el mensaje <strong>verbatim</strong> del legacy y
 * el número de Acta generado.
 *
 * @param ok {@code true} si el Acta se persistió
 * @param mensaje mensaje verbatim del legacy (de error o de éxito)
 * @param f276Id número de Acta generado, o {@code null} si el alta falló
 */
public record ResultadoAltaActa(boolean ok, String mensaje, Integer f276Id) {
}
