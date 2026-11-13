package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import java.util.Map;

/**
 * Tabla <strong>única</strong> de mensajes de acceso (D-LOG-04, R-LOG-018).
 *
 * <p>Los 7 pares (código 0-6 → mensaje) se transcriben <strong>carácter a carácter</strong> del
 * {@code Login.Event Enter} del legacy, <strong>incluidas sus erratas</strong>: "esta" sin tilde y
 * "Receptoria" sin tilde. No se traducen, no se capitalizan y no se corrigen.
 *
 * <p>El código 1 es <strong>inalcanzable</strong> en {@link ServicioAcceso} (el legacy sólo
 * asigna 0, 2, 3, 4, 5 y 6): su rama es código muerto. Se conserva igual porque es parte del
 * contrato observable.
 *
 * <p>El canal REST <strong>no</strong> incluye el mensaje: el SDT {@code SDTSalidaLoginWS} sólo
 * tiene 3 campos.
 */
public final class MensajesAcceso {

    /** Credenciales inválidas (códigos 1 y 2). */
    public static final String CREDENCIALES_INVALIDAS = "Usuario/Contraseña Incorrecto";

    /** El usuario no está relacionado con una persona del sistema (código 3). */
    public static final String SIN_PERSONA =
            "Este Usuario no esta relacionado con una Persona del sistema";

    /** Sin receptoría ni puesto caminerero asignado (código 4). */
    public static final String SIN_SUCURSAL =
            "No existe Receptoria o puesto caminero asignado";

    /** Debe cambiar la clave desde el Sistema Integrado (código 5). */
    public static final String DEBE_CAMBIAR_CLAVE =
            "Debe cambiar su clave desde el Sistema Integrado (win)";

    /** La IP del cliente no está habilitada para el puesto (código 6). */
    public static final String IP_NO_HABILITADA =
            "Dispositivo no habilitado para el uso en el puesto caminero, "
                    + "contacte con el administrador";

    /** Los 7 pares de la matriz de §3.4 de la spec. */
    private static final Map<Integer, String> MENSAJES = Map.of(
            0, "",
            1, CREDENCIALES_INVALIDAS,
            2, CREDENCIALES_INVALIDAS,
            3, SIN_PERSONA,
            4, SIN_SUCURSAL,
            5, DEBE_CAMBIAR_CLAVE,
            6, IP_NO_HABILITADA);

    private MensajesAcceso() {
        // clase de utilería
    }

    /**
     * Mensaje verbatim del código.
     *
     * @param codigo código 0-6
     * @return el mensaje; cadena vacía para el código 0 (acceso concedido) y para cualquier código
     *         fuera de la tabla
     */
    public static String para(int codigo) {
        return MENSAJES.getOrDefault(codigo, "");
    }

    /** ¿El código está en la tabla de mensajes? */
    public static boolean esConocido(int codigo) {
        return MENSAJES.containsKey(codigo);
    }
}
