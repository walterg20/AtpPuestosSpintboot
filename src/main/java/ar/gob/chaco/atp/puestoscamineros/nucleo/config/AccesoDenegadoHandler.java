package ar.gob.chaco.atp.puestoscamineros.nucleo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Manejador de acceso denegado: responde siempre <strong>403</strong> con el cuerpo uniforme
 * {@code {code, message}} (R-LOG-039, R-LOG-041).
 *
 * <p>Nunca devuelve 200 ni una pagina HTML de error: el 403 queda reservado a las rutas MVC sin
 * token CSRF (y a las rutas API a las que, en el futuro, se les exija autenticacion).
 */
public class AccesoDenegadoHandler implements AccessDeniedHandler {

    /** Codigo del cuerpo uniforme. */
    private static final String CODIGO = "ACCESO_DENEGADO";

    /** Mensaje del cuerpo uniforme. */
    private static final String MENSAJE = "No tiene permisos para realizar esta operacion.";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"" + CODIGO + "\",\"message\":\"" + MENSAJE + "\"}");
    }
}