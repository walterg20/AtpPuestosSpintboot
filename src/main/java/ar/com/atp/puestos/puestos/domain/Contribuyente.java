package ar.gob.chaco.atp.puestoscamineros.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code Contribu} (Contribuyentes) → tabla física {@code CONTRIBU}
 * (spec 08-control-cargas §3.8, plan §2.1).
 *
 * <p>PK {@code ConCuit} ({@code decimal(11,0)}). Las columnas huérfanas (presentes en la BD y
 * no en la KB) se mapean con {@code @Column(name="...")} y default {@code null} (D-CC-05).
 * Los {@code char(n)} se leen con {@code RTRIM} (paridad con el legacy).
 */
@Entity
@Table(name = "CONTRIBU")
public class Contribuyente {

    @Id
    @Column(name = "ConCuit", nullable = false, precision = 11, scale = 0)
    private BigDecimal conCuit;

    @Column(name = "ConNumIns")
    private Integer conNumIns;

    @Column(name = "ConEstado")
    private Short conEstado;

    @Column(name = "ConTipDgi")
    private Short conTipDgi;

    @Column(name = "ConTipDoc")
    private Short conTipDoc;

    @Column(name = "ConNroDoc")
    private Integer conNroDoc;

    @ColumnTransformer(read = "RTRIM(ConRazSoc)")
    @Column(name = "ConRazSoc", length = 35)
    private String conRazSoc;

    @Column(name = "CallCodf")
    private Integer callCodf;

    @Column(name = "LocCodf")
    private Integer locCodf;

    @ColumnTransformer(read = "RTRIM(ConCProvF)")
    @Column(name = "ConCProvF", length = 1)
    private String conCProvF;

    @ColumnTransformer(read = "RTRIM(ConDptoF)")
    @Column(name = "ConDptoF", length = 3)
    private String conDptoF;

    @ColumnTransformer(read = "RTRIM(ConEPosF)")
    @Column(name = "ConEPosF", length = 6)
    private String conEPosF;

    @Column(name = "ConNMzanaF")
    private Integer conNMzanaF;

    @ColumnTransformer(read = "RTRIM(ConPisoF)")
    @Column(name = "ConPisoF", length = 2)
    private String conPisoF;

    @ColumnTransformer(read = "RTRIM(ConTBarrF)")
    @Column(name = "ConTBarrF", length = 15)
    private String conTBarrF;

    @ColumnTransformer(read = "RTRIM(ConTelf)")
    @Column(name = "ConTelf", length = 15)
    private String conTelf;

    @Column(name = "CallCodr")
    private Integer callCodr;

    @Column(name = "LocCodR")
    private Integer locCodR;

    @ColumnTransformer(read = "RTRIM(ConCProvR)")
    @Column(name = "ConCProvR", length = 15)
    private String conCProvR;

    @ColumnTransformer(read = "RTRIM(ConDptoR)")
    @Column(name = "ConDptoR", length = 3)
    private String conDptoR;

    @ColumnTransformer(read = "RTRIM(ConEPosR)")
    @Column(name = "ConEPosR", length = 6)
    private String conEPosR;

    @Column(name = "ConNMzanaR")
    private Integer conNMzanaR;

    @ColumnTransformer(read = "RTRIM(ConPisoR)")
    @Column(name = "ConPisoR", length = 2)
    private String conPisoR;

    @ColumnTransformer(read = "RTRIM(ConTBarrR)")
    @Column(name = "ConTBarrR", length = 15)
    private String conTBarrR;

    @ColumnTransformer(read = "RTRIM(ConTelfR)")
    @Column(name = "ConTelfR", length = 15)
    private String conTelfR;

    @ColumnTransformer(read = "RTRIM(ConTriIB)")
    @Column(name = "ConTriIB", length = 1)
    private String conTriIB;

    @ColumnTransformer(read = "RTRIM(ConConvMul)")
    @Column(name = "ConConvMul", length = 1)
    private String conConvMul;

    @ColumnTransformer(read = "RTRIM(ConFSP)")
    @Column(name = "ConFSP", length = 1)
    private String conFSP;

    @ColumnTransformer(read = "RTRIM(ConSello)")
    @Column(name = "ConSello", length = 1)
    private String conSello;

    @ColumnTransformer(read = "RTRIM(ConInmRu)")
    @Column(name = "ConInmRu", length = 1)
    private String conInmRu;

    @ColumnTransformer(read = "RTRIM(ConJuego)")
    @Column(name = "ConJuego", length = 1)
    private String conJuego;

    @ColumnTransformer(read = "RTRIM(ConAgeRet)")
    @Column(name = "ConAgeRet", length = 1)
    private String conAgeRet;

