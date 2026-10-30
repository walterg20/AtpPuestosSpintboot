package ar.com.atp.puestos.nucleo.web.mvc;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/**
 * Guarda y recupera el contexto de sesión en la {@code HttpSession} (spec 03-login §3.5, R-LOG-017).
 *
 * <p>Reproduce el par {@code SetContext} / {@code LoadContext} del legacy:
 * {@code &Session.Set(!"context", &Context.ToXml())} y
 * {@code &Context.FromXml(&Session.Get(!"context"))}. La <strong>clave</strong> es exactamente
 * {@code "context"} para que la traza sea legible frente a la KB (D-LOG-08); el valor es el
 * {@link ContextoSesion} (un {@code record} de 13 campos serializable), no el XML del legacy.
 *
 * <p>Es un componente de la capa de presentación: el servicio de acceso no conoce cookies ni
 * sesiones (D-LOG-15).
 */
@Component
public class GestorContextoSesion {

    /** Clave del atributo de sesión: paridad con {@code SetContext} / {@code LoadContext}. */
    public static final String CLAVE = "context";

    /**
     * Guarda el contexto en la sesión (creándola si hace falta).
     *
     * @param request petición actual
     * @param contexto contexto a persistir; {@code null} limpia el atributo
     */
    public void guardar(HttpServletRequest request, ContextoSesion contexto) {
        HttpSession sesion = request.getSession(true);
        sesion.setAttribute(CLAVE, contexto);
    }

    /**
     * Recupera el contexto de la sesión.
     *
     * @param request petición actual
     * @return el contexto persistido o un contexto vacío si la sesión no lo tiene
     */
    public ContextoSesion recuperar(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) {
            return ContextoSesion.vacio();
        }
        Object atributo = sesion.getAttribute(CLAVE);
        return atributo instanceof ContextoSesion contexto ? contexto : ContextoSesion.vacio();
    }
}
