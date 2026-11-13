package ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ProductoService;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ImportesVigentesDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoVigenciaDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.producto.FiltroAnulados;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.producto.ModoProducto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.producto.ProductoForm;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T-301 (parcial) — Lectura de la pantalla de Productos: render, view model, navegación,
 *collection y estados.
 *
 * <p>El servicio se sustituye por un mock: la pantalla no habla por HTTP con la propia API
 * (R-PROD-01, D-29). Las escrituras están en {@code ProductoMvcEscrituraTest}.
 *
 * <p><strong>03-login</strong>: desde que el núcleo trae Spring Security, las rutas MVC exigen
 * sesión ⇒ la prueba se ejecuta con un usuario autenticado ({@code @WithMockUser}), igual que la
 * prueba equivalente del módulo desplegable.
 */
@WebMvcTest(controllers = ProductoMvcController.class)
@WithMockUser(username = "tester", roles = "USER")
class ProductoMvcControllerTest {

    private static final Pattern ROTULOS = Pattern.compile("<label[^>]*>([^<]*)</label>");

    private static final List<String> ROTULOS_VERBATIM = List.of(
            "Código",
            "Producto",
            "Tipo de cálculo",
            "Tipo de Producción",
            "Importe de Entrada",
            "Importe C. Mec. Entrada",
            "Importe Salida",
            "Importre C. Mec. Salida",
            "Código",
            "Unidad de Medida",
            "Acopiador?",
            "Prod Anulado");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService productoService;

    /** La barra de navegación (contexto + menú) la provee el componente de presentación. */
    @MockitoBean
    private ProveedorNavegacion proveedorNavegacion;

    @BeforeEach
    void coleccionVaciaPorDefecto() {
        lenient().when(productoService.listarProductos(any())).thenReturn(List.of());
    }

    // ------------------------------------------------------------------ lectura

    @Test
    @DisplayName("GET /productos carga la barra de navegación (contexto + menú de programas)")
    void laPantallaCargaLaBarraDeNavegacion() throws Exception {
        mockMvc.perform(get("/productos")).andExpect(status().isOk());

        org.mockito.Mockito.verify(proveedorNavegacion).poblar(any(), any());
    }

    @Test
    @DisplayName("GET /productos devuelve la vista nucleo/productos con el view model completo")
    void listarDevuelveLaVistaYElViewModel() throws Exception {
        mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andExpect(view().name("nucleo/productos"))
                .andExpect(model().attributeExists("producto", "coleccion", "modo", "accionFormulario",
                        "filtroAnulados", "tieneErrores"))
                .andExpect(result -> {
                    Map<String, Object> modelo = result.getModelAndView().getModel();
                    assertThat(modelo.get("accionEliminar")).isNull();
                    assertThat(modelo.get("productoId")).isNull();
                    assertThat(modelo.get("mensajeError")).isNull();
                    assertThat(modelo.get("indiceAnterior")).isNull();
                    assertThat(modelo.get("indiceSiguiente")).isNull();
                    assertThat(modelo.get("primerCodigo")).isNull();
                    assertThat(modelo.get("ultimoCodigo")).isNull();
                })
                .andExpect(model().attribute("modo", ModoProducto.ALTA))
                .andExpect(model().attribute("accionFormulario", "/productos"))
                .andExpect(model().attribute("filtroAnulados", FiltroAnulados.TODOS))
                .andExpect(model().attribute("tieneErrores", false));
    }

    @Test
    @DisplayName("R-PROD-02: los 12 rótulos del nivel 0 se renderizan verbatim y en orden")
    void losDoceRotulosSeRenderizanVerbatimYEnOrden() throws Exception {
        String html = mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Matcher matcher = ROTULOS.matcher(html);
        List<String> encontrados = new ArrayList<>();
        while (matcher.find()) {
            encontrados.add(matcher.group(1).trim());
        }
        assertThat(encontrados).containsExactlyElementsOf(ROTULOS_VERBATIM);
    }

