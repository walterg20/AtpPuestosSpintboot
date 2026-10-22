package ar.com.atp.puestos.nucleo.web.rest;

import ar.com.atp.puestos.shared.error.GlobalExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

/**
 * Controlador sólo de test (T-409/R-206 y T-411/R-208): fuerza excepciones para ejercitar los
 * handlers de {@link GlobalExceptionHandler}.
 *
 * <p>Es <strong>top-level</strong> a propósito: una clase anidada estática no la registra el
 * escaneo de componentes del slice {@code @WebMvcTest}, y el controller terminaba sin mapear (el
 * request caía en el handler de recursos estáticos y la ruta "/api/v1/test/explotar" devolvía
 * {@code NoResourceFoundException} en lugar de la excepción prevista).
 */
@RestController
public class ExplodingController {

    /** Fuerza una excepción no controlada → fallback {@code 500 ERROR_INTERNO} (R-206). */
    @GetMapping("/api/v1/test/explotar")
    public String explota() {
        throw new RuntimeException("fallo-interno-xyz");
    }

    /** Fuerza el aborto del cliente → handler específico de R-208 (sin 500, log en INFO). */
    @GetMapping("/api/v1/test/abortar")
    public String aborta() throws AsyncRequestNotUsableException {
        throw new AsyncRequestNotUsableException("ServletOutputStream failed to write");
    }
}