    @ColumnTransformer(read = "RTRIM(ConAgePer)")
    @Column(name = "ConAgePer", length = 1)
    private String conAgePer;

    @ColumnTransformer(read = "RTRIM(ConDerAI)")
    @Column(name = "ConDerAI", length = 1)
    private String conDerAI;

    @Column(name = "ConQEmpl")
    private Integer conQEmpl;

    @Column(name = "ConFecIni")
    private LocalDateTime conFecIni;

    @Column(name = "ConCodBaja")
    private Short conCodBaja;

    @Column(name = "ConNroLoc")
    private Integer conNroLoc;

    @Column(name = "ConFecCS")
    private LocalDateTime conFecCS;

    @Column(name = "ConDurCS")
    private Short conDurCS;

    @ColumnTransformer(read = "RTRIM(ConFecCE)")
    @Column(name = "ConFecCE", length = 4)
    private String conFecCE;

    @Column(name = "JurCod")
    private Short jurCod;

    @ColumnTransformer(read = "RTRIM(ConNroRPC)")
    @Column(name = "ConNroRPC", length = 15)
    private String conNroRPC;

    @Column(name = "ConLocRPC")
    private Integer conLocRPC;

    @Column(name = "ConFecRPC")
    private LocalDateTime conFecRPC;

    @Column(name = "ConTama")
    private Short conTama;

    @Column(name = "ConCatIB")
    private Short conCatIB;

    @Column(name = "ConFormJUR")
    private Short conFormJUR;

    // --- columnas huerfanas (BD, no KB) ---

    @ColumnTransformer(read = "RTRIM(ConNomCall)")
    @Column(name = "ConNomCall", length = 50)
    private String conNomCall;

    @Column(name = "ConNroInmo")
    private Integer conNroInmo;

    @Column(name = "ConNroDCat")
    private Integer conNroDCat;

    @Column(name = "ConSellTip")
    private Short conSellTip;

    @Column(name = "AgeUltLin")
    private Short ageUltLin;

    @ColumnTransformer(read = "RTRIM(ConPuesCam)")
    @Column(name = "ConPuesCam", length = 1)
    private String conPuesCam;

    @Column(name = "ConNroCoMu", precision = 10, scale = 0)
    private BigDecimal conNroCoMu;

    @ColumnTransformer(read = "RTRIM(ConCNFdo)")
    @Column(name = "ConCNFdo", length = 1)
    private String conCNFdo;

    @ColumnTransformer(read = "RTRIM(ConCtrlFis)")
    @Column(name = "ConCtrlFis", length = 1)
    private String conCtrlFis;

    @Column(name = "ConInsPJur")
    private Integer conInsPJur;

    @ColumnTransformer(read = "RTRIM(ConFJRPC)")
    @Column(name = "ConFJRPC", length = 5)
    private String conFJRPC;

    @ColumnTransformer(read = "RTRIM(ConLibRPC)")
    @Column(name = "ConLibRPC", length = 5)
    private String conLibRPC;

    @ColumnTransformer(read = "RTRIM(ConMCMAnt)")
    @Column(name = "ConMCMAnt", length = 1)
    private String conMCMAnt;

    @ColumnTransformer(read = "RTRIM(CodPosFis)")
    @Column(name = "CodPosFis", length = 8)
    private String codPosFis;

    @ColumnTransformer(read = "RTRIM(CodPosReal)")
    @Column(name = "CodPosReal", length = 8)
    private String codPosReal;

    @ColumnTransformer(read = "RTRIM(ConDDJJAnu)")
    @Column(name = "ConDDJJAnu", length = 1)
    private String conDDJJAnu;

    @Column(name = "ConNomFan", length = 150)
    private String conNomFan;

    @ColumnTransformer(read = "RTRIM(ConBenPrCh)")
    @Column(name = "ConBenPrCh", length = 1)
    private String conBenPrCh;

    @ColumnTransformer(read = "RTRIM(ConBen7190)")
    @Column(name = "ConBen7190", length = 1)
    private String conBen7190;

    @Column(name = "ConCatAfipId")
    private Short conCatAfipId;

    public Contribuyente() {
    }

    public BigDecimal getConCuit() { return conCuit; }
    public void setConCuit(BigDecimal conCuit) { this.conCuit = conCuit; }

    public Integer getConNumIns() { return conNumIns; }
    public void setConNumIns(Integer conNumIns) { this.conNumIns = conNumIns; }

    public Short getConEstado() { return conEstado; }
    public void setConEstado(Short conEstado) { this.conEstado = conEstado; }

    public Short getConTipDgi() { return conTipDgi; }
    public void setConTipDgi(Short conTipDgi) { this.conTipDgi = conTipDgi; }

