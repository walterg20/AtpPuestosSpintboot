package ar.com.atp.puestos.nucleo.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.service.ServicioTurnos;
import ar.com.atp.puestos.nucleo.web.dto.TurnoDto;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Módulo {@code 06-turnos} — Lectura de la pantalla de Turnos: render de la grilla, rótulos
 * verbatim, paginación, estados vacío/error y navegación (T-TUR-E2).
 *
 * <p>El servicio se sustituye por un mock: la pantalla no habla por HTTP con la propia API
 * (igual que la pantalla de Productos).
 */
@WebMvcTest(controllers = TurnoMvcController.class)
@Import(GestorContextoSesion.class)
@WithMockUser(username = "tester", roles = "USER")
class TurnoMvcControllerTest {

    /** Cabeceras de la grilla: se extraen del HTML renderizado. */
    private static final Pattern CABECERAS = Pattern.compile("<th scope=\"col\">([^<]*)</th>");

    /** Rótulos verbatim del legacy, en el orden de la grilla (R-TUR-002). */
    private static final List<String> ROTULOS_VERBATIM = List.of(
            "Id. Turno",
            "Comisión",
            "Fecha Apertura",
            "Fecha Cierre",
            "Estado");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicioTurnos servicioTurnos;

    /** La barra de navegación (contexto + menú) la provee el componente de presentación. */
    @MockitoBean
    private ProveedorNavegacion proveedorNavegacion;

