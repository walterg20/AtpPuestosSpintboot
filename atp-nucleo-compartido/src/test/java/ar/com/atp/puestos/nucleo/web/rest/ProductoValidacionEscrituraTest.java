package ar.com.atp.puestos.nucleo.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.com.atp.puestos.nucleo.service.ProductoService;
import ar.com.atp.puestos.nucleo.web.dto.ProductoDto;
import ar.com.atp.puestos.nucleo.web.dto.ProductoVigenciaDto;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * <strong>T-102 / D-32</strong>: refuerzo de la validación del DTO de escritura de
 * {@code Producto} y su efecto en el contrato REST.
 *
 * <p>Con el refuerzo, un {@code prodCod} fuera del rango del {@code smallint} físico
 * ({@code 0…32767}) o un importe con más de 2 decimales devuelven {@code 400 VALIDACION}
 * <strong>antes</strong> de tocar el servicio. Sin el refuerzo, esos casos llegaban a SQL Server
 * y la API respondía {@code 500 ERROR_INTERNO} (hallazgo N-4).
 *
 * <p>No se toca {@code web/rest}: el cambio es exclusivamente de las restricciones del DTO
 * (R-PROD-07, R-PROD-08).
 */
@WebMvcTest(controllers = ProductoController.class)
@DisplayName("T-102 - Validacion del DTO de escritura de Producto (400 VALIDACION)")
class ProductoValidacionEscrituraTest {

    private static final String BASE = "/api/v1/productos";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService productoService;

    // ------------------------------------------------------------ prodCod

    @Nested
    @DisplayName("prodCod: rango 0..32767 (smallint de PRODUCTO.ProdCod)")
    class RangoProdCod {

        @ParameterizedTest(name = "prodCod = {0} -> 400 VALIDACION")
        @ValueSource(ints = {-1, -100, 32768, 99999, 2147483647})
        @DisplayName("T-102: prodCod fuera de rango -> 400 VALIDACION y el servicio NO se invoca")
        void prodCodFueraDeRango(int prodCod) throws Exception {
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(prodCod, "10.00")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDACION"))
                    .andExpect(jsonPath("$.message").exists());

            verify(productoService, never()).crearProducto(any());
        }

        @ParameterizedTest(name = "prodCod = {0} -> 201")
        @ValueSource(ints = {0, 1, 483, 32767})
        @DisplayName("T-102: prodCod dentro de rango -> se acepta (201)")
        void prodCodDentroDeRango(int prodCod) throws Exception {
            when(productoService.crearProducto(any())).thenReturn(producto(prodCod));

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(prodCod, "10.00")))
                    .andExpect(status().isCreated());

            verify(productoService).crearProducto(any());
        }

        @Test
        @DisplayName("T-102: PUT con prodCod fuera de rango -> 400 VALIDACION (no llega al servicio)")
        void putProdCodFueraDeRango() throws Exception {
            mockMvc.perform(put(BASE + "/{prodCod}", 7)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(99999, "10.00")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDACION"));

            verify(productoService, never()).actualizarProducto(eq(7), any());
        }
    }

    // ------------------------------------------------------------ importes

    @Nested
    @DisplayName("Importes: NUMERIC(12,2) logico -> hasta 2 decimales, +/-9999999999.99")
    class RangoImportes {

        @ParameterizedTest(name = "importe {0} -> 400 VALIDACION")
        @ValueSource(strings = {"10.001", "0.001", "-10.005", "1.234567"})
        @DisplayName("T-102: importe con mas de 2 decimales -> 400 VALIDACION")
        void importeConTresDecimales(String importe) throws Exception {
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(7, importe)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDACION"));

            verify(productoService, never()).crearProducto(any());
        }

        @ParameterizedTest(name = "importe {0} -> 400 VALIDACION")
        @ValueSource(strings = {"10000000000.00", "99999999999.99", "-10000000000.00"})
        @DisplayName("T-102: importe fuera de +/-9999999999.99 -> 400 VALIDACION")
        void importeFueraDeRango(String importe) throws Exception {
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(7, importe)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDACION"));

            verify(productoService, never()).crearProducto(any());
        }

        @ParameterizedTest(name = "importe {0} -> 201")
        @ValueSource(strings = {"0", "0.00", "10.50", "-10.50", "9999999999.99", "-9999999999.99"})
        @DisplayName("T-102: importe valido (2 decimales, dentro del rango) -> se acepta (201)")
        void importeValido(String importe) throws Exception {
            when(productoService.crearProducto(any())).thenReturn(producto(7));

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(7, importe)))
                    .andExpect(status().isCreated());

            verify(productoService).crearProducto(any());
        }

        @Test
        @DisplayName("T-102: el limite de 2 decimales se aplica a los 4 importes")
        void losCuatroImportesEstanAcotados() throws Exception {
            // prodImpu2 fuera de rango
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(7, "10.00", "99999999999.99", "1.00", "1.00")))
                    .andExpect(status().isBadRequest());
            // prodImpu3 con 3 decimales
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(7, "10.00", "1.00", "1.001", "1.00")))
                    .andExpect(status().isBadRequest());
            // prodImpu4 con 3 decimales
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(7, "10.00", "1.00", "1.00", "1.001")))
                    .andExpect(status().isBadRequest());

            verify(productoService, never()).crearProducto(any());
        }
    }

    // ------------------------------------------------------------ helpers

    /** Cuerpo con una vigencia; el 2.º/3.º/4.º importe se pueden sobreescribir. */
    private static String cuerpo(int prodCod, String impu1) {
        return cuerpo(prodCod, impu1, "1.00", "1.00", "1.00");
    }

    private static String cuerpo(int prodCod, String impu1, String impu2, String impu3, String impu4) {
        return """
                {
                  "prodCod": %d,
                  "prodDesc": "Producto de prueba",
                  "prodTipCal": 1,
                  "tpPriID": 1,
                  "uMedNroId": "UN",
                  "prodAnula": 0,
                  "vigencias": [
                    {
                      "prodFchVig": "2026-01-01T00:00:00",
                      "prodImpu1": %s,
                      "prodImpu2": %s,
                      "prodImpu3": %s,
                      "prodImpu4": %s
                    }
                  ]
                }
                """.formatted(prodCod, impu1, impu2, impu3, impu4);
    }

    private static ProductoDto producto(int prodCod) {
        return new ProductoDto(prodCod, "Producto de prueba", 1, 1, "UN", null, null, 0,
                List.of(new ProductoVigenciaDto(LocalDateTime.of(2026, 1, 1, 0, 0),
                        java.math.BigDecimal.TEN, java.math.BigDecimal.ONE,
                        java.math.BigDecimal.ONE, java.math.BigDecimal.ONE)));
    }
}