    public Short getConTipDoc() { return conTipDoc; }
    public void setConTipDoc(Short conTipDoc) { this.conTipDoc = conTipDoc; }

    public Integer getConNroDoc() { return conNroDoc; }
    public void setConNroDoc(Integer conNroDoc) { this.conNroDoc = conNroDoc; }

    public String getConRazSoc() { return conRazSoc; }
    public void setConRazSoc(String conRazSoc) { this.conRazSoc = conRazSoc; }

    public Integer getCallCodf() { return callCodf; }
    public void setCallCodf(Integer callCodf) { this.callCodf = callCodf; }

    public Integer getLocCodf() { return locCodf; }
    public void setLocCodf(Integer locCodf) { this.locCodf = locCodf; }

    public String getConCProvF() { return conCProvF; }
    public void setConCProvF(String conCProvF) { this.conCProvF = conCProvF; }

    public String getConDptoF() { return conDptoF; }
    public void setConDptoF(String conDptoF) { this.conDptoF = conDptoF; }

    public String getConEPosF() { return conEPosF; }
    public void setConEPosF(String conEPosF) { this.conEPosF = conEPosF; }

    public Integer getConNMzanaF() { return conNMzanaF; }
    public void setConNMzanaF(Integer conNMzanaF) { this.conNMzanaF = conNMzanaF; }

    public String getConPisoF() { return conPisoF; }
    public void setConPisoF(String conPisoF) { this.conPisoF = conPisoF; }

    public String getConTBarrF() { return conTBarrF; }
    public void setConTBarrF(String conTBarrF) { this.conTBarrF = conTBarrF; }

    public String getConTelf() { return conTelf; }
    public void setConTelf(String conTelf) { this.conTelf = conTelf; }

    public Integer getCallCodr() { return callCodr; }
    public void setCallCodr(Integer callCodr) { this.callCodr = callCodr; }

    public Integer getLocCodR() { return locCodR; }
    public void setLocCodR(Integer locCodR) { this.locCodR = locCodR; }

    public String getConCProvR() { return conCProvR; }
    public void setConCProvR(String conCProvR) { this.conCProvR = conCProvR; }

    public String getConDptoR() { return conDptoR; }
    public void setConDptoR(String conDptoR) { this.conDptoR = conDptoR; }

    public String getConEPosR() { return conEPosR; }
    public void setConEPosR(String conEPosR) { this.conEPosR = conEPosR; }

    public Integer getConNMzanaR() { return conNMzanaR; }
    public void setConNMzanaR(Integer conNMzanaR) { this.conNMzanaR = conNMzanaR; }

    public String getConPisoR() { return conPisoR; }
    public void setConPisoR(String conPisoR) { this.conPisoR = conPisoR; }

    public String getConTBarrR() { return conTBarrR; }
    public void setConTBarrR(String conTBarrR) { this.conTBarrR = conTBarrR; }

    public String getConTelfR() { return conTelfR; }
    public void setConTelfR(String conTelfR) { this.conTelfR = conTelfR; }

    public String getConTriIB() { return conTriIB; }
    public void setConTriIB(String conTriIB) { this.conTriIB = conTriIB; }

    public String getConConvMul() { return conConvMul; }
    public void setConConvMul(String conConvMul) { this.conConvMul = conConvMul; }

    public String getConFSP() { return conFSP; }
    public void setConFSP(String conFSP) { this.conFSP = conFSP; }

    public String getConSello() { return conSello; }
    public void setConSello(String conSello) { this.conSello = conSello; }

    public String getConInmRu() { return conInmRu; }
    public void setConInmRu(String conInmRu) { this.conInmRu = conInmRu; }

    public String getConJuego() { return conJuego; }
    public void setConJuego(String conJuego) { this.conJuego = conJuego; }

    public String getConAgeRet() { return conAgeRet; }
    public void setConAgeRet(String conAgeRet) { this.conAgeRet = conAgeRet; }

    public String getConAgePer() { return conAgePer; }
    public void setConAgePer(String conAgePer) { this.conAgePer = conAgePer; }

    public String getConDerAI() { return conDerAI; }
    public void setConDerAI(String conDerAI) { this.conDerAI = conDerAI; }

    public Integer getConQEmpl() { return conQEmpl; }
    public void setConQEmpl(Integer conQEmpl) { this.conQEmpl = conQEmpl; }

    public LocalDateTime getConFecIni() { return conFecIni; }
    public void setConFecIni(LocalDateTime conFecIni) { this.conFecIni = conFecIni; }

    public Short getConCodBaja() { return conCodBaja; }
    public void setConCodBaja(Short conCodBaja) { this.conCodBaja = conCodBaja; }

