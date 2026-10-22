package ar.com.atp.puestos.nucleo.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * DTO de alta/modificación de {@code Feriado}. La auditoría ({@code ferFchReg}/{@code ferUsuario})
 * se rellena en el servidor (R-033) y por eso no forma parte de este DTO.
 */
public record FeriadoEscrituraDto(
        @NotNull LocalDateTime ferFecha,
        @NotBlank @Size(max = 100) String ferDescripcion,
        @Size(max = 1) String ferTipo) {
}
