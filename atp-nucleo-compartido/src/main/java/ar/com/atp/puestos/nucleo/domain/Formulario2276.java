package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code F2276} → tabla física {@code F2276} (spec {@code 06-turnos} §2.2,
 * R-TUR-013, R-TUR-015).
 *
 * <p><strong>Proyección de lectura</strong>: la tabla tiene 54 columnas y el módulo {@code 06-turnos}
 * sólo necesita 11. Se mapean las que intervienen en las dos reglas de negocio del módulo:
 *
 * <ul>
 *   <li>la detección de formularios abiertos (R-TUR-013): {@code SucNroId}, {@code SucTipo},
 *       {@code TurNroId} y {@code LF276ID} (el vínculo con {@code LOTES227});</li>
 *   <li>la totalización del SN (R-TUR-015): {@code F276Estad}, {@code F276GenWeb},
 *       {@code F276ImpuFl}, {@code F276AdicFl}, {@code F276AdIBTo} y {@code F276AdCMTo}.</li>
 * </ul>
 *
 * <p><strong>Nota importante sobre {@code F276ImIBTot} y {@code F276ImCMTot}</strong>: la spec
 * §2.2 los lista como columnas de {@code F2276}, pero <strong>no existen</strong> en la tabla
 * física (verificado contra {@code Rentas} el 2026-10-23: 54 columnas, ninguna con esos nombres).
 * En la KB son <em>atributos calculados</em>:
 *
 * <pre>
 *   F276ImIBTot : NUMERIC(12,2)  [Formula: sum(F276RImpIB) IF F276GenWeb &lt;&gt; 'A';]
 *   F276ImCMTot : NUMERIC(12,2)  [Formula: sum(F276RImpCM) IF F276GenWeb &lt;&gt; 'A';]
 * </pre>
 *
 * <p>es decir, se calculan sumando el detalle del formulario ({@code F22761.F276RImpIB} y
 * {@code F22761.F276RImpCM}), que <strong>sí</strong> es una columna física. La totalización
 * reproduce esa fórmula (ver {@code Formulario2276Repositorio#totalizarTurno}).
 */
@Entity
@Table(name = "F2276")
public class Formulario2276 {

    /** PK: número de formulario ({@code int}). */
    @Id
    @Column(name = "F276Id")
    private Integer f276Id;

    /** Tipo de sucursal ({@code char(3) NOT NULL}). */
    @ColumnTransformer(read = "RTRIM(SucTipo)")
    @Column(name = "SucTipo", nullable = false, length = 3)
    private String sucTipo;

    /** Sucursal ({@code smallint NOT NULL}). */
    @Column(name = "SucNroId", nullable = false)
    private Integer sucNroId;

    /** Turno al que pertenece el formulario ({@code int NULL}). */
    @Column(name = "TurNroId")
    private Integer turNroId;

    /** Número de lote ({@code int NULL}); vínculo con {@code LOTES227.LF276ID}. */
    @Column(name = "LF276ID")
    private Integer lf276Id;

    /** Estado del formulario ({@code char(1) NULL}): {@code 'A'} = anulado. */
    @ColumnTransformer(read = "RTRIM(F276Estad)")
    @Column(name = "F276Estad", length = 1)
    private String f276Estad;

    /** Fecha de movimiento ({@code datetime NULL}). */
    @Column(name = "F276FchMov")
    private LocalDateTime f276FchMov;

    /** Generación web ({@code char(1) NULL}): {@code 'A'} = generado por la web. */
    @ColumnTransformer(read = "RTRIM(F276GenWeb)")
    @Column(name = "F276GenWeb", length = 1)
    private String f276GenWeb;

    /** Impuesto de flete ({@code money}). */
    @Column(name = "F276ImpuFl", precision = 19, scale = 4)
    private BigDecimal f276ImpuFl;

    /** Adicional de flete ({@code money}). */
    @Column(name = "F276AdicFl", precision = 19, scale = 4)
    private BigDecimal f276AdicFl;

    /** Adicional de ingresos brutos ({@code money}). */
    @Column(name = "F276AdIBTo", precision = 19, scale = 4)
    private BigDecimal f276AdIBTo;

    /** Adicional de cosecha mecánica ({@code money}). */
    @Column(name = "F276AdCMTo", precision = 19, scale = 4)
    private BigDecimal f276AdCMTo;

    public Formulario2276() {
        // requerido por JPA
    }

    public Formulario2276(Integer f276Id) {
        this.f276Id = f276Id;
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

    public Integer getTurNroId() {
        return turNroId;
    }

    public void setTurNroId(Integer turNroId) {
        this.turNroId = turNroId;
    }

    public Integer getLf276Id() {
        return lf276Id;
    }

    public void setLf276Id(Integer lf276Id) {
        this.lf276Id = lf276Id;
    }

    public String getF276Estad() {
        return f276Estad;
    }

    public void setF276Estad(String f276Estad) {
        this.f276Estad = f276Estad;
    }

    public LocalDateTime getF276FchMov() {
        return f276FchMov;
    }

    public void setF276FchMov(LocalDateTime f276FchMov) {
        this.f276FchMov = f276FchMov;
    }

    public String getF276GenWeb() {
        return f276GenWeb;
    }

    public void setF276GenWeb(String f276GenWeb) {
        this.f276GenWeb = f276GenWeb;
    }

    public BigDecimal getF276ImpuFl() {
        return f276ImpuFl;
    }

    public void setF276ImpuFl(BigDecimal f276ImpuFl) {
        this.f276ImpuFl = f276ImpuFl;
    }

    public BigDecimal getF276AdicFl() {
        return f276AdicFl;
    }

    public void setF276AdicFl(BigDecimal f276AdicFl) {
        this.f276AdicFl = f276AdicFl;
    }

    public BigDecimal getF276AdIBTo() {
        return f276AdIBTo;
    }

    public void setF276AdIBTo(BigDecimal f276AdIBTo) {
        this.f276AdIBTo = f276AdIBTo;
    }

    public BigDecimal getF276AdCMTo() {
        return f276AdCMTo;
    }

    public void setF276AdCMTo(BigDecimal f276AdCMTo) {
        this.f276AdCMTo = f276AdCMTo;
    }
}
