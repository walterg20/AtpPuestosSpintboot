package ar.com.atp.puestos.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.ProveedorNavegacion;
import ar.com.atp.puestos.puestos.domain.DetalleProductoPrimario;
import ar.com.atp.puestos.puestos.service.CargaService;
import ar.com.atp.puestos.puestos.service.ResultadoMarcado;
import ar.com.atp.puestos.puestos.web.mvc.CargaMvcController;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CargaMvcController.class)
@WithMockUser(username = "tester", roles = "USER")
class CargaModalDetalleGuiaMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CargaService cargaService;

    @MockitoBean
    private ProveedorNavegacion proveedorNavegacion;

    @MockitoBean
    private ar.com.atp.puestos.nucleo.web.mvc.GestorContextoSesion gestorContextoSesion;

    private ContextoSesion contextoValido;

    @BeforeEach
    void setUp() {
        contextoValido = ContextoSesion.completo("TESTER", "Tester", 1, 1, 1, 5, "Puesto5", "PUE", 1, "Resp");
        lenient().when(gestorContextoSesion.recuperar(any())).thenReturn(contextoValido);
        lenient().when(cargaService.buscarFormularios(any())).thenReturn(java.util.List.of());
        lenient().when(cargaService.obtenerDetalle(any(), any())).thenReturn(Optional.empty());
        lenient().when(cargaService.marcarGuia(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResultadoMarcado(true, 1, CargaService.MSG_EXITO));
    }

    private DetalleProductoPrimario detallePoblado() {
        DetalleProductoPrimario d = new DetalleProductoPrimario();
        d.setConCuit(new BigDecimal("20123456789"));
        d.setConRazSoc("Acopiador SA");
        d.setDppNroForm("1234567890");
        d.setDppTipoGui("G");
        d.setDppFchMov(LocalDateTime.of(2024, 6, 15, 10, 30));
        d.setDppActaPue((short) 5);
        d.setDppUltPaOp("FISC01");
        d.setDppActaNro(42);
        d.setDppAnuObs("Domicilio Fiscal Test");
        d.setDppChasis("CHASIS1");
        d.setDppAcopla("ACOP1");
        d.setDppKmFle(new BigDecimal("1234.56"));
        d.setDppTarxKm(new BigDecimal("10.50"));
        d.setDppMImpFle(new BigDecimal("12962.88"));
        d.setDppAlicFle(new BigDecimal("21.00"));
        d.setDppImpuFle(new BigDecimal("2722.20"));
        d.setDppAdicFle(new BigDecimal("1296.29"));
        d.setDppImIBTot(new BigDecimal("14259.17"));
        d.setDppAdIBTot(new BigDecimal("5000.00"));
        d.setTotalGeneral(new BigDecimal("19259.17"));
        return d;
    }

    // 1. renderiza fragmento
    @Test
    @DisplayName("MDG-01: GET /cargas/detalle renderiza el fragmento modalDetalleGuia")
    void detalleRenderizaModalDetalleGuia() throws Exception {
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890"))
                .andExpect(status().isOk())
                .andExpect(view().name("puestos/control-cargas"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("id=\"modalDetalle\"");
        // th:fragment check en la plantilla fuente
        String src = readFileTemplate("templates/puestos/fragments/modal-detalle-guia.html");
        assertThat(src).contains("th:fragment=\"modalDetalleGuia\"");
    }

    // 2. Cabecera
    @Test
    @DisplayName("MDG-07: cabecera de la guia presente con valores")
    void cabeceraPresente() throws Exception {
        when(cargaService.obtenerDetalle(any(), any())).thenReturn(Optional.of(detallePoblado()));
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890")
                        .param("conCuit", "20123456789")
                        .param("dpptipoGui", "G"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("1234567890");
        assertThat(html).contains("20123456789");
        assertThat(html).contains("Acopiador SA");
        assertThat(html).contains("G"); // tipo comprobante
        assertThat(html).contains("15/06/2024 10:30");
        assertThat(html).contains("5"); // puesto de ingreso
    }

    // 3. Fiscalizacion
    @Test
    @DisplayName("MDG-08: seccion de fiscalizacion presente")
    void fiscalizacionPresente() throws Exception {
        when(cargaService.obtenerDetalle(any(), any())).thenReturn(Optional.of(detallePoblado()));
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890")
                        .param("conCuit", "20123456789"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Fiscalizacion");
        assertThat(html).contains("FISC01");
        assertThat(html).contains("HASH-");
        assertThat(html).contains("42");
        assertThat(html).contains("Domicilio Fiscal Test");
    }

    // 4. Flete
    @Test
    @DisplayName("MDG-09: seccion de flete presente")
    void fletePresente() throws Exception {
        when(cargaService.obtenerDetalle(any(), any())).thenReturn(Optional.of(detallePoblado()));
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890")
                        .param("conCuit", "20123456789"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("CHASIS1");
        assertThat(html).contains("ACOP1");
        assertThat(html).contains("KM Flete");
        assertThat(html).contains("Tarifa x KM");
        assertThat(html).contains("Monto Flete");
        assertThat(html).contains("Alicuota");
        assertThat(html).contains("Impuesto Flete");
        assertThat(html).contains("Adicional 10% Flete");
    }

    // 5. Totales
    @Test
    @DisplayName("MDG-10: seccion de totales presente")
    void totalesPresentes() throws Exception {
        when(cargaService.obtenerDetalle(any(), any())).thenReturn(Optional.of(detallePoblado()));
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890")
                        .param("conCuit", "20123456789"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Subtotal Flete");
        assertThat(html).contains("Subtotal Traslado");
        assertThat(html).contains("Total General");
    }

    // 6. Fallbacks
    @Test
    @DisplayName("MDG-11: fallbacks sin null/undefined")
    void fallbacksSinNull() throws Exception {
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("null");
        assertThat(html).doesNotContain("undefined");
        assertThat(html).contains("0");
    }

    // 7. Formato importes
    @Test
    @DisplayName("MDG-12: formato de importes con 2 decimales y separador de miles")
    void formatoImportes() throws Exception {
        when(cargaService.obtenerDetalle(any(), any())).thenReturn(Optional.of(detallePoblado()));
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890")
                        .param("conCuit", "20123456789"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("1.234,56");
    }

    // 8. Formato fecha
    @Test
    @DisplayName("MDG-13: formato de fecha dd/MM/yyyy HH:mm")
    void formatoFecha() throws Exception {
        when(cargaService.obtenerDetalle(any(), any())).thenReturn(Optional.of(detallePoblado()));
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890")
                        .param("conCuit", "20123456789"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("15/06/2024 10:30");
    }

    // 9. Assets
    @Test
    @DisplayName("MDG-19: carga CSS y JS del modal")
    void assetsPresentes() throws Exception {
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("modal-detalle-guia.css");
        assertThat(html).contains("modal-detalle-guia.js");
    }

    // 10. Tokens
    @Test
    @DisplayName("MDG-20: CSS del modal sin literales hex/rgb/rgba")
    void cssSinTokens() throws Exception {
        String css = null;
        ClassPathResource cpr = new ClassPathResource("static/css/modal-detalle-guia.css");
        if (cpr.exists()) {
            css = new String(cpr.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } else {
            for (String p : new String[]{"src/main/resources/static/css/modal-detalle-guia.css",
                    "atp-puestos-camineros/src/main/resources/static/css/modal-detalle-guia.css"}) {
                Path path = Paths.get(p);
                if (Files.exists(path)) {
                    css = Files.readString(path, StandardCharsets.UTF_8);
                    break;
                }
            }
        }
        assertThat(css).isNotNull();
        java.util.regex.Pattern hex = java.util.regex.Pattern.compile("#[0-9a-fA-F]{3,8}\\b");
        assertThat(hex.matcher(css).find()).isFalse();
        java.util.regex.Pattern rgb = java.util.regex.Pattern.compile("rgba?\\s*\\(");
        assertThat(rgb.matcher(css).find()).isFalse();
    }

    // 11a. Mensaje exito condicional
    @Test
    @DisplayName("MDG-MSG: mensaje de exito condicional")
    void mensajeExitoCondicional() throws Exception {
        // marcar exitoso renderiza el modal con el banner de exito
        String html = mockMvc.perform(post("/cargas/marcar")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("conCuit", "20123456789")
                        .param("dppNroForm", "1234567890"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("La guia ha sido marcada con exito");
    }

    // 11b. Mensaje duplicada condicional
    @Test
    @DisplayName("MDG-MSG: mensaje duplicada condicional")
    void mensajeDuplicadaCondicional() throws Exception {
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890")
                        .param("duplicado", "2"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Guia Duplicada , ya paso por algun puesto");
    }

    @Test
    @DisplayName("MDG-MSG: sin duplicado no muestra mensaje duplicada")
    void sinDuplicadoNoMuestraDuplicada() throws Exception {
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("Guia Duplicada , ya paso por algun puesto");
    }

    // 12. Sin regresion include legacy
    @Test
    @DisplayName("MDG-17: include legacy reemplazado")
    void includeLegacyReemplazado() throws Exception {
        String src = readFileTemplate("templates/puestos/control-cargas.html");
        assertThat(src).doesNotContain("form506-detalle :: modal");
        assertThat(src).contains("modal-detalle-guia :: modalDetalleGuia");
    }

    private String readFileTemplate(String classpath) {
        try {
            ClassPathResource cpr = new ClassPathResource(classpath);
            if (cpr.exists()) {
                return new String(cpr.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
            for (String p : new String[]{"src/main/resources/" + classpath,
                    "atp-puestos-camineros/src/main/resources/" + classpath}) {
                Path path = Paths.get(p);
                if (Files.exists(path)) {
                    return Files.readString(path, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }
}
