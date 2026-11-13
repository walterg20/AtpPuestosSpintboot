package ar.gob.chaco.atp.puestoscamineros.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta de {@link ActaProducto} ({@code F22761}) — G-DATA 2026-11-04
 * (spec {@code 09-formularios-acta} §2.2, plan §2.2, T-ACTA-102).
 *
 * <p>La clave física es ({@code F276Id}, {@code SucTipo}, {@code SucNroId}, {@code ProdCod},
 * {@code F276RZona}) — constraint {@code PK__F22761__67E624F0} (verificado contra {@code Rentas}).
 *
 * <p>Con {@code @IdClass} el mapeo de las columnas vive en la entidad ({@link ActaProducto}); esta
 * clase es la clave lógica.
 */
public class ActaProductoId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Número de Acta ({@code F276Id}, {@code int NOT NULL}). */
    private Integer f276Id;

    /** Tipo de sucursal ({@code SucTipo}, {@code char(3) NOT NULL}). */
    private String sucTipo;

    /** Número de sucursal ({@code SucNroId}, {@code smallint NOT NULL}). */
    private Integer sucNroId;

    /** Código de producto ({@code ProdCod}, {@code smallint NOT NULL}). */
    private Integer prodCod;

    /** Zona ({@code F276RZona}, {@code char(1) NOT NULL}). */
    private String f276RZona;

    public ActaProductoId() {
        // requerido por JPA
    }

    public ActaProductoId(Integer f276Id, String sucTipo, Integer sucNroId, Integer prodCod,
            String f276RZona) {
        this.f276Id = f276Id;
        this.sucTipo = sucTipo;
        this.sucNroId = sucNroId;
        this.prodCod = prodCod;
        this.f276RZona = f276RZona;
    }

    public Integer getF276Id() { return f276Id; }
    public void setF276Id(Integer f276Id) { this.f276Id = f276Id; }
    public String getSucTipo() { return sucTipo; }
    public void setSucTipo(String sucTipo) { this.sucTipo = sucTipo; }
    public Integer getSucNroId() { return sucNroId; }
    public void setSucNroId(Integer sucNroId) { this.sucNroId = sucNroId; }
    public Integer getProdCod() { return prodCod; }
    public void setProdCod(Integer prodCod) { this.prodCod = prodCod; }
    public String getF276RZona() { return f276RZona; }
    public void setF276RZona(String f276RZona) { this.f276RZona = f276RZona; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ActaProductoId other)) {
            return false;
        }
        return Objects.equals(f276Id, other.f276Id)
                && Objects.equals(sucTipo, other.sucTipo)
                && Objects.equals(sucNroId, other.sucNroId)
                && Objects.equals(prodCod, other.prodCod)
                && Objects.equals(f276RZona, other.f276RZona);
    }

    @Override
    public int hashCode() {
        return Objects.hash(f276Id, sucTipo, sucNroId, prodCod, f276RZona);
    }

    @Override
    public String toString() {
        return "ActaProductoId[f276Id=" + f276Id + ", sucTipo=" + sucTipo + ", sucNroId=" + sucNroId
                + ", prodCod=" + prodCod + ", f276RZona=" + f276RZona + "]";
    }
}
