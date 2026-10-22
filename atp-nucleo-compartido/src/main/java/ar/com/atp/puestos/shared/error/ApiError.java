package ar.com.atp.puestos.shared.error;

/**
 * Cuerpo de error uniforme de la API: {@code { "code", "message" }} (R-023, R-031, R-032).
 */
public record ApiError(String code, String message) {
}
