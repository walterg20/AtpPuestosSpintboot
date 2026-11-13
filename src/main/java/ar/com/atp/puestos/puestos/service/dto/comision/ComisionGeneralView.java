package ar.gob.chaco.atp.puestoscamineros.service.dto.comision;

/**
 * Vista del detalle General de una comisión (spec {@code 13-comisiones} §5.2).
 */
public record ComisionGeneralView(
        Integer pueCod,
        String pueDesc,
        Integer cpcId,
        String fechaInicio,
        String horaInicio,
        String fechaFin,
        String horaFin,
        String estado,
        Integer cpcResId,
        String responsable) {
}