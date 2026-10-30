package ar.com.atp.puestos;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Configuracion de Spring Boot **solo de test** para el modulo {@code atp-nucleo-compartido}.
 *
 * <p>El nucleo es una libreria: la clase ejecutable ({@code PuestosApplication}) vive en el
 * modulo desplegable {@code atp-puestos-camineros}, que no esta en el classpath de este
 * modulo. Los slices de test ({@code @DataJpaTest}, {@code @SpringBootTest}) buscan una
 * {@code @SpringBootConfiguration} subiendo desde el paquete del test, y sin esta clase no
 * la encontrarian.
 *
 * <p>Se ubica en {@code ar.com.atp.puestos} (y no mas adentro) para que el escaneo de
 * componentes alcance tanto {@code ar.com.atp.puestos.nucleo.**} como
 * {@code ar.com.atp.puestos.shared.**}: el advice global de errores vive en
 * {@code shared.error} y los tests de escritura del nucleo dependen de el.
 *
 * <p>Como el escaneo alcanza el arbol de test, tambien registra los dobles de prueba REST
 * ({@code ExplodingController}, {@code MetodoUnicoController}). Es inocuo: solo agregan dos
 * rutas bajo {@code /api/v1/test/} que ningun test ejercita.
 *
 * <p><strong>03-login / D-LOG-16</strong>: la exclusion de
 * {@code UserDetailsServiceAutoConfiguration} (para que no se cree el usuario {@code user} con
 * contrasena aleatoria) esta en la propiedad {@code spring.autoconfigure.exclude} del
 * {@code application.yml} del nucleo: es la unica via que cubre tambien los slices
 * {@code @WebMvcTest}.</p>
 */
@SpringBootApplication
public class NucleoTestApplication {
}
