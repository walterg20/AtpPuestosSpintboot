package ar.gob.chaco.atp.puestoscamineros.service.dto.comision;

/**
 * Fila de la grilla de integrantes de una comisión (spec {@code 13-comisiones} §5.2).
 */
public record IntegranteView(Integer cpcPerId, String perNomyApe) {
}