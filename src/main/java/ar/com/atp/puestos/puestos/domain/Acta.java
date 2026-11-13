package ar.gob.chaco.atp.puestoscamineros.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code F2276} (Actas) → tabla física {@code F2276}
 * (spec {@code 09-formularios-acta} §2.1, plan §2.1, T-ACTA-101).
 *
 * <p><strong>Mapa completo</strong>: las <strong>54 columnas físicas</strong> verificadas contra
 * {@code Rentas} el 2026-11-04 (G-DATA). La PK es compuesta
 * ({@code F276Id}, {@code SucTipo}, {@code SucNroId}) y se declara con {@code @IdClass}
 * ({@link ActaId}).
 *
 * <p><strong>Caracteres fijos</strong>: los {@code char(n)} se leen con {@code RTRIM} (R-ACTA-052)
 * y se escriben recortados (los escribe el servicio con el valor ya recortado).
 *
 * <p><strong>Columnas huérfanas</strong> (existen en la BD y no en la KB): {@code PueCod},
 * {@code CPCId} — se mapean con default {@code null} (R-ACTA-051).
 *
 * <p><strong>Atributos derivados que NO son columnas</strong> (verificado contra
 * {@code INFORMATION_SCHEMA.COLUMNS}): {@code F276Total}, {@code F276AdiTot},
 * {@code F276ImpTotal}, {@code F276ImIBTot}, {@code F276ImCMTot}, {@code F276ProCan},
 * {@code F276GenMan}, {@code F276CodBar}, {@code FCalFlete} y las localidades/provincias de las
 * tres partes. Se declaran {@code @Transient} (D-ACTA-07) y los calcula el servicio.
 */
@Entity
@Table(name = "F2276")
@IdClass(ActaId.class)
public class Acta {

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

    /** Código de puesto ({@code PueCod}, {@code smallint NULL}) — huérfana, default {@code null}. */
    @Column(name = "PueCod")
    private Integer pueCod;

    /** Comisión ({@code CPCId}, {@code int NULL}) — huérfana, default {@code null}. */
    @Column(name = "CPCId")
    private Integer cpcId;

    /** Persona que genera el Acta ({@code CPCPerId}, {@code int NOT NULL}). */
    @Column(name = "CPCPerId", nullable = false)
    private Integer cpcPerId;

    /** CUIT del vendedor ({@code F276CuitVe}, {@code decimal(11,0)}). */
    @Column(name = "F276CuitVe", precision = 11, scale = 0)
    private BigDecimal f276CuitVe;

    /** Razón social del vendedor ({@code F276VenRS}, {@code char(35)}). */
    @ColumnTransformer(read = "RTRIM(F276VenRS)")
    @Column(name = "F276VenRS", length = 35)
    private String f276VenRS;

    /** Domicilio del vendedor ({@code F276VenDom}, {@code char(50)}). */
    @ColumnTransformer(read = "RTRIM(F276VenDom)")
    @Column(name = "F276VenDom", length = 50)
    private String f276VenDom;

    /** Localidad del vendedor ({@code F276VenCLo}, {@code int}). */
    @Column(name = "F276VenCLo")
    private Integer f276VenCLo;

    /** Tipo de comprobante ({@code F276TCpbte}, {@code smallint}). */
    @Column(name = "F276TCpbte")
    private Integer f276TCpbte;

    /** Número de comprobante ({@code F276NCpbte}, {@code char(15)}). */
    @ColumnTransformer(read = "RTRIM(F276NCpbte)")
    @Column(name = "F276NCpbte", length = 15)
    private String f276NCpbte;

    /** Fecha de movimiento ({@code F276FchMov}, {@code datetime}). */
    @Column(name = "F276FchMov")
    private LocalDateTime f276FchMov;

    /** CUIT del destinatario ({@code F276CuitDe}, {@code decimal(11,0)}). */
    @Column(name = "F276CuitDe", precision = 11, scale = 0)
    private BigDecimal f276CuitDe;

    /** Razón social del destinatario ({@code F276DestRS}, {@code char(35)}). */
    @ColumnTransformer(read = "RTRIM(F276DestRS)")
    @Column(name = "F276DestRS", length = 35)
    private String f276DestRS;

    /** Domicilio del destinatario ({@code F276DestDo}, {@code char(50)}). */
    @ColumnTransformer(read = "RTRIM(F276DestDo)")
    @Column(name = "F276DestDo", length = 50)
    private String f276DestDo;

