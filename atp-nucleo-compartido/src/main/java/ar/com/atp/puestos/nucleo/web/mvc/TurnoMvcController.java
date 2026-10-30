package ar.com.atp.puestos.nucleo.web.mvc;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.service.MensajesTurno;
import ar.com.atp.puestos.nucleo.service.ResultadoTurno;
import ar.com.atp.puestos.nucleo.service.ServicioTurnos;
import ar.com.atp.puestos.nucleo.web.dto.TurnoDto;
import ar.com.atp.puestos.nucleo.web.mvc.turno.TurnoContexto;
import ar.com.atp.puestos.nucleo.web.mvc.turno.TurnoFilaView;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Pantalla de Turnos ({@code 06-turnos}) en Thymeleaf: listado, alta y cierre.
 *
 * <p>Rutas: {@code GET /turnos} (grilla paginada), {@code POST /turnos/nuevo} (alta) y
 * {@code POST /turnos/{turNroId}/cerrar} (cierre). La vista es {@code nucleo/turnos}.
 *
 * <p>La pantalla es 100 % servidor-rendered: el estado (página, turno seleccionado) viaja en la
 * URL y el turno abierto viaja en el contexto de sesión. El controller <strong>no</strong> invoca
 * la API REST por HTTP: usa el mismo {@link ServicioTurnos} que el resto de la aplicación.
 *
 * <p>Las reglas de negocio viven en el servicio; este adapter sólo mapea la ruta, invoca el
 * servicio, arma el view model y devuelve el nombre de la vista. Los mensajes de negocio
 * (<strong>verbatim</strong> del legacy) llegan en el resultado del servicio y se muestran sin
 * reescribir: la vista no literaliza ningún mensaje (R-TUR-005, R-TUR-010, R-TUR-012).
 */
@Controller
@RequestMapping("/turnos")
public class TurnoMvcController {

    /** Nombre de la vista Thymeleaf de la pantalla. */
    private static final String VISTA = "nucleo/turnos";

    /** Ruta del listado (destino de las redirecciones tras una escritura). */
    private static final String RUTA_LISTADO = "/turnos";

    /** Ruta del alta de un turno. */
    private static final String RUTA_ALTA = "/turnos/nuevo";

    /** Parámetro de la URL con el número de página (0-based). */
    private static final String PARAMETRO_PAGINA = "pagina";

    /** Parámetro de la URL con el número del turno seleccionado en la grilla. */
    private static final String PARAMETRO_TURNO = "turNroId";

    /** Prefijo del mensaje de éxito del alta (el legacy no muestra un texto propio). */
    private static final String MENSAJE_ALTA = "El turno ";

    /** Sufijo del mensaje de éxito del alta. */
    private static final String MENSAJE_ALTA_SUFIJO = " ha sido abierto";

    /** Mensaje de la vista cuando el listado no se puede cargar. */
    private static final String MENSAJE_ERROR_LISTADO = "No se pudieron mostrar los turnos.";

    private static final Logger log = LoggerFactory.getLogger(TurnoMvcController.class);

    private final ServicioTurnos servicioTurnos;
    private final ProveedorNavegacion proveedorNavegacion;
    private final GestorContextoSesion gestorContextoSesion;

    public TurnoMvcController(ServicioTurnos servicioTurnos, ProveedorNavegacion proveedorNavegacion,
            GestorContextoSesion gestorContextoSesion) {
        this.servicioTurnos = servicioTurnos;
        this.proveedorNavegacion = proveedorNavegacion;
        this.gestorContextoSesion = gestorContextoSesion;
    }

    /**
     * Grilla de turnos de la sucursal del usuario, paginada de a 10 filas (R-TUR-001, R-TUR-003).
     *
     * @param pagina número de página (0-based); si falta, la primera
     * @param turNroId turno seleccionado en la grilla (opcional)
     * @param request petición actual (de donde se recupera la sesión)
     * @param model modelo de la vista
     * @return el nombre de la vista
     */
    @GetMapping
    public String listar(@RequestParam(name = PARAMETRO_PAGINA, required = false) Integer pagina,
            @RequestParam(name = PARAMETRO_TURNO, required = false) Integer turNroId,
            HttpServletRequest request,
            Model model) {
        proveedorNavegacion.poblar(model, request);
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        int numeroPagina = pagina == null || pagina < 0 ? 0 : pagina;
        Page<TurnoDto> turnos;
        try {
            turnos = listar(contexto, numeroPagina);
        } catch (RuntimeException ex) {
            log.warn("No se pudo cargar el listado de turnos", ex);
            poblar(model, contexto, Page.empty(), null, MENSAJE_ERROR_LISTADO);
            return VISTA;
        }
        poblar(model, contexto, turnos, turNroId, null);
        return VISTA;
    }

