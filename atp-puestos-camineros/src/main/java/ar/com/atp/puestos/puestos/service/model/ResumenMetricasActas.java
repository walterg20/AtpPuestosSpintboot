package ar.com.atp.puestos.puestos.service.model;

/**
 * KPIs del listado de Actas (SCREEN_21).
 *
 * @param actasEnTurno cantidad de Actas del turno activo
 * @param cargaManual  cantidad de Actas cargadas manualmente
 * @param confirmadas  cantidad de Actas confirmadas
 * @param enRevision   cantidad de Actas pendientes / en revisión
 */
public record ResumenMetricasActas(int actasEnTurno, int cargaManual, int confirmadas, int enRevision) {
}