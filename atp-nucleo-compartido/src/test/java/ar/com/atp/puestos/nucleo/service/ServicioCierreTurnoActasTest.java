package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.com.atp.puestos.nucleo.domain.Turno;
import ar.com.atp.puestos.nucleo.domain.TurnoId;
import ar.com.atp.puestos.nucleo.repository.TurnoRepositorio;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class ServicioCierreTurnoActasTest {

    private static final Integer SUCURSAL = 72;
    private static final String TIPO = "PUE";
    private static final Integer TURNO = 5;

    @Mock
    private TurnoRepositorio turnoRepositorio;
    @Mock
    private ServicioEstadoDeForm servicioEstadoDeForm;
    @Mock
    private ServicioGeneradorSn servicioGeneradorSn;

    private Turno turnoAbierto;

    @BeforeEach
    void setUp() {
        turnoAbierto = new Turno(new TurnoId(SUCURSAL, TIPO, TURNO));
        turnoAbierto.setTurEstado("A");
        when(turnoRepositorio.findById(new TurnoId(SUCURSAL, TIPO, TURNO)))
                .thenReturn(Optional.of(turnoAbierto));
        when(servicioEstadoDeForm.hayFormularioAbierto(SUCURSAL, TIPO, TURNO)).thenReturn(false);
    }

    @Test
    @DisplayName("B2: con actas pendientes el turno no se cierra")
    void conActasPendientesNoCierra() {
        ValidacionCierreTurno validacion = mock(ValidacionCierreTurno.class);
        when(validacion.hayActasPendientes(SUCURSAL, TIPO, TURNO)).thenReturn(true);
        ObjectProvider<ValidacionCierreTurno> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(validacion);

        ServicioCierreTurno servicio = new ServicioCierreTurno(turnoRepositorio,
                servicioEstadoDeForm, servicioGeneradorSn, provider);
        ResultadoTurno resultado = servicio.cerrar(new TurnoId(SUCURSAL, TIPO, TURNO));

        assertThat(resultado.exito()).isFalse();
    }

    @Test
    @DisplayName("B2: sin actas pendientes el turno se cierra")
    void sinActasPendientesCierra() {
        ValidacionCierreTurno validacion = mock(ValidacionCierreTurno.class);
        when(validacion.hayActasPendientes(SUCURSAL, TIPO, TURNO)).thenReturn(false);
        ObjectProvider<ValidacionCierreTurno> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(validacion);

        ServicioCierreTurno servicio = new ServicioCierreTurno(turnoRepositorio,
                servicioEstadoDeForm, servicioGeneradorSn, provider);
        ResultadoTurno resultado = servicio.cerrar(new TurnoId(SUCURSAL, TIPO, TURNO));

        assertThat(resultado.exito()).isTrue();
    }

    @Test
    @DisplayName("B2: sin implementacion se cierra como antes")
    void sinImplementacionCierra() {
        ServicioCierreTurno servicio = new ServicioCierreTurno(turnoRepositorio,
                servicioEstadoDeForm, servicioGeneradorSn);
        ResultadoTurno resultado = servicio.cerrar(new TurnoId(SUCURSAL, TIPO, TURNO));

        assertThat(resultado.exito()).isTrue();
    }
}