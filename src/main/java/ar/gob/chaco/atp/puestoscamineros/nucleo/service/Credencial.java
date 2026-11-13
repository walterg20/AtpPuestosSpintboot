package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

/**
 * Credenciales de un intento de acceso (D-LOG-09).
 *
 * @param usuario identificador tipeado por el usuario (se normaliza a mayúsculas y se rellena a 10
 *        caracteres dentro del servicio — R-LOG-001)
 * @param clave clave tipeada por el usuario (se rellena a 10 caracteres dentro del servicio)
 * @param tipo tipo de acceso: {@code PUE} (puesto caminerero) o {@code REC} (receptoría)
 */
public record Credencial(String usuario, String clave, String tipo) {
}
