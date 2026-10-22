package ar.com.atp.puestos.nucleo.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTO de alta/modificación de {@code Calle}. */
public record CalleEscrituraDto(
        @NotNull Integer callCod,
        @NotBlank @Size(max = 22) String callNom) {
}
