package ar.gob.chaco.atp.puestoscamineros.service;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ValidacionCierreTurno;
import org.springframework.stereotype.Component;

/**
 * Adaptador del SPI {@link ValidacionCierreTurno} (B2, R-ACTA-043) que conecta el núcleo de
 * turnos con el módulo de Actas sin invertir dependencias: el núcleo sólo conoce la interfaz.
 */
@Component
public class AdaptadorValidacionCierreTurnoActas implements ValidacionCierreTurno {

    private final ActaService actaService;

    public AdaptadorValidacionCierreTurnoActas(ActaService actaService) {
        this.actaService = actaService;
    }

    @Override
    public boolean hayActasPendientes(Integer sucNroId, String sucTipo, Integer turNroId) {
        return actaService.hayActasPendientes(sucNroId, sucTipo, turNroId);
    }
}