    /** Localidad del destinatario ({@code F276DestCL}, {@code int}). */
    @Column(name = "F276DestCL")
    private Integer f276DestCL;

    /** CUIT del transportista ({@code F276CuitTr}, {@code decimal(11,0)}). */
    @Column(name = "F276CuitTr", precision = 11, scale = 0)
    private BigDecimal f276CuitTr;

    /** Razón social del transportista ({@code F276TranRS}, {@code char(35)}). */
    @ColumnTransformer(read = "RTRIM(F276TranRS)")
    @Column(name = "F276TranRS", length = 35)
    private String f276TranRS;

    /** Domicilio del transportista ({@code F276TranDo}, {@code char(50)}). */
    @ColumnTransformer(read = "RTRIM(F276TranDo)")
    @Column(name = "F276TranDo", length = 50)
    private String f276TranDo;

    /** Localidad del transportista ({@code F276TranCL}, {@code int}). */
    @Column(name = "F276TranCL")
    private Integer f276TranCL;

    /** Vehículo ({@code F276Vehicu}, {@code char(20)}). */
    @ColumnTransformer(read = "RTRIM(F276Vehicu)")
    @Column(name = "F276Vehicu", length = 20)
    private String f276Vehicu;

    /** Patente del chasis ({@code F276Chasis}, {@code char(10)}). */
    @ColumnTransformer(read = "RTRIM(F276Chasis)")
    @Column(name = "F276Chasis", length = 10)
    private String f276Chasis;

    /** Patente del acoplado ({@code F276Acopla}, {@code char(10)}). */
    @ColumnTransformer(read = "RTRIM(F276Acopla)")
    @Column(name = "F276Acopla", length = 10)
    private String f276Acopla;

    /** Monto impurity del flete ({@code F276MImpFl}, {@code money}). */
    @Column(name = "F276MImpFl", precision = 19, scale = 4)
    private BigDecimal f276MImpFl;

    /** Kilómetros de flete ({@code F276KmFle}, {@code smallmoney}). */
    @Column(name = "F276KmFle", precision = 10, scale = 4)
    private BigDecimal f276KmFle;

    /** Tarifa por km ({@code F276TarxKm}, {@code money}) — fórmula en KB, columna física. */
    @Column(name = "F276TarxKm", precision = 19, scale = 4)
    private BigDecimal f276TarxKm;

    /** Alícuota del flete ({@code F276AlicFl}, {@code smallmoney}). */
    @Column(name = "F276AlicFl", precision = 10, scale = 4)
    private BigDecimal f276AlicFl;

    /** Impuesto del flete ({@code F276ImpuFl}, {@code money}) ⇒ {@code 0} con {@code F276GenWeb='A'}. */
    @Column(name = "F276ImpuFl", precision = 19, scale = 4)
    private BigDecimal f276ImpuFl;

    /** Adicional del flete ({@code F276AdicFl}, {@code money}). */
    @Column(name = "F276AdicFl", precision = 19, scale = 4)
    private BigDecimal f276AdicFl;

    /** Adicional de ingresos brutos ({@code F276AdIBTo}, {@code money}). */
    @Column(name = "F276AdIBTo", precision = 19, scale = 4)
    private BigDecimal f276AdIBTo;

    /** Adicional de cosecha mecánica ({@code F276AdCMTo}, {@code money}). */
    @Column(name = "F276AdCMTo", precision = 19, scale = 4)
    private BigDecimal f276AdCMTo;

    /** Patente del vehículo ({@code F276PatVeh}, {@code char(10)}). */
    @ColumnTransformer(read = "RTRIM(F276PatVeh)")
    @Column(name = "F276PatVeh", length = 10)
    private String f276PatVeh;

    /** Usuario que genera el Acta ({@code F276Operad}, {@code char(10)}) — en mayúsculas. */
    @ColumnTransformer(read = "RTRIM(F276Operad)")
    @Column(name = "F276Operad", length = 10)
    private String f276Operad;

    /** Vínculo con el lote ({@code LF276ID}, {@code int}) → {@code LOTES227.LF276ID}. */
    @Column(name = "LF276ID")
    private Integer lf276Id;

    /** Fecha de primer uso ({@code F276FchPCC}, {@code datetime}). */
    @Column(name = "F276FchPCC")
    private LocalDateTime f276FchPCC;

    /** Turno de la sesión ({@code TurNroId}, {@code int}). */
    @Column(name = "TurNroId")
    private Integer turNroId;

