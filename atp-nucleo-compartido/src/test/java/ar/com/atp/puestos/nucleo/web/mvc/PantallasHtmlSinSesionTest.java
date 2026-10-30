package ar.com.atp.puestos.nucleo.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.com.atp.puestos.nucleo.config.RegistradorAutenticacion;
import ar.com.atp.puestos.nucleo.service.ProductoService;
import ar.com.atp.puestos.nucleo.service.ServicioAcceso;
import ar.com.atp.puestos.nucleo.service.ServicioAutorizacion;
import ar.com.atp.puestos.nucleo.service.ServicioTurnos;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T-311 (parte de vistas) — Regresión del hallazgo <strong>H4</strong> del QA del módulo 02.
 *
 * <p>Toda pantalla HTML del sistema debe ser servible <strong>sin cookie de sesión previa</strong>:
 * HTTP 200, con su formulario y su campo {@code _csrf}, y <strong>sin</strong> la página de error
 * interna (R-LOG-042, R-LOG-043).
 *
 * <p>La causa raíz de H4 era que el token CSRF se materializaba tarde (durante el render, con la
 * respuesta ya confirmada) ⇒ {@code IllegalStateException: Cannot create a session after the
 * response has been committed} y una página {@code ERROR_INTERNO} en lugar de la pantalla. La
 * materialización anticipada (D-LOG-18(a)) lo evita sin cambiar el contrato del formulario.
 *
 * <p>Las pruebas de {@code /login} <strong>no</strong> llevan usuario: son el escenario exacto de
 * H4 (una petición sin sesión previa, cuya sesión se crea durante la materialización del token).
 * Las de {@code /home} y {@code /productos} sí, porque son rutas protegidas (R-LOG-031).
 */
@WebMvcTest(controllers = {AuthMvcController.class, ProductoMvcController.class,
        TurnoMvcController.class})
@Import({GestorContextoSesion.class, ProveedorNavegacion.class})
class PantallasHtmlSinSesionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicioAcceso servicioAcceso;

    @MockitoBean
    private ServicioAutorizacion servicioAutorizacion;

    @MockitoBean
    private RegistradorAutenticacion registradorAutenticacion;

    @MockitoBean
    private ProductoService productoService;

    @MockitoBean
    private ServicioTurnos servicioTurnos;

    @Test
    @DisplayName("R-LOG-043: GET /login sin cookie de sesión devuelve 200 con el formulario y el _csrf")
    void getLoginSinSesionPrevia() throws Exception {
        var resultado = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn();

        String html = resultado.getResponse().getContentAsString();
        assertThat(html).contains("<form");
        assertThat(html).contains("name=\"_csrf\"");
        assertThat(html).doesNotContain("ERROR_INTERNO");
        assertThat(resultado.getResponse().getStatus()).isNotEqualTo(500);
    }

    @Test
    @DisplayName("R-LOG-042: la sesión creada por la petición de /login ya contiene el token CSRF")
    void laSesionDeLaPeticionContieneElTokenCsrf() throws Exception {
        var resultado = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession sesion = (MockHttpSession) resultado.getRequest().getSession(false);
        assertThat(sesion).as("la materialización anticipada crea la sesión durante la petición")
                .isNotNull();
        // El nombre del atributo cambió entre versiones de Spring Security (la constante no es
        // pública): lo que importa es que la sesión guarde el token.
        boolean contieneToken = java.util.Collections.list(sesion.getAttributeNames()).stream()
                .anyMatch(nombre -> sesion.getAttribute(nombre) instanceof CsrfToken);
        assertThat(contieneToken).as("token CSRF materializado en la sesión").isTrue();
    }

    @Test
    @DisplayName("R-LOG-043: GET /login sin sesión previa tampoco devuelve la página de error")
    void getLoginSinSesionPreviaSinPaginaDeError() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("ERROR_INTERNO"));
    }

    @Test
    @DisplayName("R-LOG-043: GET /home sin cookie de sesión previa devuelve 200 con su formulario")
    @WithMockUser(username = "tester", roles = "USER")
    void getHomeSinSesionPrevia() throws Exception {
        var resultado = mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andReturn();

        String html = resultado.getResponse().getContentAsString();
        assertThat(html).contains("<form");
        assertThat(html).contains("name=\"_csrf\"");
        assertThat(html).doesNotContain("ERROR_INTERNO");
    }

    @Test
    @DisplayName("R-LOG-043: GET /productos sin cookie de sesión previa devuelve 200 con su _csrf")
    @WithMockUser(username = "tester", roles = "USER")
    void getProductosSinSesionPrevia() throws Exception {
        lenient().when(productoService.listarProductos(any())).thenReturn(List.of());

        var resultado = mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andReturn();

        String html = resultado.getResponse().getContentAsString();
        assertThat(html).contains("<form");
        assertThat(html).contains("name=\"_csrf\"");
        assertThat(html).doesNotContain("ERROR_INTERNO");
    }

    @Test
    @DisplayName("R-LOG-043: GET /turnos sin cookie de sesión previa devuelve 200 con su _csrf")
    @WithMockUser(username = "tester", roles = "USER")
    void getTurnosSinSesionPrevia() throws Exception {
        lenient().when(servicioTurnos.listar(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        var resultado = mockMvc.perform(get("/turnos"))
                .andExpect(status().isOk())
                .andReturn();

        String html = resultado.getResponse().getContentAsString();
        assertThat(html).contains("<form");
        assertThat(html).contains("name=\"_csrf\"");
        assertThat(html).doesNotContain("ERROR_INTERNO");
    }
}
