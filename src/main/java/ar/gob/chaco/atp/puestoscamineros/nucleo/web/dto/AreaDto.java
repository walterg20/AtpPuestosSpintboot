package ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto;

import java.math.BigDecimal;

/** DTO de lectura de {@code Area} (plan §3.3). PK {@code AreaId} = {@link BigDecimal}. */
public record AreaDto(BigDecimal areaId, String areaDesc) {
}
