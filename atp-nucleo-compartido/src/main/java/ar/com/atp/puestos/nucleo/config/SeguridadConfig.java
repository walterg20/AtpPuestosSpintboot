package ar.com.atp.puestos.nucleo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracion de seguridad del nucleo (spec 03-login 3.7, plan 4.5 a 4.7).
 *
 * <p>Implementa las <strong>tres decisiones aprobadas</strong> el 2026-10-22:
 *
 * <ul>
 *   <li><strong>D-LOG-17(a)</strong> - el CSRF se exige en las rutas <strong>MVC con sesion</strong>
 *       y se <strong>descarta explicitamente</strong> en todo {@code /api/**};</li>
 *   <li><strong>D-LOG-18(a)</strong> - se conserva el {@code HttpSessionCsrfTokenRepository} y el
 *       token se materializa temprano ({@link MaterializadorTokenCsrf});</li>
 *   <li><strong>D-LOG-19(a)</strong> - {@code /api/v1/**} queda {@code permitAll} en este modulo
 *       (deuda SA-LOG-14, seguimiento T-135).</li>
 * </ul>
 *
 * <p>La politica CSRF es <strong>explicita</strong> por familia de rutas: no depende del
 * comportamiento implicito del filtro por defecto (R-LOG-041). El acceso denegado responde
 * <strong>403</strong> (nunca 200 ni una pagina HTML de error) y el acceso no autenticado a una
 * ruta MVC redirige con <strong>302</strong> a {@code /login}.
 *
 * <p><strong>Mitigacion acordada por la ausencia de CSRF en {@code /api/**} (R-LOG-041, T-136)</strong>:
 * (i) ningun endpoint de {@code /api/**} toma autoridad de la cookie de sesion (el canal es
 * stateless y no lee la {@code HttpSession}); (ii) la cookie de sesion {@code JSESSIONID} y la
 * cookie {@code SUCURSAL} viajan con {@code SameSite=Lax}, de modo que el navegador no las envia
 * en un POST intersitio; (iii) cuando la API se endurezca (SA-LOG-14 / T-135), la credencial
 * viajara <strong>siempre</strong> en la cabecera {@code Authorization} y <strong>nunca</strong> en
 * una cookie, con lo cual el CSRF sera inaplicable por construccion.
 *
 * <p><strong>Por que esta clase implementa {@link WebMvcConfigurer}</strong>: los slices
 * {@code @WebMvcTest} solo cargan los componentes de la familia web (controladores, advice,
 * convertidores, {@code WebMvcConfigurer}, filtros...). Una clase {@code @Configuration} "plana"
 * no se cargaria en el slice y el filtro por defecto de Spring Boot (todo autenticado, CSRF global)
 * sustituiria a esta politica, rompiendo el canal {@code /api/**} (que debe ser {@code permitAll}
 * y sin CSRF) y las rutas MVC (que deben redirigir a {@code /login}). Implementar
 * {@code WebMvcConfigurer} -sin comportamiento propio- garantiza que la cadena quede aplicada
 * tambien en los slices.
 */
@Configuration
public class SeguridadConfig implements WebMvcConfigurer {

    /** Rutas del canal REST (stateless, sin autenticacion en este modulo). */
    private static final String[] RUTAS_API = {"/api/v1/**"};

    /** Rutas publicas del canal MVC. */
    private static final String[] RUTAS_PUBLICAS = {
            "/login",
            "/api/v1/auth/login",
            "/error",
            "/css/**",
            "/js/**",
            "/img/**",
            "/webjars/**",
            "/favicon.ico"
    };

    /**
     * Cadena de seguridad del nucleo.
     *
     * @param http builder de Spring Security
     * @return la cadena aplicada
     * @throws Exception si la configuracion es invalida
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(autorizacion -> autorizacion
                        // D-LOG-19(a): el canal REST es stateless y no exige credenciales.
                        .requestMatchers(RUTAS_API).permitAll()
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf
                        // D-LOG-18(a): se conserva el repositorio de sesion.
                        .csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                        // D-LOG-17(a): el descarte explicito alcanza a TODO /api/**.
                        .ignoringRequestMatchers("/api/**"))
                .exceptionHandling(excepciones -> excepciones
                        // Ruta MVC sin sesion => 302 a /login.
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                        // Token CSRF ausente (o permiso insuficiente) => 403, nunca 200 ni HTML.
                        .accessDeniedHandler(new AccesoDenegadoHandler()))
                .sessionManagement(sesion -> sesion
                        .sessionFixation(fijacion -> fijacion.changeSessionId()))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        // El logout borra la cookie del tipo de acceso.
                        .deleteCookies("SUCURSAL"))
                .securityContext(contexto -> contexto
                        .securityContextRepository(securityContextRepository()))
                // D-LOG-18(a): el token se materializa antes de que la respuesta se confirme.
                .addFilterAfter(new MaterializadorTokenCsrf(), CsrfFilter.class)
                // La autenticacion por formulario la maneja el controlador MVC del login.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * Repositorio del contexto de seguridad: sesion HTTP.
     *
     * @return el repositorio usado por la cadena y por el registrador de autenticacion
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
