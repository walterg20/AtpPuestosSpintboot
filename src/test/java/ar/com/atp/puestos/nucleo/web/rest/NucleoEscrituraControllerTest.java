package ar.gob.chaco.atp.puestoscamineros.nucleo.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.BusquedaService;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.NucleoConsultaService;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.NucleoEscrituraService;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ProductoService;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.AreaDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.CalleDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.CaminoDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.FeriadoDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.JurisdiccionDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.LocalidadDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoVigenciaDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProvinciaDto;
import ar.gob.chaco.atp.puestoscamineros.shared.error.ConflictException;
import ar.gob.chaco.atp.puestoscamineros.shared.error.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T-406 (R-030…R-032, plan §5): contrato HTTP de <strong>escritura</strong> (ABM) de los ocho
 * recursos del núcleo. Se cargan sólo los controladores bajo prueba y se sustituyen los servicios
 * por mocks, de modo que se verifica el verbo, la ruta, el {@code 201 + Location}, el {@code 200},
 * el {@code 204} y el mapeo de errores {@code 409}/{@code 400} de {@code GlobalExceptionHandler}.
 *
 * <p>El ciclo crear → modificar → eliminar se ejerce por entidad; los conflictos (PK duplicada,
 * provincia inexistente) y los cuerpos inválidos se representan con los códigos uniformes
 * {@code { code, message }}.
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
class NucleoEscrituraControllerTest {

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

    // ------------------------------------------------------------- Provincias

    @Nested
    @DisplayName("Provincias (/api/v1/provincias)")
    class Provincias {

        @Test
        @DisplayName("POST → 201 + Location + cuerpo")
        void crear() throws Exception {
            when(escrituraService.crearProvincia(any())).thenReturn(new ProvinciaDto(5, "CHACO"));

            mockMvc.perform(post("/api/v1/provincias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"provCod\":5,\"provNombre\":\"CHACO\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/provincias/5")))
                    .andExpect(jsonPath("$.provCod").value(5))
                    .andExpect(jsonPath("$.provNombre").value("CHACO"));
        }

        @Test
        @DisplayName("PUT → 200 + cuerpo")
        void modificar() throws Exception {
            when(escrituraService.actualizarProvincia(eq(5), any())).thenReturn(new ProvinciaDto(5, "CHACO NUEVO"));

            mockMvc.perform(put("/api/v1/provincias/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"provCod\":5,\"provNombre\":\"CHACO NUEVO\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.provNombre").value("CHACO NUEVO"));
        }

        @Test
        @DisplayName("DELETE → 204")
        void eliminar() throws Exception {
            mockMvc.perform(delete("/api/v1/provincias/5"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("POST con PK duplicada → 409 PK_DUPLICADA")
        void duplicada() throws Exception {
            when(escrituraService.crearProvincia(any())).thenThrow(new ConflictException(
                    "PK_DUPLICADA", "La provincia 5 ya existe."));

            mockMvc.perform(post("/api/v1/provincias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"provCod\":5,\"provNombre\":\"CHACO\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PK_DUPLICADA"));
        }

        @Test
        @DisplayName("POST sin PK → 400 VALIDACION")
        void invalido() throws Exception {
            mockMvc.perform(post("/api/v1/provincias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"provNombre\":\"CHACO\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDACION"));
        }
    }

    // ------------------------------------------------------------- Localidades

    @Nested
    @DisplayName("Localidades (/api/v1/localidades)")
    class Localidades {

        @Test
        @DisplayName("POST → 201 + Location + cuerpo")
        void crear() throws Exception {
            when(escrituraService.crearLocalidad(any()))
                    .thenReturn(new LocalidadDto(10, "CHARATA", 3730, 5, "CHACO"));

            mockMvc.perform(post("/api/v1/localidades")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"locCod\":10,\"locNom\":\"CHARATA\",\"locCodPost\":3730,\"provCod\":5}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/localidades/10")))
                    .andExpect(jsonPath("$.locCod").value(10))
                    .andExpect(jsonPath("$.provCod").value(5));
        }

        @Test
        @DisplayName("PUT → 200 + cuerpo")
        void modificar() throws Exception {
            when(escrituraService.actualizarLocalidad(eq(10), any()))
                    .thenReturn(new LocalidadDto(10, "CHARATA NORTE", 3730, 5, "CHACO"));

            mockMvc.perform(put("/api/v1/localidades/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"locCod\":10,\"locNom\":\"CHARATA NORTE\",\"provCod\":5}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.locNom").value("CHARATA NORTE"));
        }

        @Test
        @DisplayName("DELETE → 204")
        void eliminar() throws Exception {
            mockMvc.perform(delete("/api/v1/localidades/10"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("POST de localidad cuya provincia no existe → 409 PROVINCIA_INEXISTENTE")
        void provinciaInexistente() throws Exception {
            when(escrituraService.crearLocalidad(any())).thenThrow(new ConflictException(
                    "PROVINCIA_INEXISTENTE", "La provincia 99 no existe."));

            mockMvc.perform(post("/api/v1/localidades")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"locCod\":10,\"locNom\":\"CHARATA\",\"provCod\":99}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PROVINCIA_INEXISTENTE"));
        }

        @Test
        @DisplayName("POST sin FK provCod → 400 VALIDACION")
        void invalido() throws Exception {
            mockMvc.perform(post("/api/v1/localidades")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"locCod\":10,\"locNom\":\"CHARATA\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDACION"));
        }
    }

    // ----------------------------------------------------------------- Calles

    @Nested
    @DisplayName("Calles (/api/v1/calles)")
    class Calles {

        @Test
        @DisplayName("POST → 201 + Location + cuerpo")
        void crear() throws Exception {
            when(escrituraService.crearCalle(any())).thenReturn(new CalleDto(50, "SAN MARTIN"));

            mockMvc.perform(post("/api/v1/calles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"callCod\":50,\"callNom\":\"SAN MARTIN\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/calles/50")))
                    .andExpect(jsonPath("$.callNom").value("SAN MARTIN"));
        }

        @Test
        @DisplayName("PUT → 200 + cuerpo")
        void modificar() throws Exception {
            when(escrituraService.actualizarCalle(eq(50), any())).thenReturn(new CalleDto(50, "BELGRANO"));

            mockMvc.perform(put("/api/v1/calles/50")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"callCod\":50,\"callNom\":\"BELGRANO\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.callNom").value("BELGRANO"));
        }

