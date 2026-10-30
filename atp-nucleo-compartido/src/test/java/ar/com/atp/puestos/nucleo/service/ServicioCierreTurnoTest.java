package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Módulo {@code 06-turnos} — pruebas del cierre de turno (spec R-TUR-012, R-TUR-014; T-TUR-E1).
 *
 * <p>Verifica el <strong>orden exacto</strong> de las comprobaciones del legacy y que la
 * generación del SN para receptoría pueda abortar la operación completa.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServicioCierreTurnoTest {

    private static final Integer SUCURSAL = 72;
    private static final String TIPO_PUE = "PUE";
    private static final String TIPO_REC = "REC";
    private static final Integer TURNO = 5;

    @Mock
    private TurnoRepositorio turnoRepositorio;
    @Mock
    private ServicioEstadoDeForm servicioEstadoDeForm;
    @Mock
    private ServicioGeneradorSn servicioGeneradorSn;

    private ServicioCierreTurno servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioCierreTurno(turnoRepositorio, servicioEstadoDeForm,
                servicioGeneradorSn);
    }

    @Test
    @DisplayName("R-TUR-012 (1): un turno ya cerrado avisa 'El turno se encuentra cerrado'")
    void unTurnoYaCerradoAvisaYNoSeEscribe() {
        TurnoId id = new TurnoId(SUCURSAL, TIPO_PUE, TURNO);
        when(turnoRepositorio.findById(id)).thenReturn(Optional.of(turno(TIPO_PUE, "C")));

        ResultadoTurno resultado = servicio.cerrar(id);

        assertThat(resultado.exito()).isFalse();
        assertThat(resultado.codigo()).isEqualTo(ResultadoTurno.TURNO_YA_CERRADO);
        assertThat(resultado.mensaje()).isEqualTo("El turno se encuentra cerrado");
        verify(turnoRepositorio, never()).save(any());
        verify(servicioEstadoDeForm, never()).hayFormularioAbierto(anyInt(), anyString(), anyInt());
    }

    @Test
    @DisplayName("R-TUR-012 (2): con formularios abiertos avisa 'Existen Formularios Abiertos'")
    void conFormulariosAbiertosAvisaYNoSeEscribe() {
        TurnoId id = new TurnoId(SUCURSAL, TIPO_PUE, TURNO);
        when(turnoRepositorio.findById(id)).thenReturn(Optional.of(turno(TIPO_PUE, "A")));
        when(servicioEstadoDeForm.hayFormularioAbierto(SUCURSAL, TIPO_PUE, TURNO)).thenReturn(true);

        ResultadoTurno resultado = servicio.cerrar(id);

        assertThat(resultado.exito()).isFalse();
        assertThat(resultado.codigo()).isEqualTo(ResultadoTurno.FORMULARIOS_ABIERTOS);
        assertThat(resultado.mensaje()).isEqualTo("Existen Formularios Abiertos");
        verify(turnoRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("R-TUR-012 (4): el cierre escribe fecha, hora y estado 'C'")
    void elCierreEscribeFechaHoraYEstado() {
        TurnoId id = new TurnoId(SUCURSAL, TIPO_PUE, TURNO);
        Turno turno = turno(TIPO_PUE, "A");
        when(turnoRepositorio.findById(id)).thenReturn(Optional.of(turno));
        when(servicioEstadoDeForm.hayFormularioAbierto(SUCURSAL, TIPO_PUE, TURNO)).thenReturn(false);

        ResultadoTurno resultado = servicio.cerrar(id);

        assertThat(resultado.exito()).isTrue();
        assertThat(resultado.codigo()).isEqualTo(ResultadoTurno.TURNO_CERRADO);
        assertThat(resultado.mensaje()).isEqualTo("El turno seleccionado ha sido cerrado");

        ArgumentCaptor<Turno> captor = ArgumentCaptor.forClass(Turno.class);
        verify(turnoRepositorio).save(captor.capture());
        Turno guardado = captor.getValue();
        assertThat(guardado.getTurEstado()).isEqualTo("C");
        assertThat(guardado.getTurFchCie()).isEqualTo(LocalDate.now().atStartOfDay());
        assertThat(guardado.getTurHraCie()).isNotNull();
    }

    @Test
    @DisplayName("R-TUR-014: en REC el cierre genera el SN del volante de pago")
    void enRecElCierreGeneraElSn() {
        TurnoId id = new TurnoId(SUCURSAL, TIPO_REC, TURNO);
        when(turnoRepositorio.findById(id)).thenReturn(Optional.of(turno(TIPO_REC, "A")));
        when(servicioEstadoDeForm.hayFormularioAbierto(SUCURSAL, TIPO_REC, TURNO)).thenReturn(false);

        ResultadoTurno resultado = servicio.cerrar(id);

        assertThat(resultado.exito()).isTrue();
        verify(servicioGeneradorSn).generar(SUCURSAL, TIPO_REC, TURNO);
    }

    @Test
    @DisplayName("R-TUR-014: en PUE el cierre NO genera SN (solo la receptoria)")
    void enPueElCierreNoGeneraElSn() {
        TurnoId id = new TurnoId(SUCURSAL, TIPO_PUE, TURNO);
        when(turnoRepositorio.findById(id)).thenReturn(Optional.of(turno(TIPO_PUE, "A")));
        when(servicioEstadoDeForm.hayFormularioAbierto(SUCURSAL, TIPO_PUE, TURNO)).thenReturn(false);

        servicio.cerrar(id);

        verify(servicioGeneradorSn, never()).generar(anyInt(), anyString(), anyInt());
    }

    @Test
    @DisplayName("R-TUR-014: si el SN falla, la excepcion revierte el cierre (el turno queda abierto)")
    void siElSnFallaLaExcepcionRevierteElCierre() {
        TurnoId id = new TurnoId(SUCURSAL, TIPO_REC, TURNO);
        when(turnoRepositorio.findById(id)).thenReturn(Optional.of(turno(TIPO_REC, "A")));
        when(servicioEstadoDeForm.hayFormularioAbierto(SUCURSAL, TIPO_REC, TURNO)).thenReturn(false);
        when(servicioGeneradorSn.generar(SUCURSAL, TIPO_REC, TURNO))
                .thenThrow(new IllegalStateException("fallo la generacion del SN"));

        assertThatThrownBy(() -> servicio.cerrar(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("fallo la generacion del SN");
    }

    @Test
    @DisplayName("R-TUR-012 (3): un turno inexistente avisa 'No se puede cerrar el turno'")
    void unTurnoInexistenteAvisaNoSePuedeCerrar() {
        TurnoId id = new TurnoId(SUCURSAL, TIPO_PUE, TURNO);
        when(turnoRepositorio.findById(id)).thenReturn(Optional.empty());

        ResultadoTurno resultado = servicio.cerrar(id);

        assertThat(resultado.exito()).isFalse();
        assertThat(resultado.codigo()).isEqualTo(ResultadoTurno.ERROR_CIERRE);
        assertThat(resultado.mensaje()).isEqualTo("No se puede cerrar el turno");
    }

    private Turno turno(String tipo, String estado) {
        Turno turno = new Turno(new TurnoId(SUCURSAL, tipo, TURNO));
        turno.setTurEstado(estado);
        turno.setTurUsuario("WALTERG");
        return turno;
    }
}
