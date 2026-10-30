package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Clave compuesta de {@link ReceptorPersona} ({@code RECEPTO1.ReceId} + {@code PerId}) —
 * G-DATA 2026-10-22 (spec 03-login §3.1.1, R-LOG-034).
 *
 * <p>El orden reproduce el de la clave física: ({@code ReceId}, {@code PerId}).
 */
@Embeddable
public class ReceptorPersonaId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Receptoría ({@code smallint}). */
    @Column(name = "ReceId", nullable = false)
    private Integer receId;

    /** Persona ({@code int}). */
    @Column(name = "PerId", nullable = false)
    private Integer perId;

    public ReceptorPersonaId() {
        // requerido por JPA
    }

    public ReceptorPersonaId(Integer receId, Integer perId) {
        this.receId = receId;
        this.perId = perId;
    }

    public Integer getReceId() {
        return receId;
    }

    public void setReceId(Integer receId) {
        this.receId = receId;
    }

    public Integer getPerId() {
        return perId;
    }

    public void setPerId(Integer perId) {
        this.perId = perId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReceptorPersonaId other)) {
            return false;
        }
        return Objects.equals(receId, other.receId) && Objects.equals(perId, other.perId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receId, perId);
    }
}
