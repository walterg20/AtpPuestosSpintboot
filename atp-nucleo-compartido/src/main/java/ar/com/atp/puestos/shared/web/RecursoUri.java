package ar.com.atp.puestos.shared.web;

import java.net.URI;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Construye el {@code Location} de un recurso recién creado (plan §3.2: {@code POST} → {@code 201 +
 * Location}) a partir de la URL de la petición en curso.
 */
public final class RecursoUri {

    private RecursoUri() {
    }

    /** Añade el identificador a la ruta actual, p. ej. {@code /api/v1/provincias} → {@code /api/v1/provincias/5}. */
    public static URI crear(Object id) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }
}
