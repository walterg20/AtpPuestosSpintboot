package ar.com.atp.puestos.nucleo.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO de alta/modificación de {@code Producto} con vigencias anidadas (P-03, R-034).
 * La PK {@code prodCod} viaja en el body (R-037); {@code uMedNombre} es informativo (derivado).
 */
public record ProductoEscrituraDto(
        @NotNull Integer prodCod,
        @NotBlank @Size(max = 25) String prodDesc,
        Integer prodTipCal,
        Integer tpPriID,
        @Size(max = 3) String uMedNroId,
        @Size(max = 40) String uMedNombre,
        @Size(max = 1) String prodAcop,
        Integer prodAnula,
        @Valid @NotNull List<ProductoVigenciaEscrituraDto> vigencias) {
}
