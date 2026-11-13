package ar.gob.chaco.atp.puestoscamineros.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.GestorContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.ProveedorNavegacion;
import ar.gob.chaco.atp.puestoscamineros.service.ServicioComisiones;
import ar.gob.chaco.atp.puestoscamineros.service.dto.comision.ComisionFilaView;
import ar.gob.chaco.atp.puestoscamineros.service.dto.comision.ComisionGeneralView;
import ar.gob.chaco.atp.puestoscamineros.service.dto.comision.IntegranteView;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;

@WebMvcTest(controllers = ComisionMvcController.class)
@WithMockUser(username = "tester", roles = "USER")
class ComisionMvcControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicioComisiones servicioComisiones;

    @MockitoBean
    private ProveedorNavegacion proveedorNavegacion;

    @MockitoBean
    private GestorContextoSesion gestorContextoSesion;

    private ContextoSesion contexto;

    @BeforeEach
    void setUp() {
        contexto = ContextoSesion.completo("TESTER", "Tester", 42, 0, 0, 5, "Puesto5", "PUE",
                42, "Resp");

        doAnswer(invocacion -> {
            Model model = invocacion.getArgument(0);
            model.addAttribute("contexto", contexto);
            model.addAttribute("programas", List.of());
            model.addAttribute("rutaActual", "/comisiones");
            return null;
        }).when(proveedorNavegacion).poblar(any(), any());

        lenient().when(gestorContextoSesion.recuperar(any())).thenReturn(contexto);
        lenient().when(servicioComisiones.listar(any(), any(), anyInt()))
                .thenReturn(pagina(List.of(fila()), 1));
    }

    private static ComisionFilaView fila() {
        return new ComisionFilaView(5, 10, "01/01/25", "0800", "02/01/25", "1200", "Abierto",
                "RESPONSABLE");
    }

    private static Page<ComisionFilaView> pagina(List<ComisionFilaView> filas, long total) {
        return new PageImpl<>(filas, PageRequest.of(0, 10), total);
    }

    private String html(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b)
            throws Exception {
        return mockMvc.perform(b).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("GET /comisiones devuelve 200 y la vista puestos/comisiones")
    void getListadoDevuelveVista() throws Exception {
        mockMvc.perform(get("/comisiones"))
                .andExpect(status().isOk())
                .andExpect(view().name("puestos/comisiones"));
    }

    @Test
    @DisplayName("GET /comisiones vuelca el view model")
    void getListadoVuelcaViewModel() throws Exception {
        mockMvc.perform(get("/comisiones"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("comisiones"))
                .andExpect(model().attributeExists("paginaActual"))
                .andExpect(model().attributeExists("cantidadPaginas"))
                .andExpect(model().attributeExists("totalComisiones"))
                .andExpect(model().attributeExists("hayPaginaAnterior"))
                .andExpect(model().attributeExists("hayPaginaSiguiente"))
                .andExpect(model().attributeExists("contexto"))
                .andExpect(model().attributeExists("programas"))
                .andExpect(model().attributeExists("rutaActual"))
                .andExpect(result -> {
                    java.util.Map<String, Object> modelo = result.getModelAndView().getModel();
                    org.assertj.core.api.Assertions.assertThat(modelo.get("mensajeError")).isNull();
                });
    }

    @Test
    @DisplayName("GET /comisiones renderiza las 7 columnas verbatim")
    void getListadoRenderizaColumnas() throws Exception {
        String html = html(get("/comisiones"));
        int inicio = html.indexOf("<thead>");
        int fin = html.indexOf("</thead>");
        assertThat(inicio).isGreaterThan(0);
        String enc = html.substring(inicio, fin);
        int idc = enc.indexOf("Id Comisión");
        int fi = enc.indexOf("Fecha Inicio");
        int hi = enc.indexOf("Hora Inicio");
        int ff = enc.indexOf("Fecha Fin");
        int hf = enc.indexOf("Hora Fin");
        int est = enc.indexOf("Estado");
        int resp = enc.indexOf("Responsable");
        assertThat(idc).isGreaterThan(0);
        assertThat(fi).isGreaterThan(idc);
        assertThat(hi).isGreaterThan(fi);
        assertThat(ff).isGreaterThan(hi);
        assertThat(hf).isGreaterThan(ff);
        assertThat(est).isGreaterThan(hf);
        assertThat(resp).isGreaterThan(est);
    }

    @Test
    @DisplayName("GET /comisiones paginación First/Prev/Next/Last")
    void getListadoPaginacion() throws Exception {
        when(servicioComisiones.listar(any(), any(), anyInt()))
                .thenReturn(pagina(List.of(fila()), 25));
        String html = html(get("/comisiones").param("pagina", "0"));
        assertThat(html).contains("First");
        assertThat(html).contains("Prev");
        assertThat(html).contains("Next");
        assertThat(html).contains("Last");
    }

    @Test
    @DisplayName("GET /comisiones sin resultados muestra estado vacío")
    void getListadoVacio() throws Exception {
        when(servicioComisiones.listar(any(), any(), anyInt()))
                .thenReturn(pagina(List.of(), 0));
        String html = html(get("/comisiones"));
        assertThat(html).contains("No se encontraron registros.");
    }

    @Test
    @DisplayName("GET /comisiones error de carga muestra mensajeError")
    void getListadoError() throws Exception {
        when(servicioComisiones.listar(any(), any(), anyInt()))
                .thenThrow(new RuntimeException("boom"));
        String html = html(get("/comisiones"));
        assertThat(html).contains("No se pudieron mostrar las comisiones.");
    }

    @Test
    @DisplayName("GET /comisiones carga la navegación")
    void getListadoNavegacion() throws Exception {
        mockMvc.perform(get("/comisiones")).andExpect(status().isOk());
        verify(proveedorNavegacion).poblar(any(), any());
    }

    @Test
    @DisplayName("GET /comisiones/{pueCod}/{cpcId} detalle existente renderiza tabs y campos")
    void getDetalleExistente() throws Exception {
        when(servicioComisiones.obtenerDetalle(5, 10)).thenReturn(
                new ComisionGeneralView(5, "Puesto 5", 10, "01/01/25", "0800", "02/01/25", "1200",
                        "Abierto", 42, "RESPONSABLE"));
        when(servicioComisiones.listarIntegrantes(5, 10))
                .thenReturn(List.of(new IntegranteView(7, "JUAN")));

        mockMvc.perform(get("/comisiones/5/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("puestos/comision-detalle"))
                .andExpect(model().attribute("existe", true))
                .andExpect(model().attributeExists("general"))
                .andExpect(model().attributeExists("integrantes"))
                .andExpect(model().attribute("tabActiva", "General"));

        String html = html(get("/comisiones/5/10"));
        assertThat(html).contains("General");
        assertThat(html).contains("Integrantes");
        assertThat(html).contains("Código del Puesto");
        assertThat(html).contains("Puesto Caminero");
        assertThat(html).contains("Id Comisión");
        assertThat(html).contains("Fecha Inicio");
        assertThat(html).contains("Hora Inicio");
        assertThat(html).contains("Fecha Fin");
        assertThat(html).contains("Hora Fin");
        assertThat(html).contains("Estado");
        assertThat(html).contains("Id. Responsable");
        assertThat(html).contains("Responsable");
    }

    @Test
    @DisplayName("GET /comisiones/{pueCod}/{cpcId} detalle inexistente muestra mensaje y oculta tabs")
    void getDetalleInexistente() throws Exception {
        when(servicioComisiones.obtenerDetalle(5, 999)).thenReturn(null);

        mockMvc.perform(get("/comisiones/5/999"))
                .andExpect(status().isOk())
                .andExpect(view().name("puestos/comision-detalle"))
                .andExpect(model().attribute("existe", false))
                .andExpect(model().attribute("mensajeError", "No se encontraron registros"));

        String html = html(get("/comisiones/5/999"));
        assertThat(html).contains("No se encontraron registros");
        // tabs ocultos: no enlace a tab Integrantes
        assertThat(html).doesNotContain("tab='Integrantes'");
    }

    @Test
    @DisplayName("GET /comisiones/{pueCod}/{cpcId} con tab=Integrantes activa esa tab")
    void getDetalleTabIntegrantes() throws Exception {
        when(servicioComisiones.obtenerDetalle(5, 10)).thenReturn(
                new ComisionGeneralView(5, "Puesto 5", 10, "01/01/25", "0800", "02/01/25", "1200",
                        "Abierto", 42, "RESPONSABLE"));
        when(servicioComisiones.listarIntegrantes(5, 10))
                .thenReturn(List.of(new IntegranteView(7, "JUAN")));

        mockMvc.perform(get("/comisiones/5/10").param("tab", "Integrantes"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tabActiva", "Integrantes"));

        String html = html(get("/comisiones/5/10").param("tab", "Integrantes"));
        assertThat(html).contains("Personal Afectado");
        assertThat(html).contains("Ap. y Nombre Persona Comis.");
    }
}
