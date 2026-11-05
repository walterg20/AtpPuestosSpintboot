package ar.com.atp.puestos.puestos.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Higiene de los recursos del módulo 09 "Formularios Acta" (T-ACTA-177, R-ACTA-053).
 *
 * <p>Verifica que las hojas y plantillas nuevas no dependan de frameworks CSS, CDN, fuentes
 * remotas, npm ni bundlers, y que el JavaScript no haga llamadas HTTP ni evalúe código.
 */
class ActasRecursosTest {

    private static final List<String> RECURSOS = List.of(
            "/static/css/actas.css",
            "/static/js/actas.js",
            "/templates/puestos/actas-lista.html",
            "/templates/puestos/acta-formulario.html",
            "/templates/puestos/fragments/acta-productos.html");

    private static final List<String> PROHIBIDOS = List.of(
            "bootstrap", "tailwind", "cdn.", "unpkg", "jsdelivr", "node_modules");

    @Test
    @DisplayName("Las hojas y plantillas del módulo no referencian frameworks, CDN ni npm")
    void losRecursosNoReferencianFrameworksNiCdn() throws IOException {
        for (String recurso : RECURSOS) {
            String contenido = leer(recurso).toLowerCase(Locale.ROOT);
            for (String prohibido : PROHIBIDOS) {
                assertThat(contenido)
                        .as("%s no debe contener '%s'", recurso, prohibido)
                        .doesNotContain(prohibido);
            }
        }
    }

    @Test
    @DisplayName("Las plantillas no cargan recursos remotos (ni CDN ni fuentes externas)")
    void lasPlantillasNoCarganRecursosRemotos() throws IOException {
        List<String> plantillas = List.of(
                "/templates/puestos/actas-lista.html",
                "/templates/puestos/acta-formulario.html",
                "/templates/puestos/fragments/acta-productos.html");
        for (String plantilla : plantillas) {
            String contenido = leer(plantilla).toLowerCase(Locale.ROOT);
            assertThat(contenido).as("%s no debe tener src remoto", plantilla)
                    .doesNotContain("src=\"http");
            assertThat(contenido).as("%s no debe tener href remoto", plantilla)
                    .doesNotContain("href=\"http");
        }
    }

    @Test
    @DisplayName("actas.css no contiene literales hexadecimales: todos los colores son tokens")
    void actasCssNoTieneLiteralesHexadecimales() throws IOException {
        String css = leer("/static/css/actas.css");
        assertThat(css).doesNotContain("#");
    }

    @Test
    @DisplayName("actas.css sólo usa var(--atp-*) para los colores")
    void actasCssUsaLosTokensDelSistema() throws IOException {
        String css = leer("/static/css/actas.css");
        assertThat(css).contains("var(--atp-");
        assertThat(css).doesNotContain("rgb(");
        assertThat(css).doesNotContain("hsl(");
    }

    @Test
    @DisplayName("actas.js no hace fetch, no usa XMLHttpRequest ni eval")
    void actasJsNoTieneLlamadasHttpNiEval() throws IOException {
        String js = leer("/static/js/actas.js");
        assertThat(js).doesNotContain("fetch(");
        assertThat(js).doesNotContain("XMLHttpRequest");
        assertThat(js).doesNotContain("eval(");
        assertThat(js).doesNotContain("import(");
    }

    @Test
    @DisplayName("actas.js incluye la confirmación verbatim del legacy")
    void actasJsTieneLaConfirmacionVerbatim() throws IOException {
        String js = leer("/static/js/actas.js");
        assertThat(js).contains("¿Confirma que desea enviar el Formulario de Acta?");
    }

    private static String leer(String recurso) throws IOException {
        try (InputStream entrada = ActasRecursosTest.class.getResourceAsStream(recurso)) {
            assertThat(entrada).as("no se encontró el recurso %s", recurso).isNotNull();
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
