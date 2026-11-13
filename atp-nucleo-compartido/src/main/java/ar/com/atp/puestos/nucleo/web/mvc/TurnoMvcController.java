package ar.com.atp.puestos.nucleo.web.mvc;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.service.MensajesTurno;
import ar.com.atp.puestos.nucleo.service.ResultadoTurno;
import ar.com.atp.puestos.nucleo.service.FiltrosTurno;
import ar.com.atp.puestos.nucleo.service.ServicioTurnos;
import ar.com.atp.puestos.nucleo.web.dto.TurnoDto;
import ar.com.atp.puestos.nucleo.web.mvc.turno.TurnoContexto;
import ar.com.atp.puestos.nucleo.web.mvc.turno.TurnoDetalleView;
import ar.com.atp.puestos.nucleo.web.mvc.turno.TurnoFilaView;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
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

@Controller
@RequestMapping("/turnos")
public class TurnoMvcController {

    private static final String VISTA = "nucleo/turnos";
    private static final String VISTA_DETALLE = "nucleo/detalle-turno";
    private static final String VISTA_DETALLE_ACTAS = "nucleo/detalle-turno-actas";
    private static final String RUTA_LISTADO = "/turnos";
    private static final String RUTA_ALTA = "/turnos/nuevo";
    private static final String PARAMETRO_PAGINA = "pagina";
    private static final String PARAMETRO_TURNO = "turNroId";
    private static final String MENSAJE_ALTA = "El turno ";
    private static final String MENSAJE_ALTA_SUFIJO = " ha sido abierto";
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

    @GetMapping
    public String listar(@RequestParam(name = PARAMETRO_PAGINA, required = false) Integer pagina,
            @RequestParam(name = PARAMETRO_TURNO, required = false) Integer turNroId,
            @RequestParam(name = "busqueda", required = false) String busqueda,
            @RequestParam(name = "desde", required = false) String desde,
            @RequestParam(name = "hasta", required = false) String hasta,
            @RequestParam(name = "estado", required = false) String estado,
            HttpServletRequest request,
            Model model) {
        proveedorNavegacion.poblar(model, request);
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        int numeroPagina = pagina == null || pagina < 0 ? 0 : pagina;
        LocalDate desdeFecha = parsearFecha(desde);
        LocalDate hastaFecha = parsearFecha(hasta);
        Integer busquedaId = parsearEntero(busqueda);
        LocalDateTime desdeDateTime = desdeFecha == null ? null : desdeFecha.atStartOfDay();
        LocalDateTime hastaDateTime = hastaFecha == null ? null : hastaFecha.atTime(23, 59, 59);
        FiltrosTurno filtros = new FiltrosTurno(busquedaId, desdeDateTime, hastaDateTime, estado);
        Page<TurnoDto> turnos;
        try {
            turnos = servicioTurnos.listar(contexto, filtros,
                    org.springframework.data.domain.PageRequest.of(numeroPagina,
                            ServicioTurnos.TAMANIO_PAGINA));
        } catch (RuntimeException ex) {
            log.warn("No se pudo cargar el listado de turnos", ex);
            poblar(model, contexto, Page.empty(), null, MENSAJE_ERROR_LISTADO);
            agregarFiltros(model, busqueda, desdeFecha, hastaFecha, estado);
            return VISTA;
        }
        poblar(model, contexto, turnos, turNroId, null);
        agregarFiltros(model, busqueda, desdeFecha, hastaFecha, estado);
        return VISTA;
    }

