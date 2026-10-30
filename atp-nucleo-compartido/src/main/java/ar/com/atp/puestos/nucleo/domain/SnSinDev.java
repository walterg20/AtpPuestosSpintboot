package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transacción legacy {@code SNSinDev} → tabla física {@code SNSINDEV} (spec {@code 06-turnos}
 * §2.2, R-TUR-016, T-TUR-B5).
 *
 * <p>Es la tabla donde el legacy deja el <strong>número identificatorio</strong> (SN) del volante de
 * pago generado al cerrar un turno de receptoría ({@code TurnoGenerarSN}). La fila se crea con
 * {@code CodForm = 126} (volante de pago) y {@code SNSinIdVol = 6}.
 *
 * <p><strong>PK autogenerada</strong>: {@code SNSinNum} es {@code decimal(10,0) IDENTITY} en
 * {@code Rentas} (verificado el 2026-10-23), por eso se mapea con
 * {@link GenerationType#IDENTITY} y no se escribe.
 *
 * <p><strong>Columnas de valor constante</strong>: el legacy inserta {@code 0} en todos los
 * numéricos que no asigna y la sentinela {@code 1753-01-01} en las fechas que no asigna (ver las
 * filas reales de {@code SNSINDEV} con {@code SNSinIdVol = 6}). Esa asignación se hace en la
 * declaración del atributo para que el comportamiento sea idéntico sin ensuciar el servicio.
 */
@Entity
@Table(name = "SNSINDEV")
public class SnSinDev {

    /** PK autogenerada ({@code decimal(10,0) IDENTITY}). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SNSinNum", precision = 10, scale = 0)
    private BigDecimal snSinNum;

    /** Código de formulario ({@code smallint}): {@code 126} = volante de pago. */
    @Column(name = "CodForm")
    private Integer codForm;

    /** CUIT emisor ({@code decimal(11,0)}): {@code 33999176769} (RENTAS). */
    @Column(name = "SNSinCuit", precision = 11, scale = 0)
    private BigDecimal snSinCuit;

    /** Dependencia / sucursal ({@code smallint}): {@code SucNroId} del turno. */
    @Column(name = "SNSinDpcia")
    private Integer snSinDpcia;

    /** Año ({@code smallint}). */
    @Column(name = "SNSinAnio")
    private Integer snSinAnio;

    /** Cuota / mes ({@code smallint}). */
    @Column(name = "SNSinCuo")
    private Integer snSinCuo;

    /** Fecha de vencimiento ({@code datetime}). */
    @Column(name = "SNSinFVto")
    private LocalDateTime snSinFVto;

    /** Total del impuesto base ({@code money}). */
    @Column(name = "SNSinTotIB", precision = 19, scale = 4)
    private BigDecimal snSinTotIB;

    /** Total del adicional ({@code money}). */
    @Column(name = "SNSinAdic1", precision = 19, scale = 4)
    private BigDecimal snSinAdic1;

    /** Fecha de presentación ({@code datetime}). */
    @Column(name = "SNSinFPres")
    private LocalDateTime snSinFPres;

    /** Tipo de liquidación ({@code smallint}): {@code 2} = INTERNET. */
    @Column(name = "SNSinTipLe")
    private Integer snSinTipLe;

    /** Identificador del volante ({@code smallint}): {@code 6} = MISVOLPAGO (2276). */
    @Column(name = "SNSinIdVol")
    private Integer snSinIdVol;

    /** Correlativo general ({@code money}): el legacy lo escribe en {@code 0}. */
    @Column(name = "SNSinCorrG", precision = 19, scale = 4)
    private BigDecimal snSinCorrG = BigDecimal.ZERO;

    /** Punitorio deuda ({@code money}): el legacy lo escribe en {@code 0}. */
    @Column(name = "SNSinPunDe", precision = 19, scale = 4)
    private BigDecimal snSinPunDe = BigDecimal.ZERO;

    /** Recargo por mora ({@code money}): el legacy lo escribe en {@code 0}. */
    @Column(name = "SNSinRPIIm", precision = 19, scale = 4)
    private BigDecimal snSinRPIIm = BigDecimal.ZERO;

    // --- columnas que el legacy deja en 0 o en la sentinela (ver javadoc de la clase) ---

    /** Predecesor ({@code decimal(11,0)}): {@code 0}. */
    @Column(name = "SNSinPre", precision = 11, scale = 0)
    private BigDecimal snSinPre = BigDecimal.ZERO;

    /** Retención ({@code int}): {@code 0}. */
    @Column(name = "SNSinRet")
    private Integer snSinRet = 0;

    /** Tipo de factura ({@code smallint}): {@code 0}. */
    @Column(name = "SNSinTFac")
    private Integer snSinTFac = 0;

    /** Número de factura ({@code decimal(12,0)}): {@code 0}. */
    @Column(name = "SNSinFac", precision = 12, scale = 0)
    private BigDecimal snSinFac = BigDecimal.ZERO;

    /** Fondo de seguro ({@code money}): {@code 0}. */
    @Column(name = "SNSinFSP", precision = 19, scale = 4)
    private BigDecimal snSinFSP = BigDecimal.ZERO;

    /** Sello ({@code money}): {@code 0}. */
    @Column(name = "SNSinSello", precision = 19, scale = 4)
    private BigDecimal snSinSello = BigDecimal.ZERO;

    /** Ley de judeos ({@code money}): {@code 0}. */
    @Column(name = "SNSinLeyJu", precision = 19, scale = 4)
    private BigDecimal snSinLeyJu = BigDecimal.ZERO;

    /** Retención de servicios ({@code money}): {@code 0}. */
    @Column(name = "SNSinRetSe", precision = 19, scale = 4)
    private BigDecimal snSinRetSe = BigDecimal.ZERO;

    /** Deducción anticipada ({@code money}): {@code 0}. */
    @Column(name = "SNSinDerAn", precision = 19, scale = 4)
    private BigDecimal snSinDerAn = BigDecimal.ZERO;

    /** Aporte social ({@code money}): {@code 0}. */
    @Column(name = "SNSinApoSo", precision = 19, scale = 4)
    private BigDecimal snSinApoSo = BigDecimal.ZERO;

    /** Multa ({@code money}): {@code 0}. */
    @Column(name = "SNSinMulta", precision = 19, scale = 4)
    private BigDecimal snSinMulta = BigDecimal.ZERO;

    /** Marca de mercado ({@code money}): {@code 0}. */
    @Column(name = "SNSinMarYS", precision = 19, scale = 4)
    private BigDecimal snSinMarYS = BigDecimal.ZERO;

    /** Recargo ({@code money}): {@code 0}. */
    @Column(name = "SNSinRecar", precision = 19, scale = 4)
    private BigDecimal snSinRecar = BigDecimal.ZERO;

    /** Billing ({@code money}): {@code 0}. */
    @Column(name = "SNSinBillL", precision = 19, scale = 4)
    private BigDecimal snSinBillL = BigDecimal.ZERO;

    /** Intereses ({@code money}): {@code 0}. */
    @Column(name = "SNSinInRu", precision = 19, scale = 4)
    private BigDecimal snSinInRu = BigDecimal.ZERO;

    /** Otros ({@code money}): {@code 0}. */
    @Column(name = "SNSinOtros", precision = 19, scale = 4)
    private BigDecimal snSinOtros = BigDecimal.ZERO;

    /** Actuales ({@code money}): {@code 0}. */
    @Column(name = "SNSinActua", precision = 19, scale = 4)
    private BigDecimal snSinActua = BigDecimal.ZERO;

    /** Resarcimiento ({@code money}): {@code 0}. */
    @Column(name = "SNSinResar", precision = 19, scale = 4)
    private BigDecimal snSinResar = BigDecimal.ZERO;

    /** Financiero ({@code money}): {@code 0}. */
    @Column(name = "SNSinFinan", precision = 19, scale = 4)
    private BigDecimal snSinFinan = BigDecimal.ZERO;

    /** Cobro meats ({@code money}): {@code 0}. */
    @Column(name = "SNSinCMejo", precision = 19, scale = 4)
    private BigDecimal snSinCMejo = BigDecimal.ZERO;

    /** Tipo de pago ({@code smallint}): {@code 0}. */
    @Column(name = "SNSinTiPag")
    private Integer snSinTiPag = 0;

    /** Estado de preprint ({@code smallint}): {@code 0}. */
    @Column(name = "SNSinEstPr")
    private Integer snSinEstPr = 0;

    /** Observación ({@code smallint}): {@code 0}. */
    @Column(name = "SNSinObser")
    private Integer snSinObser = 0;

    /** 405 acids ({@code money}): {@code 0}. */
    @Column(name = "SNSin405AC", precision = 19, scale = 4)
    private BigDecimal snSin405AC = BigDecimal.ZERO;

    /** 405 transport ({@code money}): {@code 0}. */
    @Column(name = "SNSin405TP", precision = 19, scale = 4)
    private BigDecimal snSin405TP = BigDecimal.ZERO;

    /** Ley 7895 ({@code money}): {@code 0}. */
    @Column(name = "SNSinL7895", precision = 19, scale = 4)
    private BigDecimal snSinL7895 = BigDecimal.ZERO;

    /** Anulación ({@code smallint}): {@code 0}. */
    @Column(name = "SNSinAnu")
    private Integer snSinAnu = 0;

    /** Fecha de proceso ({@code datetime}): la sentinela {@code 1753-01-01}. */
    @Column(name = "SNSinFHGra")
    private LocalDateTime snSinFHGra = ar.com.atp.puestos.shared.time.FechaVacia.FECHA_VACIA;

    /** Fecha de Processes ({@code datetime}): la sentinela {@code 1753-01-01}. */
    @Column(name = "SNSinFPro")
    private LocalDateTime snSinFPro = ar.com.atp.puestos.shared.time.FechaVacia.FECHA_VACIA;

    /** Fecha de retención ({@code datetime}): la sentinela {@code 1753-01-01}. */
    @Column(name = "SNSinFRet")
    private LocalDateTime snSinFRet = ar.com.atp.puestos.shared.time.FechaVacia.FECHA_VACIA;

    public SnSinDev() {
        // requerido por JPA
    }

    public BigDecimal getSnSinNum() {
        return snSinNum;
    }

    public void setSnSinNum(BigDecimal snSinNum) {
        this.snSinNum = snSinNum;
    }

    public Integer getCodForm() {
        return codForm;
    }

    public void setCodForm(Integer codForm) {
        this.codForm = codForm;
    }

    public BigDecimal getSnSinCuit() {
        return snSinCuit;
    }

    public void setSnSinCuit(BigDecimal snSinCuit) {
        this.snSinCuit = snSinCuit;
    }

    public Integer getSnSinDpcia() {
        return snSinDpcia;
    }

    public void setSnSinDpcia(Integer snSinDpcia) {
        this.snSinDpcia = snSinDpcia;
    }

    public Integer getSnSinAnio() {
        return snSinAnio;
    }

    public void setSnSinAnio(Integer snSinAnio) {
        this.snSinAnio = snSinAnio;
    }

    public Integer getSnSinCuo() {
        return snSinCuo;
    }

    public void setSnSinCuo(Integer snSinCuo) {
        this.snSinCuo = snSinCuo;
    }

    public LocalDateTime getSnSinFVto() {
        return snSinFVto;
    }

    public void setSnSinFVto(LocalDateTime snSinFVto) {
        this.snSinFVto = snSinFVto;
    }

    public BigDecimal getSnSinTotIB() {
        return snSinTotIB;
    }

    public void setSnSinTotIB(BigDecimal snSinTotIB) {
        this.snSinTotIB = snSinTotIB;
    }

    public BigDecimal getSnSinAdic1() {
        return snSinAdic1;
    }

    public void setSnSinAdic1(BigDecimal snSinAdic1) {
        this.snSinAdic1 = snSinAdic1;
    }

    public LocalDateTime getSnSinFPres() {
        return snSinFPres;
    }

    public void setSnSinFPres(LocalDateTime snSinFPres) {
        this.snSinFPres = snSinFPres;
    }

    public Integer getSnSinTipLe() {
        return snSinTipLe;
    }

    public void setSnSinTipLe(Integer snSinTipLe) {
        this.snSinTipLe = snSinTipLe;
    }

    public Integer getSnSinIdVol() {
        return snSinIdVol;
    }

    public void setSnSinIdVol(Integer snSinIdVol) {
        this.snSinIdVol = snSinIdVol;
    }

    public BigDecimal getSnSinCorrG() {
        return snSinCorrG;
    }

    public void setSnSinCorrG(BigDecimal snSinCorrG) {
        this.snSinCorrG = snSinCorrG;
    }

    public BigDecimal getSnSinPunDe() {
        return snSinPunDe;
    }

    public void setSnSinPunDe(BigDecimal snSinPunDe) {
        this.snSinPunDe = snSinPunDe;
    }

    public BigDecimal getSnSinRPIIm() {
        return snSinRPIIm;
    }

    public void setSnSinRPIIm(BigDecimal snSinRPIIm) {
        this.snSinRPIIm = snSinRPIIm;
    }

    // --- getters de las columnas de valor constante (sólo lectura; el legacy las fija en 0) ---

    public BigDecimal getSnSinPre() {
        return snSinPre;
    }

    public Integer getSnSinRet() {
        return snSinRet;
    }

    public Integer getSnSinTFac() {
        return snSinTFac;
    }

    public BigDecimal getSnSinFac() {
        return snSinFac;
    }

    public BigDecimal getSnSinFSP() {
        return snSinFSP;
    }

    public BigDecimal getSnSinSello() {
        return snSinSello;
    }

    public BigDecimal getSnSinLeyJu() {
        return snSinLeyJu;
    }

    public BigDecimal getSnSinRetSe() {
        return snSinRetSe;
    }

    public BigDecimal getSnSinDerAn() {
        return snSinDerAn;
    }

    public BigDecimal getSnSinApoSo() {
        return snSinApoSo;
    }

    public BigDecimal getSnSinMulta() {
        return snSinMulta;
    }

    public BigDecimal getSnSinMarYS() {
        return snSinMarYS;
    }

    public BigDecimal getSnSinRecar() {
        return snSinRecar;
    }

    public BigDecimal getSnSinBillL() {
        return snSinBillL;
    }

    public BigDecimal getSnSinInRu() {
        return snSinInRu;
    }

    public BigDecimal getSnSinOtros() {
        return snSinOtros;
    }

    public BigDecimal getSnSinActua() {
        return snSinActua;
    }

    public BigDecimal getSnSinResar() {
        return snSinResar;
    }

    public BigDecimal getSnSinFinan() {
        return snSinFinan;
    }

    public BigDecimal getSnSinCMejo() {
        return snSinCMejo;
    }

    public Integer getSnSinTiPag() {
        return snSinTiPag;
    }

    public Integer getSnSinEstPr() {
        return snSinEstPr;
    }

    public Integer getSnSinObser() {
        return snSinObser;
    }

    public BigDecimal getSnSin405AC() {
        return snSin405AC;
    }

    public BigDecimal getSnSin405TP() {
        return snSin405TP;
    }

    public BigDecimal getSnSinL7895() {
        return snSinL7895;
    }

    public Integer getSnSinAnu() {
        return snSinAnu;
    }

    public LocalDateTime getSnSinFHGra() {
        return snSinFHGra;
    }

    public LocalDateTime getSnSinFPro() {
        return snSinFPro;
    }

    public LocalDateTime getSnSinFRet() {
        return snSinFRet;
    }
}
