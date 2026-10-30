package ar.com.atp.puestos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend de ATPPuestos (modulo nucleo de datos).
 *
 * <p>Replatforming del sistema legacy GeneXus (15/18) a Java 17 + Spring Boot 3.x.
 * La persistencia reutiliza el esquema fisico existente en SQL Server ({@code Rentas});
 * {@code ddl-auto=none} garantiza que Hibernate no genere DDL (R-204, R-205).</p>
 *
 * <p><strong>03-login / D-LOG-16</strong>: el login legacy NO usa
 * {@code UserDetailsService} ni {@code AuthenticationProvider}: la decision de acceso vive en
 * {@code ServicioAcceso} (codigo 0-6 y mensaje verbatim) y el registro en la sesion, en
 * {@code RegistradorAutenticacion}. Para que Spring Boot NO cree un
 * {@code InMemoryUserDetailsManager} con el usuario {@code user} y una contrasena aleatoria
 * (impresa en el log de arranque) se excluye
 * {@code UserDetailsServiceAutoConfiguration} mediante la propiedad
 * {@code spring.autoconfigure.exclude} del {@code application.yml} del nucleo, que es la fuente de
 * verdad de la configuracion: esa via cubre TODOS los contextos (arranque real y slices
 * {@code @WebMvcTest}), mientras que {@code @SpringBootApplication(exclude = ...)} NO se aplica
 * a los slices (Boot desactiva el {@code @EnableAutoConfiguration} de la clase de aplicacion en
 * los slices y lista sus auto-configuraciones por otro mecanismo).</p>
 */
@SpringBootApplication
public class PuestosApplication {

    public static void main(String[] args) {
        SpringApplication.run(PuestosApplication.class, args);
    }
}
