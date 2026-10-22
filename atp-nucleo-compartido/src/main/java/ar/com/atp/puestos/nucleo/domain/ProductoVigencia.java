package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code Productos} nivel 1 → tabla física {@code PRODUCT1} (spec §4.3 / §4.5).
 *
 * <p>PK compuesta ({@link ProductoVigenciaId}): {@code ProdCod} (derivado del padre vía
 * {@code @MapsId}) + {@code ProdFchVig}. Incluye columnas legacy ausentes en la KB:
 * {@code ProdImpu5}, {@code ProdFeLog} y {@code ProdUsu} (R-036).
 */
@Entity
@Table(name = "PRODUCT1")
public class ProductoVigencia {

    @EmbeddedId
    private ProductoVigenciaId id = new ProductoVigenciaId();

    /** FK {@code ProdCod} → {@code PRODUCTO}; alimenta {@code id.prodCod} vía {@code @MapsId} (R-004). */
    @MapsId("prodCod")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ProdCod", nullable = false)
    private Producto producto;

    @Column(name = "ProdImpu1", nullable = false)
    private BigDecimal prodImpu1;

    @Column(name = "ProdImpu2", nullable = false)
    private BigDecimal prodImpu2;

    @Column(name = "ProdImpu3")
    private BigDecimal prodImpu3;

    @Column(name = "ProdImpu4")
    private BigDecimal prodImpu4;

    /** Columna legacy (no KB), opcional. */
    @Column(name = "ProdImpu5")
    private BigDecimal prodImpu5;

    /** Columna legacy (no KB), opcional. */
    @Column(name = "ProdFeLog")
    private LocalDateTime prodFeLog;

    /** Columna legacy (no KB), opcional. */
    @ColumnTransformer(read = "RTRIM(ProdUsu)")
    @Column(name = "ProdUsu", length = 10)
    private String prodUsu;

    public ProductoVigencia() {
        // requerido por JPA
    }

    public ProductoVigencia(LocalDateTime prodFchVig) {
        this.id.setProdFchVig(prodFchVig);
    }

    public ProductoVigenciaId getId() {
        return id;
    }

    public void setId(ProductoVigenciaId id) {
        this.id = id;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    /** Atajo de la parte {@code ProdFchVig} de la PK. */
    public LocalDateTime getProdFchVig() {
        return id != null ? id.getProdFchVig() : null;
    }

    public void setProdFchVig(LocalDateTime prodFchVig) {
        if (id == null) {
            id = new ProductoVigenciaId();
        }
        id.setProdFchVig(prodFchVig);
    }

    /** Atajo de la parte {@code ProdCod} de la PK (derivado del padre). */
    public Integer getProdCod() {
        return id != null ? id.getProdCod() : null;
    }

    public BigDecimal getProdImpu1() {
        return prodImpu1;
    }

    public void setProdImpu1(BigDecimal prodImpu1) {
        this.prodImpu1 = prodImpu1;
    }

    public BigDecimal getProdImpu2() {
        return prodImpu2;
    }

    public void setProdImpu2(BigDecimal prodImpu2) {
        this.prodImpu2 = prodImpu2;
    }

    public BigDecimal getProdImpu3() {
        return prodImpu3;
    }

    public void setProdImpu3(BigDecimal prodImpu3) {
        this.prodImpu3 = prodImpu3;
    }

    public BigDecimal getProdImpu4() {
        return prodImpu4;
    }

    public void setProdImpu4(BigDecimal prodImpu4) {
        this.prodImpu4 = prodImpu4;
    }

    public BigDecimal getProdImpu5() {
        return prodImpu5;
    }

    public void setProdImpu5(BigDecimal prodImpu5) {
        this.prodImpu5 = prodImpu5;
    }

    public LocalDateTime getProdFeLog() {
        return prodFeLog;
    }

    public void setProdFeLog(LocalDateTime prodFeLog) {
        this.prodFeLog = prodFeLog;
    }

    public String getProdUsu() {
        return prodUsu;
    }

    public void setProdUsu(String prodUsu) {
        this.prodUsu = prodUsu;
    }
}
