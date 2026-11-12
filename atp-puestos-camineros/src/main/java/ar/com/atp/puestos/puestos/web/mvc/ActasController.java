package ar.com.atp.puestos.puestos.web.mvc;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.GestorContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.ProveedorNavegacion;
import ar.com.atp.puestos.puestos.service.ActaService;
import ar.com.atp.puestos.puestos.service.model.ActaFila;
import ar.com.atp.puestos.puestos.service.model.ActaPreparada;
import ar.com.atp.puestos.puestos.service.model.ResultadoAltaActa;
import ar.com.atp.puestos.puestos.web.mvc.form.ActaForm;
import ar.com.atp.puestos.puestos.web.mvc.form.FiltroActasForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Pantalla de Formularios Acta (Puestos Camineros) — spec 09 §4.1/§4.2.
 *
 * <p>Rutas (plan §3.2):
 * <ul>
 *   <li>{@code GET /actas} → vista {@code puestos/actas-lista} (grilla paginada + filtros);</li>
 *   <li>{@code GET /actas/nueva} → vista {@code puestos/carga-manual-actas} (alta);</li>
 *   <li>{@code POST /actas/guardar} → re-render del formulario (200) o {@code redirect:/actas} (302).</li>
 * </ul>
 *
 * <p>Es un <strong>adaptador de presentación</strong>: mapea la ruta, invoca el servicio, arma el
 * view model y devuelve el nombre de la vista. <strong>No</strong> accede a repositorios ni a la
 * base de datos y <strong>no</strong> implementa reglas de negocio: la validación del legacy y la
 * numeración viven en {@link ActaService} (G7, D-ACTA-03).
 *
 * <p>La numeración, el total, el indicador "Gen. Manual?" y la contraparte de la fila los calcula
 * el servicio: el modelo expone el {@code ActaFila} del contrato directamente, sin una vista
 * propia (D-ACTA-22).
 */
@Controller
@RequestMapping("/actas")
public class ActasController {

    /** Vista del listado de Actas. */
    private static final String VISTA_LISTA = "puestos/actas-lista";

    /** Vista del formulario de alta. */
    private static final String VISTA_FORMULARIO = "puestos/carga-manual-actas";

    /** Mensaje de éxito verbatim del legacy (spec §3.9, mensaje 26). */
    static final String MENSAJE_EXITO = "El acta se ha creado correctamente";

    /** Mensaje verbatim del legacy cuando no hay turno (spec §3.9, mensaje 1). */
    static final String MENSAJE_SIN_TURNO = "Debe iniciar un turno...";

    /** Tamaño de página por defecto: paridad con {@code Grid276.MaxRows = 10} (R-ACTA-010). */
    private static final int TAMANIO_PAGINA = 10;

    /** Motivo de Acta por defecto: la opción "Seleccionar..." del legacy. */
    private static final String MOTIVO_POR_DEFECTO = "N";

    /** Tipo de movimiento por defecto: la opción "Seleccionar..." del legacy. */
    private static final String CLASIFICACION_POR_DEFECTO = "N";

    private static final Logger log = LoggerFactory.getLogger(ActasController.class);

    private final ActaService actaService;
    private final ProveedorNavegacion proveedorNavegacion;
    private final GestorContextoSesion gestorContextoSesion;

    public ActasController(ActaService actaService, ProveedorNavegacion proveedorNavegacion,
            GestorContextoSesion gestorContextoSesion) {
        this.actaService = actaService;
        this.proveedorNavegacion = proveedorNavegacion;
        this.gestorContextoSesion = gestorContextoSesion;
    }

    /**
     * Listado de las Actas del turno con filtros y paginación (R-ACTA-001…R-ACTA-013).
     *
     * <p>El orden es <strong>fijo</strong> (paridad legacy: {@code TurNroId, SucNroId, SucTipo,
     * F276Id}) y se construye con un único {@code ORDER BY} multi-propiedad para evitar el
     * duplicado que SQL Server rechaza (B0/P0). Se ignora el parámetro {@code sort} de la URL.
     *
     * @param page página solicitada (0-based); si es {@code null} se usa 0
     * @param size tamaño de página; si es {@code null} se usa {@value #TAMANIO_PAGINA}
     * @param filtro filtros de búsqueda de la URL
     */
    @GetMapping
    public String listar(@org.springframework.web.bind.annotation.RequestParam(value = "page", required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(value = "size", required = false) Integer size,
            @ModelAttribute("filtroActas") FiltroActasForm filtro,
            HttpServletRequest request, Model model) {
        Pageable pageable = PageRequest.of(
                page == null ? 0 : page,
                size == null ? TAMANIO_PAGINA : size,
                Sort.by("turNroId", "sucNroId", "sucTipo", "f276Id"));

        proveedorNavegacion.poblar(model, request);
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);

        Page<ActaFila> actas;
        try {
            actas = actaService.listar(filtro.aFiltro(), pageable, contexto);
        } catch (RuntimeException ex) {
            log.error("No se pudieron listar las actas del turno", ex);
            throw ex;
        }
        if (actas == null) {
            actas = Page.empty(pageable);
        }

        model.addAttribute("actas", actas);
        model.addAttribute("filtroActas", filtro);
        model.addAttribute("turnoActivo", turnoActivo(contexto));
        model.addAttribute("sortActas", sortParam(pageable));
        Object exito = model.getAttribute("mensajeExito");
        model.addAttribute("mensaje", exito == null ? "" : exito.toString());
        return VISTA_LISTA;
    }

