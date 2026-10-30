package ar.com.atp.puestos.nucleo.service;

/**
 * Datos de la petición que el <strong>adaptador</strong> resuelve y el servicio consume (D-LOG-15).
 *
 * <p>El servicio <strong>no</strong> importa nada de {@code jakarta.servlet}: la IP del cliente se
 * resuelve en el adaptador (cabecera {@code X-Forwarded-For} y, si viene vacía, la dirección
 * remota de la conexión) y se pasa aquí.
 *
 * @param ipCliente IP del cliente, o {@code null} si no se pudo determinar (o si el canal no
 *        chequea la IP, como el WS)
 */
public record DatosPeticion(String ipCliente) {

    /** Petición sin IP: el perfil WS no hace chequeo de IP. */
    public static DatosPeticion sinIp() {
        return new DatosPeticion(null);
    }
}
