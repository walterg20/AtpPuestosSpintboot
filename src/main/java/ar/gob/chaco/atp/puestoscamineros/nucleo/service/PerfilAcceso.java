package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

/**
 * Perfil de acceso: la variante del algoritmo legacy que se ejecuta (D-LOG-06).
 *
 * <p>El perfil <strong>no</strong> es una propiedad de configuración: es una consecuencia del
 * <em>canal</em> por el que llega la petición, exactamente como en la KB, donde son dos
 * procedimientos distintos.
 */
public enum PerfilAcceso {

    /**
     * {@code Seg_ControlAcceso}: arma el contexto de sesión (10 de los 13 campos), hace el chequeo
     * de IP en la rama {@code PUE} y tiene la rama "fue responsable de una comisión".
     */
    INTERNO,

    /**
     * {@code Seg_ControlAccesoWS}: <strong>sin</strong> contexto de sesión, <strong>sin</strong>
     * chequeo de IP y <strong>sin</strong> rama de historia de comisión.
     */
    WS
}
