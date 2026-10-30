package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.com.atp.puestos.nucleo.repository.Formulario2276Repositorio;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Módulo {@code 06-turnos} — pruebas de la detección de formularios abiertos (spec R-TUR-013;
 * T-TUR-E1).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServicioEstadoDeFormTest {

    private static final Integer SUCURSAL = 72;
    private static final String TIPO = "REC";
    private static final Integer TURNO = 5;

    @Mock
    private Formulario2276Repositorio formularioRepositorio;

    private ServicioEstadoDeForm servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioEstadoDeForm(formularioRepositorio);
    }

    @Test
    @DisplayName("R-TUR-013: sin formularios abiertos el turno se puede cerrar")
    void sinFormulariosAbiertosElTurnoSePuedeCerrar() {
        when(formularioRepositorio.contarFormulariosAbiertos(eq(SUCURSAL), eq(TIPO), eq(TURNO), any()))
                .thenReturn(0L);

        assertThat(servicio.hayFormularioAbierto(SUCURSAL, TIPO, TURNO)).isFalse();
    }

    @Test
    @DisplayName("R-TUR-013: con formularios abiertos el turno no se puede cerrar")
    void conFormulariosAbiertosElTurnoNoSePuedeCerrar() {
        when(formularioRepositorio.contarFormulariosAbiertos(eq(SUCURSAL), eq(TIPO), eq(TURNO), any()))
                .thenReturn(3L);

        assertThat(servicio.hayFormularioAbierto(SUCURSAL, TIPO, TURNO)).isTrue();
    }

    @Test
    @DisplayName("R-TUR-013: la comparison es contra el inicio del dia actual (today())")
    void laComparacionEsContraElInicioDelDiaActual() {
        when(formularioRepositorio.contarFormulariosAbiertos(anyInt(), anyString(), anyInt(), any()))
                .thenReturn(0L);

        servicio.hayFormularioAbierto(SUCURSAL, TIPO, TURNO);

        org.mockito.ArgumentCaptor<java.time.LocalDateTime> captor =
                org.mockito.ArgumentCaptor.forClass(java.time.LocalDateTime.class);
        verify(formularioRepositorio).contarFormulariosAbiertos(eq(SUCURSAL), eq(TIPO), eq(TURNO),
                captor.capture());
        assertThat(captor.getValue()).isEqualTo(LocalDate.now().atStartOfDay());
    }
}
