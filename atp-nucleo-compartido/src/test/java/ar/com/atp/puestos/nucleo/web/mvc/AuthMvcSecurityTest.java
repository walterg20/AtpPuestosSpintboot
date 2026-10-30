package ar.com.atp.puestos.nucleo.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.com.atp.puestos.nucleo.config.RegistradorAutenticacion;
import ar.com.atp.puestos.nucleo.service.ResultadoAcceso;
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
 * T-301 (parcelas de seguridad) — Protección de rutas, CSRF y logout.
 *
 * <p>La clase <strong>no</strong> lleva {@code @WithMockUser}: las primeras pruebas son las del
 * usuario anónimo (petición sin sesión).
 */
@WebMvcTest(controllers = AuthMvcController.class)
@Import(GestorContextoSesion.class)
class AuthMvcSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicioAcceso servicioAcceso;

    @MockitoBean
    private ServicioAutorizacion servicioAutorizacion;

    @MockitoBean
    private RegistradorAutenticacion registradorAutenticacion;

    @Test
    @DisplayName("R-LOG-024: GET /home sin sesión redirige (302) a /login")
    void getHomeSinSesionRedirigeALogin() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                        .contains("/login"));
    }

    @Test
    @DisplayName("R-LOG-026: GET /not-authorized sin sesión redirige (302) a /login")
    void getNotAuthorizedSinSesionRedirigeALogin() throws Exception {
        mockMvc.perform(get("/not-authorized"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                        .contains("/login"));
    }

    @Test
    @DisplayName("R-LOG-031: POST /login sin token CSRF devuelve 403")
    void postLoginSinTokenCsrfDevuelve403() throws Exception {
        mockMvc.perform(post("/login")
                        .param("usuario", "YOLOFFG")
                        .param("clave", "CLAVE")
                        .param("tipo", "PUE"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("R-LOG-031: POST /login con token CSRF no devuelve 403")
    void postLoginConTokenCsrfNoDevuelve403() throws Exception {
        when(servicioAcceso.autenticar(any(), any(), any()))
                .thenReturn(new ResultadoAcceso(2, null, 0, 0));

        mockMvc.perform(post("/login").with(csrf())
                        .param("usuario", "YOLOFFG")
                        .param("clave", "CLAVE")
                        .param("tipo", "PUE"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(403));
    }

    @Test
    @DisplayName("R-LOG-031: POST /logout con token CSRF invalida la sesión y redirige a /login")
    void postLogoutConTokenCsrfRedirigeALogin() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                        .contains("/login"));
    }

    @Test
    @DisplayName("R-LOG-031: POST /logout sin token CSRF devuelve 403")
    void postLogoutSinTokenCsrfDevuelve403() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Plan §3.8: GET / sin sesión redirige (302) a /login")
    void getRaizSinSesionRedirigeALogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                        .contains("/login"));
    }

    @Test
    @DisplayName("Plan §3.8: GET / con sesión redirige (302) a /home")
    @WithMockUser(username = "tester", roles = "USER")
    void getRaizConSesionRedirigeAHome() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }
}
