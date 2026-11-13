package ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.gob.chaco.atp.puestoscamineros.nucleo.config.RegistradorAutenticacion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.Credencial;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.DatosPeticion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.PerfilAcceso;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ResultadoAcceso;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ServicioAcceso;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ServicioAutorizacion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.UsuarioAutenticado;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.login.LoginForm;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T-301 / T-302 (parcial) — Pantalla de acceso: render del formulario, rótulos verbatim, token
 * CSRF, precarga desde la cookie {@code SUCURSAL}, cookie escrita en el camino de éxito,
 * redirección a {@code /home} y mensajes de error.
 *
 * <p>El servicio de acceso se sustituye por un mock: el controlador no habla con la base de datos
 * (la decisión de acceso vive en {@code ServicioAcceso} y el resultado 0-6 con su mensaje verbatim
 * es su contrato — R-LOG-019).
 */
@WebMvcTest(controllers = AuthMvcController.class)
@Import(GestorContextoSesion.class)
@WithMockUser(username = "tester", roles = "USER")
class AuthMvcLoginTest {

    private static final Pattern ROTULOS = Pattern.compile("<label[^>]*>([^<]*)</label>");

    /** Los 3 rótulos verbatim del legacy, en su orden (spec §3.1). */
    private static final List<String> ROTULOS_VERBATIM = List.of("Acceso", "Usuario", "Contraseña");

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

    // ------------------------------------------------------------------ render

