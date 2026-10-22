package ar.com.atp.puestos.nucleo.web.rest;

import ar.com.atp.puestos.nucleo.service.NucleoConsultaService;
import ar.com.atp.puestos.nucleo.service.NucleoEscrituraService;
import ar.com.atp.puestos.nucleo.web.dto.CalleDto;
import ar.com.atp.puestos.nucleo.web.dto.CalleEscrituraDto;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Calles: lectura con filtro {@code q} (plan §3.1) y ABM (R-030…R-032). */
@RestController
@RequestMapping("/api/v1/calles")
public class CalleController {

    private final NucleoConsultaService consultaService;
    private final NucleoEscrituraService escrituraService;

    public CalleController(NucleoConsultaService consultaService,
            NucleoEscrituraService escrituraService) {
        this.consultaService = consultaService;
        this.escrituraService = escrituraService;
    }

    @GetMapping
    public List<CalleDto> listar(@RequestParam(required = false) String q) {
        return consultaService.listarCalles(q);
    }

    @GetMapping("/{callCod}")
    public CalleDto obtener(@PathVariable Integer callCod) {
        return consultaService.obtenerCalle(callCod);
    }

    @PostMapping
    public ResponseEntity<CalleDto> crear(@Valid @RequestBody CalleEscrituraDto dto) {
        CalleDto creado = escrituraService.crearCalle(dto);
        return ResponseEntity.created(RecursoUri.crear(creado.callCod())).body(creado);
    }

    @PutMapping("/{callCod}")
    public CalleDto actualizar(@PathVariable Integer callCod,
            @Valid @RequestBody CalleEscrituraDto dto) {
        return escrituraService.actualizarCalle(callCod, dto);
    }

    @DeleteMapping("/{callCod}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer callCod) {
        escrituraService.eliminarCalle(callCod);
        return ResponseEntity.noContent().build();
    }
}
