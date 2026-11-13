package ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO de alta/modificación de {@code Producto} con vigencias anidadas (P-03, R-034).
 * La PK {@code prodCod} viaja en el body (R-037); {@code uMedNombre} es informativo (derivado).
 *
 * <p><strong>T-102 / D-32</strong>: {@code prodCod} queda acotado al rango de la columna física
 * {@code PRODUCTO.ProdCod} ({@code smallint}): {@code 0…32767}. Sin esta restricción, un
 * {@code prodCod} fuera de rango llegaba hasta SQL Server y devolvía {@code 500 ERROR_INTERNO}
 * (hallazgo N-4); ahora el contrato devuelve {@code 400 VALIDACION} (R-PROD-07).
 */
public record ProductoEscrituraDto(
        @NotNull @Min(0) @Max(32767) Integer prodCod,
        @NotBlank @Size(max = 25) String prodDesc,
        Integer prodTipCal,
        Integer tpPriID,
        @Size(max = 3) String uMedNroId,
        @Size(max = 40) String uMedNombre,
        @Size(max = 1) String prodAcop,
        Integer prodAnula,
        @Valid @NotNull List<ProductoVigenciaEscrituraDto> vigencias) {
}
