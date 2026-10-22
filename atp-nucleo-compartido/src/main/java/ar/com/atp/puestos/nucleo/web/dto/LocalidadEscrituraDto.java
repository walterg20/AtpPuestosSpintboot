package ar.com.atp.puestos.nucleo.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTO de alta/modificación de {@code Localidad}. La FK {@code provCod} es obligatoria (R-003). */
public record LocalidadEscrituraDto(
        @NotNull Integer locCod,
        @NotBlank @Size(max = 20) String locNom,
        @Size(max = 15) String locNomProv,
        Integer locCodPost,
        @NotNull Integer provCod) {
}
