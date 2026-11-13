package ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.turno;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ServicioTurnos;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.TurnoDto;
import ar.gob.chaco.atp.puestoscamineros.shared.time.FechaVacia;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Fila de la grilla de turnos: los 5 datos que muestra el legacy ({@code Id. Turno},
 * {@code Comisión}, {@code Fecha Apertura}, {@code Fecha Cierre} y {@code Estado}) — R-TUR-002.
 *
 * <p>El formato de la fecha es el del legacy ({@code 99/99/99}, o sea {@code dd/MM/yyyy}) y la
 * <strong>sentinela de fecha vacía</strong> ({@code 1753-01-01T00:00:00}) se muestra como celda
 * vacía: en el legacy esa marca significa "sin dato", no una fecha real (ASM-TUR-03, R-TUR-008).
 *
 * <p>El estado se expone con su <strong>código</strong> ({@code A} / {@code C}), tal como lo
 * mostraba la grilla del legacy; el atributo {@code abierto} permite que la vista distinga el
 * turno abierto del cerrado sin releer la base.
 *
 * @param turno número de turno ({@code PPRIM_TU.TurNroId}) — columna {@code Id. Turno}
 * @param comision número de comisión ({@code TurCPCId}) — columna {@code Comisión}
 * @param fechaApertura fecha de apertura ({@code TurFchApe}) — columna {@code Fecha Apertura}
 * @param fechaCierre fecha de cierre ({@code TurFchCie}) — columna {@code Fecha Cierre}
 * @param estado estado del turno ({@code TurEstado}: {@code A} / {@code C}) — columna {@code Estado}
 * @param abierto {@code true} si el turno está abierto
 */
public record TurnoFilaView(Integer turno, Integer comision, String fechaApertura, String fechaCierre,
        String estado, boolean abierto, Integer sucursal, String tipo) {

    /** Formato de fecha del legacy (atributo {@code date} con picture {@code 99/99/99}). */
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Construye la fila de la grilla a partir del DTO de lectura.
     *
     * @param dto DTO del turno (puede ser {@code null})
     * @return la fila de la grilla, o {@code null} si el DTO es {@code null}
     */
    public static TurnoFilaView de(TurnoDto dto) {
        if (dto == null) {
            return null;
        }
        String estado = dto.estado() == null ? "" : dto.estado().trim();
        return new TurnoFilaView(dto.turno(), dto.comision(), fecha(dto.fechaApertura()),
                fecha(dto.fechaCierre()), estado, ServicioTurnos.ESTADO_ABIERTO.equals(estado),
                dto.sucursal(), dto.tipo());
    }

    /**
     * Formatea una fecha de la grilla: la sentinela de "vacío" se muestra como celda vacía.
     *
     * @param valor fecha de la base
     * @return la fecha formateada, o la cadena vacía si no hay fecha
     */
    private static String fecha(LocalDateTime valor) {
        if (valor == null || FechaVacia.FECHA_VACIA.equals(valor)) {
            return "";
        }
        return FORMATO_FECHA.format(valor);
    }
}
