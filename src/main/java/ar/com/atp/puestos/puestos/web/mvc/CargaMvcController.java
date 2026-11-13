package ar.gob.chaco.atp.puestoscamineros.web.mvc;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.GestorContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.ProveedorNavegacion;
import ar.gob.chaco.atp.puestoscamineros.service.CargaService;
import ar.gob.chaco.atp.puestoscamineros.service.ResultadoMarcado;
import ar.gob.chaco.atp.puestoscamineros.web.mvc.form.CargaForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    public String listar(HttpServletRequest request, HttpServletResponse response, Model model) {
        proveedorNavegacion.poblar(model, request);
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        CargaForm cargaForm = new CargaForm();
        cargaForm.setContext(contexto);
        cargaForm.setFormularios(List.of());
        model.addAttribute("cargaForm", cargaForm);
        if (!model.containsAttribute("programas")) {
            model.addAttribute("programas", List.of());
        }
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        return VISTA;
    }

    @PostMapping("/buscar")
    public String buscar(@RequestParam(name = "barCode", required = false) String barCode,
            @RequestParam(name = "nroGuia", required = false) String nroGuia,
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
        } else if (nroGuia != null && !nroGuia.isBlank()) {
            dppNroForm = nroGuia.trim();
        }
        List<ar.gob.chaco.atp.puestoscamineros.domain.DetalleProductoPrimario> formularios;
        try {
            formularios = cargaService.buscarFormularios(dppNroForm);
        } catch (RuntimeException ex) {
            log.warn("No se pudo buscar formularios", ex);
            formularios = List.of();
        }
        CargaForm cargaForm = new CargaForm();
        cargaForm.setBarCode(barCode);
        cargaForm.setNroGuia(nroGuia);
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
        ar.gob.chaco.atp.puestoscamineros.domain.DetalleProductoPrimario d = resultado.detalle();
        if (d == null) {
            Long cc = conCuit == null ? null : conCuit.longValue();
            d = cargaService.obtenerDetalle(dppNroForm, cc).orElse(null);
        }
        BigDecimal ZERO = BigDecimal.ZERO;
        if (d == null) {
            cargaForm.setChasis("");
            cargaForm.setAcoplado("");
            cargaForm.setConRazSoc("");
            cargaForm.setKmFlete(ZERO);
            cargaForm.setTarifaKm(ZERO);
            cargaForm.setMontoFlete(ZERO);
            cargaForm.setAlicuota(ZERO);
            cargaForm.setImpuestoFlete(ZERO);
            cargaForm.setAdicional10(ZERO);
            cargaForm.setSubtotalFlete(ZERO);
            cargaForm.setSubtotalTraslado(ZERO);
            cargaForm.setTotalGeneral(ZERO);
            cargaForm.setFiscalizador("");
            cargaForm.setHashControl("");
            cargaForm.setExpedienteDigital(null);
            cargaForm.setDomicilioFiscal("");
            cargaForm.setPuestoIngreso("");
            cargaForm.setFechaControl(LocalDateTime.now());
            cargaForm.setTipoComprobante("");
            cargaForm.setIngresoBruto(ZERO);
            cargaForm.setAdicionalIB(ZERO);
            cargaForm.setCosechaMecanica(ZERO);
            cargaForm.setAdicionalCM(ZERO);
        } else {
            cargaForm.setChasis(trimOrEmpty(d.getDppChasis()));
            cargaForm.setAcoplado(trimOrEmpty(d.getDppAcopla()));
            String razSoc = trimOrEmpty(d.getConRazSoc());
            if (razSoc.isEmpty()) razSoc = trimOrEmpty(d.getDppTranRS());
            cargaForm.setConRazSoc(razSoc);
            cargaForm.setKmFlete(d.getDppKmFle() == null ? ZERO : d.getDppKmFle());
            cargaForm.setTarifaKm(d.getDppTarxKm() == null ? ZERO : d.getDppTarxKm());
            cargaForm.setMontoFlete(d.getDppMImpFle() == null ? ZERO : d.getDppMImpFle());
            cargaForm.setAlicuota(d.getDppAlicFle() == null ? ZERO : d.getDppAlicFle());
            cargaForm.setImpuestoFlete(d.getDppImpuFle() == null ? ZERO : d.getDppImpuFle());
            cargaForm.setAdicional10(d.getDppAdicFle() == null ? ZERO : d.getDppAdicFle());
            cargaForm.setSubtotalFlete(d.getDppImIBTot() == null ? ZERO : d.getDppImIBTot());
            cargaForm.setSubtotalTraslado(d.getDppAdIBTot() == null ? ZERO : d.getDppAdIBTot());
            cargaForm.setTotalGeneral(d.getTotalGeneral() != null ? d.getTotalGeneral() : cargaService.calcularTotalGeneral(d));
            String fiscalizador = trimOrEmpty(d.getDppUltPaOp());
            if (fiscalizador.isEmpty()) {
                fiscalizador = contexto == null ? "" : (contexto.usuarioNombre() == null ? "" : contexto.usuarioNombre());
            }
            cargaForm.setFiscalizador(fiscalizador);
            String puesto = d.getDppActaPue() != null ? String.valueOf(d.getDppActaPue())
                    : (contexto == null || contexto.sucursal() == null ? "" : String.valueOf(contexto.sucursal()));
            String fecha = d.getDppFchMov() == null ? "" : d.getDppFchMov().toString();
            String nro = d.getDppNroForm() == null ? (dppNroForm == null ? "" : dppNroForm) : d.getDppNroForm();
            cargaForm.setHashControl("HASH-" + nro + "-" + puesto + "-" + fecha);
            cargaForm.setExpedienteDigital(d.getDppActaNro());
            cargaForm.setDomicilioFiscal(trimOrEmpty(d.getDppAnuObs()));
            String puestoIngreso = d.getDppActaPue() != null ? String.valueOf(d.getDppActaPue())
                    : (contexto == null ? "" : (contexto.sucursalNombre() == null ? "" : contexto.sucursalNombre()));
            cargaForm.setPuestoIngreso(puestoIngreso);
            cargaForm.setFechaControl(d.getDppFchMov() == null ? LocalDateTime.now() : d.getDppFchMov());
            String tipo = trimOrEmpty(d.getDppTipoGui());
            if (tipo.isEmpty()) {
                tipo = d.getDppTipoPr() != null ? String.valueOf(d.getDppTipoPr()) : "F";
            }
            cargaForm.setDpptipoGui(tipo);
            cargaForm.setTipoComprobante(tipo);
            cargaForm.setIngresoBruto(ZERO);
            cargaForm.setAdicionalIB(ZERO);
            cargaForm.setCosechaMecanica(ZERO);
            cargaForm.setAdicionalCM(ZERO);
        }
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
            @RequestParam(name = "nroGuia", required = false) String nroGuia,
            @RequestParam(name = "barCode", required = false) String barCode,
            HttpServletRequest request, HttpServletResponse response, Model model) {
        proveedorNavegacion.poblar(model, request);
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        CargaForm cargaForm = new CargaForm();
        cargaForm.setDppNroForm(dppNroForm);
        cargaForm.setDpptipoGui(dpptipoGui);
        cargaForm.setDuplicado(duplicado);
        cargaForm.setNroGuia(nroGuia);
        cargaForm.setBarCode(barCode);
        cargaForm.setContext(contexto);
        cargaForm.setMostrarModal(true);
        Long conCuit = null;
        if (conCuitStr != null && !conCuitStr.isBlank()) {
            try {
                conCuit = new BigDecimal(conCuitStr).longValue();
                cargaForm.setConCuit(conCuit);
            } catch (NumberFormatException ex) {
                // ignore
            }
        }
        List<ar.gob.chaco.atp.puestoscamineros.domain.DetalleProductoPrimario> formularios = List.of();
        try {
            if (nroGuia != null && !nroGuia.isBlank()) {
                formularios = cargaService.buscarFormularios(nroGuia.trim());
            } else if (barCode != null && !barCode.isBlank()) {
                formularios = cargaService.buscarFormularios(barCode.trim());
            } else if (dppNroForm != null && !dppNroForm.isBlank()) {
                formularios = cargaService.buscarFormularios(dppNroForm.trim());
            } else {
                formularios = List.of();
            }
        } catch (RuntimeException ex) {
            log.warn("No se pudo repoblar la grilla en detalle", ex);
            formularios = List.of();
        }
        if (formularios == null) {
            formularios = List.of();
        }
        cargaForm.setFormularios(formularios);
        java.util.Optional<ar.gob.chaco.atp.puestoscamineros.domain.DetalleProductoPrimario> opt =
                cargaService.obtenerDetalle(dppNroForm, conCuit);
        ar.gob.chaco.atp.puestoscamineros.domain.DetalleProductoPrimario d = opt.orElse(null);
        java.math.BigDecimal ZERO = java.math.BigDecimal.ZERO;
        if (d == null) {
            cargaForm.setChasis("");
            cargaForm.setAcoplado("");
            cargaForm.setConRazSoc("");
            cargaForm.setKmFlete(ZERO);
            cargaForm.setTarifaKm(ZERO);
            cargaForm.setMontoFlete(ZERO);
            cargaForm.setAlicuota(ZERO);
            cargaForm.setImpuestoFlete(ZERO);
            cargaForm.setAdicional10(ZERO);
            cargaForm.setSubtotalFlete(ZERO);
            cargaForm.setSubtotalTraslado(ZERO);
            cargaForm.setTotalGeneral(ZERO);
            cargaForm.setFiscalizador("");
            cargaForm.setHashControl("");
            cargaForm.setExpedienteDigital(null);
            cargaForm.setDomicilioFiscal("");
            cargaForm.setPuestoIngreso("");
            cargaForm.setFechaControl(java.time.LocalDateTime.now());
            cargaForm.setTipoComprobante("");
            cargaForm.setIngresoBruto(ZERO);
            cargaForm.setAdicionalIB(ZERO);
            cargaForm.setCosechaMecanica(ZERO);
            cargaForm.setAdicionalCM(ZERO);
        } else {
            cargaForm.setChasis(trimOrEmpty(d.getDppChasis()));
            cargaForm.setAcoplado(trimOrEmpty(d.getDppAcopla()));
            String razSoc = trimOrEmpty(d.getConRazSoc());
            if (razSoc.isEmpty()) razSoc = trimOrEmpty(d.getDppTranRS());
            cargaForm.setConRazSoc(razSoc);
            cargaForm.setKmFlete(d.getDppKmFle() == null ? ZERO : d.getDppKmFle());
            cargaForm.setTarifaKm(d.getDppTarxKm() == null ? ZERO : d.getDppTarxKm());
            cargaForm.setMontoFlete(d.getDppMImpFle() == null ? ZERO : d.getDppMImpFle());
            cargaForm.setAlicuota(d.getDppAlicFle() == null ? ZERO : d.getDppAlicFle());
            cargaForm.setImpuestoFlete(d.getDppImpuFle() == null ? ZERO : d.getDppImpuFle());
            cargaForm.setAdicional10(d.getDppAdicFle() == null ? ZERO : d.getDppAdicFle());
            cargaForm.setSubtotalFlete(d.getDppImIBTot() == null ? ZERO : d.getDppImIBTot());
            cargaForm.setSubtotalTraslado(d.getDppAdIBTot() == null ? ZERO : d.getDppAdIBTot());
            cargaForm.setTotalGeneral(d.getTotalGeneral() != null ? d.getTotalGeneral() : cargaService.calcularTotalGeneral(d));
            String fiscalizador = trimOrEmpty(d.getDppUltPaOp());
            if (fiscalizador.isEmpty()) {
                fiscalizador = contexto == null ? "" : (contexto.usuarioNombre() == null ? "" : contexto.usuarioNombre());
            }
            cargaForm.setFiscalizador(fiscalizador);
            String puesto = d.getDppActaPue() != null ? String.valueOf(d.getDppActaPue())
                    : (contexto == null || contexto.sucursal() == null ? "" : String.valueOf(contexto.sucursal()));
            String fecha = d.getDppFchMov() == null ? "" : d.getDppFchMov().toString();
            String nro = d.getDppNroForm() == null ? (dppNroForm == null ? "" : dppNroForm) : d.getDppNroForm();
            cargaForm.setHashControl("HASH-" + nro + "-" + puesto + "-" + fecha);
            cargaForm.setExpedienteDigital(d.getDppActaNro());
            cargaForm.setDomicilioFiscal(trimOrEmpty(d.getDppAnuObs()));
            String puestoIngreso = d.getDppActaPue() != null ? String.valueOf(d.getDppActaPue())
                    : (contexto == null ? "" : (contexto.sucursalNombre() == null ? "" : contexto.sucursalNombre()));
            cargaForm.setPuestoIngreso(puestoIngreso);
            cargaForm.setFechaControl(d.getDppFchMov() == null ? java.time.LocalDateTime.now() : d.getDppFchMov());
            String tipo = trimOrEmpty(d.getDppTipoGui());
            if (tipo.isEmpty()) {
                tipo = d.getDppTipoPr() != null ? String.valueOf(d.getDppTipoPr()) : "F";
            }
            cargaForm.setTipoComprobante(tipo);
            cargaForm.setIngresoBruto(ZERO);
            cargaForm.setAdicionalIB(ZERO);
            cargaForm.setCosechaMecanica(ZERO);
            cargaForm.setAdicionalCM(ZERO);
        }
        model.addAttribute("cargaForm", cargaForm);
        if (!model.containsAttribute("programas")) {
            model.addAttribute("programas", List.of());
        }
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
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

    private static String trimOrEmpty(String v) {
        return v == null ? "" : v.trim();
    }
}
