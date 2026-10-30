package ar.com.atp.puestos.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.com.atp.puestos.nucleo.service.ProductoService;
import ar.com.atp.puestos.nucleo.web.dto.ProductoDto;
import ar.com.atp.puestos.nucleo.web.mvc.ProductoMvcController;
import ar.com.atp.puestos.nucleo.web.mvc.ProveedorNavegacion;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * <strong>T-212</strong>: verificacion del token CSRF en el <em>modulo desplegable</em>
 * ({@code atp-puestos-camineros}).
 *
 * <p>Contexto (D-36): el nucleo compartido <strong>no</strong> tiene Spring Security (ni
 * {@code compile} ni {@code test}), y por eso sus {@code @WebMvcTest} renderizan la pantalla sin
 * el campo oculto {@code _csrf}. El nucleo es una libreria; el unico artefacto desplegable es
 * {@code atp-puestos-camineros}, que si tiene {@code spring-boot-starter-security}. Esta prueba
 * corre <strong>aca</strong>, en el modulo desplegable, para demostrar las tres comprobaciones:
 *
 * <ol>
 *   <li>el HTML renderizado de {@code GET /productos} incluye el input oculto {@code _csrf};</li>
 *   <li>un {@code POST /productos} <strong>sin</strong> token devuelve <strong>403</strong>;</li>
 *   <li>un {@code POST /productos} <strong>con</strong> token valido no es bloqueado: llega al
 *       controller (2xx al re-renderizar el formulario con errores de validacion; 3xx al
 *       redirigir tras un alta valida).</li>
 * </ol>
 *
 * <p><strong>No</strong> se declara aqui ningun {@code SecurityFilterChain}: la seguridad de la
 * aplicacion es del modulo {@code 03-login} (D-37). Lo que se ejercita es la autoconfiguracion
 * de Spring Security (CSRF habilitado por defecto) tal como queda en el modulo desplegable.
 *
 * <p>El servicio se sustituye por un mock: la pantalla no habla por HTTP con la propia API
 * (R-PROD-01, D-29).
 */
@WebMvcTest(controllers = ProductoMvcController.class)
@WithMockUser(username = "tester", roles = "USER")
class ProductoMvcCsrfTest {

    /** Input oculto del token CSRF, tal como lo emite Thymeleaf. */
    private static final Pattern CAMPO_CSRF = Pattern.compile(
            "<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"");

    /** Codigo de producto de la prueba (no se persiste: el servicio esta mockeado). */
    private static final int PROD_COD = 32001;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService productoService;

    /** La barra de navegación (contexto + menú) la provee el componente de presentación. */
    @MockitoBean
    private ProveedorNavegacion proveedorNavegacion;

    @BeforeEach
    void coleccionVaciaPorDefecto() {
        lenient().when(productoService.listarProductos(any())).thenReturn(List.of());
        lenient().when(productoService.crearProducto(any())).thenReturn(
                new ProductoDto(PROD_COD, "PRODUCTO CSRF", 1, 1, "KG", null, "N", 0, List.of()));
    }

    // --------------------------------------------------- 1) el token esta en el HTML

    @Test
    @DisplayName("T-212: el HTML de GET /productos incluye el input oculto _csrf con token")
    void elFormularioRenderizadoIncluyeElCampoOcultoCsrf() throws Exception {
        String html = mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andExpect(view().name("nucleo/productos"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html)
                .as("el formulario debe emitir el input oculto del token CSRF")
                .contains("name=\"_csrf\"");

        Matcher matcher = CAMPO_CSRF.matcher(html);
        assertThat(matcher.find())
                .as("no se encontro <input ... name=\"_csrf\" ... value=\"...\"> en el HTML renderizado")
                .isTrue();
        assertThat(matcher.group(1))
                .as("el token CSRF renderizado no puede estar vacio")
                .isNotBlank();
        System.out.println("[T-212] input oculto renderizado: "
                + html.substring(matcher.start(), matcher.end()));
    }

    // --------------------------------------------------- 2) POST sin token -> 403

    @Test
    @DisplayName("T-212: un POST /productos sin token CSRF devuelve 403")
    void unPostSinTokenCsfrDevuelve403() throws Exception {
        int estado = mockMvc.perform(post("/productos")
                        .param("prodCod", String.valueOf(PROD_COD))
                        .param("prodDesc", "SIN TOKEN"))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getStatus();
        System.out.println("[T-212] POST /productos SIN token -> HTTP " + estado);

        // El filtro de CSRF corta el pedido antes del controller: el servicio no se invoca.
        verify(productoService, never()).crearProducto(any());
    }

    // --------------------------------------------------- 3) POST con token -> no 403

    @Test
    @DisplayName("T-212: un POST /productos con token CSRF valido devuelve el 2xx esperado")
    void unPostConTokenCsfrValidoDevuelveEl2xxEsperado() throws Exception {
        // Formulario incompleto (sin prodCod): el controller re-renderiza la pantalla con los
        // errores de validacion -> 200. Lo relevante es que NO hay 403: el token fue aceptado.
        int estado = mockMvc.perform(post("/productos")
                        .with(csrf())
                        .param("prodDesc", "SIN CODIGO"))
                .andExpect(status().isOk())
                .andExpect(view().name("nucleo/productos"))
                .andExpect(model().attribute("tieneErrores", true))
                .andReturn().getResponse().getStatus();
        System.out.println("[T-212] POST /productos CON token (formulario incompleto) -> HTTP " + estado);

        verify(productoService, never()).crearProducto(any());
    }

    @Test
    @DisplayName("T-212: un POST /productos con token CSRF valido y alta valida llega al servicio")
    void unPostConTokenCsfrValidoLlegaAlServicio() throws Exception {
        int estado = mockMvc.perform(post("/productos")
                        .with(csrf())
                        .param("prodCod", String.valueOf(PROD_COD))
                        .param("prodDesc", "ALTA CON TOKEN")
                        .param("vigencias[0].prodFchVig", "2020-01-01")
                        .param("vigencias[0].prodImpu1", "10.00")
                        .param("vigencias[0].prodImpu2", "20.00")
                        .param("vigencias[0].prodImpu3", "30.00")
                        .param("vigencias[0].prodImpu4", "40.00"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getStatus();
        System.out.println("[T-212] POST /productos CON token (alta valida) -> HTTP " + estado);

        // Evidencia de que el request llego al controller (no fue cortado por el CSRF).
        verify(productoService).crearProducto(any());
    }
}