    @Test
    @DisplayName("T-201: GET /login devuelve la vista nucleo/login con el view model completo")
    void getLoginDevuelveLaVistaYElViewModel() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("nucleo/login"))
                .andExpect(model().attributeExists("loginForm", "tipos", "tieneErrores"))
                .andExpect(model().attribute("tieneErrores", false))
                .andExpect(result -> {
                    // `mensajeError` se agrega con valor null en el estado inicial: el atributo
                    // existe pero con null (por eso no se usa `attributeExists`).
                    assertThat(result.getModelAndView().getModel()).containsKey("mensajeError");
                    assertThat(result.getModelAndView().getModel().get("mensajeError")).isNull();
                });
    }

    @Test
    @DisplayName("R-LOG-022: los 3 rótulos verbatim se renderizan en orden: Acceso, Usuario, Contraseña")
    void losTresRotulosSeRenderizanEnOrden() throws Exception {
        String html = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Matcher matcher = ROTULOS.matcher(html);
        List<String> encontrados = new ArrayList<>();
        while (matcher.find()) {
            encontrados.add(matcher.group(1).trim());
        }
        assertThat(encontrados).containsExactlyElementsOf(ROTULOS_VERBATIM);
    }

    @Test
    @DisplayName("R-LOG-022: el botón dice Ingresar, la clave es type=password y el formulario trae _csrf")
    void elFormularioTieneBotonIngresarClaveOcultaYTokenCsrf() throws Exception {
        String html = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(">Ingresar<");
        assertThat(html).contains("type=\"password\"");
        assertThat(html).contains("name=\"_csrf\"");
        assertThat(html).contains("action=\"/login\"");
    }

    @Test
    @DisplayName("T-202: el selector de Acceso ofrece PUE y REC y ninguna opción vacía")
    void elSelectorDeAccesoOfrecePueYRecSinOpcionVacia() throws Exception {
        String html = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("value=\"PUE\"").contains("Puesto Caminero");
        assertThat(html).contains("value=\"REC\"").contains("Receptoría");
        assertThat(html).doesNotContain("<option value=\"\">");
    }

    @Test
    @DisplayName("R-LOG-022: el tipo de acceso se precarga desde la cookie SUCURSAL")
    void elTipoSePrecargaDesdeLaCookieSucursal() throws Exception {
        mockMvc.perform(get("/login").cookie(new Cookie("SUCURSAL", "REC")))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    LoginForm form = (LoginForm) result.getModelAndView().getModel().get("loginForm");
                    assertThat(form.getTipo()).isEqualTo("REC");
                });
    }

    @Test
    @DisplayName("SA-LOG-13: sin cookie, el selector precarga PUE (no hay opción vacía)")
    void sinCookieElSelectorPrecargaPue() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    LoginForm form = (LoginForm) result.getModelAndView().getModel().get("loginForm");
                    assertThat(form.getTipo()).isEqualTo("PUE");
                });
    }

    @Test
    @DisplayName("T-203: el estado inicial no muestra banner de error")
    void elEstadoInicialNoMuestraBannerDeError() throws Exception {
        String html = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("atp-alert--error");
        assertThat(html).doesNotContain("Usuario inválido");
    }

    // ------------------------------------------------------- camino de error de negocio

    @Test
    @DisplayName("R-LOG-021: usuario vacío ⇒ 'Usuario inválido' y el servicio NO se invoca")
    void conUsuarioVacioSeMuestraUsuarioInvalidoYNoSeInvocaElServicio() throws Exception {
        String html = mockMvc.perform(post("/login").with(csrf())
                        .param("usuario", "   ")
                        .param("clave", "CLAVE")
                        .param("tipo", "PUE"))
                .andExpect(status().isOk())
                .andExpect(view().name("nucleo/login"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Usuario inválido");
        assertThat(html).contains("atp-alert--error");
        verifyNoInteractions(servicioAcceso);
    }

    @Test
    @DisplayName("R-LOG-020: el mensaje del código != 0 se muestra junto al campo de contraseña")
    void elMensajeDelCodigoDeErrorSeMuestraJuntoALaContrasena() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.INTERNO), any(), any()))
                .thenReturn(new ResultadoAcceso(2, null, 0, 0));

        String html = mockMvc.perform(post("/login").with(csrf())
                        .param("usuario", "YOLOFFG")
                        .param("clave", "CLAVE")
                        .param("tipo", "PUE"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Usuario/Contraseña Incorrecto");
        assertThat(html).contains("atp-alert--error");
        assertThat(html.indexOf("Contraseña")).isLessThan(html.indexOf("Usuario/Contraseña Incorrecto"));
    }

    @Test
    @DisplayName("T-203: tras un login fallido se conservan el usuario y el tipo, y NO la clave")
    void trasUnLoginFallidoSeConservanUsuarioYTipoYNoLaClave() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.INTERNO), any(), any()))
                .thenReturn(new ResultadoAcceso(2, null, 0, 0));

        String html = mockMvc.perform(post("/login").with(csrf())
                        .param("usuario", "YOLOFFG")
                        .param("clave", "CLAVE")
                        .param("tipo", "REC"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("value=\"YOLOFFG\"");
        assertThat(html).contains("value=\"REC\"");
        assertThat(html).doesNotContain("CLAVE");
    }

    @Test
    @DisplayName("T-203: los errores de binding se muestran junto al campo y deshabilitan Ingresar")
    void losErroresDeBindingSeMuestranJuntoAlCampoYDeshabilitanElBoton() throws Exception {
        String html = mockMvc.perform(post("/login").with(csrf())
                        .param("usuario", "YOLOFFG")
                        .param("clave", "")
                        .param("tipo", "PUE"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("La contraseña es obligatoria.");
        assertThat(html).contains("disabled");
        assertThat(html.indexOf("Contraseña")).isLessThan(html.indexOf("La contraseña es obligatoria."));
        verifyNoInteractions(servicioAcceso);
    }

    // ------------------------------------------------------------ camino de éxito

    @Test
    @DisplayName("R-LOG-023: el login exitoso escribe la cookie SUCURSAL y redirige a /home")
    void elLoginExitosoEscribeLaCookieSucursalYRedirigeAHome() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.INTERNO), any(), any()))
                .thenReturn(new ResultadoAcceso(0, CONTEXTO, 10, 345));

        var resultado = mockMvc.perform(post("/login").with(csrf())
                        .param("usuario", "YOLOFFG")
                        .param("clave", "CLAVE")
                        .param("tipo", "PUE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andReturn();

        String cookie = cookieSucursal(resultado.getResponse().getHeaders(HttpHeaders.SET_COOKIE));
        assertThat(cookie).isNotNull();
        assertThat(cookie).contains("SUCURSAL=PUE");
        assertThat(cookie).contains("HttpOnly");
        assertThat(cookie).contains("SameSite=Lax");
        assertThat(cookie).contains("Path=/");
        assertThat(cookie).doesNotContain("Max-Age");
    }

    @Test
    @DisplayName("R-LOG-023: el login fallido NO escribe la cookie SUCURSAL")
    void elLoginFallidoNoEscribeLaCookieSucursal() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.INTERNO), any(), any()))
                .thenReturn(new ResultadoAcceso(2, null, 0, 0));

        var resultado = mockMvc.perform(post("/login").with(csrf())
                        .param("usuario", "YOLOFFG")
                        .param("clave", "CLAVE")
                        .param("tipo", "PUE"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(cookieSucursal(resultado.getResponse().getHeaders(HttpHeaders.SET_COOKIE))).isNull();
    }

    @Test
    @DisplayName("R-LOG-017: el login exitoso guarda el contexto en la sesión bajo la clave 'context'")
    void elLoginExitosoGuardaElContextoEnLaSesion() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.INTERNO), any(), any()))
                .thenReturn(new ResultadoAcceso(0, CONTEXTO, 10, 345));

        var resultado = mockMvc.perform(post("/login").with(csrf())
                        .param("usuario", "YOLOFFG")
                        .param("clave", "CLAVE")
                        .param("tipo", "PUE"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession sesion = (MockHttpSession) resultado.getRequest().getSession(false);
        assertThat(sesion).isNotNull();
        assertThat(sesion.getAttribute("context")).isEqualTo(CONTEXTO);
    }

    @Test
    @DisplayName("R-LOG-017: la autenticación se registra con el usuario en mayúsculas")
    void laAutenticacionSeRegistraConElUsuarioEnMayusculas() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.INTERNO), any(), any()))
                .thenReturn(new ResultadoAcceso(0, CONTEXTO, 10, 345));

        mockMvc.perform(post("/login").with(csrf())
                        .param("usuario", "yoloffg")
                        .param("clave", "CLAVE")
                        .param("tipo", "PUE"))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<UsuarioAutenticado> principal = ArgumentCaptor.forClass(UsuarioAutenticado.class);
        verify(registradorAutenticacion).registrar(any(), any(), principal.capture());
        assertThat(principal.getValue().getName()).isEqualTo("YOLOFFG");
        assertThat(principal.getValue().contexto()).isEqualTo(CONTEXTO);
    }

    @Test
    @DisplayName("D-LOG-15: el servicio se invoca con el perfil INTERNO y la credencial del formulario")
    void elServicioSeInvocaConElPerfilInternoYLaCredencial() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.INTERNO), any(), any()))
                .thenReturn(new ResultadoAcceso(0, CONTEXTO, 10, 345));

        mockMvc.perform(post("/login").with(csrf())
                        .param("usuario", "YOLOFFG")
                        .param("clave", "CLAVE")
                        .param("tipo", "REC"))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<Credencial> credencial = ArgumentCaptor.forClass(Credencial.class);
        verify(servicioAcceso).autenticar(eq(PerfilAcceso.INTERNO), credencial.capture(), any());
        assertThat(credencial.getValue().usuario()).isEqualTo("YOLOFFG");
        assertThat(credencial.getValue().clave()).isEqualTo("CLAVE");
        assertThat(credencial.getValue().tipo()).isEqualTo("REC");
    }

    @Test
    @DisplayName("R-LOG-013: la IP del cliente se resuelve de X-Forwarded-For")
    void laIpDelClienteSeResuelveDeXForwardedFor() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.INTERNO), any(), any()))
                .thenReturn(new ResultadoAcceso(0, CONTEXTO, 10, 345));

        mockMvc.perform(post("/login").with(csrf())
                        .header("X-Forwarded-For", "10.13.26.7")
                        .param("usuario", "YOLOFFG")
                        .param("clave", "CLAVE")
                        .param("tipo", "PUE"))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<DatosPeticion> datos = ArgumentCaptor.forClass(DatosPeticion.class);
        verify(servicioAcceso).autenticar(eq(PerfilAcceso.INTERNO), any(), datos.capture());
        assertThat(datos.getValue().ipCliente()).isEqualTo("10.13.26.7");
    }

    @Test
    @DisplayName("R-LOG-013: sin X-Forwarded-For se usa la dirección remota de la conexión")
    void sinXForwardedForSeUsaLaDireccionRemota() throws Exception {
        when(servicioAcceso.autenticar(eq(PerfilAcceso.INTERNO), any(), any()))
                .thenReturn(new ResultadoAcceso(0, CONTEXTO, 10, 345));

        mockMvc.perform(post("/login").with(csrf())
                        .param("usuario", "YOLOFFG")
                        .param("clave", "CLAVE")
                        .param("tipo", "PUE"))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<DatosPeticion> datos = ArgumentCaptor.forClass(DatosPeticion.class);
        verify(servicioAcceso).autenticar(eq(PerfilAcceso.INTERNO), any(), datos.capture());
        assertThat(datos.getValue().ipCliente()).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("R-LOG-021: con usuario vacío el servicio de acceso nunca se invoca")
    void conUsuarioVacioElServicioNuncaSeInvoca() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("usuario", "")
                        .param("clave", "CLAVE")
                        .param("tipo", "PUE"))
                .andExpect(status().isOk());

        verify(servicioAcceso, never()).autenticar(any(), any(), any());
        verifyNoInteractions(servicioAcceso);
    }

    // ------------------------------------------------------------------ helpers

    /** Devuelve el valor de la cabecera {@code Set-Cookie} de la cookie {@code SUCURSAL}. */
    private static String cookieSucursal(List<String> cabeceras) {
        for (String cabecera : cabeceras) {
            if (cabecera.startsWith("SUCURSAL=")) {
                return cabecera;
            }
        }
        return null;
    }
}
