package ar.com.atp.puestos.nucleo.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO de alta/modificación de {@code Camino}. PK {@code CamID} es {@code char(3)} (R-037). */
public record CaminoEscrituraDto(
        @NotBlank @Size(max = 3) String camID,
        @Size(max = 100) String canRuta) {
}
