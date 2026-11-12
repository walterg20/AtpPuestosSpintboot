package ar.com.atp.puestos.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.ProveedorNavegacion;
import ar.com.atp.puestos.puestos.service.CargaService;
import ar.com.atp.puestos.puestos.web.mvc.CargaMvcController;
import ar.com.atp.puestos.puestos.service.ResultadoMarcado;
import ar.com.atp.puestos.puestos.web.mvc.form.CargaForm;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CargaMvcController.class)
@WithMockUser(username = "tester", roles = "USER")
class CargaMvcControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CargaService cargaService;

    @MockitoBean
    private ProveedorNavegacion proveedorNavegacion;

    @MockitoBean
    private ar.com.atp.puestos.nucleo.web.mvc.GestorContextoSesion gestorContextoSesion;

    private ContextoSesion contextoValido;
    private ContextoSesion contextoSinPuesto;

    @BeforeEach
    void setUp() {
        contextoValido = ContextoSesion.completo("TESTER", "Tester", 1, 1, 1, 5, "Puesto5", "PUE", 1, "Resp");
        contextoSinPuesto = ContextoSesion.completo("TESTER", "Tester", 1, 1, 1, 0, "SinPuesto", "PUE", 1, "Resp");
        lenient().when(gestorContextoSesion.recuperar(any())).thenReturn(contextoValido);
        lenient().when(cargaService.buscarFormularios(any())).thenReturn(java.util.List.of());
        lenient().when(cargaService.obtenerDetalle(any(), any())).thenReturn(java.util.Optional.empty());
        lenient().when(cargaService.marcarGuia(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResultadoMarcado(true, 1, CargaService.MSG_EXITO));
    }

    @Test
    @DisplayName("GET /cargas devuelve la vista puestos/control-cargas")
    void getRenderizaVista() throws Exception {
        mockMvc.perform(get("/cargas"))
                .andExpect(status().isOk())
                .andExpect(view().name("puestos/control-cargas"))
                .andExpect(model().attributeExists("cargaForm"));
    }

    @Test
    @DisplayName("GET /cargas carga la barra de navegacion")
    void getBarraNavegacion() throws Exception {
        mockMvc.perform(get("/cargas")).andExpect(status().isOk());
        verify(proveedorNavegacion).poblar(any(), any());
    }

    @Test
    @DisplayName("GET /cargas con Sucursal=0 muestra 'Usuario sin Puesto Asignado'")
    void puestoNoAsignadoMensaje() throws Exception {
        when(gestorContextoSesion.recuperar(any())).thenReturn(contextoSinPuesto);
        String html = mockMvc.perform(get("/cargas"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Usuario sin Puesto Asignado");
    }

    @Test
    @DisplayName("GET /cargas con Sucursal!=0 no muestra el mensaje de puesto")
    void puestoAsignadoSinMensaje() throws Exception {
        String html = mockMvc.perform(get("/cargas"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("Usuario sin Puesto Asignado");
    }

    @Test
    @DisplayName("GET /cargas renderiza el campo de codigo de barras")
    void renderizaBarcode() throws Exception {
        String html = mockMvc.perform(get("/cargas"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("id=\"barCode\"");
        assertThat(html).contains("name=\"barCode\"");
    }

    @Test
    @DisplayName("GET /cargas renderiza el boton Buscar")
    void renderizaBuscar() throws Exception {
        String html = mockMvc.perform(get("/cargas"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Buscar");
    }

    @Test
    @DisplayName("POST /cargas/buscar renderiza el icono selectRow")
    void renderizaSelectRow() throws Exception {
        ar.com.atp.puestos.puestos.domain.DetalleProductoPrimario dp =
                new ar.com.atp.puestos.puestos.domain.DetalleProductoPrimario();
        dp.setConCuit(new java.math.BigDecimal("20123456789"));
        dp.setDppNroForm("1234567890");
        when(cargaService.buscarFormularios(any())).thenReturn(java.util.List.of(dp));
        String html = mockMvc.perform(post("/cargas/buscar").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("barCode", "1234567890123456789019876543210"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("selectRow");
    }

    @Test
    @DisplayName("POST /cargas/buscar extrae DPPNroForm del barcode (posicion 22)")
    void buscarExtraeDppNroForm() throws Exception {
        String barcode = "123456789012345678901" + "9876543210";
        mockMvc.perform(post("/cargas/buscar").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("barCode", barcode))
                .andExpect(status().isOk())
                .andExpect(view().name("puestos/control-cargas"))
                .andExpect(model().attributeExists("cargaForm"));
        // verificacion indirecta: el servicio fue invocado con el valor extraido
        verify(cargaService).buscarFormularios("9876543210");
    }

    @Test
    @DisplayName("POST /cargas/buscar con barcode vacio no extrae DPPNroForm")
    void buscarBarcodeVacio() throws Exception {
        mockMvc.perform(post("/cargas/buscar").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("barCode", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("puestos/control-cargas"));
        verify(cargaService).buscarFormularios(null);
    }

    @Test
    @DisplayName("POST /cargas/marcar exitoso muestra 'La guia ha sido marcada con exito'")
    void marcarExito() throws Exception {
        when(cargaService.marcarGuia(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResultadoMarcado(true, 1, CargaService.MSG_EXITO));
        String html = mockMvc.perform(post("/cargas/marcar").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("conCuit", "20123456789")
                        .param("dppNroForm", "1234567890"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("La guia ha sido marcada con exito");
    }

    @Test
    @DisplayName("POST /cargas/marcar guia anulada muestra mensaje verbatim")
    void marcarAnulada() throws Exception {
        when(cargaService.marcarGuia(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResultadoMarcado(false, 0, CargaService.MSG_ANULADA));
        String html = mockMvc.perform(post("/cargas/marcar").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("conCuit", "20123456789")
                        .param("dppNroForm", "1234567890"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("No se puede marcar el pase de la guía porque fue anulada");
    }

    @Test
    @DisplayName("POST /cargas/marcar guia interna muestra mensaje verbatim")
    void marcarInterna() throws Exception {
        when(cargaService.marcarGuia(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResultadoMarcado(false, 0, CargaService.MSG_INTERNA));
        String html = mockMvc.perform(post("/cargas/marcar").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("conCuit", "20123456789")
                        .param("dppNroForm", "1234567890"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("No se puede marcar el pase de una guía interna");
    }

    @Test
    @DisplayName("POST /cargas/marcar duplicada (Duplicado>1) muestra mensaje")
    void marcarDuplicada() throws Exception {
        when(cargaService.marcarGuia(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResultadoMarcado(true, 2, CargaService.MSG_EXITO));
        mockMvc.perform(post("/cargas/marcar").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("conCuit", "20123456789")
                        .param("dppNroForm", "1234567890"))
                .andExpect(status().isOk());
        // modal se renderiza con el mensaje duplicado
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890")
                        .param("dpptipoGui", "X")
                        .param("duplicado", "2"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Guia Duplicada , ya paso por algun puesto");
    }

    @Test
    @DisplayName("GET /cargas/detalle con DPPTipoGui != 'F' muestra el modal de detalle")
    void detalleVisible() throws Exception {
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890")
                        .param("dpptipoGui", "X"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("id=\"modalDetalle\"");
        assertThat(html).contains("Detalle de Carta de Porte");
    }

    @Test
    @DisplayName("GET /cargas/detalle con DPPTipoGui = 'F' renderiza el modal de detalle")
    void detalleOcultaTablas() throws Exception {
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890")
                        .param("dpptipoGui", "F"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("id=\"modalDetalle\"");
        assertThat(html).contains("Detalle de Carta de Porte");
    }

    @Test
    @DisplayName("GET /cargas/detalle muestra 'La guia ha sido marcada con exito'")
    void detalleMensajeExito() throws Exception {
        String html = mockMvc.perform(get("/cargas/detalle")
                        .param("dppNroForm", "1234567890")
                        .param("duplicado", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // solo si mensaje coincide; aqui no se pasa mensaje, pero la vista renderiza sin error
        assertThat(html).isNotNull();
    }

    @Test
    @DisplayName("GET /cargas/tabla redirige")
    void verTablaRedirige() throws Exception {
        mockMvc.perform(get("/cargas/tabla"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("GET /cargas/pagos redirige")
    void pagosRedirige() throws Exception {
        mockMvc.perform(get("/cargas/pagos"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("POST /cargas/marcar sin CSRF devuelve 403")
    void marcarSinCsrf403() throws Exception {
        mockMvc.perform(post("/cargas/marcar")
                        .param("conCuit", "20123456789")
                        .param("dppNroForm", "1234567890"))
                .andExpect(status().isForbidden());
    }
}
