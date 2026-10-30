package ar.com.atp.puestos.nucleo.service;

/**
 * Regla de autorización por programa (spec 03-login §3.7, D-LOG-13).
 *
 * <p>Desacoplada de {@link ServicioAutorizacion} para que la regla pueda cambiar sin tocar la
 * construcción de la lista de programas.
 */
public interface VerificadorAutorizacion {

    /**
     * ¿El usuario puede acceder al programa indicado?
     *
     * @param nombrePrograma nombre del objeto legacy (p. ej. {@code WWPPrim_Turnos})
     * @return {@code true} si está autorizado
     */
    boolean isAuthorized(String nombrePrograma);
}
