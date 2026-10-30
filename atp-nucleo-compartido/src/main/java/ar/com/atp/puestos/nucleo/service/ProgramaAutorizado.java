package ar.com.atp.puestos.nucleo.service;

/**
 * Programa accesible por el usuario (spec 03-login §3.7).
 *
 * @param nombre nombre del objeto legacy (clave de la autorización)
 * @param descripcion descripción del programa (rótulo del menú)
 * @param enlace enlace del programa; {@code null} mientras no exista equivalente en el sistema
 *        nuevo (SA-LOG-10)
 */
public record ProgramaAutorizado(String nombre, String descripcion, String enlace) {
}
