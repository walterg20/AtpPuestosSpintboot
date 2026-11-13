package ar.gob.chaco.atp.puestoscamineros.web.mvc;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Turno;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.TurnoRepositorio;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.GestorContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.ProveedorNavegacion;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Listado de turnos abiertos para el flujo de "Carga Manual" (spec 12, R-TURNO-001…R-TURNO-006).
 *
 * <p>Es un adaptador de presentación: delega el filtrado en {@link TurnoRepositorio} y arma el
 * view model. No implementa reglas de negocio (G7).
 */
@Controller
@RequestMapping("/actas")
public class CargaManualController {

    /** Vista del listado de turnos abiertos. */
    private static final String VISTA_TURNOS_ABIERTOS = "puestos/turnos-abiertos";

    /** Tamaño de página por defecto: paridad con {@code GridTurnos.MaxRows = 10}. */
    private static final int TAMANIO_PAGINA = 10;

    private final TurnoRepositorio turnoRepositorio;
    private final ProveedorNavegacion proveedorNavegacion;
    private final GestorContextoSesion gestorContextoSesion;

    public CargaManualController(TurnoRepositorio turnoRepositorio,
            ProveedorNavegacion proveedorNavegacion, GestorContextoSesion gestorContextoSesion) {
        this.turnoRepositorio = turnoRepositorio;
        this.proveedorNavegacion = proveedorNavegacion;
        this.gestorContextoSesion = gestorContextoSesion;
    }

    /**
     * Listado de turnos abiertos filtrado por sucursal y tipo del contexto.
     */
    @GetMapping("/carga-manual")
    public String cargaManual(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            HttpServletRequest request, Model model) {
        proveedorNavegacion.poblar(model, request);
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);

        Integer sucursal = contexto == null ? null : contexto.sucursal();
        String tipo = contexto == null ? null : contexto.tipo();

        Pageable pageable = PageRequest.of(
                page == null ? 0 : page,
                size == null ? TAMANIO_PAGINA : size,
                Sort.by("sucNroId", "sucTipo", "turNroId"));

        Page<Turno> turnos;
        try {
            turnos = turnoRepositorio.listarAbiertosPorSucursal(sucursal, tipo, pageable);
        } catch (RuntimeException ex) {
            turnos = Page.empty(pageable);
        }
        if (turnos == null) {
            turnos = Page.empty(pageable);
        }

        model.addAttribute("turnos", turnos);
        model.addAttribute("contexto", contexto);
        return VISTA_TURNOS_ABIERTOS;
    }
}
