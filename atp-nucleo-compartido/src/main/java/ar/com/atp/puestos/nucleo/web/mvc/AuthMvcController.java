package ar.com.atp.puestos.nucleo.web.mvc;

import ar.com.atp.puestos.nucleo.config.RegistradorAutenticacion;
import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.service.Credencial;
import ar.com.atp.puestos.nucleo.service.DatosPeticion;
import ar.com.atp.puestos.nucleo.service.PerfilAcceso;
import ar.com.atp.puestos.nucleo.service.ProgramaAutorizado;
import ar.com.atp.puestos.nucleo.service.SmoothNavMenuData;
import ar.com.atp.puestos.nucleo.service.ResultadoAcceso;
import ar.com.atp.puestos.nucleo.service.ServicioAcceso;
import ar.com.atp.puestos.nucleo.service.ServicioAutorizacion;
import ar.com.atp.puestos.nucleo.service.UsuarioAutenticado;
import ar.com.atp.puestos.nucleo.web.mvc.login.LoginForm;
import ar.com.atp.puestos.nucleo.web.mvc.login.TipoAcceso;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Pantallas de acceso del sistema (spec 03-login §3.2, §3.6 y §3.7; plan §3.2 y §4.3).
 *
 * <p>Rutas (plan §3.8):
 *
 * <table>
 *   <caption>Rutas del canal MVC</caption>
 *   <tr><th>Método</th><th>Ruta</th><th>Vista</th><th>Respuesta</th></tr>
 *   <tr><td>GET</td><td>{@code /login}</td><td>{@code nucleo/login}</td><td>formulario</td></tr>
 *   <tr><td>POST</td><td>{@code /login}</td><td>{@code nucleo/login}</td><td>200 (error) o 302 a
 *       {@code /home}</td></tr>
 *   <tr><td>GET</td><td>{@code /home}</td><td>{@code nucleo/home}</td><td>pantalla de inicio</td></tr>
 *   <tr><td>GET</td><td>{@code /not-authorized}</td><td>{@code nucleo/not-authorized}</td><td>
 *       mensaje</td></tr>
 *   <tr><td>GET</td><td>{@code /}</td><td>—</td><td>302 a {@code /home}</td></tr>
 * </table>
 *
 * <p>El {@code POST /logout} <strong>no</strong> lo implementa este controlador: lo resuelve la
 * {@code SecurityFilterChain} del núcleo ({@code nucleo.config.SeguridadConfig}), que invalida la
 * sesión, borra la cookie {@code SUCURSAL} y redirige a {@code /login}.
 *
 * <p><strong>El controlador no lleva reglas de negocio</strong>: la decisión de acceso vive en
 * {@link ServicioAcceso} (que devuelve el código 0-6 del legacy y su mensaje verbatim) y este
 * adaptador sólo mapea la ruta, invoca el servicio, arma el view model y devuelve el nombre de la
 * vista (D-LOG-16, R-LOG-019).
 *
 * <p>Dos decisiones del plan se aplican acá porque son del <strong>adaptador</strong>:
 *
 * <ul>
 *   <li>la cookie {@code SUCURSAL} se escribe <strong>en el controlador</strong>, sólo en el camino
 *       de éxito, y la IP del cliente se resuelve en el adaptador (D-LOG-07, D-LOG-15);</li>
 *   <li>la autenticación se registra con {@link RegistradorAutenticacion}, que aplica la mitigación
 *       de fijación de sesión ({@code changeSessionId}).</li>
 * </ul>
 */
@Controller
public class AuthMvcController {

    /** Vista del formulario de acceso. */
    private static final String VISTA_LOGIN = "nucleo/login";

    /** Vista de la pantalla de inicio ({@code Home} / {@code Inicio} del legacy). */
    private static final String VISTA_HOME = "nucleo/home";

    /** Vista de la pantalla {@code Inicio} del legacy (texto descriptivo). */
    private static final String VISTA_INICIO = "nucleo/inicio";

