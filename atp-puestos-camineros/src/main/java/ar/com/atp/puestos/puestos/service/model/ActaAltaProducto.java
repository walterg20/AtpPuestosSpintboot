package ar.com.atp.puestos.puestos.service.model;

/**
 * Producto de la grilla del alta (spec {@code 09-formularios-acta} §3.3, plan §3.3, D-ACTA-22).
 *
 * @param prodCod código de producto ({@code ProdCod})
 * @param f276RCant cantidad informada ({@code F276RCant})
 * @param f276RSPre precio informado ({@code F276RSPre}); {@code 0} ⇒ el servicio asume el precio
 *        fijo registrado (R-ACTA-037)
 */
public record ActaAltaProducto(Integer prodCod, java.math.BigDecimal f276RCant,
        java.math.BigDecimal f276RSPre) {
}
