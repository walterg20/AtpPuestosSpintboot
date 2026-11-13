package ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto;

/**
 * Cuerpo de la respuesta del canal WS ({@code POST /api/v1/auth/login}) — forma del SDT
 * {@code SDTSalidaLoginWS} del legacy (spec 03-login §4.1).
 *
 * <p>El WS legacy <strong>nunca</strong> devuelve un estado de error: siempre {@code 200} con este
 * cuerpo, incluso para un acceso denegado (R-LOG-027). Las tres claves son numeros y nunca
 * {@code null} ⇒ siempre presentes.
 *
 * @param error codigo de resultado 0-6 (0 = acceso concedido)
 * @param sucursal sucursal resuelta; {@code 0} si no se resolvio
 * @param comision comision resuelta; {@code 0} si no se resolvio
 */
public record AuthLoginResponseDto(int error, int sucursal, int comision) {
}
