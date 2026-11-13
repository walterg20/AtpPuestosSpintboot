package ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** DTO de alta/modificación de {@code Area}. PK {@code areaId} = {@link BigDecimal} (R-037). */
public record AreaEscrituraDto(
        @NotNull BigDecimal areaId,
        @NotBlank @Size(max = 50) String areaDesc) {
}
