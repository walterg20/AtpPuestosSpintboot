package ar.com.atp.puestos.puestos.web.mvc;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.GestorContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.ProveedorNavegacion;
import ar.com.atp.puestos.puestos.service.MensajesComision;
import ar.com.atp.puestos.puestos.service.ServicioComisiones;
import ar.com.atp.puestos.puestos.service.dto.comision.ComisionFilaView;
import ar.com.atp.puestos.puestos.service.dto.comision.ComisionGeneralView;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/comisiones")
public class ComisionMvcController {

    private static final String VISTA_LISTADO = "puestos/comisiones";
    private static final String VISTA_DETALLE = "puestos/comision-detalle";
    private static final String MENSAJE_ERROR_LISTADO = "No se pudieron mostrar las comisiones.";

    private static final Logger log = LoggerFactory.getLogger(ComisionMvcController.class);

    private final ServicioComisiones servicioComisiones;
    private final ProveedorNavegacion proveedorNavegacion;
    private final GestorContextoSesion gestorContextoSesion;

    public ComisionMvcController(ServicioComisiones servicioComisiones,
            ProveedorNavegacion proveedorNavegacion, GestorContextoSesion gestorContextoSesion) {
        this.servicioComisiones = servicioComisiones;
        this.proveedorNavegacion = proveedorNavegacion;
        this.gestorContextoSesion = gestorContextoSesion;
    }

    @GetMapping
    public String listar(
            @RequestParam(name = "pagina", required = false) Integer pagina,
            HttpServletRequest request, Model model) {
        proveedorNavegacion.poblar(model, request);
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        Integer pueCod = (contexto == null) ? null : contexto.sucursal();
        Integer personaId = (contexto == null) ? null : contexto.personaId();

        int numeroPagina = (pagina == null || pagina < 0) ? 0 : pagina;
        try {
            Page<ComisionFilaView> paginaComisiones = servicioComisiones.listar(pueCod, personaId,
                    numeroPagina);
            model.addAttribute("comisiones", paginaComisiones.getContent());
            model.addAttribute("paginaActual", paginaComisiones.getNumber());
            model.addAttribute("cantidadPaginas", paginaComisiones.getTotalPages());
            model.addAttribute("totalComisiones", paginaComisiones.getTotalElements());
            model.addAttribute("hayPaginaAnterior", paginaComisiones.hasPrevious());
            model.addAttribute("hayPaginaSiguiente", paginaComisiones.hasNext());
            model.addAttribute("mensajeError", null);
        } catch (RuntimeException ex) {
            log.warn("No se pudo cargar el listado de comisiones", ex);
            model.addAttribute("comisiones", List.of());
            model.addAttribute("paginaActual", 0);
            model.addAttribute("cantidadPaginas", 0);
            model.addAttribute("totalComisiones", 0L);
            model.addAttribute("hayPaginaAnterior", false);
            model.addAttribute("hayPaginaSiguiente", false);
            model.addAttribute("mensajeError", MENSAJE_ERROR_LISTADO);
        }
        return VISTA_LISTADO;
    }

    @GetMapping("/{pueCod}/{cpcId}")
    public String detalle(
            @PathVariable("pueCod") Integer pueCod,
            @PathVariable("cpcId") Integer cpcId,
            @RequestParam(name = "tab", required = false, defaultValue = "General") String tab,
            HttpServletRequest request, Model model) {
        proveedorNavegacion.poblar(model, request);
        ComisionGeneralView general = servicioComisiones.obtenerDetalle(pueCod, cpcId);
        if (general == null) {
            model.addAttribute("existe", false);
            model.addAttribute("general", null);
            model.addAttribute("integrantes", List.of());
            model.addAttribute("tabActiva", tab);
            model.addAttribute("mensajeError", MensajesComision.NO_SE_ENCONTRARON_REGISTROS);
        } else {
            model.addAttribute("existe", true);
            model.addAttribute("general", general);
            model.addAttribute("integrantes", servicioComisiones.listarIntegrantes(pueCod, cpcId));
            model.addAttribute("tabActiva", tab);
            model.addAttribute("mensajeError", null);
        }
        return VISTA_DETALLE;
    }
}
