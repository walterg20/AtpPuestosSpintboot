package ar.gob.chaco.atp.puestoscamineros.shared.time;

import java.time.LocalDateTime;

/**
 * Sentinela de "fecha vacía" del legacy (spec {@code 06-turnos} §2.1, ASM-TUR-03, T-TUR-A5).
 *
 * <p>En GeneXus un atributo {@code DATETIME} sin valor se materializa en SQL Server con el mínimo
 * del tipo {@code datetime}: {@code 1753-01-01 00:00:00}. El legacy usa ese valor como
 * "vacío" en columnas {@code NOT NULL} sin valor por defecto ({@code PPRIM_TU.TurFchCie},
 * {@code PPRIM_TU.TurHraCie}, {@code SNSINDEV.SNSinFPro}, …).
 *
 * <p>Se centraliza aquí porque la comparten el dominio de turnos ({@code PPRIM_TU}) y el de los
 * números identificatorios ({@code SNSINDEV}), y porque el valor debe ser <strong>idéntico</strong>
 * en ambos: es la marca que permite distinguir "sin dato" de una fecha real (R-TUR-008).
 */
public final class FechaVacia {

    /** Valor centinela: {@code 1753-01-01T00:00:00}. */
    public static final LocalDateTime FECHA_VACIA = LocalDateTime.of(1753, 1, 1, 0, 0);

    private FechaVacia() {
        // clase de utilería
    }
}
