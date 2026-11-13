package ar.com.atp.puestos.puestos.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.GestorContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.ProveedorNavegacion;
import ar.com.atp.puestos.puestos.service.ActaService;
import ar.com.atp.puestos.puestos.service.model.ActaFila;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;

/**
 * Pruebas de la pantalla de <strong>listado</strong> del módulo 09 "Formularios Acta"
 * ({@code GET /actas}): grilla, filtros, paginación y estado vacío.
 *
 * <p>El servicio ({@code ActaService}) está mockeado: la pantalla no habla con la base de datos
 * (R-ACTA-001, D-ACTA-04). El alta se prueba en {@code ActasMvcFormularioTest}.
 */
@WebMvcTest(controllers = ActasController.class)
@WithMockUser(username = "tester", roles = "USER")
class ActasMvcControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActaService actaService;

    @MockitoBean
    private ProveedorNavegacion proveedorNavegacion;

    @MockitoBean
    private GestorContextoSesion gestorContextoSesion;

    private ContextoSesion contextoConTurno;
    private ContextoSesion contextoSinTurno;

    @BeforeEach
    void setUp() {
        contextoConTurno = ContextoSesion.completo("TESTER", "Tester", 1, 1, 7, 5, "Puesto5", "PUE",
                1, "Resp");
        contextoSinTurno = ContextoSesion.completo("TESTER", "Tester", 1, 1, 0, 5, "Puesto5", "PUE",
                1, "Resp");

        // El ProveedorNavegacion es un componente de presentación: se simula su efecto sobre el modelo.
        doAnswer(invocacion -> {
            Model model = invocacion.getArgument(0);
            model.addAttribute("contexto", contextoConTurno);
            model.addAttribute("programas", List.of());
            model.addAttribute("rutaActual", "/actas");
            return null;
        }).when(proveedorNavegacion).poblar(any(), any());

        lenient().when(gestorContextoSesion.recuperar(any())).thenReturn(contextoConTurno);
        lenient().when(actaService.listar(any(), any(), any()))
                .thenReturn(pagina(List.of(fila()), 1));
    }

    @Test
    @DisplayName("GET /actas devuelve 200 y la vista puestos/actas-lista")
    void getActasDevuelveLaVista() throws Exception {
        mockMvc.perform(get("/actas"))
                .andExpect(status().isOk())
                .andExpect(view().name("puestos/actas-listado"));
    }

    @Test
    @DisplayName("GET /actas vuelca el view model (actas, filtroActas, contexto, programas, rutaActual)")
    void getActasVuelcaElViewModel() throws Exception {
        mockMvc.perform(get("/actas"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("actas"))
                .andExpect(model().attributeExists("filtroActas"))
                .andExpect(model().attributeExists("contexto"))
                .andExpect(model().attributeExists("programas"))
                .andExpect(model().attributeExists("rutaActual"))
                .andExpect(model().attributeExists("mensaje"));
    }

    @Test
    @DisplayName("GET /actas carga la barra de navegación (ProveedorNavegacion)")
    void getActasCargaLaNavegacion() throws Exception {
        mockMvc.perform(get("/actas")).andExpect(status().isOk());
        verify(proveedorNavegacion).poblar(any(), any());
    }

    @Test
    @DisplayName("GET /actas pagina por defecto en 10 y ordena por TurNroId, SucNroId, SucTipo, F276Id")
    void getActasPaginacionPorDefecto() throws Exception {
        mockMvc.perform(get("/actas")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(actaService).listar(any(), captor.capture(), any());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("turNroId")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("sucNroId")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("sucTipo")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("f276Id")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("f276Id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("GET /actas renderiza las 6 columnas visibles del legacy Grid276, en orden")
    void getActasRenderizaLasColumnasEnOrden() throws Exception {
        String html = html(get("/actas"));
        int inicio = html.indexOf("<thead>");
        int fin = html.indexOf("</thead>");
        assertThat(inicio).as("la grilla debe renderizar su thead").isGreaterThan(0);
        assertThat(fin).isGreaterThan(inicio);
        String encabezado = html.substring(inicio, fin);

        int acto = encabezado.indexOf("ACTA NRO.");
        int cuit = encabezado.indexOf("CUIT");
        int razon = encabezado.indexOf("RAZÓN SOCIAL");
        int manual = encabezado.indexOf("GEN. MANUAL?");
        int operad = encabezado.indexOf("USUARIO GEN.");
        int imprimir = encabezado.indexOf("ACCIONES/IMPRIMIR");

        assertThat(acto).isGreaterThan(0);
        assertThat(cuit).isGreaterThan(acto);
        assertThat(razon).isGreaterThan(cuit);
        assertThat(manual).isGreaterThan(razon);
        assertThat(operad).isGreaterThan(manual);
        assertThat(imprimir).isGreaterThan(operad);
    }

    @Test
    @DisplayName("GET /actas muestra el enlace Crear Acta apuntando a /actas/nueva")
    void getActasMuestraElBotonCrear() throws Exception {
        String html = html(get("/actas"));
        assertThat(html).contains("Crear Acta");
        assertThat(html).contains("href=\"/actas/nueva\"");
    }

    @Test
    @DisplayName("GET /actas con turno <= 0 muestra 'Debe iniciar un turno...' y deshabilita Crear Acta")
    void getActasSinTurnoMuestraElBanner() throws Exception {
        when(gestorContextoSesion.recuperar(any())).thenReturn(contextoSinTurno);

        String html = html(get("/actas"));
        assertThat(html).contains("Debe iniciar un turno...");
        assertThat(html).contains("aria-disabled=\"true\"");
        assertThat(html).doesNotContain("href=\"/actas/nueva\"");
    }

    @Test
    @DisplayName("GET /actas con turno > 0 no muestra el banner de turno inactivo")
    void getActasConTurnoNoMuestraElBanner() throws Exception {
        String html = html(get("/actas"));
        assertThat(html).doesNotContain("Debe iniciar un turno...");
    }

    @Test
    @DisplayName("GET /actas sin resultados muestra el estado vacío y no renderiza la grilla")
    void getActasSinResultadosMuestraEstadoVacio() throws Exception {
        when(actaService.listar(any(), any(), any())).thenReturn(pagina(List.of(), 0));

        String html = html(get("/actas"));
        assertThat(html).contains("Sin actas para mostrar.");
        assertThat(html).doesNotContain("<tbody>");
        // el botón Crear Acta sigue visible (R-ACTA-013)
        assertThat(html).contains("Crear Acta");
    }

    @Test
    @DisplayName("GET /actas renderiza una fila por acta con los datos del contrato")
    void getActasRenderizaLasFilas() throws Exception {
        String html = html(get("/actas"));
        assertThat(html).contains("1234");
        assertThat(html).contains("20123456789");
        assertThat(html).contains("RAZON SOCIAL DEL VENDEDOR");
        assertThat(html).contains("Sí");
        assertThat(html).contains("TESTER");
        assertThat(html).contains("No Imprime");
    }

    @Test
    @DisplayName("GET /actas renderiza la paginación preservando los filtros aplicados")
    void getActasPaginacionPreservaLosFiltros() throws Exception {
        when(actaService.listar(any(), any(), any()))
                .thenReturn(pagina(List.of(fila()), 25));

        String html = html(get("/actas").param("page", "0")
                .param("f276Id", "1234").param("cuit", "20123456789")
                .param("razonSocial", "VENDEDOR").param("desde", "2020-01-01")
                .param("hasta", "2020-12-31"));

        assertThat(html).contains("page=1");
        assertThat(html).contains("size=10");
        assertThat(html).contains("f276Id=1234");
        assertThat(html).contains("cuit=20123456789");
        assertThat(html).contains("razonSocial=VENDEDOR");
        assertThat(html).contains("desde=2020-01-01");
        assertThat(html).contains("hasta=2020-12-31");
    }

    @Test
    @DisplayName("GET /actas ignora el parámetro sort de la URL y usa el orden fijo del legacy")
    void getActasIgnoraElSortDeLaUrl() throws Exception {
        mockMvc.perform(get("/actas").param("sort", "f276Id,asc")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(actaService).listar(any(), captor.capture(), any());
        Sort orden = captor.getValue().getSort();
        assertThat(orden.getOrderFor("turNroId")).isNotNull();
        assertThat(orden.getOrderFor("sucNroId")).isNotNull();
        assertThat(orden.getOrderFor("sucTipo")).isNotNull();
        assertThat(orden.getOrderFor("f276Id")).isNotNull();
    }

    @Test
    @DisplayName("GET /actas muestra el mensaje de éxito del flash tras el alta")
    void getActasMuestraElFlashDeExito() throws Exception {
        mockMvc.perform(get("/actas").flashAttr("mensajeExito", "El acta se ha creado correctamente"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("mensaje", "El acta se ha creado correctamente"));
        assertThat(html(get("/actas").flashAttr("mensajeExito",
                "El acta se ha creado correctamente")))
                .contains("El acta se ha creado correctamente");
    }

    @Test
    @DisplayName("GET /actas renderiza los 5 campos del formulario de filtros")
    void getActasRenderizaLosFiltros() throws Exception {
        String html = html(get("/actas"));
        assertThat(html).contains("name=\"f276Id\"");
        assertThat(html).contains("name=\"cuit\"");
        assertThat(html).contains("name=\"razonSocial\"");
        assertThat(html).contains("name=\"desde\"");
        assertThat(html).contains("name=\"hasta\"");
        assertThat(html).contains("Buscar");
    }

    @Test
    @DisplayName("GET /actas no duplica cabecera ni pie (R-ACTA-056)")
    void getActasNoDuplicaCabeceraNiPie() throws Exception {
        String html = html(get("/actas"));
        assertThat(cuantas(html, "<header")).isEqualTo(1);
        assertThat(cuantas(html, "<footer")).isEqualTo(1);
    }

    // ------------------------------------------------------------- Utilidades

    private String html(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder)
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

    private static ActaFila fila() {
        return new ActaFila(1234, 20123456789L, "RAZON SOCIAL DEL VENDEDOR", "Sí", "TESTER",
                new BigDecimal("100.00"), false);
    }

    private static Page<ActaFila> pagina(List<ActaFila> filas, long total) {
        return new PageImpl<>(filas, PageRequest.of(0, 10, Sort.by("f276Id")), total);
    }
}
