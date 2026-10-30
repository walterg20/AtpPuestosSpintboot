package ar.com.atp.puestos.nucleo.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vista de lectura de un turno (módulo {@code 06-turnos}, R-TUR-002).
 *
 * <p>Los campos coinciden con las columnas de la grilla del legacy {@code TurnosListado}:
 * {@code Id. Turno}, {@code Comisión}, {@code Fecha Apertura}, {@code Fecha Cierre} y {@code
 * Estado}; más las columnas que el legacy oculta ({@code SucNroId}, {@code SucTipo} y
 * {@code SNSinNum}) y las de auditoría ({@code TurUsuario}, {@code TurHraApe}, {@code TurHraCie}).
 *
 * @param sucursal {@code PPRIM_TU.SucNroId} (columna oculta en la grilla)
 * @param tipo {@code PPRIM_TU.SucTipo} (columna oculta en la grilla)
 * @param turno {@code PPRIM_TU.TurNroId} — columna {@code Id. Turno}
 * @param usuario {@code PPRIM_TU.TurUsuario} — usuario que abrió el turno
 * @param comision {@code PPRIM_TU.TurCPCId} — columna {@code Comisión}
 * @param fechaApertura {@code PPRIM_TU.TurFchApe} — columna {@code Fecha Apertura}
 * @param fechaCierre {@code PPRIM_TU.TurFchCie} — columna {@code Fecha Cierre}
 * @param horaApertura {@code PPRIM_TU.TurHraApe}
 * @param horaCierre {@code PPRIM_TU.TurHraCie}
 * @param estado {@code PPRIM_TU.TurEstado} — columna {@code Estado} ({@code A} / {@code C})
 * @param snNum {@code PPRIM_TU.SNSinNum} (columna oculta en la grilla)
 */
public record TurnoDto(Integer sucursal, String tipo, Integer turno, String usuario, Integer comision,
        LocalDateTime fechaApertura, LocalDateTime fechaCierre, LocalDateTime horaApertura,
        LocalDateTime horaCierre, String estado, BigDecimal snNum) {
}
