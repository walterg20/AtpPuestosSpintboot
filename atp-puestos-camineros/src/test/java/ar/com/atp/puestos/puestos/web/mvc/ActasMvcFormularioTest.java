package ar.com.atp.puestos.puestos.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.GestorContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.ProveedorNavegacion;
import ar.com.atp.puestos.puestos.service.ActaService;
import ar.com.atp.puestos.puestos.service.model.ActaPreparada;
import ar.com.atp.puestos.puestos.service.model.ActaProductoDisponible;
import ar.com.atp.puestos.puestos.service.model.ResultadoAltaActa;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;

/**
 * Pruebas de la pantalla de <strong>alta</strong> del módulo 09 "Formularios Acta": el formulario
 * de carga manual ({@code GET /actas/nueva}) y su envío ({@code POST /actas/guardar}).
 *
 * <p>El servicio ({@code ActaService}) está mockeado: la pantalla no habla con la base de datos
 * (R-ACTA-001, D-ACTA-04).
 */
@WebMvcTest(controllers = ActasController.class)
@WithMockUser(username = "tester", roles = "USER")
class ActasMvcFormularioTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActaService actaService;

    @MockitoBean
    private ProveedorNavegacion proveedorNavegacion;

    @MockitoBean
    private GestorContextoSesion gestorContextoSesion;

    private ContextoSesion contextoConTurno;

    @BeforeEach
    void setUp() {
        contextoConTurno = ContextoSesion.completo("TESTER", "Tester", 1, 1, 7, 5, "Puesto5", "PUE",
                1, "Resp");

        doAnswer(invocacion -> {
            Model model = invocacion.getArgument(0);
            model.addAttribute("contexto", contextoConTurno);
            model.addAttribute("programas", List.of());
            model.addAttribute("rutaActual", "/actas/nueva");
            return null;
        }).when(proveedorNavegacion).poblar(any(), any());

        lenient().when(gestorContextoSesion.recuperar(any())).thenReturn(contextoConTurno);
        lenient().when(actaService.productosDisponibles())
                .thenReturn(List.of(new ActaProductoDisponible(101, "GRANO", 1, "N",
                        new BigDecimal("10.00"), BigDecimal.ZERO)));
        lenient().when(actaService.prepararFormulario(any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(new ActaPreparada(false, null, "A", 123, LocalDate.of(2020, 1, 15)));
        lenient().when(actaService.guardar(any(), any()))
                .thenReturn(new ResultadoAltaActa(true, null, 123));
    }

    // --------------------------------------------------------- /actas/nueva

    @Test
    @DisplayName("GET /actas/nueva devuelve 200 y la vista puestos/acta-formulario")
    void getNuevaDevuelveLaVista() throws Exception {
        mockMvc.perform(get("/actas/nueva"))
                .andExpect(status().isOk())
                .andExpect(view().name("puestos/acta-formulario"))
                .andExpect(model().attributeExists("actaForm"))
                .andExpect(model().attributeExists("productosDisponibles"));
    }

    @Test
    @DisplayName("GET /actas/nueva con bloqueo NO renderiza el formulario y muestra el motivo")
    void getNuevaBloqueadaNoRenderizaElFormulario() throws Exception {
        when(actaService.prepararFormulario(any(), any(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(new ActaPreparada(true,
                "No se puede generar la guia porque el turno asociado esta cerrado", "C", 123,
                LocalDate.of(2020, 1, 15)));

        String html = html(get("/actas/nueva"));
        assertThat(html).contains("No se puede generar la guia porque el turno asociado esta cerrado");
        assertThat(html).doesNotContain("id=\"f276ActaTi\"");
        assertThat(html).doesNotContain("id=\"f276CuitVe\"");
        assertThat(html).doesNotContain("id=\"acta-agregar-producto\"");
    }

    @Test
    @DisplayName("GET /actas/nueva renderiza los 6 fieldsets del Figma")
    void getNuevaRenderizaLosFieldsets() throws Exception {
        String html = html(get("/actas/nueva"));
        assertThat(html).contains("Datos del Acta");
        assertThat(html).contains("Vendedor");
        assertThat(html).contains("Destinatario");
        assertThat(html).contains("Transportista");
        assertThat(html).contains("Datos del Transporte");
        assertThat(html).contains("Productos");
    }

    @Test
    @DisplayName("GET /actas/nueva renderiza las opciones verbatim de los 3 selectores")
    void getNuevaRenderizaLasOpcionesVerbatim() throws Exception {
        String html = html(get("/actas/nueva"));
        assertThat(html).contains("Seleccionar...");
        assertThat(html).contains("Falta Documentación de Fletes");
        assertThat(html).contains("Falta Documentación de Prod. Prim.");
        assertThat(html).contains("Sin Documentación");
        assertThat(html).contains("Salida");
        assertThat(html).contains("Entrada");
        assertThat(html).contains("Carta Porte");
        assertThat(html).contains("Factura");
        assertThat(html).contains("Remito");
        assertThat(html).contains("DT-e");
        assertThat(html).contains("DT-v");
        assertThat(html).contains("Guia Forestal");
    }

    @Test
    @DisplayName("GET /actas/nueva NO renderiza el campo Nro. Acta (V-11)")
    void getNuevaNoRenderizaElNumeroDeActa() throws Exception {
        String html = html(get("/actas/nueva"));
        assertThat(html).doesNotContain("Nro. Acta");
        assertThat(html).doesNotContain("name=\"f276Id\"");
    }

    @Test
    @DisplayName("GET /actas/nueva renderiza la fecha del acta en sólo lectura (V-10)")
    void getNuevaRenderizaLaFechaEnSoloLectura() throws Exception {
        String html = html(get("/actas/nueva"));
        assertThat(html).contains("Fecha Acta");
        assertThat(html).contains("value=\"2020-01-15\"");
        assertThat(html).contains("readonly");
    }

    @Test
    @DisplayName("GET /actas/nueva renderiza los 15 controles de las partes con label asociado")
    void getNuevaRenderizaLosControlesDeLasPartes() throws Exception {
        String html = html(get("/actas/nueva"));
        List<String> ids = List.of("f276CuitVe", "f276VenRS", "f276VenDom", "f276VenLoc",
                "f276VenProv", "f276CuitDe", "f276DestRS", "f276DestDo", "f276DestLoc",
                "f276DestProv", "f276CuitTr", "f276TranRS", "f276TranDo", "f276TranLoc",
                "f276TranProv");
        ids.forEach(id -> {
            assertThat(html).as("control %s", id).contains("id=\"" + id + "\"");
            assertThat(html).as("label de %s", id).contains("for=\"" + id + "\"");
        });
    }

    @Test
    @DisplayName("GET /actas/nueva deja Localidad y Provincia en sólo lectura (V-12)")
    void getNuevaLocalidadYProvinciaEnSoloLectura() throws Exception {
        String html = html(get("/actas/nueva"));
        assertThat(html).contains("id=\"f276VenLoc\" type=\"text\" readonly");
        assertThat(html).contains("id=\"f276VenProv\" type=\"text\" readonly");
        assertThat(html).contains("id=\"f276DestLoc\" type=\"text\" readonly");
        assertThat(html).contains("id=\"f276TranProv\" type=\"text\" readonly");
    }

    @Test
    @DisplayName("GET /actas/nueva renderiza la grilla de productos con el botón +")
    void getNuevaRenderizaLaGrillaDeProductos() throws Exception {
        String html = html(get("/actas/nueva"));
        assertThat(html).contains(">Código<");
        assertThat(html).contains(">Descripción<");
        assertThat(html).contains(">Tn.<");
        assertThat(html).contains("id=\"acta-agregar-producto\"");
        assertThat(html).contains("id=\"acta-productos\"");
        assertThat(html).contains("GRANO");
    }

    @Test
    @DisplayName("GET /actas/nueva muestra la confirmación verbatim y el enlace Cancelar")
    void getNuevaMuestraLaConfirmacionYCancelar() throws Exception {
        String html = html(get("/actas/nueva"));
        assertThat(html).contains("¿Confirma que desea enviar el Formulario de Acta?");
        assertThat(html).contains("Confirmar");
        assertThat(html).contains("Cancelar");
        assertThat(html).contains("href=\"/actas\"");
    }

    @Test
    @DisplayName("GET /actas/nueva no duplica cabecera ni pie (R-ACTA-056)")
    void getNuevaNoDuplicaCabeceraNiPie() throws Exception {
        String html = html(get("/actas/nueva"));
        assertThat(cuantas(html, "<header")).isEqualTo(1);
        assertThat(cuantas(html, "<footer")).isEqualTo(1);
    }

    // ------------------------------------------------------- /actas/guardar

    @Test
    @DisplayName("POST /actas/guardar con error de negocio re-renderiza el formulario con el mensaje verbatim")
    void postGuardarConErroresDeBinding() throws Exception {
        when(actaService.guardar(any(), any()))
                .thenReturn(new ResultadoAltaActa(false, "Seleccionar el motivo de Acta", null));

        String html = html(post("/actas/guardar").with(csrf())
                .param("f276ActaTi", "")
                .param("f276Clasif", "S"));

        assertThat(html).contains("Seleccionar el motivo de Acta");
        assertThat(html).contains("atp-alert--error");
        assertThat(html).contains("role=\"alert\"");
        verify(actaService).guardar(any(), any());
    }

    @Test
    @DisplayName("POST /actas/guardar re-renderiza la grilla de productos con el binding indexado")
    void postGuardarReRenderizaLaGrillaDeProductos() throws Exception {
        when(actaService.guardar(any(), any()))
                .thenReturn(new ResultadoAltaActa(false, "Debe seleccionar algun producto", null));
        String html = html(post("/actas/guardar").with(csrf())
                .param("f276ActaTi", "")
                .param("f276Clasif", "S")
                .param("productos[0].prodCod", "101")
                .param("productos[0].f276RCant", "2"));

        // El binding indexado del legacy: name="productos[i].<campo>".
        assertThat(html).contains("name=\"productos[0].prodCod\"");
        assertThat(html).contains("name=\"productos[0].f276RCant\"");
        // Cada control de la grilla tiene su label asociado (R-ACTA-055).
        assertThat(html).contains("for=\"productos_0_prodCod\"");
        assertThat(html).contains("for=\"productos_0_f276RCant\"");
        // El producto elegido queda seleccionado y el catálogo ofrece sólo productos no anulados.
        assertThat(html).contains("value=\"101\"");
    }

    @Test
    @DisplayName("POST /actas/guardar con error de negocio muestra el banner verbatim")
    void postGuardarConErrorDeNegocio() throws Exception {
        when(actaService.guardar(any(), any()))
                .thenReturn(new ResultadoAltaActa(false, "Debe seleccionar algun producto", null));

        String html = html(post("/actas/guardar").with(csrf()).params(parametrosValidos()));

        assertThat(html).contains("Debe seleccionar algun producto");
        assertThat(html).contains("atp-alert--error");
    }

    @Test
    @DisplayName("POST /actas/guardar exitoso redirige a /actas con el flash verbatim")
    void postGuardarExitoso() throws Exception {
        mockMvc.perform(post("/actas/guardar").with(csrf()).params(parametrosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/actas"))
                .andExpect(flash().attribute("mensajeExito", "El acta se ha creado correctamente"));
    }

    @Test
    @DisplayName("POST /actas/guardar sin token CSRF devuelve 403")
    void postGuardarSinCsrf() throws Exception {
        mockMvc.perform(post("/actas/guardar").params(parametrosValidos()))
                .andExpect(status().isForbidden());
        verify(actaService, never()).guardar(any(), any());
    }

    @Test
    @DisplayName("POST /actas/guardar toma el turno y la sucursal del contexto, no del formulario")
    void postGuardarUsaElContextoDeSesion() throws Exception {
        mockMvc.perform(post("/actas/guardar").with(csrf()).params(parametrosValidos())
                        .param("turNroId", "9999").param("sucNroId", "8888"))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<ContextoSesion> captor = ArgumentCaptor.forClass(ContextoSesion.class);
        verify(actaService).guardar(any(), captor.capture());
        assertThat(captor.getValue().turno()).isEqualTo(7);
        assertThat(captor.getValue().sucursal()).isEqualTo(5);
    }

    // --------------------------------------------------------- Estructura

    @Test
    @DisplayName("El controller no depende de repositorios: sólo del servicio y de la navegación")
    void elControllerNoAccedeARepositorios() {
        List<String> tipos = java.util.Arrays.stream(ActasController.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getType)
                .map(Class::getName)
                .toList();
        assertThat(tipos).noneMatch(tipo -> tipo.contains("repository")
                || tipo.contains("Repository")
                || tipo.contains("Repositorio"));
        assertThat(tipos).contains(ActaService.class.getName());
    }

    @Test
    @DisplayName("El controller expone las rutas del módulo y ningún otro endpoint")
    void elControllerExponeLasRutasDelModulo() {
        List<String> requestMappings = java.util.Arrays.stream(
                        ActasController.class.getDeclaredMethods())
                .flatMap(metodo -> java.util.Arrays.stream(
                                metodo.getAnnotationsByType(
                                        org.springframework.web.bind.annotation.RequestMapping.class)))
                .flatMap(anotacion -> java.util.Arrays.stream(anotacion.value()))
                .toList();
        assertThat(requestMappings).doesNotContain("/api/**");

        org.springframework.web.bind.annotation.RequestMapping deClase =
                ActasController.class.getAnnotation(
                        org.springframework.web.bind.annotation.RequestMapping.class);
        assertThat(deClase).isNotNull();
        assertThat(deClase.value()).contains("/actas");
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

    /** Parámetros de un alta válida (todos los campos obligatorios informados). */
    private static org.springframework.util.MultiValueMap<String, String> parametrosValidos() {
        org.springframework.util.MultiValueMap<String, String> params =
                new org.springframework.util.LinkedMultiValueMap<>();
        params.add("f276ActaTi", "P");
        params.add("f276Clasif", "S");
        params.add("f276FchMov", "2020-01-15");
        params.add("f276CuitVe", "20123456789");
        params.add("f276CuitDe", "20123456790");
        params.add("f276CuitTr", "20123456791");
        params.add("f276TCpbte", "1");
        params.add("f276NCpbte", "0000000001");
        params.add("f276Chasis", "AB123CD");
        params.add("f276Acopla", "EF456GH");
        params.add("f276KmFle", "150");
        params.add("productos[0].prodCod", "101");
        params.add("productos[0].f276RCant", "2");
        return params;
    }
}
