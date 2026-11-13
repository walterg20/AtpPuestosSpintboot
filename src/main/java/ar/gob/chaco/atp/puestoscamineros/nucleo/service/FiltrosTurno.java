package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import java.time.LocalDateTime;

/**
 * Filtros opcionales del listado de turnos (Fase 1, modulo 06-turnos).
 *
 * <p>Todos los campos son opcionales: un {@code null} (o, en el caso de {@code estado}, un
 * valor en blanco) significa "sin filtro".
 *
 * @param busqueda filtra por {@code TurNroId} o {@code TurCPCId}; {@code null} = sin filtro
 * @param desde    filtra {@code TurFchApe >= desde}; {@code null} = sin filtro
 * @param hasta    filtra {@code TurFchApe <= hasta}; {@code null} = sin filtro
 * @param estado   filtra {@code TurEstado = estado}; {@code null}/en blanco = sin filtro
 */
public record FiltrosTurno(Integer busqueda, LocalDateTime desde, LocalDateTime hasta, String estado) {

    /** Filtros vacios: ningun filtro activo. */
    public static FiltrosTurno vacio() {
        return new FiltrosTurno(null, null, null, null);
    }
}
