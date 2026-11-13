package ar.gob.chaco.atp.puestoscamineros.nucleo.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.Credencial;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.DatosPeticion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.PerfilAcceso;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ResultadoAcceso;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ServicioAcceso;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T-120 / T-121 / T-303: contrato del canal WS ({@code POST /api/v1/auth/login}).
 *
 * <p>El canal WS replica {@code Seg_ControlAccesoWS}: es <strong>stateless</strong>, no crea
 * sesión, no escribe cookies, no carga contexto y no exige token CSRF (R-LOG-028). La respuesta es
 * <strong>siempre HTTP 200</strong> con la forma del SDT {@code SDTSalidaLoginWS}.
 */
@WebMvcTest(controllers = AuthController.class)
class AuthControllerTest {

    private static final String BODY_CANONICO =
            "{\"usuario\":\"YOLOFFG\",\"pass\":\"CLAVE\",\"tipo\":\"PUE\"}";

    private static final String BODY_CON_ALIAS_LEGACY =
            "{\"Usuario\":\"YOLOFFG\",\"Pass\":\"CLAVE\",\"Tipo\":\"REC\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicioAcceso servicioAcceso;

    @Test
    @DisplayName("R-LOG-027: un login valido responde 200 con el JSON exacto del SDT")
    void unLoginValidoResponde200ConElJsonDelSdt() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.WS), any(), any()))
                .thenReturn(new ResultadoAcceso(0, null, 10, 123));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_CANONICO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(0))
                .andExpect(jsonPath("$.sucursal").value(10))
                .andExpect(jsonPath("$.comision").value(123));
    }

    @Test
    @DisplayName("R-LOG-027/029: un login fallido responde 200 con error = 2 (nunca 401)")
    void unLoginFallidoResponde200ConError2() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.WS), any(), any()))
                .thenReturn(new ResultadoAcceso(2, null, 0, 0));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_CANONICO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(2))
                .andExpect(jsonPath("$.sucursal").value(0))
                .andExpect(jsonPath("$.comision").value(0));
    }

    @Test
    @DisplayName("R-LOG-028: la respuesta NO incluye Set-Cookie (el canal es stateless)")
    void laRespuestaNoIncluyeSetCookie() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.WS), any(), any()))
                .thenReturn(new ResultadoAcceso(0, null, 10, 123));

        List<String> cookies = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_CANONICO))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE);

        assertThat(cookies).isEmpty();
    }

    @Test
    @DisplayName("R-LOG-028: una segunda llamada no hereda estado")
    void unaSegundaLlamadaNoHeredaEstado() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.WS), any(), any()))
                .thenReturn(new ResultadoAcceso(0, null, 10, 123));

        String primera = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_CANONICO))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String segunda = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_CANONICO))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(primera).isEqualTo(segunda);
        assertThat(primera).contains("\"sucursal\":10").contains("\"comision\":123");
    }

    @Test
    @DisplayName("SA-LOG-12: el body con los alias legacy (Usuario/Pass/Tipo) se acepta")
    void elBodyConAliasLegacySeAcepta() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.WS), any(), any()))
                .thenReturn(new ResultadoAcceso(0, null, 5, 0));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_CON_ALIAS_LEGACY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(0));

        ArgumentCaptor<Credencial> captor = ArgumentCaptor.forClass(Credencial.class);
        verify(servicioAcceso).autenticar(eq(PerfilAcceso.WS), captor.capture(), any());
        assertThat(captor.getValue().usuario()).isEqualTo("YOLOFFG");
        assertThat(captor.getValue().clave()).isEqualTo("CLAVE");
        assertThat(captor.getValue().tipo()).isEqualTo("REC");
    }

    @Test
    @DisplayName("R-LOG-028: el canal invoca el perfil WS y no pasa IP (el codigo 6 no es producible)")
    void elCanalInvocaElPerfilWsSinIp() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.WS), any(), any()))
                .thenReturn(new ResultadoAcceso(0, null, 10, 123));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_CANONICO))
                .andExpect(status().isOk());

        ArgumentCaptor<DatosPeticion> captor = ArgumentCaptor.forClass(DatosPeticion.class);
        verify(servicioAcceso).autenticar(eq(PerfilAcceso.WS), any(), captor.capture());
        assertThat(captor.getValue().ipCliente()).isNull();
    }

    @Test
    @DisplayName("R-LOG-027: un body invalido responde 400 con el codigo del advice global")
    void unBodyInvalidoResponde400ConElCodigoDelAdvice() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"\",\"pass\":\"CLAVE\",\"tipo\":\"PUE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
    }
}
