package ar.gob.chaco.atp.puestoscamineros.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta de {@link Acta} ({@code F2276}) — G-DATA 2026-11-04
 * (spec {@code 09-formularios-acta} §2.1, plan §2.1, T-ACTA-101).
 *
 * <p>La clave física es ({@code F276Id}, {@code SucTipo}, {@code SucNroId}) — constraint
 * {@code PK__F2276__66F200B7} (verificado contra {@code Rentas}).
 *
 * <p>Se usa {@code @IdClass} (y no {@code @EmbeddedId}) para que el recorte del
 * {@code char(3)} {@code SucTipo} se aplique con {@code @ColumnTransformer} sobre la propiedad
 * {@code @Id} de la entidad, sin duplicar el mapeo de las columnas en dos clases.
 *
 * <p>Los campos <strong>no</strong> se anotan: con {@code @IdClass} el mapeo de las columnas vive
 * en la entidad ({@link Acta}); esta clase es sólo la clave lógica que usan las consultas
 * derivadas y {@code findById}.
 */
public class ActaId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Número de Acta ({@code F276Id}, {@code int NOT NULL}). */
    private Integer f276Id;

    /** Tipo de sucursal ({@code SucTipo}, {@code char(3) NOT NULL}). */
    private String sucTipo;

    /** Número de sucursal ({@code SucNroId}, {@code smallint NOT NULL}). */
    private Integer sucNroId;

    public ActaId() {
        // requerido por JPA
    }

    public ActaId(Integer f276Id, String sucTipo, Integer sucNroId) {
        this.f276Id = f276Id;
        this.sucTipo = sucTipo;
        this.sucNroId = sucNroId;
    }

    public Integer getF276Id() {
        return f276Id;
    }

    public void setF276Id(Integer f276Id) {
        this.f276Id = f276Id;
    }

    public String getSucTipo() {
        return sucTipo;
    }

    public void setSucTipo(String sucTipo) {
        this.sucTipo = sucTipo;
    }

    public Integer getSucNroId() {
        return sucNroId;
    }

    public void setSucNroId(Integer sucNroId) {
        this.sucNroId = sucNroId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ActaId other)) {
            return false;
        }
        return Objects.equals(f276Id, other.f276Id)
                && Objects.equals(sucTipo, other.sucTipo)
                && Objects.equals(sucNroId, other.sucNroId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(f276Id, sucTipo, sucNroId);
    }

    @Override
    public String toString() {
        return "ActaId[f276Id=" + f276Id + ", sucTipo=" + sucTipo + ", sucNroId=" + sucNroId + "]";
    }
}
