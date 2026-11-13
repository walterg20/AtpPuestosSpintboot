package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

/**
 * Resultado de un intento de acceso (D-LOG-04).
 *
 * <p>El login <strong>no lanza excepciones</strong>: devuelve el código 0-6 del legacy
 * (R-LOG-019). El mensaje verbatim lo decide {@link MensajesAcceso} (tabla única para los dos
 * adaptadores).
 *
 * @param codigo código de resultado 0-6 (0 = acceso concedido)
 * @param contexto contexto de sesión (sólo en el perfil INTERNO; {@code null} en el WS)
 * @param sucursal sucursal resuelta ({@code PUEPPRIM.PueCod} o {@code RECEPTOR.ReceId}); {@code 0}
 *        si no se resolvió
 * @param comision comisión resuelta ({@code COMISION.CPCId}); {@code 0} si no se resolvió
 */
public record ResultadoAcceso(int codigo, ContextoSesion contexto, int sucursal, int comision) {

    /** ¿Se concedió el acceso? */
    public boolean concedido() {
        return codigo == 0;
    }

    /** Mensaje verbatim del código (tabla única de {@link MensajesAcceso}). */
    public String mensaje() {
        return MensajesAcceso.para(codigo);
    }
}
