package ar.com.atp.puestos.nucleo.config;

import ar.com.atp.puestos.nucleo.service.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Registra la autenticacion en la sesion HTTP (R-LOG-017, plan 3.7).
 *
 * <p>El login <strong>no</strong> usa {@code AuthenticationManager} (D-LOG-16): la decision de
 * acceso es de negocio y vive en el servicio, que devuelve el codigo 0-6 y el mensaje verbatim.
 * Este componente hace lo que la infraestructura de Spring Security haria, sin perder ese codigo.
 *
 * <p>Mitiga la <strong>fijacion de sesion</strong> rotando el identificador de la sesion
 * ({@code changeSessionId}) en el momento del login, de modo que el {@code JSESSIONID} anterior
 * deja de ser valido.
 *
 * <p>Lo usa el controlador MVC del login (perfil INTERNO).
 */
@Component
public class RegistradorAutenticacion {

    /** Autoridad que se concede al usuario autenticado. */
    private static final String AUTORIDAD = "ROLE_USUARIO";

    private final SecurityContextRepository securityContextRepository;

    public RegistradorAutenticacion(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Registra la autenticacion y la persiste en la sesion.
     *
     * @param request peticion actual
     * @param response respuesta actual
     * @param principal usuario autenticado con su contexto de sesion
     */
    public void registrar(HttpServletRequest request, HttpServletResponse response,
            UsuarioAutenticado principal) {
        request.getSession(true);
        // Mitigacion de fijacion de sesion: rota el identificador de la sesion.
        request.changeSessionId();

        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority(AUTORIDAD)));
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(authentication);
        SecurityContextHolder.setContext(contexto);
        securityContextRepository.saveContext(contexto, request, response);
    }
}