    public Integer getConNroLoc() { return conNroLoc; }
    public void setConNroLoc(Integer conNroLoc) { this.conNroLoc = conNroLoc; }

    public LocalDateTime getConFecCS() { return conFecCS; }
    public void setConFecCS(LocalDateTime conFecCS) { this.conFecCS = conFecCS; }

    public Short getConDurCS() { return conDurCS; }
    public void setConDurCS(Short conDurCS) { this.conDurCS = conDurCS; }

    public String getConFecCE() { return conFecCE; }
    public void setConFecCE(String conFecCE) { this.conFecCE = conFecCE; }

    public Short getJurCod() { return jurCod; }
    public void setJurCod(Short jurCod) { this.jurCod = jurCod; }

    public String getConNroRPC() { return conNroRPC; }
    public void setConNroRPC(String conNroRPC) { this.conNroRPC = conNroRPC; }

    public Integer getConLocRPC() { return conLocRPC; }
    public void setConLocRPC(Integer conLocRPC) { this.conLocRPC = conLocRPC; }

    public LocalDateTime getConFecRPC() { return conFecRPC; }
    public void setConFecRPC(LocalDateTime conFecRPC) { this.conFecRPC = conFecRPC; }

    public Short getConTama() { return conTama; }
    public void setConTama(Short conTama) { this.conTama = conTama; }

    public Short getConCatIB() { return conCatIB; }
    public void setConCatIB(Short conCatIB) { this.conCatIB = conCatIB; }

    public Short getConFormJUR() { return conFormJUR; }
    public void setConFormJUR(Short conFormJUR) { this.conFormJUR = conFormJUR; }

    public String getConNomCall() { return conNomCall; }
    public void setConNomCall(String conNomCall) { this.conNomCall = conNomCall; }

    public Integer getConNroInmo() { return conNroInmo; }
    public void setConNroInmo(Integer conNroInmo) { this.conNroInmo = conNroInmo; }

    public Integer getConNroDCat() { return conNroDCat; }
    public void setConNroDCat(Integer conNroDCat) { this.conNroDCat = conNroDCat; }

    public Short getConSellTip() { return conSellTip; }
    public void setConSellTip(Short conSellTip) { this.conSellTip = conSellTip; }

    public Short getAgeUltLin() { return ageUltLin; }
    public void setAgeUltLin(Short ageUltLin) { this.ageUltLin = ageUltLin; }

    public String getConPuesCam() { return conPuesCam; }
    public void setConPuesCam(String conPuesCam) { this.conPuesCam = conPuesCam; }

    public BigDecimal getConNroCoMu() { return conNroCoMu; }
    public void setConNroCoMu(BigDecimal conNroCoMu) { this.conNroCoMu = conNroCoMu; }

    public String getConCNFdo() { return conCNFdo; }
    public void setConCNFdo(String conCNFdo) { this.conCNFdo = conCNFdo; }

    public String getConCtrlFis() { return conCtrlFis; }
    public void setConCtrlFis(String conCtrlFis) { this.conCtrlFis = conCtrlFis; }

    public Integer getConInsPJur() { return conInsPJur; }
    public void setConInsPJur(Integer conInsPJur) { this.conInsPJur = conInsPJur; }

    public String getConFJRPC() { return conFJRPC; }
    public void setConFJRPC(String conFJRPC) { this.conFJRPC = conFJRPC; }

    public String getConLibRPC() { return conLibRPC; }
    public void setConLibRPC(String conLibRPC) { this.conLibRPC = conLibRPC; }

    public String getConMCMAnt() { return conMCMAnt; }
    public void setConMCMAnt(String conMCMAnt) { this.conMCMAnt = conMCMAnt; }

    public String getCodPosFis() { return codPosFis; }
    public void setCodPosFis(String codPosFis) { this.codPosFis = codPosFis; }

    public String getCodPosReal() { return codPosReal; }
    public void setCodPosReal(String codPosReal) { this.codPosReal = codPosReal; }

    public String getConDDJJAnu() { return conDDJJAnu; }
    public void setConDDJJAnu(String conDDJJAnu) { this.conDDJJAnu = conDDJJAnu; }

    public String getConNomFan() { return conNomFan; }
    public void setConNomFan(String conNomFan) { this.conNomFan = conNomFan; }

    public String getConBenPrCh() { return conBenPrCh; }
    public void setConBenPrCh(String conBenPrCh) { this.conBenPrCh = conBenPrCh; }

    public String getConBen7190() { return conBen7190; }
    public void setConBen7190(String conBen7190) { this.conBen7190 = conBen7190; }

    public Short getConCatAfipId() { return conCatAfipId; }
    public void setConCatAfipId(Short conCatAfipId) { this.conCatAfipId = conCatAfipId; }
}