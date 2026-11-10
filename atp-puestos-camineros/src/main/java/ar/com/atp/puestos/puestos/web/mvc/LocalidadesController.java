package ar.com.atp.puestos.puestos.web.mvc;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.GestorContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.ProveedorNavegacion;
import ar.com.atp.puestos.puestos.service.LocalidadPopupService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/localidades")
public class LocalidadesController {

    private static final String VISTA = "puestos/localidades-seleccion";
    private static final int TAMANIO_PAGINA = 5;

    private final LocalidadPopupService localidadPopupService;
    private final ProveedorNavegacion proveedorNavegacion;
    private final GestorContextoSesion gestorContextoSesion;

    public LocalidadesController(LocalidadPopupService localidadPopupService,
            ProveedorNavegacion proveedorNavegacion, GestorContextoSesion gestorContextoSesion) {
        this.localidadPopupService = localidadPopupService;
        this.proveedorNavegacion = proveedorNavegacion;
        this.gestorContextoSesion = gestorContextoSesion;
    }

    @GetMapping
    public String listar(
            @RequestParam(value = "filtroLocCod", required = false) Integer filtroLocCod,
            @RequestParam(value = "locCod", required = false) Integer locCodAlt,
            @RequestParam(value = "filtroLocNom", required = false) String filtroLocNom,
            @RequestParam(value = "locNom", required = false) String locNomAlt,
            @RequestParam(value = "filtroProvNombre", required = false) String filtroProvNombre,
            @RequestParam(value = "provNombre", required = false) String provNombreAlt,
            @RequestParam(value = "filtroLocCodPost", required = false) Integer filtroLocCodPost,
            @RequestParam(value = "locCodPost", required = false) Integer locCodPostAlt,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            HttpServletRequest request, Model model) {

        Integer locCod = filtroLocCod != null ? filtroLocCod : locCodAlt;
        String locNom = filtroLocNom != null ? filtroLocNom : locNomAlt;
        String provNombre = filtroProvNombre != null ? filtroProvNombre : provNombreAlt;
        Integer locCodPost = filtroLocCodPost != null ? filtroLocCodPost : locCodPostAlt;

        proveedorNavegacion.poblar(model, request);
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        if (contexto == null || contexto.usuario() == null || contexto.usuario().isBlank()) {
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(
                page == null ? 0 : page,
                size == null ? TAMANIO_PAGINA : size,
                Sort.by("locCod"));

        Page<?> localidades;
        try {
            localidades = localidadPopupService.listar(locCod, locNom, provNombre, locCodPost, pageable);
        } catch (RuntimeException ex) {
            localidades = Page.empty(pageable);
        }
        if (localidades == null) {
            localidades = Page.empty(pageable);
        }

        model.addAttribute("localidades", localidades);
        model.addAttribute("filtroLocCod", locCod);
        model.addAttribute("locCod", locCod);
        model.addAttribute("filtroLocNom", locNom);
        model.addAttribute("locNom", locNom);
        model.addAttribute("filtroProvNombre", provNombre);
        model.addAttribute("provNombre", provNombre);
        model.addAttribute("filtroLocCodPost", locCodPost);
        model.addAttribute("locCodPost", locCodPost);
        model.addAttribute("page", page == null ? 0 : page);
        model.addAttribute("size", size == null ? TAMANIO_PAGINA : size);
        model.addAttribute("totalPages", localidades.getTotalPages());
        model.addAttribute("totalElements", localidades.getTotalElements());
        model.addAttribute("number", localidades.getNumber());
        model.addAttribute("contexto", contexto);
        return VISTA;
    }
}
