package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.com.atp.puestos.nucleo.domain.Turno;
import ar.com.atp.puestos.nucleo.domain.TurnoId;
import ar.com.atp.puestos.nucleo.repository.ComisionRepository;
import ar.com.atp.puestos.nucleo.repository.TurnoRepositorio;
import ar.com.atp.puestos.nucleo.web.dto.TurnoDto;
import ar.com.atp.puestos.shared.time.FechaVacia;
import java.time.LocalDate;
import java.util.List;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Módulo {@code 06-turnos} — pruebas del servicio de turnos (spec §3.1, §3.2 y §3.3; T-TUR-E1).
 *
 * <p>Se aísla el acceso a la base: los repositorios y los servicios especializados se mockean.
 * Lo que se verifica es la <strong>paridad de reglas</strong> con el legacy y la exactitud de los
 * mensajes verbatim.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServicioTurnosTest {

    private static final Integer SUCURSAL = 72;
    private static final Integer COMISION = 345;
    private static final String USUARIO = "WALTERG";
    private static final String TIPO_PUE = "PUE";
    private static final String TIPO_REC = "REC";

    @Mock
    private TurnoRepositorio turnoRepositorio;
    @Mock
    private ComisionRepository comisionRepository;
    @Mock
    private ServicioEstadoDeForm servicioEstadoDeForm;
    @Mock
    private ServicioCierreTurno servicioCierreTurno;

    private ServicioTurnos servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioTurnos(turnoRepositorio, comisionRepository, servicioEstadoDeForm,
                servicioCierreTurno);
    }

    // ------------------------------------------------------------------ alta

    @Test
    @DisplayName("R-TUR-006/007/008: el alta escribe el turno con max+1, estado A y las sentinelas")
    void elAltaEscribeElTurnoConLaNumeracionYLasSentinelas() {
        when(comisionRepository.estadoComision(SUCURSAL, COMISION)).thenReturn(Optional.of("A"));
        when(turnoRepositorio.maximoTurNroId(SUCURSAL, TIPO_PUE)).thenReturn(7);

        ResultadoTurno resultado = servicio.nuevoTurno(contexto(TIPO_PUE));

        assertThat(resultado.exito()).isTrue();
        assertThat(resultado.codigo()).isEqualTo(ResultadoTurno.OK);
        assertThat(resultado.numeroTurno()).isEqualTo(8);

        ArgumentCaptor<Turno> captor = ArgumentCaptor.forClass(Turno.class);
        verify(turnoRepositorio).save(captor.capture());
        Turno guardado = captor.getValue();

        assertThat(guardado.getSucNroId()).isEqualTo(SUCURSAL);
        assertThat(guardado.getSucTipo()).isEqualTo(TIPO_PUE);
        assertThat(guardado.getTurNroId()).isEqualTo(8);
        assertThat(guardado.getTurUsuario()).isEqualTo(USUARIO);
        assertThat(guardado.getTurEstado()).isEqualTo("A");
        assertThat(guardado.getTurCpcId()).isEqualTo(COMISION);
        assertThat(guardado.getTurFchApe()).isEqualTo(LocalDate.now().atStartOfDay());
        assertThat(guardado.getTurHraApe()).isNotNull();
        // R-TUR-008: sentinelas y TurFchPago en NULL.
        assertThat(guardado.getTurFchCie()).isEqualTo(FechaVacia.FECHA_VACIA);
        assertThat(guardado.getTurHraCie()).isEqualTo(FechaVacia.FECHA_VACIA);
        assertThat(guardado.getTurFchPago()).isNull();
        assertThat(guardado.getSnSinNum()).isNull();
    }

    @Test
    @DisplayName("R-TUR-006: sin turnos previos el numero es 1")
    void sinTurnosPreviosElNumeroEsUno() {
        when(comisionRepository.estadoComision(SUCURSAL, COMISION)).thenReturn(Optional.empty());
        when(turnoRepositorio.maximoTurNroId(SUCURSAL, TIPO_PUE)).thenReturn(null);

        ResultadoTurno resultado = servicio.nuevoTurno(contexto(TIPO_PUE));

        assertThat(resultado.exito()).isTrue();
        assertThat(resultado.numeroTurno()).isEqualTo(1);
    }

    @Test
    @DisplayName("R-TUR-010: con la comision cerrada el alta se rechaza con el mensaje verbatim")
    void conLaComisionCerradaElAltaSeRechaza() {
        when(comisionRepository.estadoComision(SUCURSAL, COMISION)).thenReturn(Optional.of("C"));

        ResultadoTurno resultado = servicio.nuevoTurno(contexto(TIPO_PUE));

        assertThat(resultado.exito()).isFalse();
        assertThat(resultado.codigo()).isEqualTo(ResultadoTurno.COMISION_CERRADA);
        assertThat(resultado.mensaje())
                .isEqualTo("La comisión actual esta cerrada , no se puede abrir nuevos turnos");
        assertThat(resultado.mensaje()).isEqualTo(MensajesTurno.COMISION_CERRADA);
        verify(turnoRepositorio, never()).save(any());
        verify(turnoRepositorio, never()).maximoTurNroId(anyInt(), anyString());
    }

    @Test
    @DisplayName("D-TUR-002: si la comision no existe el alta se permite")
    void sinComisionElAltaSePermite() {
        when(comisionRepository.estadoComision(SUCURSAL, COMISION)).thenReturn(Optional.empty());
        when(turnoRepositorio.maximoTurNroId(SUCURSAL, TIPO_PUE)).thenReturn(3);

        ResultadoTurno resultado = servicio.nuevoTurno(contexto(TIPO_PUE));

        assertThat(resultado.exito()).isTrue();
        assertThat(resultado.numeroTurno()).isEqualTo(4);
    }

    @Test
    @DisplayName("R-TUR-011: en REC el alta no consulta el estado de la comision")
    void enRecElAltaNoConsultaLaComision() {
        when(turnoRepositorio.maximoTurNroId(SUCURSAL, TIPO_REC)).thenReturn(null);

        ResultadoTurno resultado = servicio.nuevoTurno(contexto(TIPO_REC));

        assertThat(resultado.exito()).isTrue();
        assertThat(resultado.numeroTurno()).isEqualTo(1);
        verify(comisionRepository, never()).estadoComision(any(), any());
    }

    @Test
    @DisplayName("D-TUR-003: sin comision en el contexto se persiste 0 en TurCPCId")
    void sinComisionEnElContextoSePersisteCero() {
        ContextoSesion contexto = ContextoSesion.completo(USUARIO, "Walter G", 10, 0, 0, SUCURSAL,
                "Sucursal", TIPO_PUE, 1, "Responsable");
        when(comisionRepository.estadoComision(SUCURSAL, 0)).thenReturn(Optional.of("A"));
        when(turnoRepositorio.maximoTurNroId(SUCURSAL, TIPO_PUE)).thenReturn(1);

        servicio.nuevoTurno(contexto);

        ArgumentCaptor<Turno> captor = ArgumentCaptor.forClass(Turno.class);
        verify(turnoRepositorio).save(captor.capture());
        assertThat(captor.getValue().getTurCpcId()).isZero();
    }

    // ------------------------------------------------------------------ listado

    @Test
    @DisplayName("R-TUR-001/003: el listado usa la sucursal y el tipo del contexto y pagina de a 10")
    void elListadoUsaLaSucursalDelContextoYPaginaDeADiez() {
        Turno turno = turno(5, "A");
        when(turnoRepositorio.listarPorSucursal(eq(SUCURSAL), eq(TIPO_PUE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(turno), PageRequest.of(0, 10), 1));

        Page<TurnoDto> pagina = servicio.listar(contexto(TIPO_PUE));

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).turno()).isEqualTo(5);
        assertThat(pagina.getContent().get(0).estado()).isEqualTo("A");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(turnoRepositorio).listarPorSucursal(eq(SUCURSAL), eq(TIPO_PUE), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getPageNumber()).isZero();
    }

    // ------------------------------------------------------------------ cierre

    @Test
    @DisplayName("R-TUR-012: el cierre exitoso informa el mensaje verbatim del legacy")
    void elCierreExitosoInformaElMensajeVerbatim() {
        TurnoId id = new TurnoId(SUCURSAL, TIPO_PUE, 5);
        when(servicioCierreTurno.cerrar(id)).thenReturn(ResultadoTurno.cerrado());

        ResultadoTurno resultado = servicio.cerrarTurno(id);

        assertThat(resultado.exito()).isTrue();
        assertThat(resultado.codigo()).isEqualTo(ResultadoTurno.TURNO_CERRADO);
        assertThat(resultado.mensaje()).isEqualTo("El turno seleccionado ha sido cerrado");
        assertThat(resultado.mensaje()).isEqualTo(MensajesTurno.TURNO_CERRADO);
    }

    @Test
    @DisplayName("R-TUR-012: el turno ya cerrado no se cierra y avisa con su mensaje verbatim")
    void elTurnoYaCerradoNoSeCierra() {
        TurnoId id = new TurnoId(SUCURSAL, TIPO_PUE, 5);
        when(servicioCierreTurno.cerrar(id)).thenReturn(ResultadoTurno.error(
                ResultadoTurno.TURNO_YA_CERRADO, MensajesTurno.TURNO_YA_CERRADO));

        ResultadoTurno resultado = servicio.cerrarTurno(id);

        assertThat(resultado.exito()).isFalse();
        assertThat(resultado.codigo()).isEqualTo(ResultadoTurno.TURNO_YA_CERRADO);
        assertThat(resultado.mensaje()).isEqualTo("El turno se encuentra cerrado");
        assertThat(resultado.mensaje()).isEqualTo(MensajesTurno.TURNO_YA_CERRADO);
    }

    @Test
    @DisplayName("R-TUR-012: con formularios abiertos el cierre se aborta con su mensaje verbatim")
    void conFormulariosAbiertosElCierreSeAborta() {
        TurnoId id = new TurnoId(SUCURSAL, TIPO_PUE, 5);
        when(servicioCierreTurno.cerrar(id)).thenReturn(ResultadoTurno.error(
                ResultadoTurno.FORMULARIOS_ABIERTOS, MensajesTurno.FORMULARIOS_ABIERTOS));

        ResultadoTurno resultado = servicio.cerrarTurno(id);

        assertThat(resultado.exito()).isFalse();
        assertThat(resultado.codigo()).isEqualTo(ResultadoTurno.FORMULARIOS_ABIERTOS);
        assertThat(resultado.mensaje()).isEqualTo("Existen Formularios Abiertos");
        assertThat(resultado.mensaje()).isEqualTo(MensajesTurno.FORMULARIOS_ABIERTOS);
    }

    @Test
    @DisplayName("R-TUR-014: si falla el SN el cierre se revierte y avisa 'No se puede cerrar el turno'")
    void siFallaElSnElCierreSeRevierte() {
        TurnoId id = new TurnoId(SUCURSAL, TIPO_REC, 5);
        when(servicioCierreTurno.cerrar(id))
                .thenThrow(new IllegalStateException("fallo la generacion del SN"));

        ResultadoTurno resultado = servicio.cerrarTurno(id);

        assertThat(resultado.exito()).isFalse();
        assertThat(resultado.codigo()).isEqualTo(ResultadoTurno.ERROR_CIERRE);
        assertThat(resultado.mensaje()).isEqualTo("No se puede cerrar el turno");
        assertThat(resultado.mensaje()).isEqualTo(MensajesTurno.ERROR_CIERRE);
    }

    @Test
    @DisplayName("R-TUR-012: el cierre se identifica con la sucursal y el tipo del contexto")
    void elCierreSeIdentificaConLaSucursalYElTipoDelContexto() {
        TurnoId id = new TurnoId(SUCURSAL, TIPO_PUE, 5);
        when(servicioCierreTurno.cerrar(id)).thenReturn(ResultadoTurno.cerrado());

        ResultadoTurno resultado = servicio.cerrarTurno(contexto(TIPO_PUE), 5);

        assertThat(resultado.exito()).isTrue();
        verify(servicioCierreTurno).cerrar(id);
    }

    // ------------------------------------------------------------------ auxiliares

    private ContextoSesion contexto(String tipo) {
        return ContextoSesion.completo(USUARIO, "Walter G", 10, COMISION, 0, SUCURSAL, "Sucursal",
                tipo, 1, "Responsable");
    }

    private Turno turno(int numero, String estado) {
        Turno turno = new Turno(new TurnoId(SUCURSAL, TIPO_PUE, numero));
        turno.setTurEstado(estado);
        turno.setTurUsuario(USUARIO);
        turno.setTurFchApe(LocalDate.now().atStartOfDay());
        turno.setTurHraApe(LocalDate.now().atStartOfDay());
        turno.setTurFchCie(FechaVacia.FECHA_VACIA);
        turno.setTurHraCie(FechaVacia.FECHA_VACIA);
        return turno;
    }
}
