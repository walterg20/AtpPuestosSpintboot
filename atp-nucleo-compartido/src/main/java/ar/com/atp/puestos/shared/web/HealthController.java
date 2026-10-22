package ar.com.atp.puestos.shared.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de salud (T-005). No depende de la base de datos: permite verificar el
 * arranque de la aplicacion incluso sin conexion a SQL Server (perfil de test).
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("application", "atp-puestos");
        return body;
    }
}
