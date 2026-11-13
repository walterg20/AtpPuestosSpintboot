package ar.com.atp.puestos.puestos.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TurnoReporteFilaDto(
        Integer f276Id,
        String f276NCpbte,
        String f276VenRS,
        BigDecimal f276CuitVe,
        String f276TranRS,
        BigDecimal f276CuitTr,
        String f276ActaTi,
        LocalDateTime f276FchMov,
        BigDecimal ib,
        BigDecimal cm,
        BigDecimal adicIB,
        BigDecimal adicCM,
        BigDecimal adicFletes,
        BigDecimal impuFletes) {
}