    /**
     * Formulario de alta (R-ACTA-014…R-ACTA-025).
     *
     * <p>Antes de renderizar se delega en {@link ActaService#prepararFormulario(ContextoSesion)},
     * que valida los lotes abiertos y el estado del turno (R-ACTA-041, R-ACTA-042). Si el alta está
     * bloqueada <strong>no se renderiza el formulario</strong>: se muestra el motivo (V-17).
     */
    @GetMapping("/nueva")
    public String nueva(
            @org.springframework.web.bind.annotation.RequestParam(value = "turNroId", required = false) Integer turNroId,
            @org.springframework.web.bind.annotation.RequestParam(value = "manual", required = false, defaultValue = "false") boolean manual,
            HttpServletRequest request, Model model) {
        proveedorNavegacion.poblar(model, request);
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);

        ActaPreparada preparada = actaService.prepararFormulario(contexto, turNroId, manual);
        boolean bloqueado = preparada != null && preparada.bloqueado();
        model.addAttribute("bloqueado", bloqueado);
        model.addAttribute("mensajeBloqueo", preparada == null ? null : preparada.mensajeBloqueo());
        model.addAttribute("turnoEstado", preparada == null ? null : preparada.turnoEstado());
        model.addAttribute("turnoActivo", turnoActivo(contexto));
        model.addAttribute("manual", manual);
        model.addAttribute("turNroId", turNroId);

        ActaForm formInicial = null;
        if (!bloqueado) {
            formInicial = formularioInicial(preparada, turNroId, manual);
            model.addAttribute("actaForm", formInicial);
            model.addAttribute("productosDisponibles", productosDisponibles());
        }
        model.addAttribute("locCodVenDerivado",
                formInicial != null && formInicial.getF276VenCLo() != null);
        model.addAttribute("locCodDesDerivado",
                formInicial != null && formInicial.getF276DestCL() != null);
        model.addAttribute("locCodTranDerivado",
                formInicial != null && formInicial.getF276TranCL() != null);
        return VISTA_FORMULARIO;
    }

    /**
     * Alta del Acta (R-ACTA-024, R-ACTA-025, R-ACTA-040).
     *
     * <p>Tres caminos: errores de binding ⇒ se re-renderiza el formulario (200) con los errores
     * por campo; error de negocio ⇒ se re-renderiza con el banner del mensaje verbatim; éxito ⇒
     * {@code redirect:/actas} con el flash {@code mensajeExito}.
     */
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("actaForm") ActaForm form,
            BindingResult bindingResult, HttpServletRequest request, Model model,
            RedirectAttributes redirectAttributes) {
        proveedorNavegacion.poblar(model, request);
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);

        model.addAttribute("bloqueado", false);
        model.addAttribute("turnoActivo", turnoActivo(contexto));
        model.addAttribute("productosDisponibles", productosDisponibles());
        model.addAttribute("manual", form.isManual());
        model.addAttribute("turNroId", form.getTurNroId());
        model.addAttribute("locCodVenDerivado", form.getF276VenCLo() != null);
        model.addAttribute("locCodDesDerivado", form.getF276DestCL() != null);
        model.addAttribute("locCodTranDerivado", form.getF276TranCL() != null);

        if (bindingResult.hasErrors()) {
            model.addAttribute("mensajeError", null);
            return VISTA_FORMULARIO;
        }

        ResultadoAltaActa resultado;
        if (form.isManual()) {
            resultado = actaService.guardar(form.aAlta(), contexto, true);
        } else {
            resultado = actaService.guardar(form.aAlta(), contexto);
        }
        if (resultado == null || !resultado.ok()) {
            model.addAttribute("mensajeError", resultado == null ? null : resultado.mensaje());
            return VISTA_FORMULARIO;
        }

        redirectAttributes.addFlashAttribute("mensajeExito", MENSAJE_EXITO);
        return "redirect:/actas";
    }

    // ------------------------------------------------------------- Helpers

    /** Catálogo de productos elegibles ({@code ProdAnula = 0}); lista vacía si falla. */
    private List<?> productosDisponibles() {
        try {
            List<?> productos = actaService.productosDisponibles();
            return productos == null ? List.of() : productos;
        } catch (RuntimeException ex) {
            log.warn("No se pudo cargar el catalogo de productos", ex);
            return List.of();
        }
    }

    /**
     * Arma el formulario de alta con los valores por defecto del legacy
     * ({@code f276ActaTi = 'N'}, {@code f276Clasif = 'N'}, {@code f276FchMov = hoy}).
     */
    private ActaForm formularioInicial(ActaPreparada preparada, Integer turNroId, boolean manual) {
        ActaForm form = new ActaForm();
        form.setF276ActaTi(MOTIVO_POR_DEFECTO);
        form.setF276Clasif(CLASIFICACION_POR_DEFECTO);
        LocalDate fecha = preparada == null ? null : preparada.fechaMovimiento();
        form.setF276FchMov(fecha == null ? LocalDate.now() : fecha);
        form.setManual(manual);
        form.setTurNroId(turNroId);
        return form;
    }

    private ActaForm formularioInicial(ActaPreparada preparada) {
        return formularioInicial(preparada, null, false);
    }

    /** Hay turno activo si el turno del contexto es mayor que cero (R-ACTA-002). */
    private static boolean turnoActivo(ContextoSesion contexto) {
        Integer turno = contexto == null ? null : contexto.turno();
        return turno != null && turno > 0;
    }

    /**
     * Expresion del orden activo para los links de paginación, en el formato que entiende el
     * resolver de {@code Pageable} ({@code propiedad,direccion}).
     */
    private static String sortParam(Pageable pageable) {
        Sort sort = pageable == null ? null : pageable.getSort();
        if (sort == null || sort.isUnsorted()) {
            return "";
        }
        return sort.stream()
                .map(orden -> orden.getProperty() + "," + orden.getDirection().name().toLowerCase())
                .collect(Collectors.joining(","));
    }
}
