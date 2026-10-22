package ar.com.atp.puestos.shared.error;

/**
 * Excepción para conflictos (p. ej. PK duplicada) → HTTP 409 con cuerpo {@code { code, message }}
 * (R-031).
 */
public class ConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
