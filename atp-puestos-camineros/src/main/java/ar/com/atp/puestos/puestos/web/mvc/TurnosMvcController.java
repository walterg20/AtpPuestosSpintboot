package ar.com.atp.puestos.puestos.web.mvc;

import ar.com.atp.puestos.puestos.service.TurnoReporteService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reportes/turnos")
public class TurnosMvcController {

    private final TurnoReporteService turnoReporteService;

    public TurnosMvcController(TurnoReporteService turnoReporteService) {
        this.turnoReporteService = turnoReporteService;
    }

    @GetMapping("/{sucNroId}/{sucTipo}/{turNroId}/resumen.pdf")
    public ResponseEntity<byte[]> resumenPdf(@PathVariable("sucNroId") Integer sucNroId,
            @PathVariable("sucTipo") String sucTipo,
            @PathVariable("turNroId") Integer turNroId) {
        byte[] pdf = turnoReporteService.generarPdf(sucNroId, sucTipo, turNroId);
        String filename = "ResumenTurno_" + sucNroId + "_" + sucTipo + "_" + turNroId + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20") + "\"");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}