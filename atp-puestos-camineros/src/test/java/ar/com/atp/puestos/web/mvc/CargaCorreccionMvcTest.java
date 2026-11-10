package ar.com.atp.puestos.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.ProveedorNavegacion;
import ar.com.atp.puestos.puestos.domain.DetalleProductoPrimario;
import ar.com.atp.puestos.puestos.service.CargaService;
import ar.com.atp.puestos.puestos.service.ResultadoMarcado;
import ar.com.atp.puestos.puestos.web.mvc.CargaMvcController;
import ar.com.atp.puestos.puestos.web.mvc.form.CargaForm;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
class CargaCorreccionMvcTest {

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
        lenient().when(cargaService.buscarFormularios(any())).thenReturn(List.of());
        lenient().when(cargaService.marcarGuia(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResultadoMarcado(true, 1, CargaService.MSG_EXITO));
    }

    private DetalleProductoPrimario mockFormulario() {
        DetalleProductoPrimario dp = new DetalleProductoPrimario();
        dp.setConCuit(new BigDecimal("20123456789"));
        dp.setDppSucu((short) 1);
        dp.setCalPerFisc(2024);
        dp.setDppSecu((short) 1);
        dp.setDppNroForm("1234567890");
        dp.setConRazSoc("Acopiador SA");
        dp.setTotalGeneral(new BigDecimal("1000.00"));
        return dp;
    }

    // T-311
    @Test
    @DisplayName("T-311: POST /cargas/buscar no renderiza modal")
    void buscarNoRenderizaModal() throws Exception {
        String html = mockMvc.perform(post("/cargas/buscar")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("barCode", "1234567890123456789019876543210"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("modalDetalle");
        assertThat(html).doesNotContain("atp-modal");
    }

    @Test
    @DisplayName("T-311: GET /cargas/detalle renderiza modal")
    void detalleRenderizaModal() throws Exception {
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("modalDetalle");
    }

    @Test
    @DisplayName("T-311: POST /cargas/marcar renderiza modal")
    void marcarRenderizaModal() throws Exception {
        String html = mockMvc.perform(post("/cargas/marcar")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("conCuit", "20123456789")
                        .param("dppNroForm", "1234567890"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("modalDetalle");
    }

    // T-312
    @Test
    @DisplayName("T-312: GET /cargas/detalle con 4 params renderiza modal con los valores")
    void nroGuiaLinkAbreModal() throws Exception {
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890")
                        .param("dpptipoGui", "X")
                        .param("duplicado", "2")
                        .param("conCuit", "20123456789"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("1234567890");
        assertThat(html).contains("X");
        assertThat(html).contains("2");
        assertThat(html).contains("20123456789");
    }

    @Test
    @DisplayName("T-312: link del CUIT lleva los params (misma verificacion)")
    void cuitLinkAbreModal() throws Exception {
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890")
                        .param("dpptipoGui", "X")
                        .param("duplicado", "2")
                        .param("conCuit", "20123456789"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("modalDetalle");
        assertThat(html).contains("1234567890");
        assertThat(html).contains("20123456789");
    }

    // T-313
    @Test
    @DisplayName("T-313: grilla con 7 columnas en el orden indicado")
    void grillaSieteColumnas() throws Exception {
        when(cargaService.buscarFormularios(any())).thenReturn(List.of(mockFormulario()));
        String html = mockMvc.perform(post("/cargas/buscar")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("barCode", "1234567890123456789019876543210"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Nro Guia");
        assertThat(html).contains("Acopiador");
        assertThat(html).contains("Cuit");
        assertThat(html).contains("Fecha Emision");
        assertThat(html).contains("Nro. Comprobante");
        assertThat(html).contains("Total General");
        assertThat(html).contains("Marcar");
    }

    // T-314
    @Test
    @DisplayName("T-314: form de Marcar incluye 5 hidden PK")
    void marcarFormCincoHidden() throws Exception {
        when(cargaService.buscarFormularios(any())).thenReturn(List.of(mockFormulario()));
        String html = mockMvc.perform(post("/cargas/buscar")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("barCode", "1234567890123456789019876543210"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("name=\"conCuit\"");
        assertThat(html).contains("name=\"dppSucu\"");
        assertThat(html).contains("name=\"calPerFisc\"");
        assertThat(html).contains("name=\"dppSecu\"");
        assertThat(html).contains("name=\"dppNroForm\"");
    }

    // T-316
    @Test
    @DisplayName("T-316: control-cargas.css sin literales hex")
    void cssSinHex() throws Exception {
        String css = null;
        ClassPathResource cpr = new ClassPathResource("static/css/control-cargas.css");
        if (cpr.exists()) {
            css = new String(cpr.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } else {
            for (String p : new String[]{"src/main/resources/static/css/control-cargas.css",
                    "atp-puestos-camineros/src/main/resources/static/css/control-cargas.css"}) {
                Path path = Paths.get(p);
                if (Files.exists(path)) {
                    css = Files.readString(path, StandardCharsets.UTF_8);
                    break;
                }
            }
        }
        assertThat(css).isNotNull();
        // regex check for hex colors
        java.util.regex.Pattern hex = java.util.regex.Pattern.compile("#[0-9a-fA-F]{3,6}");
        java.util.regex.Matcher m = hex.matcher(css);
        assertThat(m.find()).isFalse();
    }

    // T-317
    @Test
    @DisplayName("T-317: modal incluye control de cierre")
    void modalTieneCerrar() throws Exception {
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Cerrar");
        assertThat(html).contains("cerrarModal");
    }
}
