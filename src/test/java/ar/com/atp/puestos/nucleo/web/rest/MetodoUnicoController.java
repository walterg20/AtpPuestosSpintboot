package ar.gob.chaco.atp.puestoscamineros.nucleo.web.rest;

import ar.gob.chaco.atp.puestoscamineros.shared.error.GlobalExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador sólo de test (T-415/R-209): expone un recurso que <strong>sólo</strong> admite
 * {@code GET}, para poder provocar un método no soportado (405) contra una ruta que sí existe.
 *
 * <p>Es <strong>top-level</strong> a propósito, por la misma razón que
 * {@link ExplodingController}: una clase anidada estática no la registra el escaneo de componentes
 * del slice {@code @WebMvcTest} y el controller quedaría sin mapear.
 */
@RestController
public class MetodoUnicoController {

    /** Recurso que sólo admite GET: un POST contra esta ruta ⇒ 405 (R-209). */
    @GetMapping("/api/v1/test/solo-get")
    public String soloGet() {
        return "ok";
    }
}
