package ar.com.atp.puestos.puestos.web.mvc;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.GestorContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.ProveedorNavegacion;
import ar.com.atp.puestos.puestos.service.CargaService;
import ar.com.atp.puestos.puestos.service.ResultadoMarcado;
import ar.com.atp.puestos.puestos.web.mvc.form.CargaForm;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/cargas")
public class CargaMvcController {

    private static final String VISTA = "puestos/control-cargas";
    private static final Logger log = LoggerFactory.getLogger(CargaMvcController.class);

    private final CargaService cargaService;
    private final ProveedorNavegacion proveedorNavegacion;
    private final GestorContextoSesion gestorContextoSesion;

    public CargaMvcController(CargaService cargaService, ProveedorNavegacion proveedorNavegacion,
            GestorContextoSesion gestorContextoSesion) {
        this.cargaService = cargaService;
        this.proveedorNavegacion = proveedorNavegacion;
        this.gestorContextoSesion = gestorContextoSesion;
    }

    @GetMapping
    public String listar(HttpServletRequest request, Model model) {
        proveedorNavegacion.poblar(model, request);
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        CargaForm cargaForm = new CargaForm();
        cargaForm.setContext(contexto);
        cargaForm.setFormularios(List.of());
        model.addAttribute("cargaForm", cargaForm);
        if (!model.containsAttribute("programas")) {
            model.addAttribute("programas", List.of());
        }
        return VISTA;
    }

    @PostMapping("/buscar")
    public String buscar(@RequestParam(name = "barCode", required = false) String barCode,
            HttpServletRequest request, Model model) {
        proveedorNavegacion.poblar(model, request);
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        String dppNroForm = null;
        if (barCode != null && !barCode.isBlank()) {
            if (barCode.length() >= 31) {
                dppNroForm = barCode.substring(21, 31);
            } else {
                dppNroForm = barCode.trim();
            }
        }
        List<ar.com.atp.puestos.puestos.domain.DetalleProductoPrimario> formularios;
        try {
            formularios = cargaService.buscarFormularios(dppNroForm);
        } catch (RuntimeException ex) {
            log.warn("No se pudo buscar formularios", ex);
            formularios = List.of();
        }
        CargaForm cargaForm = new CargaForm();
        cargaForm.setBarCode(barCode);
        cargaForm.setDppNroForm(dppNroForm);
        cargaForm.setContext(contexto);
        cargaForm.setFormularios(formularios);
        model.addAttribute("cargaForm", cargaForm);
        if (!model.containsAttribute("programas")) {
            model.addAttribute("programas", List.of());
        }
        return VISTA;
    }

    @PostMapping("/marcar")
    public String marcar(@RequestParam(name = "conCuit", required = false) String conCuitStr,
            @RequestParam(name = "dppSucu", required = false) Short dppSucu,
            @RequestParam(name = "calPerFisc", required = false) Integer calPerFisc,
            @RequestParam(name = "dppSecu", required = false) Short dppSecu,
            @RequestParam(name = "dppNroForm", required = false) String dppNroForm,
            @RequestParam(name = "dppPaseOperador", required = false) String dppPaseOperador,
            @RequestParam(name = "sucTipo", required = false) String sucTipo,
            @RequestParam(name = "sucNroId", required = false) Short sucNroId,
            HttpServletRequest request, Model model) {
        proveedorNavegacion.poblar(model, request);
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        BigDecimal conCuit = null;
        if (conCuitStr != null && !conCuitStr.isBlank()) {
            try {
                conCuit = new BigDecimal(conCuitStr);
            } catch (NumberFormatException ex) {
                conCuit = null;
            }
        }
        if (dppPaseOperador == null) {
            dppPaseOperador = contexto == null ? null : contexto.usuario();
        }
        if (sucTipo == null) {
            sucTipo = contexto == null ? null : contexto.tipo();
        }
        if (sucNroId == null && contexto != null && contexto.sucursal() != null) {
            sucNroId = contexto.sucursal().shortValue();
        }
        ResultadoMarcado resultado = cargaService.marcarGuia(conCuit, dppSucu, calPerFisc, dppSecu,
                dppNroForm, dppPaseOperador, sucTipo, sucNroId, contexto);
        CargaForm cargaForm = new CargaForm();
        cargaForm.setConCuit(conCuit == null ? null : conCuit.longValue());
        cargaForm.setDppNroForm(dppNroForm);
        cargaForm.setContext(contexto);
        cargaForm.setMensaje(resultado.mensaje());
        cargaForm.setDuplicado(resultado.duplicado());
        cargaForm.setMostrarModal(true);
        model.addAttribute("cargaForm", cargaForm);
        if (!model.containsAttribute("programas")) {
            model.addAttribute("programas", List.of());
        }
        return VISTA;
    }

    @GetMapping("/detalle")
    public String detalle(@RequestParam(name = "dppNroForm", required = false) String dppNroForm,
            @RequestParam(name = "dpptipoGui", required = false) String dpptipoGui,
            @RequestParam(name = "duplicado", required = false) Integer duplicado,
            @RequestParam(name = "conCuit", required = false) String conCuitStr,
            HttpServletRequest request, Model model) {
        proveedorNavegacion.poblar(model, request);
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        CargaForm cargaForm = new CargaForm();
        cargaForm.setDppNroForm(dppNroForm);
        cargaForm.setDpptipoGui(dpptipoGui);
        cargaForm.setDuplicado(duplicado);
        cargaForm.setContext(contexto);
        cargaForm.setMostrarModal(true);
        if (conCuitStr != null && !conCuitStr.isBlank()) {
            try {
                cargaForm.setConCuit(new BigDecimal(conCuitStr).longValue());
            } catch (NumberFormatException ex) {
                // ignore
            }
        }
        model.addAttribute("cargaForm", cargaForm);
        if (!model.containsAttribute("programas")) {
            model.addAttribute("programas", List.of());
        }
        return VISTA;
    }

    @GetMapping("/tabla")
    public String tabla() {
        return "redirect:/cargas";
    }

    @GetMapping("/pagos")
    public String pagos() {
        return "redirect:/cargas";
    }
}
