package ar.com.atp.puestos.nucleo.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.com.atp.puestos.nucleo.service.ProductoService;
import ar.com.atp.puestos.nucleo.web.dto.ProductoDto;
import ar.com.atp.puestos.nucleo.web.dto.ProductoEscrituraDto;
import ar.com.atp.puestos.nucleo.web.dto.ProductoVigenciaEscrituraDto;
import ar.com.atp.puestos.shared.error.ConflictException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T-301 (parcial) — Escrituras de la pantalla de Productos: alta, modificación, baja, mapeo
 * {@code form → DTO}, validaciones en línea y errores del backend.
 *
 * <p>El servicio se sustituye por un mock: la pantalla no habla por HTTP con la propia API
 * (R-PROD-01, D-29).
 */
@WebMvcTest(controllers = ProductoMvcController.class)
class ProductoMvcEscrituraTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService productoService;

    @BeforeEach
    void coleccionVaciaPorDefecto() {
        lenient().when(productoService.listarProductos(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Alta válida: redirige a GET /productos?prodCod= y escribe por el servicio")
    void altaValidaRedirigeYEscribePorElServicio() throws Exception {
        mockMvc.perform(post("/productos")
                        .param("prodCod", "42")
                        .param("prodDesc", "  Algodón  ")
                        .param("prodTipCal", "1")
                        .param("uMedNroId", " KG")
                        .param("prodAcop", "N"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/productos?prodCod=42"));

        ArgumentCaptor<ProductoEscrituraDto> captor =
                ArgumentCaptor.forClass(ProductoEscrituraDto.class);
        verify(productoService).crearProducto(captor.capture());
        ProductoEscrituraDto dto = captor.getValue();
        assertThat(dto.prodCod()).isEqualTo(42);
        assertThat(dto.prodDesc()).isEqualTo("Algodón");
        assertThat(dto.uMedNroId()).isEqualTo("KG");
        assertThat(dto.prodAcop()).isEqualTo("N");
        assertThat(dto.prodTipCal()).isEqualTo(1);
    }

    @Test
    @DisplayName("T-107: prodIMax1..4 y uMedNombre quedan fuera del cuerpo de escritura")
    void losDerivadosYLaUnidadDeMedidaQuedanFueraDelCuerpo() throws Exception {
        mockMvc.perform(post("/productos")
                        .param("prodCod", "42")
                        .param("prodDesc", "ALGODON")
                        .param("prodIMax1", "10")
                        .param("prodIMax2", "20")
                        .param("prodIMax3", "30")
                        .param("prodIMax4", "40")
                        .param("uMedNombre", "KILOGRAMO"))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<ProductoEscrituraDto> captor =
                ArgumentCaptor.forClass(ProductoEscrituraDto.class);
        verify(productoService).crearProducto(captor.capture());
        ProductoEscrituraDto dto = captor.getValue();
        assertThat(dto.uMedNombre()).isNull();
        assertThat(dto.vigencias()).isEmpty();
    }

    @Test
    @DisplayName("R-PROD-29: los enteros opcionales vacíos se envían ausentes")
    void losEnterosOpcionalesVaciosSeEnvianAusentes() throws Exception {
        mockMvc.perform(post("/productos")
                        .param("prodCod", "42")
                        .param("prodDesc", "ALGODON"))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<ProductoEscrituraDto> captor =
                ArgumentCaptor.forClass(ProductoEscrituraDto.class);
        verify(productoService).crearProducto(captor.capture());
        ProductoEscrituraDto dto = captor.getValue();
        assertThat(dto.prodTipCal()).isNull();
        assertThat(dto.tpPriID()).isNull();
        assertThat(dto.prodAnula()).isNull();
    }

    @Test
    @DisplayName("R-PROD-05 y R-PROD-23: la vigencia viaja anidada y con la fecha a medianoche")
    void laVigenciaViajaAnidadaYAMedianoche() throws Exception {
        mockMvc.perform(post("/productos")
                        .param("prodCod", "42")
                        .param("prodDesc", "ALGODON")
                        .param("vigencias[0].prodFchVig", "2026-01-01")
                        .param("vigencias[0].prodImpu1", "10.50")
                        .param("vigencias[0].prodImpu2", "0")
                        .param("vigencias[0].prodImpu3", "0")
                        .param("vigencias[0].prodImpu4", "0"))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<ProductoEscrituraDto> captor =
                ArgumentCaptor.forClass(ProductoEscrituraDto.class);
        verify(productoService).crearProducto(captor.capture());
        List<ProductoVigenciaEscrituraDto> vigencias = captor.getValue().vigencias();
        assertThat(vigencias).hasSize(1);
        assertThat(vigencias.get(0).prodFchVig()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        assertThat(vigencias.get(0).prodImpu1()).isEqualByComparingTo("10.50");
    }

    @Test
    @DisplayName("R-PROD-07: una prodCod fuera de rango no ejecuta la escritura")
    void unaProdCodFueraDeRangoNoEscribe() throws Exception {
        mockMvc.perform(post("/productos")
                        .param("prodCod", "99999")
                        .param("prodDesc", "ALGODON"))
                .andExpect(status().isOk())
                .andExpect(view().name("nucleo/productos"))
                .andExpect(model().attribute("tieneErrores", true));

        verify(productoService, never()).crearProducto(any());
    }

    @Test
    @DisplayName("R-PROD-10 y T-210: el error de binding se muestra junto al campo y Guardar queda off")
    void elErrorDeBindingSeMuestraJuntoAlCampo() throws Exception {
        String html = mockMvc.perform(post("/productos")
                        .param("prodCod", "99999")
                        .param("prodDesc", "ALGODON"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("atp-field__error");
        assertThat(html).contains("El código debe estar entre 0 y 32767.");
        assertThat(html).contains("disabled");
    }

    @Test
    @DisplayName("R-PROD-10: el error de una fila de la grilla se muestra junto a la celda")
    void elErrorDeUnaFilaSeMuestraJuntoALaCelda() throws Exception {
        String html = mockMvc.perform(post("/productos")
                        .param("prodCod", "42")
                        .param("prodDesc", "ALGODON")
                        .param("vigencias[0].prodImpu1", "10.00")
                        .param("vigencias[0].prodImpu2", "0")
                        .param("vigencias[0].prodImpu3", "0")
                        .param("vigencias[0].prodImpu4", "0"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tieneErrores", true))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("La fecha de vigencia es obligatoria.");
        assertThat(html).contains("vigencias[0].prodFchVig");
    }

    @Test
    @DisplayName("R-PROD-08: un importe con 3 decimales se marca y no se escribe")
    void unImporteConTresDecimalesNoSeEscribe() throws Exception {
        mockMvc.perform(post("/productos")
                        .param("prodCod", "42")
                        .param("prodDesc", "ALGODON")
                        .param("vigencias[0].prodFchVig", "2026-01-01")
                        .param("vigencias[0].prodImpu1", "10.005")
                        .param("vigencias[0].prodImpu2", "0")
                        .param("vigencias[0].prodImpu3", "0")
                        .param("vigencias[0].prodImpu4", "0"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tieneErrores", true));

        verify(productoService, never()).crearProducto(any());
    }

    @Test
    @DisplayName("R-PROD-26: el filtro de anulados se conserva al navegar y al volver de una escritura")
    void elFiltroSeConservaAlNavegarYAlEscribir() throws Exception {
        when(productoService.listarProductos(any())).thenReturn(List.of(
                producto(10, "DIEZ", 0),
                producto(20, "VEINTE", 0)));

        // La navegación conserva el filtro activo.
        String html = mockMvc.perform(get("/productos").param("prodCod", "10").param("anulados", "false"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("href=\"/productos?prodCod=20&amp;anulados=false\"");

        // La escritura vuelve con el mismo filtro.
        mockMvc.perform(post("/productos")
                        .param("prodCod", "42")
                        .param("prodDesc", "ALGODON")
                        .param("anulados", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/productos?prodCod=42&anulados=false"));
    }

    @Test
    @DisplayName("R-PROD-11: un conflicto del backend se muestra inline y conserva el formulario")
    void unConflictoSeMuestraInlineYConservaElFormulario() throws Exception {
        when(productoService.crearProducto(any()))
                .thenThrow(new ConflictException("PK_DUPLICADA", "Ya existe un producto con ProdCod 42."));

        String html = mockMvc.perform(post("/productos")
                        .param("prodCod", "42")
                        .param("prodDesc", "ALGODON"))
                .andExpect(status().isOk())
                .andExpect(view().name("nucleo/productos"))
                .andExpect(model().attribute("mensajeError", "Ya existe un producto con ProdCod 42."))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Ya existe un producto con ProdCod 42.");
        assertThat(html).contains("ALGODON");
    }

    @Test
    @DisplayName("Modificación: redirige a GET /productos?prodCod={prodCod}")
    void laModificacionRedirigeAlProducto() throws Exception {
        mockMvc.perform(post("/productos/42")
                        .param("prodCod", "42")
                        .param("prodDesc", "ALGODON"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/productos?prodCod=42"));

        verify(productoService).actualizarProducto(eq(42), any());
    }

    @Test
    @DisplayName("Baja: redirige a GET /productos")
    void laBajaRedirigeALaColeccion() throws Exception {
        mockMvc.perform(post("/productos/42/eliminar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/productos"));

        verify(productoService).eliminarProducto(42);
    }

    // ------------------------------------------------------------------ helpers

    private static ProductoDto producto(Integer cod, String descripcion, Integer anulada) {
        return new ProductoDto(cod, descripcion, 1, 2, "KG", null, "N", anulada, List.of());
    }
}
