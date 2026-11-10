package ar.com.atp.puestos.puestos.web.mvc.form;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.puestos.service.model.ActaAlta;
import ar.com.atp.puestos.puestos.service.model.ActaAltaProducto;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Objeto de binding del <strong>formulario de alta</strong> del Acta (Formulario 2276)
 * (spec 09 §4.2, R-ACTA-014…R-ACTA-025).
 *
 * <p>Es un adaptador de presentación (D-ACTA-22): no conoce las reglas del legacy, sólo traduce
 * al comando del servicio mediante {@link #aAlta()}. Los {@code NOT NULL}/{@code NOT BLANK} son la
 * <strong>primera línea de defensa</strong>; las reglas del legacy (orden de evaluación, CUIT
 * válido, máximo de 6 productos, precio inferior al registrado, turno cerrado) se validan en
 * {@code ActaService}, que devuelve el primer mensaje verbatim (R-ACTA-040).
 *
 * <p>Los mensajes de Bean Validation son los <strong>mensajes verbatim</strong> del legacy
 * (spec §3.9) para que el texto observable sea el mismo tanto si rechaza el binding como si
 * rechaza el servicio.
 */
public class ActaForm {

    // ------------------------------------------------------- Datos del Acta

    /** Número de Acta ({@code F276Id}): sólo editable en modo manual. */
    private Integer f276Id;

    /** Motivo del Acta ({@code F276ActaTi}): {@code N} | {@code F} | {@code P} | {@code T}. */
    private String f276ActaTi;

    /** Tipo de movimiento ({@code F276Clasif}): {@code N} | {@code S} | {@code E}. */
    private String f276Clasif;

    /** Fecha del Acta ({@code F276FchMov}): la precarga el servidor y no se edita en el alta. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate f276FchMov;

    // ------------------------------------------------------------- Vendedor

    /** CUIT del vendedor ({@code F276CuitVe}). */
    private Long f276CuitVe;

    /** Razón social del vendedor ({@code F276VenRS}). */
    private String f276VenRS;

    /** Domicilio del vendedor ({@code F276VenDom}). */
    private String f276VenDom;

    /** Código de localidad del vendedor ({@code F276VenCLo}). */
    private Integer f276VenCLo;

    /** Nombre de la localidad del vendedor: derivada del CUIT, sólo lectura. */
    private String f276VenLoc;

    /** Nombre de la provincia del vendedor: derivada del CUIT, sólo lectura y no persistida. */
    private String f276VenProv;

    // --------------------------------------------------------- Destinatario

    /** CUIT del destinatario ({@code F276CuitDe}). */
    private Long f276CuitDe;

    /** Razón social del destinatario ({@code F276DestRS}). */
    private String f276DestRS;

    /** Domicilio del destinatario ({@code F276DestDo}). */
    private String f276DestDo;

    /** Código de localidad del destinatario ({@code F276DestCL}). */
    private Integer f276DestCL;

    /** Nombre de la localidad del destinatario: derivada del CUIT, sólo lectura. */
    private String f276DestLoc;

    /** Nombre de la provincia del destinatario: derivada del CUIT, sólo lectura y no persistida. */
    private String f276DestProv;

    // --------------------------------------------------------- Transportista

    /** CUIT del transportista ({@code F276CuitTr}). */
    private Long f276CuitTr;

    /** Razón social del transportista ({@code F276TranRS}). */
    private String f276TranRS;

    /** Domicilio del transportista ({@code F276TranDo}). */
    private String f276TranDo;

    /** Código de localidad del transportista ({@code F276TranCL}). */
    private Integer f276TranCL;

    /** Nombre de la localidad del transportista: derivada del CUIT, sólo lectura. */
    private String f276TranLoc;

    /** Nombre de la provincia del transportista: derivada del CUIT, sólo lectura y no persistida. */
    private String f276TranProv;

    // --------------------------------------------------- Datos del Transporte

    /** Tipo de comprobante ({@code F276TCpbte}): 1 | 2 | 3 | 5 | 6 | 7. */
    private Integer f276TCpbte;

    /** Número de comprobante ({@code F276NCpbte}). */
    private String f276NCpbte;

    /** Patente del chasis ({@code F276Chasis}). */
    private String f276Chasis;

    /** Patente del acoplado ({@code F276Acopla}). */
    private String f276Acopla;

    /** Kilómetros de flete ({@code F276KmFle}). */
    private BigDecimal f276KmFle;

    // -------------------------------------------------------------- Detalle

    /** Detalle de productos ({@code F22761}): una fila por producto. */
    @Valid
    private List<ActaProductoForm> productos = new ArrayList<>();

    /** Modo manual (carga manual de turno). */
    private boolean manual;

    /** Turno seleccionado (sólo en modo manual). */
    private Integer turNroId;

    /**
     * Contexto de sesión: sólo lectura, no se bindea ni se persiste (R-ACTA-044). El controller lo
     * inyecta para que la vista pueda mostrar los datos derivados.
     */
    private ContextoSesion contexto;

    // ------------------------------------------------------------- Getters

    public String getF276ActaTi() {
        return f276ActaTi;
    }

    public void setF276ActaTi(String f276ActaTi) {
        this.f276ActaTi = f276ActaTi;
    }

    public String getF276Clasif() {
        return f276Clasif;
    }

    public void setF276Clasif(String f276Clasif) {
        this.f276Clasif = f276Clasif;
    }

    public LocalDate getF276FchMov() {
        return f276FchMov;
    }

    public void setF276FchMov(LocalDate f276FchMov) {
        this.f276FchMov = f276FchMov;
    }

    public Long getF276CuitVe() {
        return f276CuitVe;
    }

    public void setF276CuitVe(Long f276CuitVe) {
        this.f276CuitVe = f276CuitVe;
    }

    public String getF276VenRS() {
        return f276VenRS;
    }

    public void setF276VenRS(String f276VenRS) {
        this.f276VenRS = f276VenRS;
    }

    public String getF276VenDom() {
        return f276VenDom;
    }

    public void setF276VenDom(String f276VenDom) {
        this.f276VenDom = f276VenDom;
    }

    public Integer getF276VenCLo() {
        return f276VenCLo;
    }

    public void setF276VenCLo(Integer f276VenCLo) {
        this.f276VenCLo = f276VenCLo;
    }

    public String getF276VenLoc() {
        return f276VenLoc;
    }

    public void setF276VenLoc(String f276VenLoc) {
        this.f276VenLoc = f276VenLoc;
    }

    public String getF276VenProv() {
        return f276VenProv;
    }

    public void setF276VenProv(String f276VenProv) {
        this.f276VenProv = f276VenProv;
    }

    public Long getF276CuitDe() {
        return f276CuitDe;
    }

    public void setF276CuitDe(Long f276CuitDe) {
        this.f276CuitDe = f276CuitDe;
    }

    public String getF276DestRS() {
        return f276DestRS;
    }

    public void setF276DestRS(String f276DestRS) {
        this.f276DestRS = f276DestRS;
    }

    public String getF276DestDo() {
        return f276DestDo;
    }

    public void setF276DestDo(String f276DestDo) {
        this.f276DestDo = f276DestDo;
    }

    public Integer getF276DestCL() {
        return f276DestCL;
    }

    public void setF276DestCL(Integer f276DestCL) {
        this.f276DestCL = f276DestCL;
    }

    public String getF276DestLoc() {
        return f276DestLoc;
    }

    public void setF276DestLoc(String f276DestLoc) {
        this.f276DestLoc = f276DestLoc;
    }

    public String getF276DestProv() {
        return f276DestProv;
    }

    public void setF276DestProv(String f276DestProv) {
        this.f276DestProv = f276DestProv;
    }

    public Long getF276CuitTr() {
        return f276CuitTr;
    }

    public void setF276CuitTr(Long f276CuitTr) {
        this.f276CuitTr = f276CuitTr;
    }

    public String getF276TranRS() {
        return f276TranRS;
    }

    public void setF276TranRS(String f276TranRS) {
        this.f276TranRS = f276TranRS;
    }

    public String getF276TranDo() {
        return f276TranDo;
    }

    public void setF276TranDo(String f276TranDo) {
        this.f276TranDo = f276TranDo;
    }

    public Integer getF276TranCL() {
        return f276TranCL;
    }

    public void setF276TranCL(Integer f276TranCL) {
        this.f276TranCL = f276TranCL;
    }

    public String getF276TranLoc() {
        return f276TranLoc;
    }

    public void setF276TranLoc(String f276TranLoc) {
        this.f276TranLoc = f276TranLoc;
    }

    public String getF276TranProv() {
        return f276TranProv;
    }

    public void setF276TranProv(String f276TranProv) {
        this.f276TranProv = f276TranProv;
    }

    public Integer getF276TCpbte() {
        return f276TCpbte;
    }

    public void setF276TCpbte(Integer f276TCpbte) {
        this.f276TCpbte = f276TCpbte;
    }

    public String getF276NCpbte() {
        return f276NCpbte;
    }

    public void setF276NCpbte(String f276NCpbte) {
        this.f276NCpbte = f276NCpbte;
    }

    public String getF276Chasis() {
        return f276Chasis;
    }

    public void setF276Chasis(String f276Chasis) {
        this.f276Chasis = f276Chasis;
    }

    public String getF276Acopla() {
        return f276Acopla;
    }

    public void setF276Acopla(String f276Acopla) {
        this.f276Acopla = f276Acopla;
    }

    public BigDecimal getF276KmFle() {
        return f276KmFle;
    }

    public void setF276KmFle(BigDecimal f276KmFle) {
        this.f276KmFle = f276KmFle;
    }

    public List<ActaProductoForm> getProductos() {
        return productos;
    }

    public void setProductos(List<ActaProductoForm> productos) {
        this.productos = productos == null ? new ArrayList<>() : productos;
    }

    public Integer getF276Id() {
        return f276Id;
    }

    public void setF276Id(Integer f276Id) {
        this.f276Id = f276Id;
    }

    public boolean isManual() {
        return manual;
    }

    public void setManual(boolean manual) {
        this.manual = manual;
    }

    public Integer getTurNroId() {
        return turNroId;
    }

    public void setTurNroId(Integer turNroId) {
        this.turNroId = turNroId;
    }

    public ContextoSesion getContexto() {
        return contexto;
    }

    public void setContexto(ContextoSesion contexto) {
        this.contexto = contexto;
    }

    // --------------------------------------------------------- Traducción

    /**
     * Traduce el formulario de la vista al comando del servicio.
     *
     * <p>La provincia <strong>no se persiste</strong> (no es columna de {@code F2276}); el nombre
     * de la localidad y el de la provincia viajan sólo como datos derivados de la vista
     * (D-ACTA-17).
     *
     * @return el alta equivalente en el contrato {@code service.model.ActaAlta}
     */
    public ActaAlta aAlta() {
        List<ActaAltaProducto> detalle = new ArrayList<>();
        for (ActaProductoForm producto : productos) {
            detalle.add(producto.aProducto());
        }
        return new ActaAlta(f276ActaTi, f276Clasif, f276FchMov,
                f276CuitVe, f276VenRS, f276VenDom, f276VenCLo,
                f276CuitDe, f276DestRS, f276DestDo, f276DestCL,
                f276CuitTr, f276TranRS, f276TranDo, f276TranCL,
                f276TCpbte, f276NCpbte, f276Chasis, f276Acopla, f276KmFle, detalle,
                f276Id, turNroId);
    }
}
