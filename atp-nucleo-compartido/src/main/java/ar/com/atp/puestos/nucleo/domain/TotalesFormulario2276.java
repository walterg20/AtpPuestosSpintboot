package ar.com.atp.puestos.nucleo.domain;

import java.math.BigDecimal;

/**
 * Totales de los formularios 2276 de un turno (spec {@code 06-turnos} §3.3, R-TUR-015).
 *
 * <p>Reproduce la acumulación del procedimiento legacy {@code TurnoGenerarSN}:
 *
 * <pre>
 *   &amp;TotIB      += F276ImIBTot
 *   &amp;TotFletes  += F276ImpuFl
 *   &amp;TotCM      += F276ImCMTot
 *   &amp;TotAdCM    += F276AdCMTo
 *   &amp;TotAdFletes+= F276AdicFl
 *   &amp;TotAdIB    += F276AdIBTo
 *   &amp;TotalIB = &amp;TotIB + &amp;TotFletes + &amp;TotCM
 *   &amp;TotalAd = &amp;TotAdIB + &amp;TotAdCM + &amp;TotAdFletes
 * </pre>
 *
 * <p>Los importes son {@code money} en la base (escala 4) y se suman sin cambiar de escala, como
 * en el legacy.
 *
 * @param totIB total de ingresos brutos de los formularios del turno
 * @param totFletes total de impuesto de flete
 * @param totCM total de cosecha mecánica
 * @param totAdCM total de adicional de cosecha mecánica
 * @param totAdFletes total de adicional de flete
 * @param totAdIB total de adicional de ingresos brutos
 */
public record TotalesFormulario2276(BigDecimal totIB, BigDecimal totFletes, BigDecimal totCM,
        BigDecimal totAdCM, BigDecimal totAdFletes, BigDecimal totAdIB) {

    /** Total del impuesto base: {@code TotIB + TotFletes + TotCM}. */
    public BigDecimal totalIB() {
        return totIB.add(totFletes).add(totCM);
    }

    /** Total del adicional: {@code TotAdIB + TotAdCM + TotAdFletes}. */
    public BigDecimal totalAd() {
        return totAdIB.add(totAdCM).add(totAdFletes);
    }

    /**
     * ¿El turno no tiene monto? Es la condición del legacy
     * {@code if &TotalIB.IsEmpty() and &TotalAd.IsEmpty()} (R-TUR-015): en GeneXus un numérico
     * "vacío" es el cero, de modo que la condición equivale a <strong>ambos</strong> totales en
     * cero. En ese caso <strong>no</strong> se genera el SN del volante de pago.
     */
    public boolean sinMonto() {
        return totalIB().signum() == 0 && totalAd().signum() == 0;
    }

    /**
     * Construye los totales desde la fila del agregado SQL.
     *
     * <p>El orden de las columnas es el del {@code SELECT} de
     * {@code Formulario2276Repositorio#totalizarTurno}: {@code totFletes}, {@code totAdCM},
     * {@code totAdFletes}, {@code totAdIB}, {@code totIB}, {@code totCM}.
     *
     * @param fila fila devuelta por la consulta agregada
     * @return los totales normalizados (nunca {@code null})
     */
    public static TotalesFormulario2276 deFila(Object[] fila) {
        return new TotalesFormulario2276(
                valor(fila, 4),
                valor(fila, 0),
                valor(fila, 5),
                valor(fila, 1),
                valor(fila, 2),
                valor(fila, 3));
    }

    private static BigDecimal valor(Object[] fila, int indice) {
        Object bruto = fila == null || fila.length <= indice ? null : fila[indice];
        if (bruto == null) {
            return BigDecimal.ZERO;
        }
        if (bruto instanceof BigDecimal decimal) {
            return decimal;
        }
        if (bruto instanceof Number numero) {
            return new BigDecimal(numero.toString());
        }
        return new BigDecimal(bruto.toString());
    }
}
