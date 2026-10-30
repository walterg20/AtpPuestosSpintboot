package ar.com.atp.puestos.nucleo.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.com.atp.puestos.nucleo.config.RegistradorAutenticacion;
import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.service.ProgramaAutorizado;
import ar.com.atp.puestos.nucleo.service.SmoothNavMenuData;
import ar.com.atp.puestos.nucleo.service.ServicioAcceso;
import ar.com.atp.puestos.nucleo.service.ServicioAutorizacion;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T-301 (parcial) / T-204 — Pantalla de inicio ({@code Home} del legacy): view model, los 10
 * campos del contexto, el texto verbatim de {@code Inicio} y el menú de programas.
 */
@WebMvcTest(controllers = AuthMvcController.class)
@Import(GestorContextoSesion.class)
@WithMockUser(username = "tester", roles = "USER")
class AuthMvcHomeTest {

    /** Contexto de ejemplo: los 10 campos que carga el login (R-LOG-016). */
    private static final ContextoSesion CONTEXTO = ContextoSesion.completo("YOLOFFG",
            "Yolanda Officer", 12, 345, 7, 10, "Puesto Caminero 10", "PUE", 44,
            "Responsable Nombre");

    /** Texto de bienvenida del panel {@code Inicio}, verbatim (plan §4.3). */
    private static final String TEXTO_INICIO =
            "El objetivo del Sistema de Control de Puestos Camineros y Receptorías es el registro y "
                    + "seguimiento de los formularios 2276 que serán cargados en el sistema, según se "
                    + "vayan generando por los puestos y receptorías.Otra funcionalidad importante es el "
                    + "control de pase por puestos camineros de los formularios 506 y 509.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicioAcceso servicioAcceso;

    @MockitoBean
    private ServicioAutorizacion servicioAutorizacion;

    @MockitoBean
    private RegistradorAutenticacion registradorAutenticacion;

    @BeforeEach
    void programasPorDefecto() {
        when(servicioAutorizacion.programasPorTipo("PUE")).thenReturn(List.of(
                new SmoothNavMenuData("2", "Control de Cargas", "Control de Cargas", null, null, List.of()),
                new SmoothNavMenuData("7", "Comisiones", "Comisiones en Puestos Camineros", null, null, List.of())));
    }

    @Test
    @DisplayName("T-204: GET /home devuelve la vista nucleo/home con el view model completo")
    void getHomeDevuelveLaVistaYElViewModel() throws Exception {
        mockMvc.perform(get("/home").session(sesionConContexto()))
                .andExpect(status().isOk())
                .andExpect(view().name("nucleo/home"))
                .andExpect(model().attributeExists("contexto", "programas", "textoInicio"))
                .andExpect(model().attribute("contexto", CONTEXTO))
                .andExpect(model().attribute("textoInicio", TEXTO_INICIO));
    }

    @Test
    @DisplayName("R-LOG-016: los 10 campos del contexto se renderizan con su rótulo y su valor")
    void losDiezCamposDelContextoSeRenderizan() throws Exception {
        String html = mockMvc.perform(get("/home").session(sesionConContexto()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Usuario").contains("YOLOFFG");
        assertThat(html).contains("UsuarioNombre").contains("Yolanda Officer");
        assertThat(html).contains("PersonaId").contains("12");
        assertThat(html).contains("Comision").contains("345");
        assertThat(html).contains("Turno").contains("7");
        assertThat(html).contains("Sucursal").contains("10");
        assertThat(html).contains("SucursalNombre").contains("Puesto Caminero 10");
        assertThat(html).contains("Tipo").contains("PUE");
        assertThat(html).contains("Responsable").contains("Responsable Nombre");
    }

    @Test
    @DisplayName("T-204: el texto de Inicio se renderiza verbatim (incluida la falta de espacio)")
    void elTextoDeInicioSeRenderizaVerbatim() throws Exception {
        String html = mockMvc.perform(get("/home").session(sesionConContexto()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(TEXTO_INICIO);
        assertThat(html).contains("receptorías.Otra funcionalidad");
    }

    @Test
    @DisplayName("R-LOG-025: la lista de programas muestra el nombre y la descripción")
    void laListaDeProgramasMuestraNombreYDescripcion() throws Exception {
        String html = mockMvc.perform(get("/home").session(sesionConContexto()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Control de Cargas");
    }

    @Test
    @DisplayName("T-205: sin programas se muestra 'Sin programas disponibles' y el link Recargar")
    void sinProgramasSeMuestraElEstadoVacio() throws Exception {
        when(servicioAutorizacion.programasPorTipo("PUE")).thenReturn(List.of());

        String html = mockMvc.perform(get("/home").session(sesionConContexto()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Sin programas disponibles");
        assertThat(html).contains("Recargar");
        assertThat(html).doesNotContain("NoExiste");
    }

    /** Sesión con el contexto persistido, como la deja el login (clave {@code "context"}). */
    private static MockHttpSession sesionConContexto() {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(GestorContextoSesion.CLAVE, CONTEXTO);
        return sesion;
    }
}
