package ar.gob.chaco.atp.puestoscamineros.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Clave compuesta de {@link Turno} ({@code PPRIM_TU.SucNroId} + {@code SucTipo} +
 * {@code TurNroId}) — G-DATA 2026-10-22 (spec 03-login §3.1.1, R-LOG-034), ampliada por el módulo
 * {@code 06-turnos} (spec §2.1, T-TUR-A1).
 *
 * <p>El orden reproduce el de la clave física: ({@code SucNroId}, {@code SucTipo}, {@code TurNroId}).
 * Los tres componentes son {@code NOT NULL} en la tabla.
 *
 * <p>Los tipos son los <strong>tipos físicos</strong>: {@code smallint}, {@code char(3)} e
 * {@code int}. El {@code char(3)} se lee con relleno; el recorte a {@code "REC"} / {@code "PUE"}
 * lo hace el mapper ({@code TurnoMapper}) y no la entidad, porque el recorte en la lectura
 * rompería la comparación de la clave (D-TUR-004).
 */
@Embeddable
public class TurnoId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Sucursal ({@code smallint}): puesto caminerero o receptoría. */
    @Column(name = "SucNroId", nullable = false)
    private Integer sucNroId;

    /** Tipo de sucursal ({@code char(3)}): {@code PUE} o {@code REC}. */
    @Column(name = "SucTipo", nullable = false, length = 3)
    private String sucTipo;

    /** Número de turno ({@code int}). */
    @Column(name = "TurNroId", nullable = false)
    private Integer turNroId;

    public TurnoId() {
        // requerido por JPA
    }

    public TurnoId(Integer sucNroId, String sucTipo, Integer turNroId) {
        this.sucNroId = sucNroId;
        this.sucTipo = sucTipo;
        this.turNroId = turNroId;
    }

    public Integer getSucNroId() {
        return sucNroId;
    }

    public void setSucNroId(Integer sucNroId) {
        this.sucNroId = sucNroId;
    }

    public String getSucTipo() {
        return sucTipo;
    }

    public void setSucTipo(String sucTipo) {
        this.sucTipo = sucTipo;
    }

    public Integer getTurNroId() {
        return turNroId;
    }

    public void setTurNroId(Integer turNroId) {
        this.turNroId = turNroId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TurnoId other)) {
            return false;
        }
        return Objects.equals(sucNroId, other.sucNroId)
                && Objects.equals(sucTipo, other.sucTipo)
                && Objects.equals(turNroId, other.turNroId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sucNroId, sucTipo, turNroId);
    }
}
