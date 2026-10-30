package ar.com.atp.puestos.puestos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "DETPRODP")
public class DetalleProductoPrimario {

    @EmbeddedId
    private DetalleProductoPrimarioId id;

    /** No persistido: razon social del contribuyente (resuelta en servicio/vista). */
    @Transient
    private String conRazSoc;

    @Column(name = "DPPTCpbte")
    private Short dppTCpbte;

    @ColumnTransformer(read = "RTRIM(DPPNCpbte)")
    @Column(name = "DPPNCpbte", length = 15)
    private String dppNCpbte;

    @Column(name = "DPPFchMov")
    private LocalDateTime dppFchMov;

    @Column(name = "DPPCuitDes", precision = 11, scale = 0)
    private BigDecimal dppCuitDes;

    @ColumnTransformer(read = "RTRIM(DPPDestRS)")
    @Column(name = "DPPDestRS", length = 35)
    private String dppDestRS;

    @ColumnTransformer(read = "RTRIM(DPPDestDom)")
    @Column(name = "DPPDestDom", length = 50)
    private String dppDestDom;

    @Column(name = "DPPCuitTra", precision = 11, scale = 0)
    private BigDecimal dppCuitTra;

    @ColumnTransformer(read = "RTRIM(DPPTranRS)")
    @Column(name = "DPPTranRS", length = 35)
    private String dppTranRS;

    @ColumnTransformer(read = "RTRIM(DPPTranDom)")
    @Column(name = "DPPTranDom", length = 50)
    private String dppTranDom;

    @ColumnTransformer(read = "RTRIM(DPPVehicu)")
    @Column(name = "DPPVehicu", length = 10)
    private String dppVehicu;

    @ColumnTransformer(read = "RTRIM(DPPChasis)")
    @Column(name = "DPPChasis", length = 10)
    private String dppChasis;

    @ColumnTransformer(read = "RTRIM(DPPAcopla)")
    @Column(name = "DPPAcopla", length = 10)
    private String dppAcopla;

    @Column(name = "DPPMImpFle", precision = 19, scale = 4)
    private BigDecimal dppMImpFle;

    @Column(name = "DPPKmFle", precision = 10, scale = 4)
    private BigDecimal dppKmFle;

    @Column(name = "DPPAlicFle", precision = 10, scale = 4)
    private BigDecimal dppAlicFle;

    @Column(name = "DPPImpuFle", precision = 19, scale = 4)
    private BigDecimal dppImpuFle;

    @Column(name = "DPPAdicFle", precision = 19, scale = 4)
    private BigDecimal dppAdicFle;

    @Column(name = "DPPImIBTot", precision = 19, scale = 4)
    private BigDecimal dppImIBTot;

    @Column(name = "DPPAdIBTot", precision = 19, scale = 4)
    private BigDecimal dppAdIBTot;

    @Column(name = "DPPImCMTot", precision = 19, scale = 4)
    private BigDecimal dppImCMTot;

    @Column(name = "DPPAdCMTot", precision = 19, scale = 4)
    private BigDecimal dppAdCMTot;

    @Column(name = "DPPMarcAnu")
    private Short dppMarcAnu;

    @Column(name = "DPPBarrMar")
    private Short dppBarrMar;

    @Column(name = "DPPTipoPr")
    private Short dppTipoPr;

    @Column(name = "DPPTarxKm", precision = 19, scale = 4)
    private BigDecimal dppTarxKm;

    @Column(name = "DPPMEIBrut")
    private Short dppMEIBrut;

    @Column(name = "DPPMECMeca")
    private Short dppMECMeca;

    @Column(name = "DPPMEFlete")
    private Short dppMEFlete;

    @Column(name = "DPPFchGrab")
    private LocalDateTime dppFchGrab;

    @Column(name = "DPPMarcInt")
    private Short dppMarcInt;

    @ColumnTransformer(read = "RTRIM(DPPTipoGui)")
    @Column(name = "DPPTipoGui", length = 1)
    private String dppTipoGui;

    @Column(name = "DPPMarImp")
    private Short dppMarImp;

    @ColumnTransformer(read = "RTRIM(DPPClasif)")
    @Column(name = "DPPClasif", length = 1)
    private String dppClasif;

    @ColumnTransformer(read = "RTRIM(DPPTipoTra)")
    @Column(name = "DPPTipoTra", nullable = false, length = 1)
    private String dppTipoTra;

    // huerfanas
    @Column(name = "DPPLocOr")
    private Integer dppLocOr;

    @Column(name = "DPPLocDe")
    private Integer dppLocDe;

    @ColumnTransformer(read = "RTRIM(DPPIncluid)")
    @Column(name = "DPPIncluid", length = 1)
    private String dppIncluid;

    @Column(name = "DPPCorGre", precision = 19, scale = 4)
    private BigDecimal dppCorGre;

    @ColumnTransformer(read = "RTRIM(DPPUltPaOp)")
    @Column(name = "DPPUltPaOp", length = 10)
    private String dppUltPaOp;