    @BeforeEach
    void listadoVacioPorDefecto() {
        lenient().when(servicioTurnos.listar(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
    }

    // ------------------------------------------------------------------ lectura

    @Test
    @DisplayName("GET /turnos carga la barra de navegación (contexto + menú de programas)")
    void laPantallaCargaLaBarraDeNavegacion() throws Exception {
        mockMvc.perform(get("/turnos")).andExpect(status().isOk());

        verify(proveedorNavegacion).poblar(any(), any());
    }

    @Test
    @DisplayName("GET /turnos devuelve la vista nucleo/turnos con el view model completo")
    void listarDevuelveLaVistaYElViewModel() throws Exception {
        mockMvc.perform(get("/turnos"))
                .andExpect(status().isOk())
                .andExpect(view().name("nucleo/turnos"))
                .andExpect(model().attributeExists("turnos", "mensajeConfirmacionCierre",
                        "paginaActual", "cantidadPaginas", "hayPaginaAnterior", "hayPaginaSiguiente"))
                .andExpect(result -> {
                    Map<String, Object> modelo = result.getModelAndView().getModel();
                    assertThat(modelo.get("mensajeError")).isNull();
                    assertThat(modelo.get("turnoObjetivo")).isNull();
                    assertThat(modelo.get("turnoSeleccionado")).isNull();
                });
    }

    @Test
    @DisplayName("R-TUR-002: los 5 rótulos de la grilla se renderizan verbatim y en orden")
    void losCincoRotulosSeRenderizanVerbatimYEnOrden() throws Exception {
        whenListar(turno(5, "A"), turno(6, "C"));

        String html = mockMvc.perform(get("/turnos"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Matcher matcher = CABECERAS.matcher(html);
        List<String> encontrados = new ArrayList<>();
        while (matcher.find()) {
            encontrados.add(matcher.group(1).trim());
        }
        assertThat(encontrados).containsExactlyElementsOf(ROTULOS_VERBATIM);
    }

    @Test
    @DisplayName("R-TUR-002: las columnas ocultas del legacy no se renderizan")
    void lasColumnasOcultasDelLegacyNoSeRenderizan() throws Exception {
        whenListar(turno(5, "A"));

        String html = mockMvc.perform(get("/turnos"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("Id. Sucursal");
        assertThat(html).doesNotContain("Tipo de Suc.");
        assertThat(html).doesNotContain("Número identificatorio");
    }

    @Test
    @DisplayName("R-TUR-002: la fila muestra los datos del turno con el formato del legacy")
    void laFilaMuestraLosDatosDelTurno() throws Exception {
        whenListar(turno(5, "A"));

        String html = mockMvc.perform(get("/turnos"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("href=\"/turnos?turNroId=5\"");
        assertThat(html).contains("345");
        assertThat(html).contains("01/10/2026");
        // La fecha de cierre es la sentinela de "vacío": la celda queda vacía.
        assertThat(html).doesNotContain("01/01/1753");
    }

    @Test
    @DisplayName("R-TUR-004: la pantalla ofrece las acciones Nuevo Turno y Cerrar Turno")
    void laPantallaOfreceLasDosAcciones() throws Exception {
        whenListar(turno(5, "A"));

        String html = mockMvc.perform(get("/turnos"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(">Nuevo Turno</button>");
        assertThat(html).contains(">Cerrar Turno</button>");
        assertThat(html).contains("action=\"/turnos/nuevo\"");
        // T-TUR-C4: los formularios de escritura llevan el token CSRF.
        assertThat(html).contains("name=\"_csrf\"");
    }

    @Test
    @DisplayName("R-TUR-005: la confirmación del cierre es la verbatim del legacy")
    void laConfirmacionDelCierreEsLaVerbatim() throws Exception {
        whenListar(turno(5, "A"));

        String html = mockMvc.perform(get("/turnos"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("data-confirmar=\"¿Confirma que desea cerrar el Turno?\"");
    }

    @Test
    @DisplayName("El botón Cerrar Turno actúa sobre el turno elegido en la grilla")
    void elBotonCerrarTurnoActuaSobreElTurnoElegido() throws Exception {
        whenListar(turno(5, "A"), turno(6, "C"));

        String html = mockMvc.perform(get("/turnos").param("turNroId", "6"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("turnoSeleccionado", 6))
                .andExpect(model().attribute("turnoObjetivo", 6))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("action=\"/turnos/6/cerrar\"");
    }

    @Test
    @DisplayName("Sin turno elegido, el botón Cerrar Turno actúa sobre el turno abierto de la sesión")
    void sinTurnoElegidoElBotonActuaSobreElTurnoDeLaSesion() throws Exception {
        whenListar(turno(5, "A"));

        var resultado = mockMvc.perform(get("/turnos").session(sesionConTurno(5)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("turnoObjetivo", 5))
                .andReturn();

        assertThat(resultado.getResponse().getContentAsString())
                .contains("action=\"/turnos/5/cerrar\"");
    }

    @Test
    @DisplayName("R-TUR-003: la grilla pagina de a 10 filas")
    void laGrillaPaginaDeADiezFilas() throws Exception {
        List<TurnoDto> filas = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            filas.add(turno(i, "A"));
        }
        when(servicioTurnos.listar(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(filas, PageRequest.of(0, 10), 25));

        mockMvc.perform(get("/turnos"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cantidadPaginas", 3))
                .andExpect(model().attribute("hayPaginaSiguiente", true))
                .andExpect(model().attribute("hayPaginaAnterior", false));

        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(servicioTurnos).listar(any(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getPageNumber()).isZero();
    }

    @Test
    @DisplayName("La paginación ofrece la página siguiente y la última")
    void laPaginacionOfreceLaPaginaSiguienteYLaUltima() throws Exception {
        when(servicioTurnos.listar(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(turno(1, "A")), PageRequest.of(0, 10), 25));

        String html = mockMvc.perform(get("/turnos"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("href=\"/turnos?pagina=1\"");
        assertThat(html).contains("href=\"/turnos?pagina=2\"");
    }

    @Test
    @DisplayName("R-TUR-012: sin turnos se muestra el estado vacío y no la grilla")
    void sinTurnosSeMuestraElEstadoVacio() throws Exception {
        String html = mockMvc.perform(get("/turnos"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("No hay turnos para mostrar");
        assertThat(html).doesNotContain("<table");
    }

    @Test
    @DisplayName("Si el listado no se puede cargar se muestra el estado de error")
    void siElListadoNoSeCargaSeMuestraElEstadoDeError() throws Exception {
        when(servicioTurnos.listar(any(), any(Pageable.class)))
                .thenThrow(new IllegalStateException("no se pudo leer la base"));

        String html = mockMvc.perform(get("/turnos"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("mensajeError", "No se pudieron mostrar los turnos."))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("atp-alert--error");
        assertThat(html).contains("No se pudieron mostrar los turnos.");
    }

    // ------------------------------------------------------------------ helpers

    private void whenListar(TurnoDto... filas) {
        when(servicioTurnos.listar(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(filas), PageRequest.of(0, 10), filas.length));
    }

    private static TurnoDto turno(int numero, String estado) {
        return new TurnoDto(72, "PUE", numero, "WALTERG", 345,
                LocalDateTime.of(2026, 10, 1, 8, 30),
                LocalDateTime.of(1753, 1, 1, 0, 0),
                LocalDateTime.of(2026, 10, 1, 8, 30),
                LocalDateTime.of(1753, 1, 1, 0, 0),
                estado, null);
    }

    private static MockHttpSession sesionConTurno(Integer turno) {
        ContextoSesion contexto = ContextoSesion.completo("WALTERG", "Walter G", 10, 345, turno, 72,
                "Sucursal", "PUE", 1, "Responsable");
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(GestorContextoSesion.CLAVE, contexto);
        return sesion;
    }
}
