package ar.com.atp.puestos.puestos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "DETPROD2")
public class PaseGuia {

    @EmbeddedId
    private PaseGuiaId id;

    @Column(name = "DPPPaseFec", nullable = false)
    private LocalDateTime dppPaseFec;

    @ColumnTransformer(read = "RTRIM(DPPPaseOpe)")
    @Column(name = "DPPPaseOpe", nullable = false, length = 10)
    private String dppPaseOpe;

    @Column(name = "DPPPaseHor", nullable = false)
    private LocalDateTime dppPaseHor;

    @ColumnTransformer(read = "RTRIM(DPPPaseEst)")
    @Column(name = "DPPPaseEst", nullable = false, length = 1)
    private String dppPaseEst;

    @ColumnTransformer(read = "RTRIM(SucTipo)")
    @Column(name = "SucTipo", nullable = false, length = 3)
    private String sucTipo;

    @Column(name = "SucNroId", nullable = false)
    private Short sucNroId;

    @Column(name = "DPPPaseCo")
    private Integer dppPaseCo;

    public PaseGuia() {
    }

    public PaseGuiaId getId() { return id; }
    public void setId(PaseGuiaId id) { this.id = id; }

    public BigDecimal getConCuit() { return id == null ? null : id.getConCuit(); }
    public Short getDppSucu() { return id == null ? null : id.getDppSucu(); }
    public Integer getCalPerFisc() { return id == null ? null : id.getCalPerFisc(); }
    public Short getDppSecu() { return id == null ? null : id.getDppSecu(); }
    public String getDppNroForm() { return id == null ? null : id.getDppNroForm(); }
    public Short getDppPaseId() { return id == null ? null : id.getDppPaseId(); }

    public void setConCuit(java.math.BigDecimal conCuit) {
        if (id == null) id = new PaseGuiaId();
        id.setConCuit(conCuit);
    }
    public void setDppSucu(Short dppSucu) {
        if (id == null) id = new PaseGuiaId();
        id.setDppSucu(dppSucu);
    }
    public void setCalPerFisc(Integer calPerFisc) {
        if (id == null) id = new PaseGuiaId();
        id.setCalPerFisc(calPerFisc);
    }
    public void setDppSecu(Short dppSecu) {
        if (id == null) id = new PaseGuiaId();
        id.setDppSecu(dppSecu);
    }
    public void setDppNroForm(String dppNroForm) {
        if (id == null) id = new PaseGuiaId();
        id.setDppNroForm(dppNroForm);
    }
    public void setDppPaseId(Short dppPaseId) {
        if (id == null) id = new PaseGuiaId();
        id.setDppPaseId(dppPaseId);
    }

    public LocalDateTime getDppPaseFec() { return dppPaseFec; }
    public void setDppPaseFec(LocalDateTime dppPaseFec) { this.dppPaseFec = dppPaseFec; }

    public String getDppPaseOpe() { return dppPaseOpe; }
    public void setDppPaseOpe(String dppPaseOpe) { this.dppPaseOpe = dppPaseOpe; }

    public LocalDateTime getDppPaseHor() { return dppPaseHor; }
    public void setDppPaseHor(LocalDateTime dppPaseHor) { this.dppPaseHor = dppPaseHor; }

    public String getDppPaseEst() { return dppPaseEst; }
    public void setDppPaseEst(String dppPaseEst) { this.dppPaseEst = dppPaseEst; }

    public String getSucTipo() { return sucTipo; }
    public void setSucTipo(String sucTipo) { this.sucTipo = sucTipo; }

    public Short getSucNroId() { return sucNroId; }
    public void setSucNroId(Short sucNroId) { this.sucNroId = sucNroId; }

    public Integer getDppPaseCo() { return dppPaseCo; }
    public void setDppPaseCo(Integer dppPaseCo) { this.dppPaseCo = dppPaseCo; }
}