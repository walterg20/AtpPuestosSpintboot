package ar.com.atp.puestos.puestos.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.com.atp.puestos.nucleo.domain.Turno;
import ar.com.atp.puestos.nucleo.domain.TurnoId;
import ar.com.atp.puestos.nucleo.repository.TurnoRepositorio;
import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.GestorContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.ProveedorNavegacion;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;

@WebMvcTest(controllers = CargaManualController.class)
@WithMockUser(username = "tester", roles = "USER")
class TurnosAbiertosMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TurnoRepositorio turnoRepositorio;

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
            model.addAttribute("rutaActual", "/actas/carga-manual");
            return null;
        }).when(proveedorNavegacion).poblar(any(), any());

        lenient().when(gestorContextoSesion.recuperar(any())).thenReturn(contextoConTurno);
    }

    private static Turno turno(int turNroId) {
        Turno t = new Turno();
        t.setId(new TurnoId(5, "PUE", turNroId));
        t.setTurFchApe(LocalDateTime.of(2020, 1, 15, 8, 0));
        t.setTurEstado("A");
        return t;
    }

    private static Page<Turno> pagina(List<Turno> filas, long total) {
        return new PageImpl<>(filas, PageRequest.of(0, 10, Sort.by("sucNroId", "sucTipo", "turNroId")), total);
    }

    @Test
    @DisplayName("GET /actas/carga-manual devuelve 200 y la vista puestos/turnos-abiertos")
    void getTurnosAbiertosDevuelveLaVista() throws Exception {
        when(turnoRepositorio.listarAbiertosPorSucursal(any(), any(), any()))
                .thenReturn(pagina(List.of(turno(7), turno(8)), 2));

        mockMvc.perform(get("/actas/carga-manual"))
                .andExpect(status().isOk())
                .andExpect(view().name("puestos/turnos-abiertos"));
    }

    @Test
    @DisplayName("GET /actas/carga-manual vuelca turnos y contexto al modelo")
    void getTurnosAbiertosVuelcaElViewModel() throws Exception {
        when(turnoRepositorio.listarAbiertosPorSucursal(any(), any(), any()))
                .thenReturn(pagina(List.of(turno(7)), 1));

        mockMvc.perform(get("/actas/carga-manual"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("turnos"))
                .andExpect(model().attributeExists("contexto"))
                .andExpect(model().attributeExists("programas"))
                .andExpect(model().attributeExists("rutaActual"));
    }

    @Test
    @DisplayName("GET /actas/carga-manual carga la navegacion (ProveedorNavegacion)")
    void getTurnosAbiertosCargaLaNavegacion() throws Exception {
        when(turnoRepositorio.listarAbiertosPorSucursal(any(), any(), any()))
                .thenReturn(pagina(List.of(turno(7)), 1));

        mockMvc.perform(get("/actas/carga-manual")).andExpect(status().isOk());
        org.mockito.Mockito.verify(proveedorNavegacion).poblar(any(), any());
    }

    @Test
    @DisplayName("GET /actas/carga-manual renderiza los encabezados Turno, Fecha apertura, Seleccionar")
    void getTurnosAbiertosRenderizaLosEncabezados() throws Exception {
        when(turnoRepositorio.listarAbiertosPorSucursal(any(), any(), any()))
                .thenReturn(pagina(List.of(turno(7)), 1));

        String html = html(get("/actas/carga-manual"));
        int inicio = html.indexOf("<thead>");
        int fin = html.indexOf("</thead>");
        assertThat(inicio).isGreaterThan(0);
        assertThat(fin).isGreaterThan(inicio);
        String encabezado = html.substring(inicio, fin);
        assertThat(encabezado).contains("Turno");
        assertThat(encabezado).contains("Fecha apertura");
        assertThat(encabezado).contains("Seleccionar");
    }

    @Test
    @DisplayName("GET /actas/carga-manual renderiza el enlace Seleccionar con href a /actas/nueva?turNroId=...&manual=true")
    void getTurnosAbiertosRenderizaElEnlaceSeleccionar() throws Exception {
        when(turnoRepositorio.listarAbiertosPorSucursal(any(), any(), any()))
                .thenReturn(pagina(List.of(turno(7)), 1));

        String html = html(get("/actas/carga-manual"));
        assertThat(html).contains("Seleccionar");
        assertThat(html).contains("/actas/nueva?turNroId=7");
        assertThat(html).contains("manual=true");
    }

    @Test
    @DisplayName("GET /actas/carga-manual sin turnos muestra el estado vacio")
    void getTurnosAbiertosSinTurnosMuestraEstadoVacio() throws Exception {
        when(turnoRepositorio.listarAbiertosPorSucursal(any(), any(), any()))
                .thenReturn(pagina(List.of(), 0));

        String html = html(get("/actas/carga-manual"));
        assertThat(html).contains("Sin turnos abiertos.");
    }

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
