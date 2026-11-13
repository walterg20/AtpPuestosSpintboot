package ar.com.atp.puestos.nucleo.web.mvc.turno;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ActaView(Integer nroActa, LocalDateTime fechaHora, BigDecimal importe, String estado) {
}