        @Test
        @DisplayName("DELETE → 204")
        void eliminar() throws Exception {
            mockMvc.perform(delete("/api/v1/calles/50"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("POST con PK duplicada → 409 PK_DUPLICADA")
        void duplicada() throws Exception {
            when(escrituraService.crearCalle(any())).thenThrow(new ConflictException(
                    "PK_DUPLICADA", "La calle 50 ya existe."));

            mockMvc.perform(post("/api/v1/calles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"callCod\":50,\"callNom\":\"SAN MARTIN\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PK_DUPLICADA"));
        }

        @Test
        @DisplayName("POST sin nombre → 400 VALIDACION")
        void invalido() throws Exception {
            mockMvc.perform(post("/api/v1/calles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"callCod\":50}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDACION"));
        }
    }

    // ---------------------------------------------------------------- Caminos

    @Nested
    @DisplayName("Caminos (/api/v1/caminos)")
    class Caminos {

        @Test
        @DisplayName("POST → 201 + Location con PK de texto")
        void crear() throws Exception {
            when(escrituraService.crearCamino(any())).thenReturn(new CaminoDto("001", "RUTA NORTE"));

            mockMvc.perform(post("/api/v1/caminos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"camID\":\"001\",\"canRuta\":\"RUTA NORTE\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/caminos/001")))
                    .andExpect(jsonPath("$.camID").value("001"))
                    .andExpect(jsonPath("$.canRuta").value("RUTA NORTE"));
        }

        @Test
        @DisplayName("PUT → 200 + cuerpo")
        void modificar() throws Exception {
            when(escrituraService.actualizarCamino(eq("001"), any()))
                    .thenReturn(new CaminoDto("001", "RUTA SUR"));

            mockMvc.perform(put("/api/v1/caminos/001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"camID\":\"001\",\"canRuta\":\"RUTA SUR\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.canRuta").value("RUTA SUR"));
        }

        @Test
        @DisplayName("DELETE → 204")
        void eliminar() throws Exception {
            mockMvc.perform(delete("/api/v1/caminos/001"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("PUT sobre camino inexistente → 404 NO_ENCONTRADO")
        void inexistente() throws Exception {
            when(escrituraService.actualizarCamino(eq("ZZZ"), any())).thenThrow(new NotFoundException(
                    "NO_ENCONTRADO", "Camino ZZZ no encontrado."));

            mockMvc.perform(put("/api/v1/caminos/ZZZ")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"camID\":\"ZZZ\",\"canRuta\":\"X\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"));
        }

        @Test
        @DisplayName("POST sin PK camID → 400 VALIDACION")
        void invalido() throws Exception {
            mockMvc.perform(post("/api/v1/caminos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"canRuta\":\"RUTA NORTE\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDACION"));
        }
    }

    // ------------------------------------------------------------------ Areas

    @Nested
    @DisplayName("Áreas (/api/v1/areas) — PK BigDecimal")
    class Areas {

        @Test
        @DisplayName("POST → 201 + Location + cuerpo")
        void crear() throws Exception {
            when(escrituraService.crearArea(any()))
                    .thenReturn(new AreaDto(new BigDecimal("12"), "ADMINISTRACION"));

            mockMvc.perform(post("/api/v1/areas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"areaId\":12,\"areaDesc\":\"ADMINISTRACION\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/areas/12")))
                    .andExpect(jsonPath("$.areaId").value(12))
                    .andExpect(jsonPath("$.areaDesc").value("ADMINISTRACION"));
        }

        @Test
        @DisplayName("PUT → 200 + cuerpo")
        void modificar() throws Exception {
            when(escrituraService.actualizarArea(eq(new BigDecimal("12")), any()))
                    .thenReturn(new AreaDto(new BigDecimal("12"), "RRHH"));

            mockMvc.perform(put("/api/v1/areas/12")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"areaId\":12,\"areaDesc\":\"RRHH\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.areaDesc").value("RRHH"));
        }

        @Test
        @DisplayName("DELETE → 204")
        void eliminar() throws Exception {
            mockMvc.perform(delete("/api/v1/areas/12"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("POST con PK duplicada → 409 PK_DUPLICADA")
        void duplicada() throws Exception {
            when(escrituraService.crearArea(any())).thenThrow(new ConflictException(
                    "PK_DUPLICADA", "El area 12 ya existe."));

            mockMvc.perform(post("/api/v1/areas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"areaId\":12,\"areaDesc\":\"ADMINISTRACION\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PK_DUPLICADA"));
        }

        @Test
        @DisplayName("POST sin descripción → 400 VALIDACION")
        void invalido() throws Exception {
            mockMvc.perform(post("/api/v1/areas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"areaId\":12}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDACION"));
        }
    }

    // ---------------------------------------------------------- Jurisdicciones

    @Nested
    @DisplayName("Jurisdicciones (/api/v1/jurisdicciones)")
    class Jurisdicciones {

        @Test
        @DisplayName("POST → 201 + Location + cuerpo")
        void crear() throws Exception {
            when(escrituraService.crearJurisdiccion(any()))
                    .thenReturn(new JurisdiccionDto(3, "CIVIL"));

            mockMvc.perform(post("/api/v1/jurisdicciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"jurCod\":3,\"jurNom\":\"CIVIL\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/jurisdicciones/3")))
                    .andExpect(jsonPath("$.jurCod").value(3))
                    .andExpect(jsonPath("$.jurNom").value("CIVIL"));
        }

        @Test
        @DisplayName("PUT → 200 + cuerpo")
        void modificar() throws Exception {
            when(escrituraService.actualizarJurisdiccion(eq(3), any()))
                    .thenReturn(new JurisdiccionDto(3, "LABORAL"));

            mockMvc.perform(put("/api/v1/jurisdicciones/3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"jurCod\":3,\"jurNom\":\"LABORAL\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jurNom").value("LABORAL"));
        }

        @Test
        @DisplayName("DELETE → 204")
        void eliminar() throws Exception {
            mockMvc.perform(delete("/api/v1/jurisdicciones/3"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("POST sin PK → 400 VALIDACION")
        void invalido() throws Exception {
            mockMvc.perform(post("/api/v1/jurisdicciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"jurNom\":\"CIVIL\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDACION"));
        }
    }

    // --------------------------------------------------------------- Feriados

    @Nested
    @DisplayName("Feriados (/api/v1/feriados) — PK LocalDateTime")
    class Feriados {

        @Test
        @DisplayName("POST → 201 + Location + cuerpo")
        void crear() throws Exception {
            when(escrituraService.crearFeriado(any())).thenReturn(feriado("FERIADO TEST"));

            mockMvc.perform(post("/api/v1/feriados")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ferFecha\":\"2025-04-10T00:00:00\",\"ferDescripcion\":\"FERIADO TEST\"}"))
                    .andExpect(status().isCreated())
                    // LocalDateTime.toString() omite los segundos en cero → Location = .../2025-04-10T00:00
                    .andExpect(header().string("Location",
                            org.hamcrest.Matchers.endsWith("/api/v1/feriados/2025-04-10T00:00")))
                    .andExpect(jsonPath("$.ferDescripcion").value("FERIADO TEST"));
        }

        @Test
        @DisplayName("PUT → 200 + cuerpo")
        void modificar() throws Exception {
            when(escrituraService.actualizarFeriado(eq(FECHA), any()))
                    .thenReturn(feriado("FERIADO MODIFICADO"));

            mockMvc.perform(put("/api/v1/feriados/2025-04-10T00:00:00")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ferFecha\":\"2025-04-10T00:00:00\",\"ferDescripcion\":\"FERIADO MODIFICADO\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ferDescripcion").value("FERIADO MODIFICADO"));
        }

        @Test
        @DisplayName("DELETE → 204")
        void eliminar() throws Exception {
            mockMvc.perform(delete("/api/v1/feriados/2025-04-10T00:00:00"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("POST sin fecha → 400 VALIDACION")
        void invalido() throws Exception {
            mockMvc.perform(post("/api/v1/feriados")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ferDescripcion\":\"SIN FECHA\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDACION"));
        }

        private FeriadoDto feriado(String descripcion) {
            return new FeriadoDto(FECHA, descripcion, null, FECHA, "TEST");
        }
    }

    // -------------------------------------------------------------- Productos

    @Nested
    @DisplayName("Productos (/api/v1/productos) — vigencias anidadas")
    class Productos {

        @Test
        @DisplayName("POST → 201 + Location + cuerpo con vigencias")
        void crear() throws Exception {
            when(productoService.crearProducto(any())).thenReturn(producto(7));

            mockMvc.perform(post("/api/v1/productos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"prodCod\":7,\"prodDesc\":\"PRODUCTO TEST\","
                                    + "\"vigencias\":[{\"prodFchVig\":\"2025-01-01T00:00:00\","
                                    + "\"prodImpu1\":10,\"prodImpu2\":20,\"prodImpu3\":30,\"prodImpu4\":40}]}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/productos/7")))
                    .andExpect(jsonPath("$.prodCod").value(7))
                    .andExpect(jsonPath("$.vigencias[0].prodImpu1").value(10));
        }

        @Test
        @DisplayName("PUT → 200 + cuerpo")
        void modificar() throws Exception {
            when(productoService.actualizarProducto(eq(7), any())).thenReturn(producto(7));

            mockMvc.perform(put("/api/v1/productos/7")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"prodCod\":7,\"prodDesc\":\"PRODUCTO MODIFICADO\",\"vigencias\":[]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.prodCod").value(7));
        }

        @Test
        @DisplayName("DELETE → 204")
        void eliminar() throws Exception {
            mockMvc.perform(delete("/api/v1/productos/7"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("POST con PK duplicada → 409 PK_DUPLICADA")
        void duplicada() throws Exception {
            when(productoService.crearProducto(any())).thenThrow(new ConflictException(
                    "PK_DUPLICADA", "El producto 7 ya existe."));

            mockMvc.perform(post("/api/v1/productos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"prodCod\":7,\"prodDesc\":\"PRODUCTO TEST\",\"vigencias\":[]}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PK_DUPLICADA"));
        }

        @Test
        @DisplayName("POST con fecha de vigencia repetida → 409 VIGENCIA_DUPLICADA")
        void vigenciaDuplicada() throws Exception {
            when(productoService.crearProducto(any())).thenThrow(new ConflictException(
                    "VIGENCIA_DUPLICADA", "La fecha de vigencia 2025-01-01T00:00 está repetida."));

            mockMvc.perform(post("/api/v1/productos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"prodCod\":7,\"prodDesc\":\"PRODUCTO TEST\",\"vigencias\":["
                                    + "{\"prodFchVig\":\"2025-01-01T00:00:00\",\"prodImpu1\":10,\"prodImpu2\":10,\"prodImpu3\":10,\"prodImpu4\":10},"
                                    + "{\"prodFchVig\":\"2025-01-01T00:00:00\",\"prodImpu1\":20,\"prodImpu2\":20,\"prodImpu3\":20,\"prodImpu4\":20}]}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("VIGENCIA_DUPLICADA"));
        }

        @Test
        @DisplayName("POST sin lista de vigencias → 400 VALIDACION")
        void invalido() throws Exception {
            mockMvc.perform(post("/api/v1/productos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"prodCod\":7,\"prodDesc\":\"PRODUCTO TEST\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDACION"));
        }

        @Test
        @DisplayName("DELETE inexistente → 404 NO_ENCONTRADO")
        void inexistente() throws Exception {
            doThrow(new NotFoundException("NO_ENCONTRADO", "Producto 99 no encontrado."))
                    .when(productoService).eliminarProducto(99);

            mockMvc.perform(delete("/api/v1/productos/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"));
        }

        private ProductoDto producto(Integer prodCod) {
            return new ProductoDto(prodCod, "PRODUCTO TEST", 1, 1, "01", "UNIDAD", null, 0,
                    List.of(new ProductoVigenciaDto(LocalDateTime.of(2025, 1, 1, 0, 0),
                            new BigDecimal("10"), new BigDecimal("20"),
                            new BigDecimal("30"), new BigDecimal("40"))));
        }
    }
}
