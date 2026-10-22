package ar.com.atp.puestos.nucleo.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties;
import org.springframework.util.ClassUtils;

/**
 * <strong>T-101 / D-36 / SA-P09</strong>: la dependencia de vistas del nucleo.
 *
 * <ul>
 *   <li>El prefijo de resolucion de plantillas es {@code classpath:/templates/} (el default de
 *       Spring Boot) y el nucleo <strong>no</strong> lo sobrescribe.</li>
 *   <li>Thymeleaf esta disponible en el classpath de <strong>compilacion</strong> (las vistas
 *       del catalogo viven en el nucleo, D-26).</li>
 *   <li><strong>Sin</strong> {@code thymeleaf-layout-dialect} (SA-P09 / D-07 de
 *       {@code 00-alcance}): los fragmentos {@code th:replace} alcanzan sin el dialecto.</li>
 *   <li><strong>Sin</strong> Spring Security en el nucleo (D-36): su autoconfiguracion se
 *       activaria en los {@code @WebMvcTest} del nucleo y romperia la suite verde de
 *       {@code 01-nucleo-datos}.</li>
 * </ul>
 */
class ThymeleafNucleoTest {

    @Test
    @DisplayName("T-101: el prefijo de resolucion de plantillas es classpath:/templates/")
    void prefijoDeResolucionDePlantillas() {
        ThymeleafProperties propiedades = new ThymeleafProperties();

        assertThat(propiedades.getPrefix()).isEqualTo("classpath:/templates/");
        assertThat(propiedades.getSuffix()).isEqualTo(".html");
    }

    @Test
    @DisplayName("T-101: el nucleo no sobrescribe el prefijo ni el sufijo de Thymeleaf")
    void elNucleoNoSobrescribeLaResolucionDePlantillas() throws IOException {
        String applicationYml = leerRecurso("/application.yml");

        assertThat(applicationYml)
                .as("el nucleo no debe redefinir spring.thymeleaf (SA-P09 / T-101)")
                .doesNotContain("thymeleaf");
    }

    @Test
    @DisplayName("T-101: Thymeleaf esta disponible (scope compile) en el nucleo")
    void thymeleafEstaEnElClasspath() {
        assertThat(ClassUtils.isPresent("org.thymeleaf.spring6.SpringTemplateEngine",
                getClass().getClassLoader())).isTrue();
        assertThat(ClassUtils.isPresent("org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration",
                getClass().getClassLoader())).isTrue();
    }

    @Test
    @DisplayName("SA-P09: no esta thymeleaf-layout-dialect en el classpath del nucleo")
    void noHayLayoutDialect() {
        assertThat(ClassUtils.isPresent("org.thymeleaf.layoutdialect.LayoutDialect",
                getClass().getClassLoader()))
                .as("D-07 de 00-alcance: thymeleaf-layout-dialect no debe agregarse")
                .isFalse();
    }

    @Test
    @DisplayName("D-36: Spring Security no esta en el classpath del nucleo (ni compile ni test)")
    void noHaySpringSecurity() {
        assertThat(ClassUtils.isPresent("org.springframework.security.core.context.SecurityContextHolder",
                getClass().getClassLoader()))
                .as("D-36: Spring Security no debe estar en el nucleo; rompe los @WebMvcTest del nucleo")
                .isFalse();
    }

    private static String leerRecurso(String ruta) throws IOException {
        try (InputStream entrada = ThymeleafNucleoTest.class.getResourceAsStream(ruta)) {
            assertThat(entrada).as("no se encontro el recurso " + ruta).isNotNull();
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