    /** Tipo de movimiento ({@code F276Clasif}, {@code char(1) NOT NULL}): {@code E}/{@code S}. */
    @ColumnTransformer(read = "RTRIM(F276Clasif)")
    @Column(name = "F276Clasif", nullable = false, length = 1)
    private String f276Clasif;

    /** Forma de pago ({@code F276ForPag}, {@code char(3)}) ⇒ {@code 'EFE'}. */
    @ColumnTransformer(read = "RTRIM(F276ForPag)")
    @Column(name = "F276ForPag", length = 3)
    private String f276ForPag;

    /** Número de pago ({@code F276NumPag}, {@code char(20)}). */
    @ColumnTransformer(read = "RTRIM(F276NumPag)")
    @Column(name = "F276NumPag", length = 20)
    private String f276NumPag;

    /** Multa ({@code F276Multa}, {@code money NOT NULL}) ⇒ default {@code 0}. */
    @Column(name = "F276Multa", nullable = false, precision = 19, scale = 4)
    private BigDecimal f276Multa;

    /** Localidad de origen ({@code F276OrCod}, {@code int}). */
    @Column(name = "F276OrCod")
    private Integer f276OrCod;

    /** Descripción del origen ({@code F276Origen}, {@code char(50)}). */
    @ColumnTransformer(read = "RTRIM(F276Origen)")
    @Column(name = "F276Origen", length = 50)
    private String f276Origen;

    /** Localidad de destino ({@code F276DeCod}, {@code int}). */
    @Column(name = "F276DeCod")
    private Integer f276DeCod;

    /** Descripción del destino ({@code F276Destin}, {@code char(50)}). */
    @ColumnTransformer(read = "RTRIM(F276Destin)")
    @Column(name = "F276Destin", length = 50)
    private String f276Destin;

    /** Estado ({@code F276Estad}, {@code char(1)}): {@code 'C'} confirmada, {@code 'A'} anulada. */
    @ColumnTransformer(read = "RTRIM(F276Estad)")
    @Column(name = "F276Estad", length = 1)
    private String f276Estad;

    /** Origen de la generación ({@code F276GenWeb}, {@code char(1)}) ⇒ {@code 'A'} (web). */
    @ColumnTransformer(read = "RTRIM(F276GenWeb)")
    @Column(name = "F276GenWeb", length = 1)
    private String f276GenWeb;

    /** Banqueo de cheque ({@code F276BanChe}, {@code char(40)}). */
    @ColumnTransformer(read = "RTRIM(F276BanChe)")
    @Column(name = "F276BanChe", length = 40)
    private String f276BanChe;

    /** Versión del sistema ({@code F276Vers}, {@code char(18)}) → {@code BuscarVersionActual}. */
    @ColumnTransformer(read = "RTRIM(F276Vers)")
    @Column(name = "F276Vers", length = 18)
    private String f276Vers;

    /** Fecha de grabación ({@code F276FeGra}, {@code datetime}) ⇒ {@code ServerNow()}. */
    @Column(name = "F276FeGra")
    private LocalDateTime f276FeGra;

    /** Número de SN ({@code F276SnSnNu}, {@code decimal(10,0)}) — sólo casa central. */
    @Column(name = "F276SnSnNu", precision = 10, scale = 0)
    private BigDecimal f276SnSnNu;

    /** Fecha de pago ({@code F276FePag}, {@code datetime}). */
    @Column(name = "F276FePag")
    private LocalDateTime f276FePag;

    /** Motivo del Acta ({@code F276ActaTi}, {@code char(1)}): {@code F}/{@code P}/{@code T}. */
    @ColumnTransformer(read = "RTRIM(F276ActaTi)")
    @Column(name = "F276ActaTi", length = 1)
    private String f276ActaTi;

    /** Tipo de formulario ({@code F276FormTi}, {@code smallint}): 1/2/3. */
    @Column(name = "F276FormTi")
    private Integer f276FormTi;

    /** Total del Acta ({@code F276Total}) — derivado, no es columna (D-ACTA-07). */
    @Transient
    private BigDecimal f276Total;

    /** Total de adicionales ({@code F276AdiTot}) — derivado, no es columna (D-ACTA-07). */
    @Transient
    private BigDecimal f276AdiTot;

    /** Total de impuestos ({@code F276ImpTotal}) — derivado, no es columna (D-ACTA-07). */
    @Transient
    private BigDecimal f276ImpTotal;

