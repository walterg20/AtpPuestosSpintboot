package ar.com.atp.puestos.nucleo.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.com.atp.puestos.nucleo.service.BusquedaService;
import ar.com.atp.puestos.nucleo.service.NucleoConsultaService;
import ar.com.atp.puestos.nucleo.service.NucleoEscrituraService;
import ar.com.atp.puestos.nucleo.service.ProductoService;
import ar.com.atp.puestos.nucleo.web.dto.AreaDto;
import ar.com.atp.puestos.nucleo.web.dto.CalleDto;
import ar.com.atp.puestos.nucleo.web.dto.CaminoDto;
import ar.com.atp.puestos.nucleo.web.dto.CaminoRutaDto;
import ar.com.atp.puestos.nucleo.web.dto.FeriadoDto;
import ar.com.atp.puestos.nucleo.web.dto.ImportesVigentesDto;
import ar.com.atp.puestos.nucleo.web.dto.JurisdiccionDto;
import ar.com.atp.puestos.nucleo.web.dto.LocalidadDto;
import ar.com.atp.puestos.nucleo.web.dto.LocalidadNombreDto;
import ar.com.atp.puestos.nucleo.web.dto.ProductoDto;
import ar.com.atp.puestos.nucleo.web.dto.ProductoVigenciaDto;
import ar.com.atp.puestos.nucleo.web.dto.ProvinciaDto;
import ar.com.atp.puestos.shared.error.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T-403 (plan §5): contrato HTTP de lectura de los ocho recursos del núcleo (R-001…R-011, R-022,
 * R-023). Se cargan sólo los controladores bajo prueba; los servicios se sustituyen por mocks.
 *
 * <p>Ejerce el camino feliz (200 + cuerpo) y el 404 uniforme {@code { code, message }} emitido por
 * {@code GlobalExceptionHandler}.
 */
@WebMvcTest(controllers = {
        ProvinciaController.class,
        LocalidadController.class,
        CalleController.class,
        CaminoController.class,
        ProductoController.class,
        FeriadoController.class,
        AreaController.class,
        JurisdiccionController.class})
class NucleoLecturaControllerTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2025, 4, 10, 0, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NucleoConsultaService consultaService;

    @MockitoBean
    private NucleoEscrituraService escrituraService;

    @MockitoBean
    private BusquedaService busquedaService;

    @MockitoBean
    private ProductoService productoService;

    @Nested
    @DisplayName("Provincias")
    class Provincias {

        @Test
        void listarDevuelve200() throws Exception {
            when(consultaService.listarProvincias())
                    .thenReturn(List.of(new ProvinciaDto(1, "CHACO"), new ProvinciaDto(2, "CORRIENTES")));

            mockMvc.perform(get("/api/v1/provincias"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].provCod").value(1))
                    .andExpect(jsonPath("$[0].provNombre").value("CHACO"));
        }

        @Test
        void obtenerExistenteDevuelve200() throws Exception {
            when(consultaService.obtenerProvincia(2)).thenReturn(new ProvinciaDto(2, "CORRIENTES"));

            mockMvc.perform(get("/api/v1/provincias/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.provCod").value(2))
                    .andExpect(jsonPath("$.provNombre").value("CORRIENTES"));
        }

        @Test
        void obtenerInexistenteDevuelve404Uniforme() throws Exception {
            when(consultaService.obtenerProvincia(99))
                    .thenThrow(new NotFoundException("NO_ENCONTRADO", "Provincia 99 no encontrada."));

            mockMvc.perform(get("/api/v1/provincias/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"))
                    .andExpect(jsonPath("$.message").value("Provincia 99 no encontrada."));
        }
    }

    @Nested
    @DisplayName("Localidades")
    class Localidades {

        @Test
        void listarConFiltroProvCodDevuelve200() throws Exception {
            when(consultaService.listarLocalidades(1)).thenReturn(
                    List.of(new LocalidadDto(10, "CHARATA", 3730, 1, "CHACO")));

            mockMvc.perform(get("/api/v1/localidades").param("provCod", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].locCod").value(10))
                    .andExpect(jsonPath("$[0].locNom").value("CHARATA"))
                    .andExpect(jsonPath("$[0].provNombre").value("CHACO"));
        }

        @Test
        void listarSinFiltroDevuelve200() throws Exception {
            when(consultaService.listarLocalidades(null)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/localidades"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void obtenerInexistenteDevuelve404() throws Exception {
            when(consultaService.obtenerLocalidad(99))
                    .thenThrow(new NotFoundException("NO_ENCONTRADO", "Localidad 99 no encontrada."));

            mockMvc.perform(get("/api/v1/localidades/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"))
                    .andExpect(jsonPath("$.message").value("Localidad 99 no encontrada."));
        }

        @Test
        @DisplayName("R-207: el listado expone la clave provCod (= 0) de la FK huérfana y omite provNombre")
        void listarConFkHuerfanaExponeProvCodYOmiteProvNombre() throws Exception {
            // Fila real huérfana de Localida (ProvCod = 0, inexistente en PROVINCI).
            when(consultaService.listarLocalidades(null)).thenReturn(
                    List.of(new LocalidadDto(16342, "ACHIRAS", 5833, 0, null)));

            String body = mockMvc.perform(get("/api/v1/localidades"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].provCod").value(0))
                    .andExpect(jsonPath("$[0].provNombre").doesNotExist())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // La clave provCod ESTÁ en el cuerpo (no la omite la inclusion non_null) y vale 0.
            assertThat(body).contains("\"provCod\":0");
            assertThat(body).doesNotContain("provNombre");
        }

        @Test
        @DisplayName("R-207: el detalle de una localidad huérfana expone provCod (= 0) y omite provNombre")
        void obtenerConFkHuerfanaExponeProvCodYOmiteProvNombre() throws Exception {
            when(consultaService.obtenerLocalidad(16342))
                    .thenReturn(new LocalidadDto(16342, "ACHIRAS", 5833, 0, null));

            String body = mockMvc.perform(get("/api/v1/localidades/16342"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.provCod").value(0))
                    .andExpect(jsonPath("$.provNombre").doesNotExist())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertThat(body).contains("\"provCod\":0");
            assertThat(body).doesNotContain("provNombre");
        }

        @Test
        @DisplayName("R-207: una localidad con provincia válida expone provCod y provNombre")
        void obtenerConProvinciaValidaExponeCodigoYNombre() throws Exception {
            when(consultaService.obtenerLocalidad(10))
                    .thenReturn(new LocalidadDto(10, "CHARATA", 3730, 1, "CHACO"));

            mockMvc.perform(get("/api/v1/localidades/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.provCod").value(1))
                    .andExpect(jsonPath("$.provNombre").value("CHACO"));
        }

        @Test
        void busDesLocNuncaEs404YDevuelveVacio() throws Exception {
            when(busquedaService.nombreLocalidad(99)).thenReturn(new LocalidadNombreDto(""));

            mockMvc.perform(get("/api/v1/localidades/99/nombre"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.locNom").value(""));
        }

        @Test
        void busDesLocExistenteDevuelveNombre() throws Exception {
            when(busquedaService.nombreLocalidad(10)).thenReturn(new LocalidadNombreDto("CHARATA"));

            mockMvc.perform(get("/api/v1/localidades/10/nombre"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.locNom").value("CHARATA"));
        }
    }

    @Nested
    @DisplayName("Calles")
    class Calles {

        @Test
        void listarConFiltroQDevuelve200() throws Exception {
            when(consultaService.listarCalles("SAN")).thenReturn(List.of(new CalleDto(5, "SAN MARTIN")));

            mockMvc.perform(get("/api/v1/calles").param("q", "SAN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].callCod").value(5))
                    .andExpect(jsonPath("$[0].callNom").value("SAN MARTIN"));
        }

        @Test
        void obtenerInexistenteDevuelve404() throws Exception {
            when(consultaService.obtenerCalle(99))
                    .thenThrow(new NotFoundException("NO_ENCONTRADO", "Calle 99 no encontrada."));

            mockMvc.perform(get("/api/v1/calles/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"))
                    .andExpect(jsonPath("$.message").value("Calle 99 no encontrada."));
        }
    }

    @Nested
    @DisplayName("Caminos")
    class Caminos {

        @Test
        void listarDevuelve200() throws Exception {
            when(consultaService.listarCaminos()).thenReturn(List.of(new CaminoDto("001", "RUTA NORTE")));

            mockMvc.perform(get("/api/v1/caminos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].camID").value("001"))
                    .andExpect(jsonPath("$[0].canRuta").value("RUTA NORTE"));
        }

        @Test
        void obtenerInexistenteDevuelve404() throws Exception {
            when(consultaService.obtenerCamino("ZZZ"))
                    .thenThrow(new NotFoundException("NO_ENCONTRADO", "Camino ZZZ no encontrado."));

            mockMvc.perform(get("/api/v1/caminos/ZZZ"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"));
        }

        @Test
        void buscarCaminoNuncaEs404() throws Exception {
            when(busquedaService.rutaCamino("ZZZ")).thenReturn(new CaminoRutaDto(""));

            mockMvc.perform(get("/api/v1/caminos/ZZZ/ruta"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.canRuta").value(""));
        }
    }

    @Nested
    @DisplayName("Áreas y jurisdicciones")
    class AreasYJurisdicciones {

        @Test
        void listarAreasDevuelve200() throws Exception {
            when(consultaService.listarAreas()).thenReturn(List.of(new AreaDto(new BigDecimal("3"), "AREA 3")));

            mockMvc.perform(get("/api/v1/areas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].areaId").value(3))
                    .andExpect(jsonPath("$[0].areaDesc").value("AREA 3"));
        }

        @Test
        void listarJurisdiccionesDevuelve200() throws Exception {
            when(consultaService.listarJurisdicciones())
                    .thenReturn(List.of(new JurisdiccionDto(1, "PROVINCIAL")));

            mockMvc.perform(get("/api/v1/jurisdicciones"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].jurCod").value(1))
                    .andExpect(jsonPath("$[0].jurNom").value("PROVINCIAL"));
        }
    }

    @Nested
    @DisplayName("Feriados")
    class Feriados {

        @Test
        void listarPorRangoDevuelve200() throws Exception {
            when(consultaService.listarFeriados(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(new FeriadoDto(FECHA, "FERIADO TEST", "F",
                            LocalDateTime.of(2025, 1, 1, 8, 0), "SISTEMA")));

            mockMvc.perform(get("/api/v1/feriados")
                            .param("desde", "2025-01-01T00:00:00")
                            .param("hasta", "2025-12-31T23:59:59"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].ferDescripcion").value("FERIADO TEST"))
                    .andExpect(jsonPath("$[0].ferTipo").value("F"))
                    .andExpect(jsonPath("$[0].ferUsuario").value("SISTEMA"));
        }

        @Test
        @DisplayName("R-023: desde/hasta son obligatorios → 400 PARAMETRO_FALTANTE")
        void listarSinParametrosDevuelve400() throws Exception {
            mockMvc.perform(get("/api/v1/feriados"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARAMETRO_FALTANTE"));
        }
    }

    @Nested
    @DisplayName("Productos")
    class Productos {

        @Test
        void listarConVigenciasAnidadasDevuelve200() throws Exception {
            ProductoDto dto = new ProductoDto(7, "PRODUCTO TEST", 1, 2, "KG", "KILOGRAMO", "N", 0,
                    List.of(new ProductoVigenciaDto(FECHA, dec("10"), dec("20"), dec("30"), dec("40"))));
            when(productoService.listarProductos(null)).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/v1/productos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].prodCod").value(7))
                    .andExpect(jsonPath("$[0].vigencias.length()").value(1))
                    .andExpect(jsonPath("$[0].vigencias[0].prodImpu1").value(10));
        }

        @Test
        void obtenerInexistenteDevuelve404() throws Exception {
            when(productoService.obtenerProducto(99))
                    .thenThrow(new NotFoundException("NO_ENCONTRADO", "Producto 99 no encontrado."));

            mockMvc.perform(get("/api/v1/productos/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"))
                    .andExpect(jsonPath("$.message").value("Producto 99 no encontrado."));
        }

        @Test
        void importesVigentesDevuelve200() throws Exception {
            when(productoService.importesVigentes(7))
                    .thenReturn(new ImportesVigentesDto(7, dec("10"), dec("20"), dec("30"), dec("40")));

            mockMvc.perform(get("/api/v1/productos/7/importes-vigentes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.prodCod").value(7))
                    .andExpect(jsonPath("$.prodIMax1").value(10))
                    .andExpect(jsonPath("$.prodIMax4").value(40));
        }
    }

    private static BigDecimal dec(String valor) {
        return new BigDecimal(valor);
    }
}
