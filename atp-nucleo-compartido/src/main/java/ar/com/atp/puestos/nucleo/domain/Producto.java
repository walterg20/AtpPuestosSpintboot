package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code Productos} (raíz) → tabla física {@code PRODUCTO} (spec §4.3 / §4.5).
 *
 * <p>PK {@code ProdCod} ({@code smallint}). Las vigencias ({@code PRODUCT1}) se modelan como
 * {@code @OneToMany} con cascada y huérfanos (R-004). {@code ProdIMax1..4} y {@code UMedNombre}
 * son atributos derivados de la KB (fórmula / join), por lo que no se persisten ({@code @Transient}).
 */
@Entity
@Table(name = "PRODUCTO")
public class Producto {

    @Id
    @Column(name = "ProdCod", nullable = false)
    private Integer prodCod;

    @ColumnTransformer(read = "RTRIM(ProdDesc)")
    @Column(name = "ProdDesc", nullable = false, length = 25)
    private String prodDesc;

    @Column(name = "ProdTipCal", nullable = false)
    private Integer prodTipCal;

    @Column(name = "TPPriID", nullable = false)
    private Integer tpPriID;

    @Column(name = "UMedNroId", length = 3)
    private String uMedNroId;

    @Column(name = "ProdAcop", length = 1)
    private String prodAcop;

    /** Columna legacy (no KB), opcional. */
    @ColumnTransformer(read = "RTRIM(ProdReppa)")
    @Column(name = "ProdReppa", length = 1)
    private String prodReppa;

    @Column(name = "ProdAnula", nullable = false)
    private Integer prodAnula;

    /** Nivel 1: vigencias por fecha. {@code ProdCod} es FK y parte de la PK compuesta (R-004). */
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("id.prodFchVig ASC")
    private List<ProductoVigencia> vigencias = new ArrayList<>();

    /** Derivado (fórmula KB, §4.1.1); no persistido. */
    @Transient
    private BigDecimal prodIMax1;

    /** Derivado (fórmula KB, §4.1.1); no persistido. */
    @Transient
    private BigDecimal prodIMax2;

    /** Derivado (fórmula KB, §4.1.1); no persistido. */
    @Transient
    private BigDecimal prodIMax3;

    /** Derivado (fórmula KB, §4.1.1); no persistido. */
    @Transient
    private BigDecimal prodIMax4;

    /** Derivado (unidad de medida, resuelto en servicio); no persistido. */
    @Transient
    private String uMedNombre;

    public Producto() {
        // requerido por JPA
    }

    public Producto(Integer prodCod, String prodDesc) {
        this.prodCod = prodCod;
        this.prodDesc = prodDesc;
    }

    /** Añade una vigencia manteniendo ambos lados de la asociación (R-004). */
    public void addVigencia(ProductoVigencia vigencia) {
        vigencia.setProducto(this);
        if (vigencia.getId() == null) {
            vigencia.setId(new ProductoVigenciaId());
        }
        vigencia.getId().setProdCod(this.prodCod);
        this.vigencias.add(vigencia);
    }

    /** Quita una vigencia manteniendo ambos lados de la asociación. */
    public void removeVigencia(ProductoVigencia vigencia) {
        vigencia.setProducto(null);
        this.vigencias.remove(vigencia);
    }

    public Integer getProdCod() {
        return prodCod;
    }

    public void setProdCod(Integer prodCod) {
        this.prodCod = prodCod;
    }

    public String getProdDesc() {
        return prodDesc;
    }

    public void setProdDesc(String prodDesc) {
        this.prodDesc = prodDesc;
    }

    public Integer getProdTipCal() {
        return prodTipCal;
    }

    public void setProdTipCal(Integer prodTipCal) {
        this.prodTipCal = prodTipCal;
    }

    public Integer getTpPriID() {
        return tpPriID;
    }

    public void setTpPriID(Integer tpPriID) {
        this.tpPriID = tpPriID;
    }

    public String getUMedNroId() {
        return uMedNroId;
    }

    public void setUMedNroId(String uMedNroId) {
        this.uMedNroId = uMedNroId;
    }

    public String getProdAcop() {
        return prodAcop;
    }

    public void setProdAcop(String prodAcop) {
        this.prodAcop = prodAcop;
    }

    public String getProdReppa() {
        return prodReppa;
    }

    public void setProdReppa(String prodReppa) {
        this.prodReppa = prodReppa;
    }

    public Integer getProdAnula() {
        return prodAnula;
    }

    public void setProdAnula(Integer prodAnula) {
        this.prodAnula = prodAnula;
    }

    public List<ProductoVigencia> getVigencias() {
        return vigencias;
    }

    public void setVigencias(List<ProductoVigencia> vigencias) {
        this.vigencias = vigencias;
    }

    public BigDecimal getProdIMax1() {
        return prodIMax1;
    }

    public void setProdIMax1(BigDecimal prodIMax1) {
        this.prodIMax1 = prodIMax1;
    }

    public BigDecimal getProdIMax2() {
        return prodIMax2;
    }

    public void setProdIMax2(BigDecimal prodIMax2) {
        this.prodIMax2 = prodIMax2;
    }

    public BigDecimal getProdIMax3() {
        return prodIMax3;
    }

    public void setProdIMax3(BigDecimal prodIMax3) {
        this.prodIMax3 = prodIMax3;
    }

    public BigDecimal getProdIMax4() {
        return prodIMax4;
    }

    public void setProdIMax4(BigDecimal prodIMax4) {
        this.prodIMax4 = prodIMax4;
    }

    public String getUMedNombre() {
        return uMedNombre;
    }

    public void setUMedNombre(String uMedNombre) {
        this.uMedNombre = uMedNombre;
    }
}