    /** Vista de la pantalla de acceso denegado ({@code NotAuthorized} del legacy). */
    private static final String VISTA_NO_AUTORIZADO = "nucleo/not-authorized";

    /** Ruta de la pantalla de inicio. */
    private static final String RUTA_HOME = "/home";

    /** Ruta del formulario de acceso. */
    private static final String RUTA_LOGIN = "/login";

    /** Cookie del tipo de acceso (paridad con {@code GetCookie('SUCURSAL')}). */
    private static final String COOKIE_SUCURSAL = "SUCURSAL";

    /** Cabecera con la IP del cliente cuando hay un proxy delante (paridad con el legacy). */
    private static final String CABECERA_IP = "X-Forwarded-For";

    /** Mensaje verbatim del legacy cuando el identificador viene vacío (R-LOG-021). */
    private static final String MENSAJE_USUARIO_INVALIDO = "Usuario inválido";

    /** Mensaje verbatim de la pantalla {@code NotAuthorized} (R-LOG-026). */
    private static final String MENSAJE_NO_AUTORIZADO = "You are not authorized to do this action";

    /**
     * Texto de bienvenida del panel {@code Inicio} del legacy, <strong>verbatim</strong>
     * (incluida la falta de espacio antes de "Otra"): plan §4.3.
     */
    private static final String TEXTO_INICIO =
            "El objetivo del Sistema de Control de Puestos Camineros y Receptorías es el registro y "
                    + "seguimiento de los formularios 2276 que serán cargados en el sistema, según se "
                    + "vayan generando por los puestos y receptorías.Otra funcionalidad importante es el "
                    + "control de pase por puestos camineros de los formularios 506 y 509.";

    private static final Logger log = LoggerFactory.getLogger(AuthMvcController.class);

    private final ServicioAcceso servicioAcceso;
    private final ServicioAutorizacion servicioAutorizacion;
    private final RegistradorAutenticacion registradorAutenticacion;
    private final GestorContextoSesion gestorContextoSesion;

    public AuthMvcController(ServicioAcceso servicioAcceso,
            ServicioAutorizacion servicioAutorizacion,
            RegistradorAutenticacion registradorAutenticacion,
            GestorContextoSesion gestorContextoSesion) {
        this.servicioAcceso = servicioAcceso;
        this.servicioAutorizacion = servicioAutorizacion;
        this.registradorAutenticacion = registradorAutenticacion;
        this.gestorContextoSesion = gestorContextoSesion;
    }

    /**
     * Formulario de acceso (R-LOG-022).
     *
     * <p>Precarga el selector de tipo con el valor de la cookie {@code SUCURSAL} (si existe) y
     * renderiza la vista con {@code mensajeError = null} (estado inicial).
     *
     * @param request petición actual
     * @param model modelo de la vista
     * @return el nombre de la vista {@code nucleo/login}
     */
    @GetMapping("/login")
    public String loginForm(HttpServletRequest request, Model model) {
        LoginForm form = new LoginForm();
        form.setUsuario("");
        form.setTipo(tipoDesdeCookie(request));
        poblarLogin(model, form, null, false);
        return VISTA_LOGIN;
    }

    /**
     * Intento de acceso (R-LOG-019, R-LOG-020, R-LOG-021, R-LOG-023).
     *
     * <ol>
     *   <li>identificador vacío ⇒ {@code Usuario inválido} <strong>sin</strong> tocar la base de
     *       datos (R-LOG-021);</li>
     *   <li>errores de binding ⇒ se re-renderiza el formulario con los mensajes por campo
     *       (HTTP 200);</li>
     *   <li>el servicio devuelve un código 0-6: si no es 0, se re-renderiza con el mensaje
     *       verbatim (HTTP 200);</li>
     *   <li>si es 0: se registra la autenticación, se guarda el contexto en la sesión, se escribe
     *       la cookie {@code SUCURSAL} y se redirige a {@code /home}.</li>
     * </ol>
     *
     * @param form objeto de binding del formulario
     * @param bindingResult errores de validación
     * @param request petición actual
     * @param response respuesta actual
     * @param model modelo de la vista
     * @return la vista {@code nucleo/login} o la redirección a {@code /home}
     */
    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        if (isVacio(form.getUsuario())) {
            // R-LOG-021: mensaje propio y NINGUNA consulta a la base de datos.
            form.setClave(null);
            poblarLogin(model, form, MENSAJE_USUARIO_INVALIDO, false);
            return VISTA_LOGIN;
        }
        if (bindingResult.hasErrors()) {
            form.setClave(null);
            poblarLogin(model, form, null, true);
            return VISTA_LOGIN;
        }

