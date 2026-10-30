package ar.com.atp.puestos.puestos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
public class DetalleProductoPrimarioId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "ConCuit", nullable = false, precision = 11, scale = 0)
    private BigDecimal conCuit;

    @Column(name = "DPPSucu", nullable = false)
    private Short dppSucu;

    @Column(name = "CalPerFisc", nullable = false)
    private Integer calPerFisc;

    @Column(name = "DPPSecu", nullable = false)
    private Short dppSecu;

    @Column(name = "DPPNroForm", nullable = false, length = 10)
    private String dppNroForm;

    public DetalleProductoPrimarioId() {
    }

    public DetalleProductoPrimarioId(BigDecimal conCuit, Short dppSucu, Integer calPerFisc, Short dppSecu,
            String dppNroForm) {
        this.conCuit = conCuit;
        this.dppSucu = dppSucu;
        this.calPerFisc = calPerFisc;
        this.dppSecu = dppSecu;
        this.dppNroForm = dppNroForm;
    }

    public BigDecimal getConCuit() { return conCuit; }
    public void setConCuit(BigDecimal conCuit) { this.conCuit = conCuit; }

    public Short getDppSucu() { return dppSucu; }
    public void setDppSucu(Short dppSucu) { this.dppSucu = dppSucu; }

    public Integer getCalPerFisc() { return calPerFisc; }
    public void setCalPerFisc(Integer calPerFisc) { this.calPerFisc = calPerFisc; }

    public Short getDppSecu() { return dppSecu; }
    public void setDppSecu(Short dppSecu) { this.dppSecu = dppSecu; }

    public String getDppNroForm() { return dppNroForm; }
    public void setDppNroForm(String dppNroForm) { this.dppNroForm = dppNroForm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DetalleProductoPrimarioId other)) return false;
        return Objects.equals(conCuit, other.conCuit)
                && Objects.equals(dppSucu, other.dppSucu)
                && Objects.equals(calPerFisc, other.calPerFisc)
                && Objects.equals(dppSecu, other.dppSecu)
                && Objects.equals(dppNroForm, other.dppNroForm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conCuit, dppSucu, calPerFisc, dppSecu, dppNroForm);
    }
}