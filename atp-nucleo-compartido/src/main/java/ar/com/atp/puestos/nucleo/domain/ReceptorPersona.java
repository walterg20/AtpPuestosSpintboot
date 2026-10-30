package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Tabla legacy {@code Recepto1} → tabla física {@code RECEPTO1} (spec 03-login §3.1.1, plan §5.3).
 *
 * <p>Asigna personas a receptorías. La PK física es compuesta ({@code ReceId}, {@code PerId}) —
 * R-LOG-034. La tercera columna física ({@code RecUsuFchD}) <strong>no</strong> se mapea: el
 * {@code defined by} del legacy que la usaba está comentado, de modo que la consulta de
 * {@code Seg_BusRece} no ordena por ella.
 */
@Entity
@Table(name = "RECEPTO1")
public class ReceptorPersona {

    /** PK compuesta: receptoría y persona. */
    @EmbeddedId
    private ReceptorPersonaId id;

    public ReceptorPersona() {
        // requerido por JPA
    }

    public ReceptorPersona(ReceptorPersonaId id) {
        this.id = id;
    }

    public ReceptorPersonaId getId() {
        return id;
    }

    public void setId(ReceptorPersonaId id) {
        this.id = id;
    }

    /** Receptoría ({@code smallint}). */
    public Integer getReceId() {
        return id == null ? null : id.getReceId();
    }

    /** Persona ({@code int}). */
    public Integer getPerId() {
        return id == null ? null : id.getPerId();
    }
}
