package ar.gob.chaco.atp.puestoscamineros.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta de {@link ActaPase} ({@code F2276PAS}) — G-DATA 2026-11-04
 * (spec {@code 09-formularios-acta} §2.3, plan §2.3, T-ACTA-103).
 *
 * <p>La clave física es ({@code SucTipo}, {@code SucNroId}, {@code F276Id}, {@code F276PPaseI}) —
 * constraint {@code PK__F2276PAS__F86DC8B00A111FC2} (verificado contra {@code Rentas}).
 */
public class ActaPaseId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Tipo de sucursal ({@code SucTipo}, {@code char(3) NOT NULL}). */
    private String sucTipo;

    /** Número de sucursal ({@code SucNroId}, {@code smallint NOT NULL}). */
    private Integer sucNroId;

    /** Número de Acta ({@code F276Id}, {@code int NOT NULL}). */
    private Integer f276Id;

    /** Índice correlativo del pase ({@code F276PPaseI}, {@code smallint NOT NULL}). */
    private Integer f276PPaseI;

    public ActaPaseId() {
        // requerido por JPA
    }

    public ActaPaseId(String sucTipo, Integer sucNroId, Integer f276Id, Integer f276PPaseI) {
        this.sucTipo = sucType(sucTipo);
        this.sucNroId = sucNroId;
        this.f276Id = f276Id;
        this.f276PPaseI = f276PPaseI;
    }

    /** Recorta el {@code char(3)} del tipo de sucursal (paridad con el legacy). */
    private static String sucType(String valor) {
        return valor == null ? null : valor.trim();
    }

    public String getSucTipo() { return sucTipo; }
    public void setSucTipo(String sucTipo) { this.sucTipo = sucTipo; }
    public Integer getSucNroId() { return sucNroId; }
    public void setSucNroId(Integer sucNroId) { this.sucNroId = sucNroId; }
    public Integer getF276Id() { return f276Id; }
    public void setF276Id(Integer f276Id) { this.f276Id = f276Id; }
    public Integer getF276PPaseI() { return f276PPaseI; }
    public void setF276PPaseI(Integer f276PPaseI) { this.f276PPaseI = f276PPaseI; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ActaPaseId other)) {
            return false;
        }
        return Objects.equals(sucTipo, other.sucTipo)
                && Objects.equals(sucNroId, other.sucNroId)
                && Objects.equals(f276Id, other.f276Id)
                && Objects.equals(f276PPaseI, other.f276PPaseI);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sucTipo, sucNroId, f276Id, f276PPaseI);
    }

    @Override
    public String toString() {
        return "ActaPaseId[sucTipo=" + sucTipo + ", sucNroId=" + sucNroId + ", f276Id=" + f276Id
                + ", f276PPaseI=" + f276PPaseI + "]";
    }
}
