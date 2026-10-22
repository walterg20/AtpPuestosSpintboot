package ar.com.atp.puestos.shared.error;

/**
 * Excepción para recursos inexistentes → HTTP 404 con cuerpo {@code { code, message }} (R-023).
 */
public class NotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public NotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
