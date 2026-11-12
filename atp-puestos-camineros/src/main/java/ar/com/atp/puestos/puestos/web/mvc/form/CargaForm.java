package ar.com.atp.puestos.puestos.web.mvc.form;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.puestos.domain.DetalleProductoPrimario;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CargaForm {
    private String barCode;
    private String nroGuia;
    private String dppNroForm;
    private Long conCuit;
    private String conRazSoc;
    private ContextoSesion context;
    private String mensaje;
    private Integer duplicado;
    private String dpptipoGui;
    private List<DetalleProductoPrimario> formularios;
    private Boolean mostrarModal;

    // Campos del modal "Detalle de Carta de Porte / Guia de Transito"
    private String chasis;
    private String acoplado;
    private BigDecimal kmFlete;
    private BigDecimal tarifaKm;
    private BigDecimal montoFlete;
    private BigDecimal alicuota;
    private BigDecimal impuestoFlete;
    private BigDecimal adicional10;
    private BigDecimal subtotalFlete;
    private BigDecimal subtotalTraslado;
    private BigDecimal totalGeneral;
    private String fiscalizador;
    private String hashControl;
    private Integer expedienteDigital;
    private String domicilioFiscal;
    private String puestoIngreso;
    private LocalDateTime fechaControl;
    private String tipoComprobante;

    // Campos del diseno Stitch (traslado de produccion primaria): sin columna equivalente en DETPRODP
    private BigDecimal ingresoBruto;
    private BigDecimal adicionalIB;
    private BigDecimal cosechaMecanica;
    private BigDecimal adicionalCM;

    public String getBarCode() { return barCode; }
    public void setBarCode(String barCode) { this.barCode = barCode; }
    public String getNroGuia() { return nroGuia; }
    public void setNroGuia(String nroGuia) { this.nroGuia = nroGuia; }
    public String getDppNroForm() { return dppNroForm; }
    public void setDppNroForm(String dppNroForm) { this.dppNroForm = dppNroForm; }
    public Long getConCuit() { return conCuit; }
    public void setConCuit(Long conCuit) { this.conCuit = conCuit; }
    public String getConRazSoc() { return conRazSoc; }
    public void setConRazSoc(String conRazSoc) { this.conRazSoc = conRazSoc; }
    public ContextoSesion getContext() { return context; }
    public void setContext(ContextoSesion context) { this.context = context; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public Integer getDuplicado() { return duplicado; }
    public void setDuplicado(Integer duplicado) { this.duplicado = duplicado; }
    public String getDpptipoGui() { return dpptipoGui; }
    public void setDpptipoGui(String dpptipoGui) { this.dpptipoGui = dpptipoGui; }
    public List<DetalleProductoPrimario> getFormularios() { return formularios; }
    public void setFormularios(List<DetalleProductoPrimario> formularios) { this.formularios = formularios; }
    public Boolean getMostrarModal() { return mostrarModal; }
    public void setMostrarModal(Boolean mostrarModal) { this.mostrarModal = mostrarModal; }

    public String getChasis() { return chasis; }
    public void setChasis(String chasis) { this.chasis = chasis; }
    public String getAcoplado() { return acoplado; }
    public void setAcoplado(String acoplado) { this.acoplado = acoplado; }
    public BigDecimal getKmFlete() { return kmFlete; }
    public void setKmFlete(BigDecimal kmFlete) { this.kmFlete = kmFlete; }
    public BigDecimal getTarifaKm() { return tarifaKm; }
    public void setTarifaKm(BigDecimal tarifaKm) { this.tarifaKm = tarifaKm; }
    public BigDecimal getMontoFlete() { return montoFlete; }
    public void setMontoFlete(BigDecimal montoFlete) { this.montoFlete = montoFlete; }
    public BigDecimal getAlicuota() { return alicuota; }
    public void setAlicuota(BigDecimal alicuota) { this.alicuota = alicuota; }
    public BigDecimal getImpuestoFlete() { return impuestoFlete; }
    public void setImpuestoFlete(BigDecimal impuestoFlete) { this.impuestoFlete = impuestoFlete; }
    public BigDecimal getAdicional10() { return adicional10; }
    public void setAdicional10(BigDecimal adicional10) { this.adicional10 = adicional10; }
    public BigDecimal getSubtotalFlete() { return subtotalFlete; }
    public void setSubtotalFlete(BigDecimal subtotalFlete) { this.subtotalFlete = subtotalFlete; }
    public BigDecimal getSubtotalTraslado() { return subtotalTraslado; }
    public void setSubtotalTraslado(BigDecimal subtotalTraslado) { this.subtotalTraslado = subtotalTraslado; }
    public BigDecimal getTotalGeneral() { return totalGeneral; }
    public void setTotalGeneral(BigDecimal totalGeneral) { this.totalGeneral = totalGeneral; }
    public String getFiscalizador() { return fiscalizador; }
    public void setFiscalizador(String fiscalizador) { this.fiscalizador = fiscalizador; }
    public String getHashControl() { return hashControl; }
    public void setHashControl(String hashControl) { this.hashControl = hashControl; }
    public Integer getExpedienteDigital() { return expedienteDigital; }
    public void setExpedienteDigital(Integer expedienteDigital) { this.expedienteDigital = expedienteDigital; }
    public String getDomicilioFiscal() { return domicilioFiscal; }
    public void setDomicilioFiscal(String domicilioFiscal) { this.domicilioFiscal = domicilioFiscal; }
    public String getPuestoIngreso() { return puestoIngreso; }
    public void setPuestoIngreso(String puestoIngreso) { this.puestoIngreso = puestoIngreso; }
    public LocalDateTime getFechaControl() { return fechaControl; }
    public void setFechaControl(LocalDateTime fechaControl) { this.fechaControl = fechaControl; }
    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public BigDecimal getIngresoBruto() { return ingresoBruto; }
    public void setIngresoBruto(BigDecimal ingresoBruto) { this.ingresoBruto = ingresoBruto; }
    public BigDecimal getAdicionalIB() { return adicionalIB; }
    public void setAdicionalIB(BigDecimal adicionalIB) { this.adicionalIB = adicionalIB; }
    public BigDecimal getCosechaMecanica() { return cosechaMecanica; }
    public void setCosechaMecanica(BigDecimal cosechaMecanica) { this.cosechaMecanica = cosechaMecanica; }
    public BigDecimal getAdicionalCM() { return adicionalCM; }
    public void setAdicionalCM(BigDecimal adicionalCM) { this.adicionalCM = adicionalCM; }
}
