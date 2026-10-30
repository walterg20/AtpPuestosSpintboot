package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Tabla legacy {@code Comisio1} → tabla física {@code COMISIO1} (spec 03-login §3.1.1, plan §5.3).
 *
 * <p>Nivel de detalle de las comisiones: los integrantes. La PK física es compuesta y completa
 * ({@code PueCod}, {@code CPCId}, {@code CPCPerId}) — R-LOG-034.
 *
 * <p><strong>{@code CPCPerNyA} NO es una columna</strong>: es un atributo <em>calculado</em> de la
 * KB (G-DATA 2026-10-22: 0 coincidencias en toda la base). El nombre y apellido del integrante se
 * resuelve por <strong>JOIN a {@code PERSONAS}</strong> ({@code PerNomyApe}) en el repositorio
 * (D-LOG-20, R-LOG-035, R-LOG-036).
 */
@Entity
@Table(name = "COMISIO1")
public class ComisionIntegrante {

    /** PK compuesta: puesto, comisión e integrante. */
    @EmbeddedId
    private ComisionIntegranteId id;

    /**
     * Nombre y apellido del integrante, resuelto por el JOIN con {@code PERSONAS}
     * ({@code PerNomyApe}). <strong>No es una columna física</strong>: {@code CPCPerNyA} es un
     * atributo calculado de la KB (D-LOG-20).
     */
    @jakarta.persistence.Transient
    private String perNomyApe;

    public ComisionIntegrante() {
        // requerido por JPA
    }

    public ComisionIntegrante(ComisionIntegranteId id) {
        this.id = id;
    }

    public ComisionIntegranteId getId() {
        return id;
    }

    public void setId(ComisionIntegranteId id) {
        this.id = id;
    }

    /** Puesto caminerero de la comisión. */
    public Integer getPueCod() {
        return id == null ? null : id.getPueCod();
    }

    /** Número de comisión. */
    public Integer getCpcId() {
        return id == null ? null : id.getCpcId();
    }

    /** Persona integrante de la comisión. */
    public Integer getCpcPerId() {
        return id == null ? null : id.getCpcPerId();
    }

    public String getPerNomyApe() {
        return perNomyApe;
    }

    public void setPerNomyApe(String perNomyApe) {
        this.perNomyApe = perNomyApe;
    }
}
