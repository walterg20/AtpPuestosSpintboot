package ar.com.atp.puestos.nucleo.web.dto;

import java.util.List;

/** DTO de lectura de {@code Producto} con vigencias anidadas (plan §3.3, R-004, P-03). */
public record ProductoDto(
        Integer prodCod,
        String prodDesc,
        Integer prodTipCal,
        Integer tpPriID,
        String uMedNroId,
        String uMedNombre,
        String prodAcop,
        Integer prodAnula,
        List<ProductoVigenciaDto> vigencias) {
}
