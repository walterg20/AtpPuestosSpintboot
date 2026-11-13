package ar.gob.chaco.atp.puestoscamineros.nucleo.web.rest;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.NucleoConsultaService;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.NucleoEscrituraService;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProvinciaDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProvinciaEscrituraDto;
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
import org.springframework.web.bind.annotation.RestController;

/** Provincias: lectura (plan §3.1) y ABM (R-030…R-032, plan §3.2). */
@RestController
@RequestMapping("/api/v1/provincias")
public class ProvinciaController {

    private final NucleoConsultaService consultaService;
    private final NucleoEscrituraService escrituraService;

    public ProvinciaController(NucleoConsultaService consultaService,
            NucleoEscrituraService escrituraService) {
        this.consultaService = consultaService;
        this.escrituraService = escrituraService;
    }

    @GetMapping
    public List<ProvinciaDto> listar() {
        return consultaService.listarProvincias();
    }

    @GetMapping("/{provCod}")
    public ProvinciaDto obtener(@PathVariable Integer provCod) {
        return consultaService.obtenerProvincia(provCod);
    }

    @PostMapping
    public ResponseEntity<ProvinciaDto> crear(@Valid @RequestBody ProvinciaEscrituraDto dto) {
        ProvinciaDto creado = escrituraService.crearProvincia(dto);
        return ResponseEntity.created(RecursoUri.crear(creado.provCod())).body(creado);
    }

    @PutMapping("/{provCod}")
    public ProvinciaDto actualizar(@PathVariable Integer provCod,
            @Valid @RequestBody ProvinciaEscrituraDto dto) {
        return escrituraService.actualizarProvincia(provCod, dto);
    }

    @DeleteMapping("/{provCod}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer provCod) {
        escrituraService.eliminarProvincia(provCod);
        return ResponseEntity.noContent().build();
    }
}
