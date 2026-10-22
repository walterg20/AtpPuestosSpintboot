package ar.com.atp.puestos.nucleo.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTO de alta/modificación de {@code Jurisdiccion}. */
public record JurisdiccionEscrituraDto(
        @NotNull Integer jurCod,
        @NotBlank @Size(max = 20) String jurNom) {
}
