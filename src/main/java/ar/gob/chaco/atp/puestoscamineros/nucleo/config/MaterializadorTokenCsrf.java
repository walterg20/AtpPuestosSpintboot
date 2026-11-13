package ar.gob.chaco.atp.puestoscamineros.nucleo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;

/**
 * Materializa el token CSRF <strong>antes</strong> del render (D-LOG-18(a), R-LOG-042).
 *
 * <p>Con {@code HttpSessionCsrfTokenRepository} el token se guarda en la sesion la <em>primera vez
 * que se accede a el</em>: ese acceso ocurre en el {@code th:action} del formulario, cuando la
 * respuesta ya esta confirmada, y produce un
 * {@code IllegalStateException: Cannot create a session after the response has been committed}
 * (hallazgo H4 del QA del modulo 02): el cliente recibe la pagina de error en lugar de la pantalla.
 *
 * <p>Este filtro fuerza {@code csrfToken.getToken()} <strong>al principio</strong> de la peticion
 * (corre justo despues del {@code CsrfFilter} y antes del controlador): la sesion se crea a
 * tiempo y el token queda disponible para el formulario, sin cambiar el contrato del formulario
 * (el {@code th:action} sigue emitiendo {@code _csrf}), sin cambiar el perfil de cookies y sin
 * tocar el HTML de las vistas.
 *
 * <p>Las peticiones del canal {@code /api/**} quedan fuera: son <em>stateless</em> y no deben
 * crear sesion (R-LOG-041 (c)).
 */
public class MaterializadorTokenCsrf implements jakarta.servlet.Filter {

    /** Prefijo del canal REST (stateless, sin sesion). */
    private static final String PREFIJO_API = "/api/";

    /** Prefijos de los recursos estaticos: no sirven paginas HTML y no necesitan sesion. */
    private static final String[] RECURSOS_ESTATICOS = {
            "/css/", "/js/", "/img/", "/webjars/", "/favicon.ico"
    };

    @Override
    public void doFilter(ServletRequest peticion, ServletResponse respuesta, FilterChain cadena)
            throws IOException, ServletException {
        if (peticion instanceof HttpServletRequest request && !esCanalApi(request)
                && !esRecursoEstatico(request)) {
            Object atributo = request.getAttribute(CsrfToken.class.getName());
            if (atributo instanceof CsrfToken token) {
                // Fuerza la carga y el guardado del token: la sesion se crea ahora.
                token.getToken();
            }
        }
        cadena.doFilter(peticion, respuesta);
    }

    /** La peticion pertenece al canal REST? (ruta dentro de la aplicacion, sin context path). */
    private static boolean esCanalApi(HttpServletRequest request) {
        return rutaDentroDeLaAplicacion(request).startsWith(PREFIJO_API);
    }

    /** La peticion pide un recurso estatico (no una pagina HTML)? */
    private static boolean esRecursoEstatico(HttpServletRequest request) {
        String uri = rutaDentroDeLaAplicacion(request);
        for (String prefijo : RECURSOS_ESTATICOS) {
            if (uri.startsWith(prefijo)) {
                return true;
            }
        }
        return false;
    }

    /** Ruta solicitada dentro de la aplicacion (sin context path). */
    private static String rutaDentroDeLaAplicacion(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contexto = request.getContextPath();
        if (contexto != null && !contexto.isEmpty() && uri != null && uri.startsWith(contexto)) {
            uri = uri.substring(contexto.length());
        }
        return uri == null ? "" : uri;
    }
}
