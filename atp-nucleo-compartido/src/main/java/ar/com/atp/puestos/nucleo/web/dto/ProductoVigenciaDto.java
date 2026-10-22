package ar.com.atp.puestos.nucleo.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** DTO de lectura de una vigencia de producto (nivel 1, plan §3.3). */
public record ProductoVigenciaDto(
        LocalDateTime prodFchVig,
        BigDecimal prodImpu1,
        BigDecimal prodImpu2,
        BigDecimal prodImpu3,
        BigDecimal prodImpu4) {
}
