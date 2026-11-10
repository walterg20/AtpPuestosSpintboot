package ar.com.atp.puestos.puestos.service.model;

import java.time.LocalDate;

/**
 * Filtros de búsqueda del listado de Actas (spec {@code 09-formularios-acta} §3.3, plan §3.3,
 * D-ACTA-22).
 *
 * <p>Es parte del <strong>contrato del servicio</strong>: lo consume la capa {@code web/mvc} y no
 * conoce los objetos de binding. Se aplica <strong>además</strong> del filtro base del turno
 * (R-ACTA-003).
 *
 * @param f276Id número de Acta (coincidencia exacta) o {@code null}
 * @param cuit CUIT de cualquiera de los tres roles o {@code null}
 * @param razonSocial coincidencia parcial, sin distinguir mayúsculas, o {@code null}
 * @param desde inicio del rango sobre {@code F276FchMov} o {@code null}
 * @param hasta fin del rango sobre {@code F276FchMov} o {@code null}
 */
public record ActaFiltro(Integer f276Id, Long cuit, String razonSocial, LocalDate desde,
        LocalDate hasta) {

    /** Filtro vacío: sólo el filtro base del turno. */
    public static ActaFiltro vacio() {
        return new ActaFiltro(null, null, null, null, null);
    }
}
