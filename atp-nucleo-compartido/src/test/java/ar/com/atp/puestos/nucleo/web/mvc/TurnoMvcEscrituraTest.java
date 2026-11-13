package ar.com.atp.puestos.nucleo.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.service.MensajesTurno;
import ar.com.atp.puestos.nucleo.service.ResultadoTurno;
import ar.com.atp.puestos.nucleo.service.FiltrosTurno;
import ar.com.atp.puestos.nucleo.service.ServicioTurnos;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Módulo {@code 06-turnos} — Escrituras de la pantalla de Turnos: alta, cierre, actualización del
 * contexto de sesión, mensajes verbatim del backend y CSRF (T-TUR-E2, T-TUR-C4).
 *
 * <p>El servicio se sustituye por un mock: la pantalla no habla por HTTP con la propia API.
 */
@WebMvcTest(controllers = TurnoMvcController.class)
@Import(GestorContextoSesion.class)
@WithMockUser(username = "tester", roles = "USER")
class TurnoMvcEscrituraTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicioTurnos servicioTurnos;

    /** La barra de navegación (contexto + menú) la provee el componente de presentación. */
    @MockitoBean
    private ProveedorNavegacion proveedorNavegacion;

    @BeforeEach
    void listadoVacioPorDefecto() {
        when(servicioTurnos.listar(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
    }

    // ------------------------------------------------------------------ alta

    @Test
    @DisplayName("R-TUR-009: el alta redirige al listado y actualiza Context.Turno")
    void elAltaRedirigeYActualizaElContexto() throws Exception {
        ContextoSesion contexto = contexto(0);
        MockHttpSession sesion = sesion(contexto);
        when(servicioTurnos.nuevoTurno(contexto)).thenReturn(ResultadoTurno.creado(8));

        mockMvc.perform(post("/turnos/nuevo").with(csrf()).session(sesion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/turnos"))
                .andExpect(flash().attribute("mensajeExito", "El turno 8 ha sido abierto"));

        ContextoSesion guardado = contextoGuardado(sesion);
        assertThat(guardado.turno()).isEqualTo(8);
        // El resto del contexto queda intacto.
        assertThat(guardado.sucursal()).isEqualTo(72);
        assertThat(guardado.comision()).isEqualTo(345);
        assertThat(guardado.usuario()).isEqualTo("WALTERG");
    }

    @Test
    @DisplayName("R-TUR-010: con la comisión cerrada el alta se rechaza con el mensaje verbatim")
    void conLaComisionCerradaElAltaSeRechaza() throws Exception {
        ContextoSesion contexto = contexto(0);
        MockHttpSession sesion = sesion(contexto);
        when(servicioTurnos.nuevoTurno(contexto)).thenReturn(ResultadoTurno.error(
                ResultadoTurno.COMISION_CERRADA, MensajesTurno.COMISION_CERRADA));

        mockMvc.perform(post("/turnos/nuevo").with(csrf()).session(sesion))
                .andExpect(status().isOk())
                .andExpect(view().name("nucleo/turnos"))
                .andExpect(model().attribute("mensajeError", MensajesTurno.COMISION_CERRADA));

        // El contexto no se toca cuando el alta se rechaza.
        assertThat(contextoGuardado(sesion).turno()).isZero();
    }

    // ------------------------------------------------------------------ cierre

    @Test
    @DisplayName("R-TUR-012: el cierre redirige al listado con el mensaje verbatim y pone Turno = 0")
    void elCierreRedirigeConElMensajeVerbatimYPoneTurnoEnCero() throws Exception {
        ContextoSesion contexto = contexto(5);
        MockHttpSession sesion = sesion(contexto);
        when(servicioTurnos.cerrarTurno(contexto, 5)).thenReturn(ResultadoTurno.cerrado());

        mockMvc.perform(post("/turnos/5/cerrar").with(csrf()).session(sesion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/turnos"))
                .andExpect(flash().attribute("mensajeExito", MensajesTurno.TURNO_CERRADO));

        assertThat(contextoGuardado(sesion).turno()).isZero();
    }

    @Test
    @DisplayName("R-TUR-012: con formularios abiertos el cierre se aborta con su mensaje verbatim")
    void conFormulariosAbiertosElCierreSeAborta() throws Exception {
        ContextoSesion contexto = contexto(5);
        MockHttpSession sesion = sesion(contexto);
        when(servicioTurnos.cerrarTurno(contexto, 5)).thenReturn(ResultadoTurno.error(
                ResultadoTurno.FORMULARIOS_ABIERTOS, MensajesTurno.FORMULARIOS_ABIERTOS));

        mockMvc.perform(post("/turnos/5/cerrar").with(csrf()).session(sesion))
                .andExpect(status().isOk())
                .andExpect(view().name("nucleo/turnos"))
                .andExpect(model().attribute("mensajeError", MensajesTurno.FORMULARIOS_ABIERTOS));

        // El turno sigue abierto: el contexto no se toca.
        assertThat(contextoGuardado(sesion).turno()).isEqualTo(5);
    }

    @Test
    @DisplayName("R-TUR-012: un turno ya cerrado se informa con su mensaje verbatim")
    void unTurnoYaCerradoSeInformaConSuMensaje() throws Exception {
        ContextoSesion contexto = contexto(5);
        when(servicioTurnos.cerrarTurno(contexto, 5)).thenReturn(ResultadoTurno.error(
                ResultadoTurno.TURNO_YA_CERRADO, MensajesTurno.TURNO_YA_CERRADO));

        mockMvc.perform(post("/turnos/5/cerrar").with(csrf()).session(sesion(contexto)))
                .andExpect(status().isOk())
                .andExpect(view().name("nucleo/turnos"))
                .andExpect(model().attribute("mensajeError", MensajesTurno.TURNO_YA_CERRADO));
    }

    @Test
    @DisplayName("R-TUR-014: si el SN no se puede generar el cierre se avisa con su mensaje verbatim")
    void siElSnNoSeGeneraElCierreSeAvisa() throws Exception {
        ContextoSesion contexto = contexto(5);
        when(servicioTurnos.cerrarTurno(contexto, 5)).thenReturn(ResultadoTurno.error(
                ResultadoTurno.ERROR_CIERRE, MensajesTurno.ERROR_CIERRE));

        mockMvc.perform(post("/turnos/5/cerrar").with(csrf()).session(sesion(contexto)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("mensajeError", MensajesTurno.ERROR_CIERRE));
    }

    // ------------------------------------------------------------------ CSRF

    @Test
    @DisplayName("T-TUR-C4: el alta sin token CSRF devuelve 403")
    void elAltaSinTokenCsrfDevuelve403() throws Exception {
        mockMvc.perform(post("/turnos/nuevo"))
                .andExpect(status().isForbidden());

        verify(servicioTurnos, never()).nuevoTurno(any());
    }

    @Test
    @DisplayName("T-TUR-C4: el cierre sin token CSRF devuelve 403")
    void elCierreSinTokenCsrfDevuelve403() throws Exception {
        mockMvc.perform(post("/turnos/5/cerrar"))
                .andExpect(status().isForbidden());

        verify(servicioTurnos, never()).cerrarTurno(any(), any());
    }

    // ------------------------------------------------------------------ helpers

    private static ContextoSesion contexto(Integer turno) {
        return ContextoSesion.completo("WALTERG", "Walter G", 10, 345, turno, 72, "Sucursal",
                "PUE", 1, "Responsable");
    }

    private static MockHttpSession sesion(ContextoSesion contexto) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(GestorContextoSesion.CLAVE, contexto);
        return sesion;
    }

    private static ContextoSesion contextoGuardado(MockHttpSession sesion) {
        return (ContextoSesion) sesion.getAttribute(GestorContextoSesion.CLAVE);
    }
}
