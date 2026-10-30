package ar.com.atp.puestos.nucleo.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.com.atp.puestos.nucleo.config.RegistradorAutenticacion;
import ar.com.atp.puestos.nucleo.service.ServicioAcceso;
import ar.com.atp.puestos.nucleo.service.ServicioAutorizacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T-301 (parcial) — Pantalla de acceso denegado ({@code NotAuthorized} del legacy).
 */
@WebMvcTest(controllers = AuthMvcController.class)
@Import(GestorContextoSesion.class)
@WithMockUser(username = "tester", roles = "USER")
class AuthMvcNotAuthorizedTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicioAcceso servicioAcceso;

    @MockitoBean
    private ServicioAutorizacion servicioAutorizacion;

    @MockitoBean
    private RegistradorAutenticacion registradorAutenticacion;

    @Test
    @DisplayName("R-LOG-026: GET /not-authorized devuelve la vista nucleo/not-authorized")
    void getNotAuthorizedDevuelveLaVista() throws Exception {
        mockMvc.perform(get("/not-authorized"))
                .andExpect(status().isOk())
                .andExpect(view().name("nucleo/not-authorized"));
    }

    @Test
    @DisplayName("R-LOG-026: el mensaje es 'You are not authorized to do this action'")
    void elMensajeEsElDelLegacy() throws Exception {
        String html = mockMvc.perform(get("/not-authorized"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("You are not authorized to do this action");
    }

    @Test
    @DisplayName("R-LOG-026: la pantalla no expone la traza ni el nombre de la excepción")
    void laPantallaNoExponeLaTraza() throws Exception {
        String html = mockMvc.perform(get("/not-authorized"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("ERROR_INTERNO");
        assertThat(html).doesNotContain("Exception");
    }
}