        Credencial credencial =
                new Credencial(form.getUsuario(), form.getClave(), form.getTipo());
        ResultadoAcceso resultado = servicioAcceso.autenticar(PerfilAcceso.INTERNO, credencial,
                new DatosPeticion(resolverIp(request)));

        if (!resultado.concedido()) {
            // R-LOG-020: el mensaje verbatim va junto al campo de contraseña.
            form.setClave(null);
            poblarLogin(model, form, resultado.mensaje(), false);
            return VISTA_LOGIN;
        }

        ContextoSesion contexto = resultado.contexto();
        String usuario = contexto != null && contexto.usuario() != null
                ? contexto.usuario()
                : form.getUsuario().trim().toUpperCase(Locale.ROOT);
        registradorAutenticacion.registrar(request, response,
                new UsuarioAutenticado(usuario, contexto));
        gestorContextoSesion.guardar(request, contexto);
        escribirCookieSucursal(response, form.getTipo());
        return "redirect:" + RUTA_HOME;
    }

    /**
     * Pantalla de inicio ({@code Home} del legacy) con el contexto de sesión, el texto de
     * {@code Inicio} y el menú de programas (R-LOG-016, R-LOG-024, R-LOG-025).
     *
     * @param request petición actual
     * @param model modelo de la vista
     * @return el nombre de la vista {@code nucleo/home}
     */
    @GetMapping("/home")
    public String home(HttpServletRequest request, Model model) {
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        model.addAttribute("contexto", contexto);
        model.addAttribute("rutaActual", request.getRequestURI());
        boolean haySesion = contexto != null && !isVacio(contexto.usuario());
        if (haySesion) {
            model.addAttribute("programas", programasAutorizados(contexto));
            model.addAttribute("textoInicio", TEXTO_INICIO);
        } else {
            // Sin sesión: se muestra el Login (R-UI-005). Se arma el view model del acceso.
            LoginForm form = new LoginForm();
            form.setUsuario("");
            form.setTipo(tipoDesdeCookie(request));
            poblarLogin(model, form, null, false);
        }
        return VISTA_HOME;
    }

    /**
     * Pantalla {@code Inicio} del legacy: texto descriptivo del sistema y menú activo
     * (R-UI-006).
     *
     * @param request petición actual
     * @param model modelo de la vista
     * @return el nombre de la vista {@code nucleo/inicio}
     */
    @GetMapping("/inicio")
    public String inicio(HttpServletRequest request, Model model) {
        model.addAttribute("textoInicio", TEXTO_INICIO);
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        model.addAttribute("contexto", contexto);
        model.addAttribute("programas", programasAutorizados(contexto));
        model.addAttribute("rutaActual", request.getRequestURI());
        return VISTA_INICIO;
    }

    /**
     * Programas autorizados del usuario ({@code ListPrograms} del legacy).
     *
     * <p>Si la carga falla, la pantalla se sirve con la lista vacía (estado {@code empty}) en
     * lugar de la página de error: el menú es un accesorio de la pantalla de inicio.
     *
     * @return la lista de programas (nunca {@code null})
     */
    private List<SmoothNavMenuData> programasAutorizados(ContextoSesion contexto) {
        String tipo = (contexto != null && contexto.tipo() != null && !contexto.tipo().isBlank())
                ? contexto.tipo() : "PUE";
        try {
            List<SmoothNavMenuData> programas = servicioAutorizacion.programasPorTipo(tipo);
            return programas == null ? List.of() : programas;
        } catch (RuntimeException ex) {
            log.warn("No se pudo cargar la lista de programas del usuario", ex);
            return List.of();
        }
    }

    /**
     * Pantalla de acceso denegado ({@code NotAuthorized} del legacy) (R-LOG-026).
     *
     * @param model modelo de la vista
     * @return el nombre de la vista {@code nucleo/not-authorized}
     */
    @GetMapping("/not-authorized")
    public String notAuthorized(Model model) {
        model.addAttribute("mensaje", MENSAJE_NO_AUTORIZADO);
        return VISTA_NO_AUTORIZADO;
    }

    /**
     * Raíz de la aplicación: redirige a la pantalla de inicio (plan §3.8).
     *
     * @return la redirección a {@code /home}
     */
    @GetMapping("/")
    public String raiz() {
        return "redirect:" + RUTA_HOME;
    }

    // ------------------------------------------------------------------ view model

    /**
     * Arma el view model de la pantalla de acceso (plan §4.3): {@code loginForm},
     * {@code mensajeError}, {@code tieneErrores} y {@code tipos}.
     *
     * @param model modelo de la vista
     * @param form formulario (siempre con la clave en blanco)
     * @param mensajeError mensaje verbatim del error, o {@code null}
     * @param tieneErrores hay errores de binding
     */
    private void poblarLogin(Model model, LoginForm form, String mensajeError,
            boolean tieneErrores) {
        model.addAttribute("loginForm", form);
        model.addAttribute("mensajeError", mensajeError);
        model.addAttribute("tieneErrores", tieneErrores);
        model.addAttribute("tipos", List.of(TipoAcceso.values()));
    }

    // --------------------------------------------------------------------- cookies

    /**
     * Tipo de acceso precargado desde la cookie {@code SUCURSAL} (R-LOG-022, R-LOG-023).
     *
     * <p>Si la cookie no existe o trae un valor desconocido, se precarga {@code PUE}: el selector
     * no renderiza una opción vacía (SA-LOG-13).
     *
     * @param request petición actual
     * @return el código del tipo de acceso
     */
    private String tipoDesdeCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_SUCURSAL.equals(cookie.getName())) {
                    TipoAcceso tipo = TipoAcceso.desdeCodigo(cookie.getValue());
                    if (tipo != null) {
                        return tipo.codigo();
                    }
                }
            }
        }
        return TipoAcceso.PUE.codigo();
    }

    /**
     * Escribe la cookie {@code SUCURSAL} con el tipo de acceso (D-LOG-07, D-LOG-15, R-LOG-023).
     *
     * <p>Cookie <strong>de sesión</strong> (sin {@code Max-Age}), {@code HttpOnly},
     * {@code SameSite=Lax} y {@code path=/}. Sólo se escribe en el camino de éxito.
     *
     * @param response respuesta actual
     * @param tipo código del tipo de acceso
     */
    private void escribirCookieSucursal(HttpServletResponse response, String tipo) {
        TipoAcceso acceso = TipoAcceso.desdeCodigo(tipo);
        ResponseCookie cookie = ResponseCookie.from(COOKIE_SUCURSAL,
                        acceso == null ? TipoAcceso.PUE.codigo() : acceso.codigo())
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // ------------------------------------------------------------------------- IP

    /**
     * IP del cliente: la cabecera {@code X-Forwarded-For} y, si viene vacía, la dirección remota
     * de la conexión (R-LOG-013, paridad con el legacy).
     *
     * @param request petición actual
     * @return la IP del cliente
     */
    private static String resolverIp(HttpServletRequest request) {
        String cabecera = request.getHeader(CABECERA_IP);
        if (cabecera != null && !cabecera.isBlank()) {
            return cabecera.trim();
        }
        return request.getRemoteAddr();
    }

    /** Vacío o compuesto sólo por espacios (como {@code IsEmpty()} del legacy). */
    private static boolean isVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