    @Test
    @DisplayName("R-PROD-04: la grilla del nivel 1 muestra las 5 cabeceras verbatim")
    void laGrillaMuestraLasCincoCabeceras() throws Exception {
        String html = mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("ProdFchVig");
        assertThat(html).contains("Importe Fijo Entrada");
        assertThat(html).contains("Imp.Cosech.Mec Entrada");
        assertThat(html).contains("Importe Fijo Salida");
        assertThat(html).contains("Imp.Cosech.Mec Salida");
        assertThat(html.indexOf("ProdFchVig")).isLessThan(html.indexOf("Importe Fijo Entrada"));
        assertThat(html.indexOf("Importe Fijo Entrada")).isLessThan(html.indexOf("Imp.Cosech.Mec Entrada"));
        assertThat(html.indexOf("Imp.Cosech.Mec Entrada")).isLessThan(html.indexOf("Importe Fijo Salida"));
        assertThat(html.indexOf("Importe Fijo Salida")).isLessThan(html.indexOf("Imp.Cosech.Mec Salida"));
    }

    @Test
    @DisplayName("T-205: la sección del nivel 1 se titula Level1 y la fila-plantilla está presente")
    void laSeccionDeVigenciasSeTitulaLevel1() throws Exception {
        String html = mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Level1");
        assertThat(html).contains("id=\"plantilla-vigencia\"");
        assertThat(html).contains("name=\"vigencias[__INDEX__].prodFchVig\"");
        assertThat(html).contains("+ Fila");
    }

