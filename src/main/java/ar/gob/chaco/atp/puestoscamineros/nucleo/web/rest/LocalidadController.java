package ar.gob.chaco.atp.puestoscamineros.nucleo.web.rest;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.BusquedaService;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.NucleoConsultaService;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.NucleoEscrituraService;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.LocalidadDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.LocalidadEscrituraDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.LocalidadNombreDto;
import ar.gob.chaco.atp.puestoscamineros.shared.web.RecursoUri;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Localidades: lectura, {@code Bus_DesLoc} (R-010) y ABM (R-030…R-032). */
@RestController
@RequestMapping("/api/v1/localidades")
public class LocalidadController {

    private final NucleoConsultaService consultaService;
    private final NucleoEscrituraService escrituraService;
    private final BusquedaService busquedaService;

    public LocalidadController(NucleoConsultaService consultaService,
            NucleoEscrituraService escrituraService,
            BusquedaService busquedaService) {
        this.consultaService = consultaService;
        this.escrituraService = escrituraService;
        this.busquedaService = busquedaService;
    }

    @GetMapping
    public List<LocalidadDto> listar(@RequestParam(required = false) Integer provCod) {
        return consultaService.listarLocalidades(provCod);
    }

    @GetMapping("/{locCod}")
    public LocalidadDto obtener(@PathVariable Integer locCod) {
        return consultaService.obtenerLocalidad(locCod);
    }

    /** {@code Bus_DesLoc} (R-010/R-022): nunca 404, cadena vacía si no existe. */
    @GetMapping("/{locCod}/nombre")
    public LocalidadNombreDto nombre(@PathVariable Integer locCod) {
        return busquedaService.nombreLocalidad(locCod);
    }

    @PostMapping
    public ResponseEntity<LocalidadDto> crear(@Valid @RequestBody LocalidadEscrituraDto dto) {
        LocalidadDto creado = escrituraService.crearLocalidad(dto);
        return ResponseEntity.created(RecursoUri.crear(creado.locCod())).body(creado);
    }

    @PutMapping("/{locCod}")
    public LocalidadDto actualizar(@PathVariable Integer locCod,
            @Valid @RequestBody LocalidadEscrituraDto dto) {
        return escrituraService.actualizarLocalidad(locCod, dto);
    }

    @DeleteMapping("/{locCod}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer locCod) {
        escrituraService.eliminarLocalidad(locCod);
        return ResponseEntity.noContent().build();
    }
}
