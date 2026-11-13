package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

/**
 * Tabla <strong>única</strong> de mensajes del módulo de turnos (spec {@code 06-turnos} §3.2 y §3.3,
 * T-TUR-B6).
 *
 * <p>Los cinco mensajes de negocio se transcriben <strong>carácter a carácter</strong> de los
 * {@code msg()} del legacy ({@code TurnosListado}), <strong>incluidas sus erratas</strong>: la
 * coma separada del texto ("cerrada , no se puede") y la ausencia de tilde en "esta". No se
 * traducen, no se capitalizan y no se corrigen.
 *
 * <p>El texto de confirmación del botón de cierre ({@code ¿Confirma que desea cerrar el Turno?})
 * también se centraliza aquí: lo consume la vista, pero su origen es el {@code JSEvent} del
 * legacy y no debe duplicarse en la plantilla (R-TUR-005).
 */
public final class MensajesTurno {

    /** La comisión está cerrada: no se pueden abrir turnos nuevos (R-TUR-010). */
    public static final String COMISION_CERRADA =
            "La comisión actual esta cerrada , no se puede abrir nuevos turnos";

    /** El turno seleccionado ya está cerrado (R-TUR-012, paso 1). */
    public static final String TURNO_YA_CERRADO = "El turno se encuentra cerrado";

    /** El turno tiene formularios abiertos: no se puede cerrar (R-TUR-012, paso 2). */
    public static final String FORMULARIOS_ABIERTOS = "Existen Formularios Abiertos";

    /** El procedimiento de cierre falló (R-TUR-012, paso 3). */
    public static final String ERROR_CIERRE = "No se puede cerrar el turno";

    /** Cierre efectivo (R-TUR-012, paso 4). */
    public static final String TURNO_CERRADO = "El turno seleccionado ha sido cerrado";

    /** Actas pendientes bloquean el cierre (R-ACTA-043, B2) — texto NUEVO, no-legacy. */
    public static final String ACTAS_PENDIENTES = "Existen Actas pendientes";

    /** Confirmación del botón {@code Cerrar Turno} (R-TUR-005). */
    public static final String CONFIRMAR_CIERRE = "¿Confirma que desea cerrar el Turno?";

    private MensajesTurno() {
        // clase de utilería
    }
}