    @ColumnTransformer(read = "RTRIM(DPPUltPSuT)")
    @Column(name = "DPPUltPSuT", length = 3)
    private String dppUltPSuT;

    @Column(name = "DPPUltPSuN")
    private Short dppUltPSuN;

    @Column(name = "DPPAnuObs", length = 200)
    private String dppAnuObs;

    @Column(name = "DPPActaNro")
    private Integer dppActaNro;

    @Column(name = "DPPActaPue")
    private Short dppActaPue;

    @Column(name = "PPTrataId")
    private Short ppTrataId;

    public DetalleProductoPrimario() {
    }

    // delegados PK
    public DetalleProductoPrimarioId getId() { return id; }
    public void setId(DetalleProductoPrimarioId id) { this.id = id; }

    public BigDecimal getConCuit() { return id == null ? null : id.getConCuit(); }
    public Short getDppSucu() { return id == null ? null : id.getDppSucu(); }
    public Integer getCalPerFisc() { return id == null ? null : id.getCalPerFisc(); }
    public Short getDppSecu() { return id == null ? null : id.getDppSecu(); }
    public String getDppNroForm() { return id == null ? null : id.getDppNroForm(); }

    public void setConCuit(BigDecimal conCuit) {
        if (id == null) id = new DetalleProductoPrimarioId();
        id.setConCuit(conCuit);
    }
    public void setDppSucu(Short dppSucu) {
        if (id == null) id = new DetalleProductoPrimarioId();
        id.setDppSucu(dppSucu);
    }
    public void setCalPerFisc(Integer calPerFisc) {
        if (id == null) id = new DetalleProductoPrimarioId();
        id.setCalPerFisc(calPerFisc);
    }
    public void setDppSecu(Short dppSecu) {
        if (id == null) id = new DetalleProductoPrimarioId();
        id.setDppSecu(dppSecu);
    }
    public void setDppNroForm(String dppNroForm) {
        if (id == null) id = new DetalleProductoPrimarioId();
        id.setDppNroForm(dppNroForm);
    }

    public String getConRazSoc() { return conRazSoc; }
    public void setConRazSoc(String conRazSoc) { this.conRazSoc = conRazSoc; }

    public Short getDppTCpbte() { return dppTCpbte; }
    public void setDppTCpbte(Short dppTCpbte) { this.dppTCpbte = dppTCpbte; }

    public String getDppNCpbte() { return dppNCpbte; }
    public void setDppNCpbte(String dppNCpbte) { this.dppNCpbte = dppNCpbte; }

    public LocalDateTime getDppFchMov() { return dppFchMov; }
    public void setDppFchMov(LocalDateTime dppFchMov) { this.dppFchMov = dppFchMov; }

    public BigDecimal getDppCuitDes() { return dppCuitDes; }
    public void setDppCuitDes(BigDecimal dppCuitDes) { this.dppCuitDes = dppCuitDes; }

    public String getDppDestRS() { return dppDestRS; }
    public void setDppDestRS(String dppDestRS) { this.dppDestRS = dppDestRS; }

    public String getDppDestDom() { return dppDestDom; }
    public void setDppDestDom(String dppDestDom) { this.dppDestDom = dppDestDom; }

    public BigDecimal getDppCuitTra() { return dppCuitTra; }
    public void setDppCuitTra(BigDecimal dppCuitTra) { this.dppCuitTra = dppCuitTra; }

    public String getDppTranRS() { return dppTranRS; }
    public void setDppTranRS(String dppTranRS) { this.dppTranRS = dppTranRS; }

    public String getDppTranDom() { return dppTranDom; }
    public void setDppTranDom(String dppTranDom) { this.dppTranDom = dppTranDom; }

    public String getDppVehicu() { return dppVehicu; }
    public void setDppVehicu(String dppVehicu) { this.dppVehicu = dppVehicu; }

    public String getDppChasis() { return dppChasis; }
    public void setDppChasis(String dppChasis) { this.dppChasis = dppChasis; }

    public String getDppAcopla() { return dppAcopla; }
    public void setDppAcopla(String dppAcopla) { this.dppAcopla = dppAcopla; }

    public BigDecimal getDppMImpFle() { return dppMImpFle; }
    public void setDppMImpFle(BigDecimal dppMImpFle) { this.dppMImpFle = dppMImpFle; }

    public BigDecimal getDppKmFle() { return dppKmFle; }
    public void setDppKmFle(BigDecimal dppKmFle) { this.dppKmFle = dppKmFle; }

    public BigDecimal getDppAlicFle() { return dppAlicFle; }
    public void setDppAlicFle(BigDecimal dppAlicFle) { this.dppAlicFle = dppAlicFle; }

    public BigDecimal getDppImpuFle() { return dppImpuFle; }
    public void setDppImpuFle(BigDecimal dppImpuFle) { this.dppImpuFle = dppImpuFle; }

