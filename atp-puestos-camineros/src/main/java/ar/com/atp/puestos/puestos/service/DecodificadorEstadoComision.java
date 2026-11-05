package ar.com.atp.puestos.puestos.service;

/**
 * Decodifica el estado de una comisión a partir de {@code CPCCierre} (R-COM-006, D-COM-003):
 * {@code 'S'} → "Cerrado", cualquier otro valor (incluido {@code null}) → "Abierto".
 */
public final class DecodificadorEstadoComision {

    private DecodificadorEstadoComision() {
        // clase de utilidad
    }

    public static String decodificar(String cpcCierre) {
        if (cpcCierre != null && cpcCierre.trim().equalsIgnoreCase("S")) {
            return "Cerrado";
        }
        return "Abierto";
    }
}