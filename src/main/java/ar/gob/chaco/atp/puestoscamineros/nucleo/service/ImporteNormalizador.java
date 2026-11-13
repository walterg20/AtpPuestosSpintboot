package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Normalizacion de los importes leidos de {@code PRODUCT1} al <strong>tipo logico</strong>
 * {@code NUMERIC(12,2)} de la KB (D-38, T-111).
 *
 * <p><strong>Por que hace falta.</strong> Las columnas fisicas {@code ProdImpu1..5} de
 * {@code PRODUCT1} son de tipo {@code money}, que en SQL Server tiene <strong>escala 4</strong>;
 * el tipo logico declarado por la KB es {@code NUMERIC(12,2)} (escala 2). El driver entrega
 * entonces un {@code BigDecimal} con escala 4 ({@code 12.0000}) que el
 * {@code @Digits(integer = 10, fraction = 2)} del DTO de escritura <strong>rechaza</strong>
 * (R-PROD-08): el ida y vuelta leer -&gt; editar -&gt; guardar se rompe en la API y en la pantalla.
 *
 * <p><strong>Regla.</strong> {@code setScale(2, RoundingMode.HALF_UP)} sobre el valor no nulo; el
 * {@code null} <strong>se conserva</strong> (no se inventa un {@code 0}): la ausencia del importe
 * es un dato de la fila ({@code ProdImpu3}/{@code ProdImpu4} estan en {@code NULL} en 293 filas
 * reales) y la normalizacion a {@code 0} es de la <strong>vista</strong> (R-PROD-20).
 *
 * <p><strong>Consecuencia asumida</strong> (D-38): las 2 filas de {@code PRODUCT1} con mas de 2
 * decimales reales se redondean al centavo en el ida y vuelta. El legacy las asignaba a un
 * {@code NUMERIC(12,2)}, con el mismo efecto.
 *
 * <p>Es la <strong>unica</strong> definicion de la regla: la consumen los dos caminos de lectura
 * ({@link NucleoMapper#toDto(ar.gob.chaco.atp.puestoscamineros.nucleo.domain.ProductoVigencia)} y
 * {@link ProductoService#importesVigentes(Integer)}), de modo que la escala no se duplica.
 */
final class ImporteNormalizador {

    /** Escala del tipo logico {@code NUMERIC(12,2)} de la KB. */
    static final int ESCALA_LOGICA = 2;

    private ImporteNormalizador() {
    }

    /**
     *Lleva el importe al tipo logico {@code NUMERIC(12,2)}.
     *
     * @param valor importe leido de la columna fisica {@code money} (escala 4), o {@code null}
     * @return el importe con escala 2, o {@code null} si el valor era {@code null}
     */
    static BigDecimal aEscalaLogica(BigDecimal valor) {
        return valor == null ? null : valor.setScale(ESCALA_LOGICA, RoundingMode.HALF_UP);
    }
}