    @Test
    @DisplayName("R-PROD-25: la colección se ordena por prodCod ascendente")
    void laColeccionSeOrdenaPorProdCodAscendente() throws Exception {
        when(productoService.listarProductos(any())).thenReturn(List.of(
                producto(30, "TREINTA", 0),
                producto(10, "DIEZ", 0),
                producto(20, "VEINTE", 0)));

        String html = mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html.indexOf("DIEZ")).isLessThan(html.indexOf("VEINTE"));
        assertThat(html.indexOf("VEINTE")).isLessThan(html.indexOf("TREINTA"));
    }

    @Test
    @DisplayName("R-PROD-13: los vecinos de navegación se calculan sobre la colección ordenada")
    void losVecinosDeNavegacionSeCalculan() throws Exception {
        when(productoService.listarProductos(any())).thenReturn(List.of(
                producto(10, "DIEZ", 0),
                producto(20, "VEINTE", 0),
                producto(30, "TREINTA", 0)));

        mockMvc.perform(get("/productos").param("prodCod", "20"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("indiceAnterior", 10))
                .andExpect(model().attribute("indiceSiguiente", 30))
                .andExpect(model().attribute("primerCodigo", 10))
                .andExpect(model().attribute("ultimoCodigo", 30))
                .andExpect(model().attribute("modo", ModoProducto.EDICION))
                .andExpect(model().attribute("productoId", 20))
                .andExpect(model().attribute("accionFormulario", "/productos/20"))
                .andExpect(model().attribute("accionEliminar", "/productos/20/eliminar"));
    }

    @Test
    @DisplayName("R-PROD-13: en el primer y el último registro el vecino de ese lado es null")
    void enLosExtremosElVecinoEsNull() throws Exception {
        when(productoService.listarProductos(any())).thenReturn(List.of(
                producto(10, "DIEZ", 0),
                producto(20, "VEINTE", 0)));

        mockMvc.perform(get("/productos").param("prodCod", "10"))
                .andExpect(model().attribute("indiceSiguiente", 20))
                .andExpect(result -> assertThat(
                        result.getModelAndView().getModel().get("indiceAnterior")).isNull());

        mockMvc.perform(get("/productos").param("prodCod", "20"))
                .andExpect(model().attribute("indiceAnterior", 10))
                .andExpect(result -> assertThat(
                        result.getModelAndView().getModel().get("indiceSiguiente")).isNull());
    }

    @Test
    @DisplayName("R-PROD-26: el filtro de anulados se pasa al servicio como parámetro")
    void elFiltroDeAnuladosSePasaAlServicio() throws Exception {
        mockMvc.perform(get("/productos").param("anulados", "false"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("filtroAnulados", FiltroAnulados.NO));
        verify(productoService).listarProductos(Boolean.FALSE);

        mockMvc.perform(get("/productos").param("anulados", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("filtroAnulados", FiltroAnulados.SI));
        verify(productoService).listarProductos(Boolean.TRUE);
    }

    @Test
    @DisplayName("R-PROD-03: los 4 derivados vienen de importes-vigentes y son de sólo lectura")
    void losDerivadosVienenDeImportesVigentes() throws Exception {
        when(productoService.listarProductos(any())).thenReturn(List.of(producto(7, "PRODUCTO", 0)));
        when(productoService.importesVigentes(7)).thenReturn(
                new ImportesVigentesDto(7, dec("10"), dec("20"), dec("30"), dec("40")));

        String html = mockMvc.perform(get("/productos").param("prodCod", "7"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        verify(productoService).importesVigentes(7);
        assertThat(html).contains("id=\"prodIMax1\"");
        assertThat(html).contains("id=\"prodIMax4\"");
        assertThat(html).contains("readonly");
    }

    @Test
    @DisplayName("R-PROD-19: el campo Unidad de Medida es de sólo lectura")
    void unidadDeMedidaEsDeSoloLectura() throws Exception {
        when(productoService.listarProductos(any())).thenReturn(List.of(producto(7, "PRODUCTO", 0)));

        String html = mockMvc.perform(get("/productos").param("prodCod", "7"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Unidad de Medida");
        assertThat(html).contains("derivado");
    }

    @Test
    @DisplayName("R-PROD-12: con la colección vacía se muestra el estado vacío y no la grilla")
    void conLaColeccionVaciaSeMuestraElEstadoVacio() throws Exception {
        String html = mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Sin registros");
        assertThat(html).doesNotContain("<th scope=\"col\">Prod Anulado</th>");
    }

    @Test
    @DisplayName("T-206: las filas de la grilla emiten name indexado e id con el índice")
    void lasFilasEmitenNameIndexadoEId() throws Exception {
        when(productoService.listarProductos(any())).thenReturn(List.of(
                producto(7, "PRODUCTO", 0, new ProductoVigenciaDto(
                        LocalDateTime.of(2026, 1, 1, 0, 0), dec("10"), dec("20"), null, null))));

        String html = mockMvc.perform(get("/productos").param("prodCod", "7"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("name=\"vigencias[0].prodFchVig\"");
        assertThat(html).contains("id=\"vigencias_0_prodFchVig\"");
        assertThat(html).contains("name=\"vigencias[0].prodImpu4\"");
        assertThat(html).contains("id=\"vigencias_0_prodImpu4\"");
    }

    @Test
    @DisplayName("R-PROD-20: un importe ausente se muestra y se envía como 0")
    void unImporteAusenteSeMuestraYSeEnviaComoCero() throws Exception {
        when(productoService.listarProductos(any())).thenReturn(List.of(
                producto(7, "PRODUCTO", 0, new ProductoVigenciaDto(
                        LocalDateTime.of(2026, 1, 1, 0, 0), dec("10"), dec("20"), null, null))));

        ProductoForm form = (ProductoForm) mockMvc.perform(get("/productos").param("prodCod", "7"))
                .andExpect(status().isOk())
                .andReturn().getModelAndView().getModel().get("producto");
        assertThat(form.getVigencias()).hasSize(1);
        assertThat(form.getVigencias().get(0).getProdImpu3()).isEqualByComparingTo("0");
        assertThat(form.getVigencias().get(0).getProdImpu4()).isEqualByComparingTo("0");
    }

    // ------------------------------------------------------------------ helpers

    private static ProductoDto producto(Integer cod, String descripcion, Integer anulada) {
        return producto(cod, descripcion, anulada, new ProductoVigenciaDto[0]);
    }

    private static ProductoDto producto(Integer cod, String descripcion, Integer anulada,
            ProductoVigenciaDto... vigencias) {
        ProductoVigenciaDto[] filas = vigencias == null ? new ProductoVigenciaDto[0] : vigencias;
        return new ProductoDto(cod, descripcion, 1, 2, "KG", null, "N", anulada, List.of(filas));
    }

    private static BigDecimal dec(String valor) {
        return new BigDecimal(valor);
    }
}
