package ar.com.atp.puestos.nucleo.web.rest;

import ar.com.atp.puestos.nucleo.service.NucleoConsultaService;
import ar.com.atp.puestos.nucleo.service.NucleoEscrituraService;
import ar.com.atp.puestos.nucleo.web.dto.FeriadoDto;
import ar.com.atp.puestos.nucleo.web.dto.FeriadoEscrituraDto;
import ar.com.atp.puestos.shared.web.RecursoUri;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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

/**
 * Feriados: lectura por rango (plan §3.1) y ABM (R-030…R-033). La auditoría {@code FerFchReg}/
 * {@code FerUsuario} se rellena en servidor (R-033); el body no la incluye.
 */
@RestController
@RequestMapping("/api/v1/feriados")
public class FeriadoController {

    private final NucleoConsultaService consultaService;
    private final NucleoEscrituraService escrituraService;

    public FeriadoController(NucleoConsultaService consultaService,
            NucleoEscrituraService escrituraService) {
        this.consultaService = consultaService;
        this.escrituraService = escrituraService;
    }

    @GetMapping
    public List<FeriadoDto> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return consultaService.listarFeriados(desde, hasta);
    }

    @PostMapping
    public ResponseEntity<FeriadoDto> crear(@Valid @RequestBody FeriadoEscrituraDto dto) {
        FeriadoDto creado = escrituraService.crearFeriado(dto);
        return ResponseEntity.created(RecursoUri.crear(creado.ferFecha())).body(creado);
    }

    @PutMapping("/{ferFecha}")
    public FeriadoDto actualizar(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ferFecha,
            @Valid @RequestBody FeriadoEscrituraDto dto) {
        return escrituraService.actualizarFeriado(ferFecha, dto);
    }

    @DeleteMapping("/{ferFecha}")
    public ResponseEntity<Void> eliminar(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ferFecha) {
        escrituraService.eliminarFeriado(ferFecha);
        return ResponseEntity.noContent().build();
    }
}
