package ar.com.atp.puestos.nucleo.web.rest;

import ar.com.atp.puestos.nucleo.service.NucleoConsultaService;
import ar.com.atp.puestos.nucleo.service.NucleoEscrituraService;
import ar.com.atp.puestos.nucleo.web.dto.AreaDto;
import ar.com.atp.puestos.nucleo.web.dto.AreaEscrituraDto;
import ar.com.atp.puestos.shared.web.RecursoUri;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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

/** Áreas: lectura (plan §3.1) y ABM (R-030…R-032). PK {@code AreaId} es {@code decimal(10,0)}. */
@RestController
@RequestMapping("/api/v1/areas")
public class AreaController {

    private final NucleoConsultaService consultaService;
    private final NucleoEscrituraService escrituraService;

    public AreaController(NucleoConsultaService consultaService,
            NucleoEscrituraService escrituraService) {
        this.consultaService = consultaService;
        this.escrituraService = escrituraService;
    }

    @GetMapping
    public List<AreaDto> listar() {
        return consultaService.listarAreas();
    }

    @PostMapping
    public ResponseEntity<AreaDto> crear(@Valid @RequestBody AreaEscrituraDto dto) {
        AreaDto creado = escrituraService.crearArea(dto);
        return ResponseEntity.created(RecursoUri.crear(creado.areaId())).body(creado);
    }

    @PutMapping("/{areaId}")
    public AreaDto actualizar(@PathVariable BigDecimal areaId,
            @Valid @RequestBody AreaEscrituraDto dto) {
        return escrituraService.actualizarArea(areaId, dto);
    }

    @DeleteMapping("/{areaId}")
    public ResponseEntity<Void> eliminar(@PathVariable BigDecimal areaId) {
        escrituraService.eliminarArea(areaId);
        return ResponseEntity.noContent().build();
    }
}
