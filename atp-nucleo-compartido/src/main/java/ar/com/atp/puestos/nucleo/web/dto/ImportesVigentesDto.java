package ar.com.atp.puestos.nucleo.web.dto;

import java.math.BigDecimal;

/** DTO de los importes derivados {@code ProdIMax1..4} (fórmulas KB, plan §3.3, R-005). */
public record ImportesVigentesDto(
        Integer prodCod,
        BigDecimal prodIMax1,
        BigDecimal prodIMax2,
        BigDecimal prodIMax3,
        BigDecimal prodIMax4) {
}