    /** Total de ingresos brutos ({@code F276ImIBTot}) — derivado del detalle (D-ACTA-07). */
    @Transient
    private BigDecimal f276ImIBTot;

    /** Total de cosecha mecánica ({@code F276ImCMTot}) — derivado del detalle (D-ACTA-07). */
    @Transient
    private BigDecimal f276ImCMTot;

    /** Cantidad de productos ({@code F276ProCan}) — derivado, no es columna (D-ACTA-07). */
    @Transient
    private Integer f276ProCan;

    /** Indicador de generación manual ({@code F276GenMan}) — derivado, no es columna (D-ACTA-07). */
    @Transient
    private String f276GenMan;

    /** Código de barras ({@code F276CodBar}) — derivado, no es columna (SA-ACTA-05). */
    @Transient
    private String f276CodBar;

    /** Cálculo de flete ({@code FCalFlete}) — derivado, no es columna (D-ACTA-07). */
    @Transient
    private Boolean fCalFlete;

    /** Localidad del vendedor ({@code F276VenLoc}) — derivada, no es columna. */
    @Transient
    private String f276VenLoc;

    /** Provincia del vendedor ({@code F276VenProv}) — derivada, no es columna. */
    @Transient
    private String f276VenProv;

    /** Localidad del destinatario ({@code F276DestLoc}) — derivada, no es columna. */
    @Transient
    private String f276DestLoc;

    /** Provincia del destinatario ({@code F276DestProv}) — derivada, no es columna. */
    @Transient
    private String f276DestProv;

    /** Localidad del transportista ({@code F276TranLoc}) — derivada, no es columna. */
    @Transient
    private String f276TranLoc;

    /** Provincia del transportista ({@code F276TranProv}) — derivada, no es columna. */
    @Transient
    private String f276TranProv;

    public Acta() {
        // requerido por JPA
    }

