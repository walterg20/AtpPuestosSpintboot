package ar.gob.chaco.atp.puestoscamineros.web.mvc;

/**
 * View model de los KPIs del listado de Actas (vista {@code puestos/actas-listado}).
 *
 * <p>Es un adaptador de presentación: los valores se derivan del resultado del servicio
 * (no implementa reglas de negocio). Los cuatro indicadores son los acordados para
 * SCREEN_21 de Stitch: Actas en Turno, Carga Manual, Confirmadas, En Revisión.
 */
public record ResumenMetricasView(long actasEnTurno, long cargaManual, long confirmadas, long enRevision) {
}