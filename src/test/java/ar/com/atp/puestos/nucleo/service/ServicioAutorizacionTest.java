package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * T-110 / R-LOG-025: la lista de programas autorizados ({@code ListPrograms} + {@code IsAuthorized}).
 */
@ExtendWith(MockitoExtension.class)
class ServicioAutorizacionTest {

    @Mock
    private VerificadorAutorizacion verificador;

    @Test
    @DisplayName("R-LOG-025: la lista incluye los 2 programas del legacy con su descripcion")
    void laListaIncluyeLosDosProgramasDelLegacy() {
        when(verificador.isAuthorized("WWPPrim_Turnos")).thenReturn(true);
        when(verificador.isAuthorized("WWTDetProdPri")).thenReturn(true);

        List<ProgramaAutorizado> programas = new ServicioAutorizacion(verificador).programasAutorizados();

        assertThat(programas).hasSize(2);
        assertThat(programas.get(0).nombre()).isEqualTo("WWPPrim_Turnos");
        assertThat(programas.get(0).descripcion()).isEqualTo("Turnos de Comisiones");
        assertThat(programas.get(1).nombre()).isEqualTo("WWTDetProdPri");
        assertThat(programas.get(1).descripcion()).isEqualTo("Buscar Guias");
    }

    @Test
    @DisplayName("R-LOG-025: un programa no autorizado queda fuera de la lista")
    void unProgramaNoAutorizadoQuedaFuera() {
        when(verificador.isAuthorized("WWPPrim_Turnos")).thenReturn(false);
        when(verificador.isAuthorized("WWTDetProdPri")).thenReturn(true);

        List<ProgramaAutorizado> programas = new ServicioAutorizacion(verificador).programasAutorizados();

        assertThat(programas).hasSize(1);
        assertThat(programas.get(0).nombre()).isEqualTo("WWTDetProdPri");
    }

    @Test
    @DisplayName("D-LOG-13: el verificador vigente devuelve siempre true (paridad con IsAuthorized)")
    void elVerificadorVigenteDevuelveSiempreTrue() {
        List<ProgramaAutorizado> programas =
                new ServicioAutorizacion(new VerificadorAutorizacionLegacy()).programasAutorizados();

        assertThat(programas).hasSize(2);
    }
}