    public Integer getF276Id() { return f276Id; }
    public void setF276Id(Integer f276Id) { this.f276Id = f276Id; }
    public String getSucTipo() { return sucTipo; }
    public void setSucTipo(String sucTipo) { this.sucTipo = sucTipo; }
    public Integer getSucNroId() { return sucNroId; }
    public void setSucNroId(Integer sucNroId) { this.sucNroId = sucNroId; }
    public Integer getPueCod() { return pueCod; }
    public void setPueCod(Integer pueCod) { this.pueCod = pueCod; }
    public Integer getCpcId() { return cpcId; }
    public void setCpcId(Integer cpcId) { this.cpcId = cpcId; }
    public Integer getCpcPerId() { return cpcPerId; }
    public void setCpcPerId(Integer cpcPerId) { this.cpcPerId = cpcPerId; }
    public BigDecimal getF276CuitVe() { return f276CuitVe; }
    public void setF276CuitVe(BigDecimal f276CuitVe) { this.f276CuitVe = f276CuitVe; }
    public String getF276VenRS() { return f276VenRS; }
    public void setF276VenRS(String f276VenRS) { this.f276VenRS = f276VenRS; }
    public String getF276VenDom() { return f276VenDom; }
    public void setF276VenDom(String f276VenDom) { this.f276VenDom = f276VenDom; }
    public Integer getF276VenCLo() { return f276VenCLo; }
    public void setF276VenCLo(Integer f276VenCLo) { this.f276VenCLo = f276VenCLo; }
    public Integer getF276TCpbte() { return f276TCpbte; }
    public void setF276TCpbte(Integer f276TCpbte) { this.f276TCpbte = f276TCpbte; }
    public String getF276NCpbte() { return f276NCpbte; }
    public void setF276NCpbte(String f276NCpbte) { this.f276NCpbte = f276NCpbte; }
    public LocalDateTime getF276FchMov() { return f276FchMov; }
    public void setF276FchMov(LocalDateTime f276FchMov) { this.f276FchMov = f276FchMov; }
    public BigDecimal getF276CuitDe() { return f276CuitDe; }
    public void setF276CuitDe(BigDecimal f276CuitDe) { this.f276CuitDe = f276CuitDe; }
    public String getF276DestRS() { return f276DestRS; }
    public void setF276DestRS(String f276DestRS) { this.f276DestRS = f276DestRS; }
    public String getF276DestDo() { return f276DestDo; }
    public void setF276DestDo(String f276DestDo) { this.f276DestDo = f276DestDo; }
    public Integer getF276DestCL() { return f276DestCL; }
    public void setF276DestCL(Integer f276DestCL) { this.f276DestCL = f276DestCL; }
    public BigDecimal getF276CuitTr() { return f276CuitTr; }
    public void setF276CuitTr(BigDecimal f276CuitTr) { this.f276CuitTr = f276CuitTr; }
    public String getF276TranRS() { return f276TranRS; }
    public void setF276TranRS(String f276TranRS) { this.f276TranRS = f276TranRS; }
    public String getF276TranDo() { return f276TranDo; }
    public void setF276TranDo(String f276TranDo) { this.f276TranDo = f276TranDo; }
    public Integer getF276TranCL() { return f276TranCL; }
    public void setF276TranCL(Integer f276TranCL) { this.f276TranCL = f276TranCL; }
    public String getF276Vehicu() { return f276Vehicu; }
    public void setF276Vehicu(String f276Vehicu) { this.f276Vehicu = f276Vehicu; }
    public String getF276Chasis() { return f276Chasis; }
    public void setF276Chasis(String f276Chasis) { this.f276Chasis = f276Chasis; }
    public String getF276Acopla() { return f276Acopla; }
    public void setF276Acopla(String f276Acopla) { this.f276Acopla = f276Acopla; }
    public BigDecimal getF276MImpFl() { return f276MImpFl; }
    public void setF276MImpFl(BigDecimal f276MImpFl) { this.f276MImpFl = f276MImpFl; }
    public BigDecimal getF276KmFle() { return f276KmFle; }
    public void setF276KmFle(BigDecimal f276KmFle) { this.f276KmFle = f276KmFle; }
    public BigDecimal getF276TarxKm() { return f276TarxKm; }
    public void setF276TarxKm(BigDecimal f276TarxKm) { this.f276TarxKm = f276TarxKm; }
    public BigDecimal getF276AlicFl() { return f276AlicFl; }
    public void setF276AlicFl(BigDecimal f276AlicFl) { this.f276AlicFl = f276AlicFl; }
    public BigDecimal getF276ImpuFl() { return f276ImpuFl; }
    public void setF276ImpuFl(BigDecimal f276ImpuFl) { this.f276ImpuFl = f276ImpuFl; }
    public BigDecimal getF276AdicFl() { return f276AdicFl; }
    public void setF276AdicFl(BigDecimal f276AdicFl) { this.f276AdicFl = f276AdicFl; }
    public BigDecimal getF276AdIBTo() { return f276AdIBTo; }
    public void setF276AdIBTo(BigDecimal f276AdIBTo) { this.f276AdIBTo = f276AdIBTo; }
    public BigDecimal getF276AdCMTo() { return f276AdCMTo; }
    public void setF276AdCMTo(BigDecimal f276AdCMTo) { this.f276AdCMTo = f276AdCMTo; }
    public String getF276PatVeh() { return f276PatVeh; }
    public void setF276PatVeh(String f276PatVeh) { this.f276PatVeh = f276PatVeh; }
    public String getF276Operad() { return f276Operad; }
    public void setF276Operad(String f276Operad) { this.f276Operad = f276Operad; }
    public Integer getLf276Id() { return lf276Id; }
    public void setLf276Id(Integer lf276Id) { this.lf276Id = lf276Id; }
    public LocalDateTime getF276FchPCC() { return f276FchPCC; }
    public void setF276FchPCC(LocalDateTime f276FchPCC) { this.f276FchPCC = f276FchPCC; }
    public Integer getTurNroId() { return turNroId; }
    public void setTurNroId(Integer turNroId) { this.turNroId = turNroId; }
    public String getF276Clasif() { return f276Clasif; }
    public void setF276Clasif(String f276Clasif) { this.f276Clasif = f276Clasif; }
    public String getF276ForPag() { return f276ForPag; }
    public void setF276ForPag(String f276ForPag) { this.f276ForPag = f276ForPag; }
    public String getF276NumPag() { return f276NumPag; }
    public void setF276NumPag(String f276NumPag) { this.f276NumPag = f276NumPag; }
    public BigDecimal getF276Multa() { return f276Multa; }
    public void setF276Multa(BigDecimal f276Multa) { this.f276Multa = f276Multa; }
    public Integer getF276OrCod() { return f276OrCod; }
    public void setF276OrCod(Integer f276OrCod) { this.f276OrCod = f276OrCod; }
    public String getF276Origen() { return f276Origen; }
    public void setF276Origen(String f276Origen) { this.f276Origen = f276Origen; }
    public Integer getF276DeCod() { return f276DeCod; }
    public void setF276DeCod(Integer f276DeCod) { this.f276DeCod = f276DeCod; }
    public String getF276Destin() { return f276Destin; }
    public void setF276Destin(String f276Destin) { this.f276Destin = f276Destin; }
    public String getF276Estad() { return f276Estad; }
    public void setF276Estad(String f276Estad) { this.f276Estad = f276Estad; }
    public String getF276GenWeb() { return f276GenWeb; }
    public void setF276GenWeb(String f276GenWeb) { this.f276GenWeb = f276GenWeb; }
    public String getF276BanChe() { return f276BanChe; }
    public void setF276BanChe(String f276BanChe) { this.f276BanChe = f276BanChe; }
    public String getF276Vers() { return f276Vers; }
    public void setF276Vers(String f276Vers) { this.f276Vers = f276Vers; }
    public LocalDateTime getF276FeGra() { return f276FeGra; }
    public void setF276FeGra(LocalDateTime f276FeGra) { this.f276FeGra = f276FeGra; }
    public BigDecimal getF276SnSnNu() { return f276SnSnNu; }
    public void setF276SnSnNu(BigDecimal f276SnSnNu) { this.f276SnSnNu = f276SnSnNu; }
    public LocalDateTime getF276FePag() { return f276FePag; }
    public void setF276FePag(LocalDateTime f276FePag) { this.f276FePag = f276FePag; }
    public String getF276ActaTi() { return f276ActaTi; }
    public void setF276ActaTi(String f276ActaTi) { this.f276ActaTi = f276ActaTi; }
    public Integer getF276FormTi() { return f276FormTi; }
    public void setF276FormTi(Integer f276FormTi) { this.f276FormTi = f276FormTi; }
    public BigDecimal getF276Total() { return f276Total; }
    public void setF276Total(BigDecimal f276Total) { this.f276Total = f276Total; }
    public BigDecimal getF276AdiTot() { return f276AdiTot; }
    public void setF276AdiTot(BigDecimal f276AdiTot) { this.f276AdiTot = f276AdiTot; }
    public BigDecimal getF276ImpTotal() { return f276ImpTotal; }
    public void setF276ImpTotal(BigDecimal f276ImpTotal) { this.f276ImpTotal = f276ImpTotal; }
    public BigDecimal getF276ImIBTot() { return f276ImIBTot; }
    public void setF276ImIBTot(BigDecimal f276ImIBTot) { this.f276ImIBTot = f276ImIBTot; }
    public BigDecimal getF276ImCMTot() { return f276ImCMTot; }
    public void setF276ImCMTot(BigDecimal f276ImCMTot) { this.f276ImCMTot = f276ImCMTot; }
    public Integer getF276ProCan() { return f276ProCan; }
    public void setF276ProCan(Integer f276ProCan) { this.f276ProCan = f276ProCan; }
    public String getF276GenMan() { return f276GenMan; }
    public void setF276GenMan(String f276GenMan) { this.f276GenMan = f276GenMan; }
    public String getF276CodBar() { return f276CodBar; }
    public void setF276CodBar(String f276CodBar) { this.f276CodBar = f276CodBar; }
    public Boolean getFCalFlete() { return fCalFlete; }
    public void setFCalFlete(Boolean fCalFlete) { this.fCalFlete = fCalFlete; }
    public String getF276VenLoc() { return f276VenLoc; }
    public void setF276VenLoc(String f276VenLoc) { this.f276VenLoc = f276VenLoc; }
    public String getF276VenProv() { return f276VenProv; }
    public void setF276VenProv(String f276VenProv) { this.f276VenProv = f276VenProv; }
    public String getF276DestLoc() { return f276DestLoc; }
    public void setF276DestLoc(String f276DestLoc) { this.f276DestLoc = f276DestLoc; }
    public String getF276DestProv() { return f276DestProv; }
    public void setF276DestProv(String f276DestProv) { this.f276DestProv = f276DestProv; }
    public String getF276TranLoc() { return f276TranLoc; }
    public void setF276TranLoc(String f276TranLoc) { this.f276TranLoc = f276TranLoc; }
    public String getF276TranProv() { return f276TranProv; }
    public void setF276TranProv(String f276TranProv) { this.f276TranProv = f276TranProv; }
}
