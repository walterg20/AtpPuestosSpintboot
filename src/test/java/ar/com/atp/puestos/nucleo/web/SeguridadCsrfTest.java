package ar.gob.chaco.atp.puestoscamineros.nucleo.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.PerfilAcceso;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ResultadoAcceso;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ServicioAcceso;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.rest.AuthController;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T-312 / T-127 / T-136: los códigos de seguridad de la política aprobada (D-LOG-17(a) y
 * D-LOG-19(a)).
 *
 * <ol>
 *   <li>(a) una escritura <strong>MVC</strong> sin token CSRF ⇒ <strong>403</strong>;</li>
 *   <li>(b) una escritura a <strong>{@code /api/**}</strong> sin token CSRF y sin credenciales ⇒
 *       <strong>no</strong> es rechazada por CSRF: responde el código de negocio;</li>
 *   <li>(c) {@code POST /api/v1/auth/login} sin token ⇒ <strong>200</strong> con el cuerpo del
 *       SDT;</li>
 *   <li>(d) ninguno devuelve <strong>500</strong> ni la página {@code ERROR_INTERNO}.</li>
 * </ol>
 */
@WebMvcTest(controllers = AuthController.class)
class SeguridadCsrfTest {

    private static final String BODY =
            "{\"usuario\":\"YOLOFFG\",\"pass\":\"CLAVE\",\"tipo\":\"PUE\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicioAcceso servicioAcceso;

    @Test
    @DisplayName("T-312 (a): una escritura MVC sin token CSRF devuelve 403")
    void unaEscrituraMvcSinTokenCsrfDevuelve403() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("T-312 (b): una escritura a /api/** sin token CSRF ni credenciales NO es rechazada")
    void unaEscrituraApiSinTokenNiCredencialesNoEsRechazada() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.WS), any(), any()))
                .thenReturn(new ResultadoAcceso(2, null, 0, 0));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(2));
    }

    @Test
    @DisplayName("T-312 (c): POST /api/v1/auth/login sin token devuelve 200 con el cuerpo del SDT")
    void elLoginWsSinTokenDevuelve200ConElCuerpoDelSdt() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.WS), any(), any()))
                .thenReturn(new ResultadoAcceso(0, null, 10, 123));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(0))
                .andExpect(jsonPath("$.sucursal").value(10))
                .andExpect(jsonPath("$.comision").value(123));
    }

    @Test
    @DisplayName("T-312 (d): ninguno de los casos devuelve 500 ni la pagina ERROR_INTERNO")
    void ningunoDevuelve500NiLaPaginaDeErrorInterno() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.WS), any(), any()))
                .thenReturn(new ResultadoAcceso(2, null, 0, 0));

        String cuerpoMvc = mockMvc.perform(post("/logout"))
                .andReturn().getResponse().getContentAsString();
        String cuerpoApi = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpoMvc).doesNotContain("ERROR_INTERNO");
        assertThat(cuerpoApi).doesNotContain("ERROR_INTERNO");
        assertThat(mockMvc.perform(post("/logout")).andReturn().getResponse().getStatus())
                .isNotEqualTo(500);
    }

    @Test
    @DisplayName("T-136 / R-LOG-041 (c): una peticion a /api/v1/** no crea sesion (sin JSESSIONID)")
    void unaPeticionApiNoCreaSesion() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.WS), any(), any()))
                .thenReturn(new ResultadoAcceso(0, null, 10, 123));

        List<String> cookies = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE);

        assertThat(cookies).isEmpty();
    }
}