    private static Integer parsearEntero(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static LocalDate parsearFecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static void agregarFiltros(Model model, String busqueda, LocalDate desde, LocalDate hasta,
            String estado) {
        model.addAttribute("busqueda", busqueda);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("estado", estado);
    }

    @PostMapping("/nuevo")
    public String nuevo(HttpServletRequest request, Model model, RedirectAttributes redirect) {
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        ResultadoTurno resultado = servicioTurnos.nuevoTurno(contexto);
        if (resultado.exito()) {
            gestorContextoSesion.guardar(request, TurnoContexto.conTurno(contexto, resultado.numeroTurno()));
            redirect.addFlashAttribute("mensajeExito",
                    MENSAJE_ALTA + resultado.numeroTurno() + MENSAJE_ALTA_SUFIJO);
            return "redirect:" + RUTA_LISTADO;
        }
        return mostrarError(model, request, contexto, resultado.mensaje());
    }

    @PostMapping("/{turNroId}/cerrar")
    public String cerrar(@PathVariable("turNroId") Integer turNroId, HttpServletRequest request,
            Model model, RedirectAttributes redirect) {
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        ResultadoTurno resultado = servicioTurnos.cerrarTurno(contexto, turNroId);
        if (resultado.exito()) {
            gestorContextoSesion.guardar(request, TurnoContexto.conTurno(contexto, 0));
            redirect.addFlashAttribute("mensajeExito", resultado.mensaje());
            return "redirect:" + RUTA_LISTADO;
        }
        return mostrarError(model, request, contexto, resultado.mensaje());
    }

    private String mostrarError(Model model, HttpServletRequest request, ContextoSesion contexto,
            String mensajeError) {
        proveedorNavegacion.poblar(model, request);
        Page<TurnoDto> turnos;
        try {
            turnos = servicioTurnos.listar(contexto, FiltrosTurno.vacio(),
                    org.springframework.data.domain.PageRequest.of(0, ServicioTurnos.TAMANIO_PAGINA));
        } catch (RuntimeException ex) {
            log.warn("No se pudo cargar el listado de turnos tras una escritura", ex);
            turnos = Page.empty();
        }
        poblar(model, contexto, turnos, null, mensajeError);
        return VISTA;
    }

    private void poblar(Model model, ContextoSesion contexto, Page<TurnoDto> pagina,
            Integer turnoSeleccionado, String mensajeError) {
        List<TurnoFilaView> filas = pagina.getContent().stream().map(TurnoFilaView::de).toList();

        model.addAttribute("turnos", filas);
        model.addAttribute("turnoSeleccionado", turnoSeleccionado);
        model.addAttribute("turnoObjetivo", turnoObjetivo(contexto, turnoSeleccionado));
        model.addAttribute("mensajeError", mensajeError);
        model.addAttribute("mensajeConfirmacionCierre", MensajesTurno.CONFIRMAR_CIERRE);
        model.addAttribute("mensajeConfirmacionNuevo", "¿Confirma que desea abrir un nuevo Turno?");
        model.addAttribute("paginaActual", pagina.getNumber());
        model.addAttribute("cantidadPaginas", pagina.getTotalPages());
        model.addAttribute("totalTurnos", pagina.getTotalElements());
        model.addAttribute("hayPaginaAnterior", pagina.hasPrevious());
        model.addAttribute("hayPaginaSiguiente", pagina.hasNext());
    }

    @GetMapping("/{turNroId}")
    public String detalle(@PathVariable("turNroId") Integer turNroId,
            @RequestParam(name = "tab", required = false, defaultValue = "general") String tab,
            HttpServletRequest request, Model model, RedirectAttributes redirect) {
        proveedorNavegacion.poblar(model, request);
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        TurnoDetalleView detalle;
        try {
            detalle = servicioTurnos.detalleTurno(contexto, turNroId);
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("mensajeError", "El turno no existe.");
            return "redirect:" + RUTA_LISTADO;
        }
        model.addAttribute("detalle", detalle);
        model.addAttribute("turno", detalle == null ? null : detalle.turno());
        List<ar.com.atp.puestos.nucleo.web.mvc.turno.ActaView> actasLista =
                detalle == null || detalle.actas() == null ? List.of() : detalle.actas();
        model.addAttribute("actas", actasLista);
        model.addAttribute("eventosBitacora",
                detalle == null || detalle.bitacora() == null ? List.of() : detalle.bitacora());
        model.addAttribute("fiscalizador", contexto == null ? "" : contexto.usuarioNombre());
        String tabNormalizada = tab == null || tab.isBlank() ? "general" : tab;
        model.addAttribute("tab", tabNormalizada);

        // Paginación reusada (in-memory) para la solapa LISTADO ACTAS.
        org.springframework.data.domain.Page<ar.com.atp.puestos.nucleo.web.mvc.turno.ActaView> actasPage =
                new org.springframework.data.domain.PageImpl<>(
                        actasLista,
                        org.springframework.data.domain.PageRequest.of(0,
                                actasLista.isEmpty() ? 10 : actasLista.size()),
                        actasLista.size());
        model.addAttribute("actasPage", actasPage);
        model.addAttribute("filtros", FiltrosTurno.vacio());

        if ("actas".equalsIgnoreCase(tabNormalizada)) {
            return VISTA_DETALLE_ACTAS;
        }
        return VISTA_DETALLE;
    }

    private static Integer turnoObjetivo(ContextoSesion contexto, Integer turnoSeleccionado) {
        if (turnoSeleccionado != null) {
            return turnoSeleccionado;
        }
        Integer turno = contexto == null ? null : contexto.turno();
        return turno == null || turno <= 0 ? null : turno;
    }
}