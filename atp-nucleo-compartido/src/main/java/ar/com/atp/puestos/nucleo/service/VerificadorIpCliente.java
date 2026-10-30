package ar.com.atp.puestos.nucleo.service;

import org.springframework.stereotype.Component;

/**
 * Chequeo de compatibilidad de la IP del cliente con el prefijo de red del puesto (D-LOG-10).
 *
 * <p>El legacy hace {@code if &ipcliente.IndexOf(&PueIPRed) = 0 // IP NO HABILITADA}. La
 * <strong>semántica exacta</strong> de {@code IndexOf} en el runtime GeneXus 18 no está confirmada
 * (SA-LOG-01), de modo que aquí se implementa la <strong>semántica intencional</strong> del legacy:
 * el prefijo configurado debe <em>matchear</em> la IP del cliente, y un prefijo vacío
 * <strong>no restringe</strong> el acceso (11 de los 14 puestos no tienen prefijo configurado).
 *
 * <p>La comparación es por <strong>prefijo de texto</strong> ({@code startsWith}), que es lo que
 * sugiere el {@code IndexOf} del legacy y lo que sugieren los valores reales
 * ({@code 10.13.26}, {@code 100.100.100}) — SA-LOG-06.
 *
 * <p>El método es el <strong>único</strong> punto donde se decide la compatibilidad: cuando se
 * resuelvan SA-LOG-01 y SA-LOG-06, el cambio queda contenido acá.
 */
@Component
public class VerificadorIpCliente {

    /**
     * ¿La IP del cliente es compatible con el prefijo de red del puesto?
     *
     * @param ip IP del cliente (puede ser {@code null})
     * @param prefijo prefijo de red configurado ({@code PUEPPRIM.PueIPRed}); vacío o {@code null}
     *        ⇒ no restringe
     * @return {@code true} si la IP es compatible
     */
    public boolean esCompatible(String ip, String prefijo) {
        String prefijoNormalizado = prefijo == null ? "" : prefijo.trim();
        if (prefijoNormalizado.isEmpty()) {
            // Sin prefijo configurado el puesto no restringe el acceso.
            return true;
        }
        if (ip == null) {
            return false;
        }
        return ip.trim().startsWith(prefijoNormalizado);
    }
}
