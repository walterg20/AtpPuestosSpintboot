package ar.com.atp.puestos.nucleo.web.rest;

import ar.com.atp.puestos.nucleo.service.BusquedaService;
import ar.com.atp.puestos.nucleo.service.NucleoConsultaService;
import ar.com.atp.puestos.nucleo.service.NucleoEscrituraService;
import ar.com.atp.puestos.nucleo.web.dto.CaminoDto;
import ar.com.atp.puestos.nucleo.web.dto.CaminoEscrituraDto;
import ar.com.atp.puestos.nucleo.web.dto.CaminoRutaDto;
import ar.com.atp.puestos.shared.web.RecursoUri;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Caminos: lectura, {@code BuscarCamino} (R-011) y ABM (R-030…R-032). */
@RestController
@RequestMapping("/api/v1/caminos")
public class CaminoController {

    private final NucleoConsultaService consultaService;
    private final NucleoEscrituraService escrituraService;
    private final BusquedaService busquedaService;

    public CaminoController(NucleoConsultaService consultaService,
            NucleoEscrituraService escrituraService,
            BusquedaService busquedaService) {
        this.consultaService = consultaService;
        this.escrituraService = escrituraService;
        this.busquedaService = busquedaService;
    }

    @GetMapping
    public List<CaminoDto> listar() {
        return consultaService.listarCaminos();
    }

    @GetMapping("/{camID}")
    public CaminoDto obtener(@PathVariable String camID) {
        return consultaService.obtenerCamino(camID);
    }

    /** {@code BuscarCamino} (R-011/R-022): nunca 404, {@code CanRuta} recortado o cadena vacía. */
    @GetMapping("/{camID}/ruta")
    public CaminoRutaDto ruta(@PathVariable String camID) {
        return busquedaService.rutaCamino(camID);
    }

    @PostMapping
    public ResponseEntity<CaminoDto> crear(@Valid @RequestBody CaminoEscrituraDto dto) {
        CaminoDto creado = escrituraService.crearCamino(dto);
        return ResponseEntity.created(RecursoUri.crear(creado.camID())).body(creado);
    }

    @PutMapping("/{camID}")
    public CaminoDto actualizar(@PathVariable String camID,
            @Valid @RequestBody CaminoEscrituraDto dto) {
        return escrituraService.actualizarCamino(camID, dto);
    }

    @DeleteMapping("/{camID}")
    public ResponseEntity<Void> eliminar(@PathVariable String camID) {
        escrituraService.eliminarCamino(camID);
        return ResponseEntity.noContent().build();
    }
}
