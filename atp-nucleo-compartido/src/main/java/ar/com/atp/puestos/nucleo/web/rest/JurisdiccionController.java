package ar.com.atp.puestos.nucleo.web.rest;

import ar.com.atp.puestos.nucleo.service.NucleoConsultaService;
import ar.com.atp.puestos.nucleo.service.NucleoEscrituraService;
import ar.com.atp.puestos.nucleo.web.dto.JurisdiccionDto;
import ar.com.atp.puestos.nucleo.web.dto.JurisdiccionEscrituraDto;
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

/** Jurisdicciones: lectura (plan §3.1) y ABM (R-030…R-032). */
@RestController
@RequestMapping("/api/v1/jurisdicciones")
public class JurisdiccionController {

    private final NucleoConsultaService consultaService;
    private final NucleoEscrituraService escrituraService;

    public JurisdiccionController(NucleoConsultaService consultaService,
            NucleoEscrituraService escrituraService) {
        this.consultaService = consultaService;
        this.escrituraService = escrituraService;
    }

    @GetMapping
    public List<JurisdiccionDto> listar() {
        return consultaService.listarJurisdicciones();
    }

    @PostMapping
    public ResponseEntity<JurisdiccionDto> crear(@Valid @RequestBody JurisdiccionEscrituraDto dto) {
        JurisdiccionDto creado = escrituraService.crearJurisdiccion(dto);
        return ResponseEntity.created(RecursoUri.crear(creado.jurCod())).body(creado);
    }

    @PutMapping("/{jurCod}")
    public JurisdiccionDto actualizar(@PathVariable Integer jurCod,
            @Valid @RequestBody JurisdiccionEscrituraDto dto) {
        return escrituraService.actualizarJurisdiccion(jurCod, dto);
    }

    @DeleteMapping("/{jurCod}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer jurCod) {
        escrituraService.eliminarJurisdiccion(jurCod);
        return ResponseEntity.noContent().build();
    }
}
