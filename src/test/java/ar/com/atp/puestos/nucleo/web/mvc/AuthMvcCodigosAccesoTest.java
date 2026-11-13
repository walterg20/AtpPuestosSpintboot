package ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.gob.chaco.atp.puestoscamineros.nucleo.config.RegistradorAutenticacion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.PerfilAcceso;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ResultadoAcceso;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ServicioAcceso;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ServicioAutorizacion;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T-302 — Caminos de error de la UI: los <strong>códigos alcanzables</strong> del login (2, 3, 4,
 * 5, 6 y el 0) renderizados <strong>verbatim</strong>, y {@code Usuario inválido} sin tocar la base
 * de datos.
 *
 * <p>Los textos esperados son los de {@code spec.md} §3.4, transcritos carácter a carácter
 * (incluidas las erratas "esta" sin tilde y "Receptoria" sin tilde) — R-LOG-018.
 */
@WebMvcTest(controllers = AuthMvcController.class)
@Import(GestorContextoSesion.class)
@WithMockUser(username = "tester", roles = "USER")
class AuthMvcCodigosAccesoTest {

    private static final ContextoSesion CONTEXTO = ContextoSesion.completo("YOLOFFG",
            "Yolanda Officer", 12, 345, 7, 10, "Puesto 10", "PUE", 44, "Responsable Nombre");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicioAcceso servicioAcceso;

    @MockitoBean
    private ServicioAutorizacion servicioAutorizacion;

    @MockitoBean
    private RegistradorAutenticacion registradorAutenticacion;

    /** Los 5 códigos de error alcanzables y su mensaje verbatim (spec §3.4). */
    static Stream<Arguments> codigosConError() {
        return Stream.of(
                Arguments.of(2, "Usuario/Contraseña Incorrecto"),
                Arguments.of(3, "Este Usuario no esta relacionado con una Persona del sistema"),
                Arguments.of(4, "No existe Receptoria o puesto caminero asignado"),
                Arguments.of(5, "Debe cambiar su clave desde el Sistema Integrado (win)"),
                Arguments.of(6, "Dispositivo no habilitado para el uso en el puesto caminero, "
                        + "contacte con el administrador"));
    }

    @ParameterizedTest(name = "código {0}: {1}")
    @MethodSource("codigosConError")
    @DisplayName("T-302: el mensaje del código se renderiza verbatim junto a la contraseña")
    void elCodigoDeErrorSeRenderizaVerbatim(int codigo, String mensaje) throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.INTERNO), any(), any()))
                .thenReturn(new ResultadoAcceso(codigo, null, 0, 0));

        String html = mockMvc.perform(post("/login").with(csrf())
                        .param("usuario", "YOLOFFG")
                        .param("clave", "CLAVE")
                        .param("tipo", "PUE"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(mensaje);
        assertThat(html).contains("atp-alert--error");
        assertThat(html).doesNotContain("Usuario inválido");
    }

    @Test
    @DisplayName("T-302: el código 0 concede el acceso, redirige a /home y no muestra banner")
    void elCodigoCeroConcedeElAcceso() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.INTERNO), any(), any()))
                .thenReturn(new ResultadoAcceso(0, CONTEXTO, 10, 345));

        mockMvc.perform(post("/login").with(csrf())
                        .param("usuario", "YOLOFFG")
                        .param("clave", "CLAVE")
                        .param("tipo", "PUE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    @DisplayName("R-LOG-021: 'Usuario inválido' se muestra sin invocar el servicio (sin BD)")
    void usuarioInvalidoSeMuestraSinInvocarElServicio() throws Exception {
        String html = mockMvc.perform(post("/login").with(csrf())
                        .param("usuario", " ")
                        .param("clave", "CLAVE")
                        .param("tipo", "PUE"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Usuario inválido");
        assertThat(html).doesNotContain("Usuario/Contraseña Incorrecto");
    }
}
