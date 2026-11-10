package ar.com.atp.puestos.puestos.service.model;

import java.math.BigDecimal;

/**
 * Producto elegible en la grilla del alta (spec {@code 09-formularios-acta} §3.3, plan §3.3,
 * D-ACTA-22).
 *
 * @param prodCod código de producto ({@code ProdCod})
 * @param prodDesc descripción ({@code ProdDesc}, recortada)
 * @param prodTipCal tipo de cálculo ({@code ProdTipCal}); {@code 3} ⇒ cantidad fija {@code 1}
 * @param prodAcop acopiador ({@code ProdAcop})
 * @param precioFijo precio fijo del producto ({@code F2276_BusProTarifa} → {@code F276RFijo})
 * @param precioCM precio de cosecha mecánica ({@code F2276_BusProTarCM} → {@code F276RPreCM})
 */
public record ActaProductoDisponible(Integer prodCod, String prodDesc, Integer prodTipCal,
        String prodAcop, BigDecimal precioFijo, BigDecimal precioCM) {
}
