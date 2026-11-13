package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

/**
 * Resultado de una operación de turnos (alta o cierre).
 *
 * <p>El resultado lleva <strong>tres</strong> cosas: si la operación tuvo éxito, el número de
 * turno afectado (para el alta, el {@code TurNroId} nuevo) y el mensaje verbatim que el legacy
 * mostraba. El mensaje va <strong>centralizado</strong> en {@link MensajesTurno}: las vistas no
 * literalan sus propios textos.
 *
 * <p>El <strong>código</strong> permite al adaptador distinguir los casos sin parsear el mensaje
 * (que es texto de negocio, no un identificador).
 *
 * @param exito {@code true} si la operación se completó
 * @param numeroTurno número de turno afectado; en el alta, el recién creado
 * @param codigo código estable del resultado (ver las constantes de esta clase)
 * @param mensaje mensaje verbatim para el usuario; {@code null} cuando no hay nada que informar
 */
public record ResultadoTurno(boolean exito, Integer numeroTurno, String codigo, String mensaje) {

    /** Operación completada. */
    public static final String OK = "OK";

    /** El alta se rechazó porque la comisión está cerrada (R-TUR-010). */
    public static final String COMISION_CERRADA = "COMISION_CERRADA";

    /** El turno ya estaba cerrado (R-TUR-012, paso 1). */
    public static final String TURNO_YA_CERRADO = "TURNO_YA_CERRADO";

    /** El turno tiene formularios abiertos (R-TUR-012, paso 2). */
    public static final String FORMULARIOS_ABIERTOS = "FORMULARIOS_ABIERTOS";

    /** El turno tiene Actas pendientes (R-ACTA-043, B2). */
    public static final String ACTAS_PENDIENTES = "ACTAS_PENDIENTES";

    /** El procedimiento de cierre falló (R-TUR-012, paso 3). */
    public static final String ERROR_CIERRE = "ERROR_CIERRE";

    /** El turno se cerró (R-TUR-012, paso 4). */
    public static final String TURNO_CERRADO = "TURNO_CERRADO";

    /** Alta exitosa: la grilla se recarga y el adaptador actualiza el contexto de sesión. */
    public static ResultadoTurno creado(Integer numeroTurno) {
        return new ResultadoTurno(true, numeroTurno, OK, null);
    }

    /** Cierre exitoso. */
    public static ResultadoTurno cerrado() {
        return new ResultadoTurno(true, null, TURNO_CERRADO, MensajesTurno.TURNO_CERRADO);
    }

    /** Operación rechazada con su mensaje verbatim. */
    public static ResultadoTurno error(String codigo, String mensaje) {
        return new ResultadoTurno(false, null, codigo, mensaje);
    }
}
