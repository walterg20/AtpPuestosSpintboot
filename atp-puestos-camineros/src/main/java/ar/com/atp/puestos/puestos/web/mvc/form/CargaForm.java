package ar.com.atp.puestos.puestos.web.mvc.form;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.puestos.domain.DetalleProductoPrimario;
import java.util.List;

public class CargaForm {
    private String barCode;
    private String dppNroForm;
    private Long conCuit;
    private String conRazSoc;
    private ContextoSesion context;
    private String mensaje;
    private Integer duplicado;
    private String dpptipoGui;
    private List<DetalleProductoPrimario> formularios;

    public String getBarCode() { return barCode; }
    public void setBarCode(String barCode) { this.barCode = barCode; }
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
}
