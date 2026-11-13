package ar.gob.chaco.atp.puestoscamineros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdaptadorValidacionCierreTurnoActasTest {

    @Mock
    private ActaService actaService;

    @Test
    void delegaEnActaService() {
        when(actaService.hayActasPendientes(1, "PUE", 2)).thenReturn(true);
        AdaptadorValidacionCierreTurnoActas adaptador =
                new AdaptadorValidacionCierreTurnoActas(actaService);
        assertThat(adaptador.hayActasPendientes(1, "PUE", 2)).isTrue();
    }
}