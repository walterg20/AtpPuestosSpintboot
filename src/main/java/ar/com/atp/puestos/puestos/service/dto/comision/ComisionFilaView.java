package ar.gob.chaco.atp.puestoscamineros.service.dto.comision;

/**
 * Fila de la grilla de listado de Comisiones (spec {@code 13-comisiones} §5.1).
 */
public record ComisionFilaView(
        Integer pueCod,
        Integer cpcId,
        String fechaInicio,
        String horaInicio,
        String fechaFin,
        String horaFin,
        String estado,
        String responsable) {
}