    public BigDecimal getDppAdicFle() { return dppAdicFle; }
    public void setDppAdicFle(BigDecimal dppAdicFle) { this.dppAdicFle = dppAdicFle; }

    public BigDecimal getDppImIBTot() { return dppImIBTot; }
    public void setDppImIBTot(BigDecimal dppImIBTot) { this.dppImIBTot = dppImIBTot; }

    public BigDecimal getDppAdIBTot() { return dppAdIBTot; }
    public void setDppAdIBTot(BigDecimal dppAdIBTot) { this.dppAdIBTot = dppAdIBTot; }

    public BigDecimal getDppImCMTot() { return dppImCMTot; }
    public void setDppImCMTot(BigDecimal dppImCMTot) { this.dppImCMTot = dppImCMTot; }

    public BigDecimal getDppAdCMTot() { return dppAdCMTot; }
    public void setDppAdCMTot(BigDecimal dppAdCMTot) { this.dppAdCMTot = dppAdCMTot; }

    public Short getDppMarcAnu() { return dppMarcAnu; }
    public void setDppMarcAnu(Short dppMarcAnu) { this.dppMarcAnu = dppMarcAnu; }

    public Short getDppBarrMar() { return dppBarrMar; }
    public void setDppBarrMar(Short dppBarrMar) { this.dppBarrMar = dppBarrMar; }

    public Short getDppTipoPr() { return dppTipoPr; }
    public void setDppTipoPr(Short dppTipoPr) { this.dppTipoPr = dppTipoPr; }

    public BigDecimal getDppTarxKm() { return dppTarxKm; }
    public void setDppTarxKm(BigDecimal dppTarxKm) { this.dppTarxKm = dppTarxKm; }

    public Short getDppMEIBrut() { return dppMEIBrut; }
    public void setDppMEIBrut(Short dppMEIBrut) { this.dppMEIBrut = dppMEIBrut; }

    public Short getDppMECMeca() { return dppMECMeca; }
    public void setDppMECMeca(Short dppMECMeca) { this.dppMECMeca = dppMECMeca; }

    public Short getDppMEFlete() { return dppMEFlete; }
    public void setDppMEFlete(Short dppMEFlete) { this.dppMEFlete = dppMEFlete; }

    public LocalDateTime getDppFchGrab() { return dppFchGrab; }
    public void setDppFchGrab(LocalDateTime dppFchGrab) { this.dppFchGrab = dppFchGrab; }

    public Short getDppMarcInt() { return dppMarcInt; }
    public void setDppMarcInt(Short dppMarcInt) { this.dppMarcInt = dppMarcInt; }

    public String getDppTipoGui() { return dppTipoGui; }
    public void setDppTipoGui(String dppTipoGui) { this.dppTipoGui = dppTipoGui; }

    public Short getDppMarImp() { return dppMarImp; }
    public void setDppMarImp(Short dppMarImp) { this.dppMarImp = dppMarImp; }

    public String getDppClasif() { return dppClasif; }
    public void setDppClasif(String dppClasif) { this.dppClasif = dppClasif; }

    public String getDppTipoTra() { return dppTipoTra; }
    public void setDppTipoTra(String dppTipoTra) { this.dppTipoTra = dppTipoTra; }

    public Integer getDppLocOr() { return dppLocOr; }
    public void setDppLocOr(Integer dppLocOr) { this.dppLocOr = dppLocOr; }

    public Integer getDppLocDe() { return dppLocDe; }
    public void setDppLocDe(Integer dppLocDe) { this.dppLocDe = dppLocDe; }

    public String getDppIncluid() { return dppIncluid; }
    public void setDppIncluid(String dppIncluid) { this.dppIncluid = dppIncluid; }

    public BigDecimal getDppCorGre() { return dppCorGre; }
    public void setDppCorGre(BigDecimal dppCorGre) { this.dppCorGre = dppCorGre; }

    public String getDppUltPaOp() { return dppUltPaOp; }
    public void setDppUltPaOp(String dppUltPaOp) { this.dppUltPaOp = dppUltPaOp; }

    public String getDppUltPSuT() { return dppUltPSuT; }
    public void setDppUltPSuT(String dppUltPSuT) { this.dppUltPSuT = dppUltPSuT; }

    public Short getDppUltPSuN() { return dppUltPSuN; }
    public void setDppUltPSuN(Short dppUltPSuN) { this.dppUltPSuN = dppUltPSuN; }

    public String getDppAnuObs() { return dppAnuObs; }
    public void setDppAnuObs(String dppAnuObs) { this.dppAnuObs = dppAnuObs; }

    public Integer getDppActaNro() { return dppActaNro; }
    public void setDppActaNro(Integer dppActaNro) { this.dppActaNro = dppActaNro; }

    public Short getDppActaPue() { return dppActaPue; }
    public void setDppActaPue(Short dppActaPue) { this.dppActaPue = dppActaPue; }

    public Short getPpTrataId() { return ppTrataId; }
    public void setPpTrataId(Short ppTrataId) { this.ppTrataId = ppTrataId; }
}