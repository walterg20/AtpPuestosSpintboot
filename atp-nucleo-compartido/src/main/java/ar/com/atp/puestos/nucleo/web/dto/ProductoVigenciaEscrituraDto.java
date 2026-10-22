package ar.com.atp.puestos.nucleo.web.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** DTO de una vigencia anidada en el alta/modificación de {@code Producto} (R-034). */
public record ProductoVigenciaEscrituraDto(
        @NotNull LocalDateTime prodFchVig,
        @NotNull BigDecimal prodImpu1,
        @NotNull BigDecimal prodImpu2,
        @NotNull BigDecimal prodImpu3,
        @NotNull BigDecimal prodImpu4) {
}
