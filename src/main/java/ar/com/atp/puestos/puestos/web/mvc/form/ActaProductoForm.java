package ar.gob.chaco.atp.puestoscamineros.web.mvc.form;

import ar.gob.chaco.atp.puestoscamineros.service.model.ActaAltaProducto;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Objeto de binding de una <strong>fila de la grilla de productos</strong> del formulario de alta
 * (spec 09 §4.2, R-ACTA-020).
 *
 * <p>Es un adaptador de presentación: traduce la fila al comando del servicio mediante
 * {@link #aProducto()}. Los campos derivados ({@code prodDesc}, {@code prodTipCal},
 * {@code f276RFijo}, {@code prodAcop}, {@code f276RZona}) <strong>no se usan en el alta</strong>:
 * el servicio los recalcula desde {@code PRODUCTO}/{@code PRODUCT1} (D-ACTA-07).
 */
public class ActaProductoForm {

    /** Código del producto ({@code ProdCod}). */
    @NotNull(message = "Debe seleccionar algun producto")
    private Integer prodCod;

    /** Descripción del producto: sólo lectura (la muestra la vista). */
    private String prodDesc;

    /** Tipo de cálculo ({@code ProdTipCal}): sólo lectura; {@code 3} ⇒ cantidad fija 1. */
    private Integer prodTipCal;

    /** Cantidad informada ({@code F276RCant}). */
    @NotNull(message = "Debe ingresar el detalle de los productos")
    private BigDecimal f276RCant;

    /** Precio fijo calculado por el servidor ({@code F276RFijo}): sólo lectura. */
    private BigDecimal f276RFijo;

    /** Precio informado ({@code F276RSPre}). */
    private BigDecimal f276RSPre;

    /** Zona ({@code F276RZona}): sólo lectura; la persiste el servicio. */
    private String f276RZona;

    /** Acopiador ({@code ProdAcop}): sólo lectura. */
    private String prodAcop;

    public Integer getProdCod() {
        return prodCod;
    }

    public void setProdCod(Integer prodCod) {
        this.prodCod = prodCod;
    }

    public String getProdDesc() {
        return prodDesc;
    }

    public void setProdDesc(String prodDesc) {
        this.prodDesc = prodDesc;
    }

    public Integer getProdTipCal() {
        return prodTipCal;
    }

    public void setProdTipCal(Integer prodTipCal) {
        this.prodTipCal = prodTipCal;
    }

    public BigDecimal getF276RCant() {
        return f276RCant;
    }

    public void setF276RCant(BigDecimal f276RCant) {
        this.f276RCant = f276RCant;
    }

    public BigDecimal getF276RFijo() {
        return f276RFijo;
    }

    public void setF276RFijo(BigDecimal f276RFijo) {
        this.f276RFijo = f276RFijo;
    }

    public BigDecimal getF276RSPre() {
        return f276RSPre;
    }

    public void setF276RSPre(BigDecimal f276RSPre) {
        this.f276RSPre = f276RSPre;
    }

    public String getF276RZona() {
        return f276RZona;
    }

    public void setF276RZona(String f276RZona) {
        this.f276RZona = f276RZona;
    }

    public String getProdAcop() {
        return prodAcop;
    }

    public void setProdAcop(String prodAcop) {
        this.prodAcop = prodAcop;
    }

    /**
     * Traduce la fila de la vista al comando del servicio.
     *
     * <p>El precio informado ({@code f276RSPre}) viaja tal cual; si la vista no lo informa queda
     * en {@code 0} y el servicio asume el precio fijo registrado (R-ACTA-037).
     *
     * @return el producto equivalente en el contrato {@code service.model.ActaAltaProducto}
     */
    public ActaAltaProducto aProducto() {
        BigDecimal precio = f276RSPre == null ? BigDecimal.ZERO : f276RSPre;
        return new ActaAltaProducto(prodCod, f276RCant, precio);
    }
}
