package ar.com.atp.puestos.puestos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Detalle de productos de un Acta → tabla física {@code F22761}
 * (spec {@code 09-formularios-acta} §2.2, plan §2.2, T-ACTA-102).
 *
 * <p><strong>Mapa completo</strong>: las <strong>15 columnas físicas</strong> verificadas contra
 * {@code Rentas} el 2026-11-04 (G-DATA). La PK es compuesta ({@code F276Id}, {@code SucTipo},
 * {@code SucNroId}, {@code ProdCod}, {@code F276RZona}) y se declara con {@code @IdClass}
 * ({@link ActaProductoId}).
 *
 * <p><strong>Columna huérfana</strong>: {@code F276Corrig} existe en la BD y no en la KB — se mapea
 * con default {@code null} (R-ACTA-051).
 *
 * <p><strong>No físicos</strong>: {@code ProdDesc} y {@code ProdTipCal} se derivan de
 * {@code PRODUCTO} (no se persisten).
 */
@Entity
@Table(name = "F22761")
@IdClass(ActaProductoId.class)
public class ActaProducto {

    /** Número de Acta ({@code F276Id}, {@code int NOT NULL}). */
    @Id
    @Column(name = "F276Id", nullable = false)
    private Integer f276Id;

    /** Tipo de sucursal ({@code SucTipo}, {@code char(3) NOT NULL}). */
    @Id
    @ColumnTransformer(read = "RTRIM(SucTipo)")
    @Column(name = "SucTipo", nullable = false, length = 3)
    private String sucTipo;

    /** Número de sucursal ({@code SucNroId}, {@code smallint NOT NULL}). */
    @Id
    @Column(name = "SucNroId", nullable = false)
    private Integer sucNroId;

    /** Código de producto ({@code ProdCod}, {@code smallint NOT NULL}). */
    @Id
    @Column(name = "ProdCod", nullable = false)
    private Integer prodCod;

    /** Zona ({@code F276RZona}, {@code char(1) NOT NULL}) — ver SA-ACTA-06. */
    @Id
    @ColumnTransformer(read = "RTRIM(F276RZona)")
    @Column(name = "F276RZona", nullable = false, length = 1)
    private String f276RZona;

    /** Cantidad ({@code F276RCant}, {@code money}). */
    @Column(name = "F276RCant", precision = 19, scale = 4)
    private BigDecimal f276RCant;

    /** Marca de cosecha mecánica ({@code F276RMarCM}, {@code char(1)}) ⇒ {@code 'N'}. */
    @ColumnTransformer(read = "RTRIM(F276RMarCM)")
    @Column(name = "F276RMarCM", length = 1)
    private String f276RMarCM;

    /** Importe de la línea ({@code F276RMImpo}, {@code money}). */
    @Column(name = "F276RMImpo", precision = 19, scale = 4)
    private BigDecimal f276RMImpo;

    /** Precio fijo ({@code F276RFijo}, {@code money}) — fórmula en KB, columna física. */
    @Column(name = "F276RFijo", precision = 19, scale = 4)
    private BigDecimal f276RFijo;

    /** Precio de cosecha mecánica ({@code F276RPreCM}, {@code money}) — fórmula en KB. */
    @Column(name = "F276RPreCM", precision = 19, scale = 4)
    private BigDecimal f276RPreCM;

    /** Alícuota ({@code F276RAlicu}, {@code smallmoney}) ⇒ {@code 1}. */
    @Column(name = "F276RAlicu", precision = 10, scale = 4)
    private BigDecimal f276RAlicu;

    /** Impuesto de ingresos brutos ({@code F276RImpIB}, {@code money}) — fórmula en KB. */
    @Column(name = "F276RImpIB", precision = 19, scale = 4)
    private BigDecimal f276RImpIB;

    /** Impuesto de cosecha mecánica ({@code F276RImpCM}, {@code money}) — fórmula en KB. */
    @Column(name = "F276RImpCM", precision = 19, scale = 4)
    private BigDecimal f276RImpCM;

    /** Columna huérfana ({@code F276Corrig}, {@code char(1)}) — default {@code null}. */
    @ColumnTransformer(read = "RTRIM(F276Corrig)")
    @Column(name = "F276Corrig", length = 1)
    private String f276Corrig;

    /** Precio informado ({@code F276RSPre}, {@code money}) — ≥ {@code F276RFijo}. */
    @Column(name = "F276RSPre", precision = 19, scale = 4)
    private BigDecimal f276RSPre;

    public ActaProducto() {
        // requerido por JPA
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
    public BigDecimal getF276RCant() { return f276RCant; }
    public void setF276RCant(BigDecimal f276RCant) { this.f276RCant = f276RCant; }
    public String getF276RMarCM() { return f276RMarCM; }
    public void setF276RMarCM(String f276RMarCM) { this.f276RMarCM = f276RMarCM; }
    public BigDecimal getF276RMImpo() { return f276RMImpo; }
    public void setF276RMImpo(BigDecimal f276RMImpo) { this.f276RMImpo = f276RMImpo; }
    public BigDecimal getF276RFijo() { return f276RFijo; }
    public void setF276RFijo(BigDecimal f276RFijo) { this.f276RFijo = f276RFijo; }
    public BigDecimal getF276RPreCM() { return f276RPreCM; }
    public void setF276RPreCM(BigDecimal f276RPreCM) { this.f276RPreCM = f276RPreCM; }
    public BigDecimal getF276RAlicu() { return f276RAlicu; }
    public void setF276RAlicu(BigDecimal f276RAlicu) { this.f276RAlicu = f276RAlicu; }
    public BigDecimal getF276RImpIB() { return f276RImpIB; }
    public void setF276RImpIB(BigDecimal f276RImpIB) { this.f276RImpIB = f276RImpIB; }
    public BigDecimal getF276RImpCM() { return f276RImpCM; }
    public void setF276RImpCM(BigDecimal f276RImpCM) { this.f276RImpCM = f276RImpCM; }
    public String getF276Corrig() { return f276Corrig; }
    public void setF276Corrig(String f276Corrig) { this.f276Corrig = f276Corrig; }
    public BigDecimal getF276RSPre() { return f276RSPre; }
    public void setF276RSPre(BigDecimal f276RSPre) { this.f276RSPre = f276RSPre; }
}
