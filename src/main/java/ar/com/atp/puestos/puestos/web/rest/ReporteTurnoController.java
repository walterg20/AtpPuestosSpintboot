package ar.gob.chaco.atp.puestoscamineros.web.rest;

import ar.gob.chaco.atp.puestoscamineros.service.TurnoReporteService;
import ar.gob.chaco.atp.puestoscamineros.shared.error.ApiError;
import ar.gob.chaco.atp.puestoscamineros.shared.error.NotFoundException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/turnos")
public class ReporteTurnoController {

    private final TurnoReporteService turnoReporteService;

    public ReporteTurnoController(TurnoReporteService turnoReporteService) {
        this.turnoReporteService = turnoReporteService;
    }

    @GetMapping({"/{sucNroId}/{sucTipo}/{turNroId}/resumen",
            "/{sucNroId}/{sucTipo}/{turNroId}/resumen.pdf"})
    public ResponseEntity<?> resumen(@PathVariable("sucNroId") Integer sucNroId,
            @PathVariable("sucTipo") String sucTipo,
            @PathVariable("turNroId") Integer turNroId) {
        try {
            byte[] pdf = turnoReporteService.generarPdf(sucNroId, sucTipo, turNroId);
            String filename = "ResumenTurno_" + sucNroId + "_" + sucTipo + "_" + turNroId + ".pdf";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20") + "\"");
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (NotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError("TURNO_NO_ENCONTRADO", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError("ERROR_GENERACION_REPORTE", "Error al generar el reporte."));
        }
    }
}