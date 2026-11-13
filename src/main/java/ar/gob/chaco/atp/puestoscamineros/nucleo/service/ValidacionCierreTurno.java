package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

/**
 * SPI (puerto) para validar el cierre de un turno/lote contra validaciones externas del dominio
 * de negocio (p. ej. Actas pendientes del módulo {@code 09-formularios-acta}).
 *
 * <p>Se consume mediante {@link org.springframework.beans.factory.ObjectProvider} en
 * {@link ServicioCierreTurno} para no romper los tests existentes que no aportan una
 * implementación (comportamiento actual: se permite el cierre).
 *
 * <p>Nombre elegido {@code ValidacionCierreTurno} (y no {@code ValidadorCierreTurno}) porque
 * describe la <em>validación</em> que se aplica al cierre, no al validador en sí; el adaptador
 * del módulo de Actas implementa este puerto delegando en {@code ActaService}.
 */
public interface ValidacionCierreTurno {

    /**
     * @return {@code true} si existen Actas pendientes que bloqueen el cierre del turno.
     */
    boolean hayActasPendientes(Integer sucNroId, String sucTipo, Integer turNroId);
}