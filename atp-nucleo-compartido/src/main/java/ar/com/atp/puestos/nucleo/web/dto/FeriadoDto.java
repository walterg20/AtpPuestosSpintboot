package ar.com.atp.puestos.nucleo.web.dto;

import java.time.LocalDateTime;

/** DTO de lectura de {@code Feriado}, incluye auditoría (plan §3.3, R-033). */
public record FeriadoDto(
        LocalDateTime ferFecha,
        String ferDescripcion,
        String ferTipo,
        LocalDateTime ferFchReg,
        String ferUsuario) {
}
