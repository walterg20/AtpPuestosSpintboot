package ar.com.atp.puestos.puestos.service.model;

import java.math.BigDecimal;

/**
 * Fila de la grilla de listado de Actas (spec {@code 09-formularios-acta} §3.3, plan §3.3,
 * D-ACTA-22).
 *
 * <p>Reproduce las columnas visibles del {@code Grid276} del legacy (R-ACTA-005) y los tres
 * valores derivados que calcula el servicio (R-ACTA-006, R-ACTA-007, R-ACTA-008).
 *
 * @param f276Id número de Acta ({@code F276Id})
 * @param conCuit CUIT de la contraparte (vendedor → destinatario → transportista) o {@code null}
 * @param conRazSoc razón social de la contraparte o {@code null}
 * @param genManual indicador "Gen. Manual?": {@code "Sí"} o {@code "No"}
 * @param f276Operad usuario que generó el Acta ({@code F276Operad}, recortado)
 * @param f276Total total derivado ({@code F276AdiTot + F276ImpTotal})
 * @param imprimible {@code true} si el Acta <strong>no</strong> es manual (columna "Imprimir")
 */
public record ActaFila(Integer f276Id, Long conCuit, String conRazSoc, String genManual,
        String f276Operad, BigDecimal f276Total, boolean imprimible) {
}
