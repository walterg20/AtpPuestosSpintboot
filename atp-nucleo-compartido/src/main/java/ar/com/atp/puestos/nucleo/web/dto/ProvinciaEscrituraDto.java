package ar.com.atp.puestos.nucleo.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTO de alta/modificación de {@code Provincia}. La PK viaja en el body (R-037). */
public record ProvinciaEscrituraDto(
        @NotNull Integer provCod,
        @NotBlank @Size(max = 40) String provNombre) {
}
