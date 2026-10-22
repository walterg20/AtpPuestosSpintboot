package ar.com.atp.puestos.nucleo.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de una vigencia anidada en el alta/modificación de {@code Producto} (R-034).
 *
 * <p><strong>T-102 / D-32</strong>: los 4 importes quedan acotados al tipo lógico
 * {@code NUMERIC(12,2)} del legacy, es decir {@code ±9999999999.99} con <strong>hasta 2
 * decimales</strong> ({@code @Digits(integer = 10, fraction = 2)}). Antes, un importe con 3
 * decimales se aceptaba y el backend respondía {@code 500 ERROR_INTERNO} al escribir en la
 * columna física {@code money} (escala 4); ahora responde {@code 400 VALIDACION} (R-PROD-08).
 */
public record ProductoVigenciaEscrituraDto(
        @NotNull LocalDateTime prodFchVig,
        @NotNull @Digits(integer = 10, fraction = 2) BigDecimal prodImpu1,
        @NotNull @Digits(integer = 10, fraction = 2) BigDecimal prodImpu2,
        @NotNull @Digits(integer = 10, fraction = 2) BigDecimal prodImpu3,
        @NotNull @Digits(integer = 10, fraction = 2) BigDecimal prodImpu4) {
}
