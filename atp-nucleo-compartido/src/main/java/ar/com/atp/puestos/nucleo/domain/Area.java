package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code Areas} → tabla física {@code Areas} (spec §4.3 / §4.5).
 *
 * <p>PK {@code AreaId} ({@code decimal(10,0)} → {@link BigDecimal}, ASM-02), no autoincremental.
 * Incluye las columnas legacy {@code AreaBaja}, {@code AreaEmails}, {@code AreaAdjun} y
 * {@code AreaRespons} (ausentes en la KB). {@code AreaEmails} es {@code NOT NULL} → default
 * {@code ""} al alta (R-036). Las columnas {@code char(n)} se leen con {@code RTRIM} (R-002).
 */
@Entity
@Table(name = "Areas")
public class Area {

    @Id
    @Column(name = "AreaId", nullable = false)
    private BigDecimal areaId;

    @ColumnTransformer(read = "RTRIM(AreaDesc)")
    @Column(name = "AreaDesc", length = 50)
    private String areaDesc;

    /** Columna legacy (no KB), opcional. */
    @Column(name = "AreaBaja")
    private Integer areaBaja;

    /** Columna legacy (no KB): {@code varchar(255) NOT NULL} → default {@code ""} al alta (R-036). */
    @Column(name = "AreaEmails", nullable = false, length = 255)
    private String areaEmails = "";

    /** Columna legacy (no KB), opcional. */
    @Column(name = "AreaAdjun")
    private Boolean areaAdjun;

    /** Columna legacy (no KB), opcional. */
    @Column(name = "AreaRespons", length = 100)
    private String areaRespons;

    public Area() {
        // requerido por JPA
    }

    public Area(BigDecimal areaId, String areaDesc) {
        this.areaId = areaId;
        this.areaDesc = areaDesc;
    }

    public BigDecimal getAreaId() {
        return areaId;
    }

    public void setAreaId(BigDecimal areaId) {
        this.areaId = areaId;
    }

    public String getAreaDesc() {
        return areaDesc;
    }

    public void setAreaDesc(String areaDesc) {
        this.areaDesc = areaDesc;
    }

    public Integer getAreaBaja() {
        return areaBaja;
    }

    public void setAreaBaja(Integer areaBaja) {
        this.areaBaja = areaBaja;
    }

    public String getAreaEmails() {
        return areaEmails;
    }

    public void setAreaEmails(String areaEmails) {
        this.areaEmails = areaEmails;
    }

    public Boolean getAreaAdjun() {
        return areaAdjun;
    }

    public void setAreaAdjun(Boolean areaAdjun) {
        this.areaAdjun = areaAdjun;
    }

    public String getAreaRespons() {
        return areaRespons;
    }

    public void setAreaRespons(String areaRespons) {
        this.areaRespons = areaRespons;
    }
}
