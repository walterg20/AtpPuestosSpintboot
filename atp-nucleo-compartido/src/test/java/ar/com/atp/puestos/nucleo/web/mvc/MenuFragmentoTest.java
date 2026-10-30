package ar.com.atp.puestos.nucleo.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.nucleo.service.SmoothNavMenuData;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Render del fragmento {@code fragments/menu} (barra horizontal de programas) en aislamiento.
 *
 * <p>Cubre las dos ramas del marcado de la tab activa:
 * <ul>
 *   <li>Con {@code rutaActual} en el modelo, el ítem cuyo {@code link} coincide recibe la clase
 *       {@code atp-menu__item--activo} y el enlace queda marcado con {@code aria-current="page"}.</li>
 *   <li>Sin {@code rutaActual} en el modelo (aún no lo expone ningún controlador), el render no
 *       falla y ningún ítem queda marcado: la comparación es segura.</li>
 * </ul>
 */
class MenuFragmentoTest {

    private static final SmoothNavMenuData INICIO =
            new SmoothNavMenuData("1", "Inicio", "Inicio", "/home", null, List.of());
    private static final SmoothNavMenuData TURNOS =
            new SmoothNavMenuData("2", "Turnos", "Turnos de Comision", "/turnos", null, List.of());

    @Test
    @DisplayName("Con rutaActual, el item cuyo link coincide queda como tab activa")
    void elItemQueCoincideConLaRutaActualQuedaActivo() {
        Context contexto = new Context();
        contexto.setVariable("programas", List.of(INICIO, TURNOS));
        contexto.setVariable("rutaActual", "/home");

        String html = motor().process("fragments/menu", Set.of("menu"), contexto);

        assertThat(html).contains("atp-menu__item--activo");
        assertThat(html).contains("aria-current=\"page\"");
        assertThat(html).contains("href=\"/home\"");
    }

    @Test
    @DisplayName("Sin rutaActual en el modelo el render no falla y no marca ningun item")
    void sinRutaActualElRenderNoFallaYMarcaNada() {
        Context contexto = new Context();
        contexto.setVariable("programas", List.of(INICIO, TURNOS));

        String html = motor().process("fragments/menu", Set.of("menu"), contexto);

        assertThat(html).doesNotContain("atp-menu__item--activo");
        assertThat(html).doesNotContain("aria-current");
        assertThat(html).contains("Inicio").contains("Turnos");
    }

    @Test
    @DisplayName("Con rutaActual, solo el item coincidente queda activo")
    void soloElItemCoincidenteQuedaActivo() {
        Context contexto = new Context();
        contexto.setVariable("programas", List.of(INICIO, TURNOS));
        contexto.setVariable("rutaActual", "/turnos");

        String html = motor().process("fragments/menu", Set.of("menu"), contexto);

        assertThat(html).containsOnlyOnce("atp-menu__item--activo");
        assertThat(html).containsOnlyOnce("aria-current=\"page\"");
    }

    /** Motor Thymeleaf mínimo sobre las plantillas del classpath (prefijo por defecto de Spring). */
    private static SpringTemplateEngine motor() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine motor = new SpringTemplateEngine();
        motor.setTemplateResolver(resolver);
        return motor;
    }
}