    /**
     * Alta de un turno: el servicio asigna el número, escribe la fila y devuelve el resultado
     * (R-TUR-006..R-TUR-011). Si el alta se rechaza, el mensaje <strong>verbatim</strong> del
     * legacy se muestra en la misma pantalla.
     *
     * @param request petición actual
     * @param model modelo de la vista
     * @param redirect atributos de redirección (mensaje de éxito)
     * @return la redirección al listado o el nombre de la vista
     */
    @PostMapping("/nuevo")
    public String nuevo(HttpServletRequest request, Model model, RedirectAttributes redirect) {
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        ResultadoTurno resultado = servicioTurnos.nuevoTurno(contexto);
        if (resultado.exito()) {
            // R-TUR-009: el contexto de sesión pasa a apuntar al turno nuevo.
            gestorContextoSesion.guardar(request, TurnoContexto.conTurno(contexto, resultado.numeroTurno()));
            redirect.addFlashAttribute("mensajeExito",
                    MENSAJE_ALTA + resultado.numeroTurno() + MENSAJE_ALTA_SUFIJO);
            return "redirect:" + RUTA_LISTADO;
        }
        return mostrarError(model, request, contexto, resultado.mensaje());
    }

    /**
     * Cierre de un turno: el servicio valida el estado, los formularios abiertos y el cierre
     * (R-TUR-012..R-TUR-014). Si el cierre se rechaza, el mensaje <strong>verbatim</strong> del
     * legacy se muestra en la misma pantalla.
     *
     * @param turNroId número del turno a cerrar
     * @param request petición actual
     * @param model modelo de la vista
     * @param redirect atributos de redirección (mensaje de éxito)
     * @return la redirección al listado o el nombre de la vista
     */
    @PostMapping("/{turNroId}/cerrar")
    public String cerrar(@PathVariable("turNroId") Integer turNroId, HttpServletRequest request,
            Model model, RedirectAttributes redirect) {
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        ResultadoTurno resultado = servicioTurnos.cerrarTurno(contexto, turNroId);
        if (resultado.exito()) {
            // R-TUR-012: al cerrar, el contexto de sesión queda sin turno abierto.
            gestorContextoSesion.guardar(request, TurnoContexto.conTurno(contexto, 0));
            redirect.addFlashAttribute("mensajeExito", resultado.mensaje());
            return "redirect:" + RUTA_LISTADO;
        }
        return mostrarError(model, request, contexto, resultado.mensaje());
    }

    /**
     * Listado paginado de la sucursal del usuario.
     *
     * @param contexto contexto de sesión
     * @param numeroPagina número de página (0-based)
     * @return la página de turnos
     */
    private Page<TurnoDto> listar(ContextoSesion contexto, int numeroPagina) {
        Pageable pageable = PageRequest.of(numeroPagina, ServicioTurnos.TAMANIO_PAGINA);
        return servicioTurnos.listar(contexto, pageable);
    }

    /**
     * Renderiza la pantalla con un mensaje de error: recarga la grilla y muestra el mensaje
     * <strong>verbatim</strong> que devolvió el servicio.
     *
     * @param model modelo de la vista
     * @param request petición actual
     * @param contexto contexto de sesión
     * @param mensajeError mensaje a mostrar
     * @return el nombre de la vista
     */
    private String mostrarError(Model model, HttpServletRequest request, ContextoSesion contexto,
            String mensajeError) {
        proveedorNavegacion.poblar(model, request);
        Page<TurnoDto> turnos;
        try {
            turnos = listar(contexto, 0);
        } catch (RuntimeException ex) {
            log.warn("No se pudo cargar el listado de turnos tras una escritura", ex);
            turnos = Page.empty();
        }
        poblar(model, contexto, turnos, null, mensajeError);
        return VISTA;
    }

    /**
     * Arma el view model de la pantalla.
     *
     * @param model modelo de la vista
     * @param contexto contexto de sesión
     * @param pagina página de turnos
     * @param turnoSeleccionado turno elegido en la grilla (puede ser {@code null})
     * @param mensajeError mensaje de error (puede ser {@code null})
     */
    private void poblar(Model model, ContextoSesion contexto, Page<TurnoDto> pagina,
            Integer turnoSeleccionado, String mensajeError) {
        List<TurnoFilaView> filas = pagina.getContent().stream().map(TurnoFilaView::de).toList();

        model.addAttribute("turnos", filas);
        model.addAttribute("turnoSeleccionado", turnoSeleccionado);
        model.addAttribute("turnoObjetivo", turnoObjetivo(contexto, turnoSeleccionado));
        model.addAttribute("mensajeError", mensajeError);
        // R-TUR-005: el texto de confirmación del legacy viaja en el modelo, no literalizado.
        model.addAttribute("mensajeConfirmacionCierre", MensajesTurno.CONFIRMAR_CIERRE);
        model.addAttribute("paginaActual", pagina.getNumber());
        model.addAttribute("cantidadPaginas", pagina.getTotalPages());
        model.addAttribute("totalTurnos", pagina.getTotalElements());
        model.addAttribute("hayPaginaAnterior", pagina.hasPrevious());
        model.addAttribute("hayPaginaSiguiente", pagina.hasNext());
    }

    /**
     * Turno sobre el que actúa el botón {@code Cerrar Turno}: el seleccionado en la grilla o, si
     * no hay ninguno, el turno abierto de la sesión ({@code Context.Turno}).
     *
     * @param contexto contexto de sesión
     * @param turnoSeleccionado turno elegido en la grilla (puede ser {@code null})
     * @return el número del turno, o {@code null} si no hay ninguno
     */
    private static Integer turnoObjetivo(ContextoSesion contexto, Integer turnoSeleccionado) {
        if (turnoSeleccionado != null) {
            return turnoSeleccionado;
        }
        Integer turno = contexto == null ? null : contexto.turno();
        return turno == null || turno <= 0 ? null : turno;
    }
}
