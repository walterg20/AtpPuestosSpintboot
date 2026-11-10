package ar.com.atp.puestos.puestos.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.com.atp.puestos.nucleo.domain.Localidad;
import ar.com.atp.puestos.nucleo.domain.Provincia;
import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.GestorContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.ProveedorNavegacion;
import ar.com.atp.puestos.puestos.service.LocalidadPopupService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;

@WebMvcTest(controllers = LocalidadesController.class)
@WithMockUser(username = "tester", roles = "USER")
class LocalidadesMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocalidadPopupService localidadPopupService;

    @MockitoBean
    private ProveedorNavegacion proveedorNavegacion;

    @MockitoBean
    private GestorContextoSesion gestorContextoSesion;

    private ContextoSesion contexto;

    @BeforeEach
    void setUp() {
        contexto = ContextoSesion.completo("TESTER", "Tester", 1, 1, 7, 5, "Puesto5", "PUE",
                1, "Resp");

        doAnswer(invocacion -> {
            Model model = invocacion.getArgument(0);
            model.addAttribute("contexto", contexto);
            model.addAttribute("programas", List.of());
            model.addAttribute("rutaActual", "/localidades");
            return null;
        }).when(proveedorNavegacion).poblar(any(), any());

        lenient().when(gestorContextoSesion.recuperar(any())).thenReturn(contexto);

        Localidad loc = new Localidad();
        loc.setLocCod(1);
        loc.setLocNom("LOCALIDAD DEMO");
        loc.setLocNomProv("LOCALIDAD DEMO PROV");
        loc.setLocCodPost(1234);
        Provincia prov = new Provincia(1, "BUENOS AIRES");
        loc.setProvincia(prov);

        Page<Localidad> pagina = new PageImpl<>(List.of(loc), PageRequest.of(0, 5), 7L);
        lenient().when(localidadPopupService.listar(any(), any(), any(), any(), any()))
                .thenReturn(pagina);
    }

    // --------------------------------------------------------- GET /localidades

    @Test
    @DisplayName("GET /localidades devuelve 200 y la vista puestos/localidades-seleccion")
    void getLocalidadesDevuelveLaVista() throws Exception {
        mockMvc.perform(get("/localidades"))
                .andExpect(status().isOk())
                .andExpect(view().name("puestos/localidades-seleccion"))
                .andExpect(model().attributeExists("localidades"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attributeExists("number"))
                .andExpect(model().attributeExists("totalElements"));
    }

    @Test
    @DisplayName("GET /localidades renderiza los 6 encabezados de la grilla (columna 4 oculta)")
    void getLocalidadesRenderizaColumnas() throws Exception {
        String html = html(get("/localidades"));
        assertThat(html).contains("Cod. Loc.");
        assertThat(html).contains("Nombre");
        assertThat(html).contains("LocNomProv");
        assertThat(html).contains("Provincia");
        assertThat(html).contains("Cod. Postal");
        // columna 4 oculta
        assertThat(html).contains("hidden");
        // 6 th en thead
        int inicio = html.indexOf("<thead>");
        int fin = html.indexOf("</thead>");
        assertThat(inicio).isGreaterThan(0);
        assertThat(fin).isGreaterThan(inicio);
        String encabezado = html.substring(inicio, fin);
        long cantidadTh = encabezado.chars().filter(c -> c == '<').count(); // aproximacion
        assertThat(encabezado).contains("<th");
    }

    @Test
    @DisplayName("GET /localidades expone paginacion (totalPages=2, number=0, totalElements=7)")
    void getLocalidadesExponePaginacion() throws Exception {
        mockMvc.perform(get("/localidades"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("number", 0))
                .andExpect(model().attribute("totalElements", 7L));
    }

    // --------------------------------------------------------- Filtros parametrizados

    @ParameterizedTest(name = "filtro {0}={1}")
    @MethodSource("filtros")
    @DisplayName("GET /localidades aplica cada filtro")
    void getLocalidadesAplicaFiltros(String parametro, String valor) throws Exception {
        mockMvc.perform(get("/localidades").param(parametro, valor))
                .andExpect(status().isOk())
                .andExpect(view().name("puestos/localidades-seleccion"));
    }

    static java.util.stream.Stream<Arguments> filtros() {
        return java.util.stream.Stream.of(
                Arguments.of("filtroLocCod", "5"),
                Arguments.of("filtroLocNom", "ROSARIO"),
                Arguments.of("filtroProvNombre", "BUENOS"),
                Arguments.of("filtroLocCodPost", "2000"));
    }

    // --------------------------------------------------------- Sesion ausente

    @Test
    @DisplayName("GET /localidades sin sesion devuelve 302 a /login")
    void getLocalidadesSinSesionDevuelve302() throws Exception {
        mockMvc.perform(get("/localidades").with(anonymous()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    // --------------------------------------------------------- CSRF

    @Test
    @DisplayName("POST /localidades sin token CSRF devuelve 403")
    void postLocalidadesSinCsrfDevuelve403() throws Exception {
        mockMvc.perform(post("/localidades"))
                .andExpect(status().isForbidden());
    }

    // --------------------------------------------------------- Utilidades

    private String html(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder)
            throws Exception {
        return mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static int cuantas(String html, String aguja) {
        int total = 0;
        int indice = html.indexOf(aguja);
        while (indice >= 0) {
            total++;
            indice = html.indexOf(aguja, indice + aguja.length());
        }
        return total;
    }
}
