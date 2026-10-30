package ar.com.atp.puestos.puestos.web.rest;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.GestorContextoSesion;
import ar.com.atp.puestos.puestos.domain.DetalleProductoPrimario;
import ar.com.atp.puestos.puestos.service.CargaService;
import ar.com.atp.puestos.puestos.service.ResultadoMarcado;
import ar.com.atp.puestos.puestos.web.dto.MarcarGuiaRequest;
import ar.com.atp.puestos.puestos.web.dto.MarcarGuiaResponse;
import ar.com.atp.puestos.shared.error.ConflictException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cargas")
public class CargaRestController {

    private final CargaService cargaService;
    private final GestorContextoSesion gestorContextoSesion;

    public CargaRestController(CargaService cargaService, GestorContextoSesion gestorContextoSesion) {
        this.cargaService = cargaService;
        this.gestorContextoSesion = gestorContextoSesion;
    }

    @GetMapping("/formularios")
    public List<DetalleProductoPrimario> formularios(@RequestParam(name = "dppNroForm", required = false) String dppNroForm) {
        return cargaService.buscarFormularios(dppNroForm);
    }

    @PostMapping("/marcar")
    public MarcarGuiaResponse marcar(@RequestBody MarcarGuiaRequest req, HttpServletRequest request) {
        ContextoSesion context = gestorContextoSesion.recuperar(request);
        Short sucNroId = req.sucNroId();
        String sucTipo = req.sucTipo();
        if (sucNroId == null && context != null) {
            sucNroId = context.sucursal() == null ? null : context.sucursal().shortValue();
        }
        if (sucTipo == null && context != null) {
            sucTipo = context.tipo();
        }
        String operador = req.dppPaseOperador();
        if (operador == null && context != null) {
            operador = context.usuario();
        }
        ResultadoMarcado resultado = cargaService.marcarGuia(
                req.conCuit(), req.dppSucu(), req.calPerFisc(), req.dppSecu(), req.dppNroForm(),
                operador, sucTipo, sucNroId, context);
        if (!resultado.exito()) {
            throw new ConflictException("MARCAR_GUIA", resultado.mensaje());
        }
        return new MarcarGuiaResponse(resultado.duplicado(), resultado.mensaje());
    }
}