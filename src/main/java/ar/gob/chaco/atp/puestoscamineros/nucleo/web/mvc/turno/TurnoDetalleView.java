package ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.turno;

import java.math.BigDecimal;
import java.util.List;

public record TurnoDetalleView(TurnoFilaView turno, List<ActaView> actas, int totalActas,
        BigDecimal percepcionEstimada, List<BitacoraEvento> bitacora) {
}