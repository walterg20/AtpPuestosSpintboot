package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Clave compuesta de {@link ComisionIntegrante} ({@code COMISIO1.PueCod} + {@code CPCId} +
 * {@code CPCPerId}) — G-DATA 2026-10-22 (spec 03-login §3.1.1, R-LOG-034).
 *
 * <p>El orden reproduce el de la clave física: ({@code PueCod}, {@code CPCId}, {@code CPCPerId}).
 */
@Embeddable
public class ComisionIntegranteId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Puesto caminerero de la comisión ({@code smallint}). */
    @Column(name = "PueCod", nullable = false)
    private Integer pueCod;

    /** Número de comisión ({@code int}). */
    @Column(name = "CPCId", nullable = false)
    private Integer cpcId;

    /** Persona integrante de la comisión ({@code int}). */
    @Column(name = "CPCPerId", nullable = false)
    private Integer cpcPerId;

    public ComisionIntegranteId() {
        // requerido por JPA
    }

    public ComisionIntegranteId(Integer pueCod, Integer cpcId, Integer cpcPerId) {
        this.pueCod = pueCod;
        this.cpcId = cpcId;
        this.cpcPerId = cpcPerId;
    }

    public Integer getPueCod() {
        return pueCod;
    }

    public void setPueCod(Integer pueCod) {
        this.pueCod = pueCod;
    }

    public Integer getCpcId() {
        return cpcId;
    }

    public void setCpcId(Integer cpcId) {
        this.cpcId = cpcId;
    }

    public Integer getCpcPerId() {
        return cpcPerId;
    }

    public void setCpcPerId(Integer cpcPerId) {
        this.cpcPerId = cpcPerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ComisionIntegranteId other)) {
            return false;
        }
        return Objects.equals(pueCod, other.pueCod)
                && Objects.equals(cpcId, other.cpcId)
                && Objects.equals(cpcPerId, other.cpcPerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pueCod, cpcId, cpcPerId);
    }
}
