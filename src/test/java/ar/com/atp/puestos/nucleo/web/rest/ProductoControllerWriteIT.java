package ar.gob.chaco.atp.puestoscamineros.nucleo.web.rest;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.ProductoRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * T-407 (plan.md §5): ABM atómico de {@code Producto} + vigencias contra la base {@code Rentas}
 * (R-034, R-035, R-005, R-037). Se ejecuta solo con {@code ATP_DB_IT=true} y revierte todo al
 * terminar cada prueba ({@link Transactional}). Usa un {@code ProdCod} lejano y fechas
 * fuera de rango para no colisionar con datos reales; omite la prueba si el código ya existe.
 *
 * <p>El {@code ProdCod} debe caber en {@code smallint} (rango -32768..32767): la columna
 * {@code PRODUCTO.ProdCod} es de ese tipo y un valor mayor provoke un error de desbordamiento
 * aritmético en SQL Server (HTTP 500). Los datos reales ocupan el rango 1..483.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-407 - ABM de Productos con vigencias (MockMvc, HTTP real)")
class ProductoControllerWriteIT {

    private static final int PROD_COD = 32000;
    private static final String BASE = "/api/v1/productos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductoRepository productoRepository;

    @BeforeEach
    void soloSiNoExiste() {
        Assumptions.assumeFalse(productoRepository.existsById(PROD_COD),
                "El ProdCod de prueba " + PROD_COD + " ya existe en la base");
    }

    /** Cuerpo con dos vigencias: la pasada (2000) es la vigente a efectos de {@code ProdIMax1..4}. */
    private static String cuerpoCompleto(String descripcion, String fecha1, String fecha2) {
        return """
                {
                  "prodCod": %d,
                  "prodDesc": "%s",
                  "prodTipCal": 1,
                  "tpPriID": 1,
                  "uMedNroId": "UN",
                  "prodAnula": 0,
                  "vigencias": [
                    {"prodFchVig":"%s","prodImpu1":100.00,"prodImpu2":200.00,"prodImpu3":300.00,"prodImpu4":400.00},
                    {"prodFchVig":"%s","prodImpu1":999.00,"prodImpu2":999.00,"prodImpu3":999.00,"prodImpu4":999.00}
                  ]
                }
                """.formatted(PROD_COD, descripcion, fecha1, fecha2);
    }

    @Test
    @DisplayName("ciclo crear -> leer -> importes -> modificar -> eliminar (R-034/R-035/R-005)")
    void cicloCompleto() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCompleto("IT producto", "2000-01-01T00:00:00",
                                "2999-01-01T00:00:00")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/" + PROD_COD)))
                .andExpect(jsonPath("$.prodCod").value(PROD_COD))
                .andExpect(jsonPath("$.vigencias.length()").value(2));

        mockMvc.perform(get(BASE + "/{prodCod}", PROD_COD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prodDesc").value("IT producto"))
                .andExpect(jsonPath("$.vigencias.length()").value(2));

        // La vigencia 2000-01-01 es la ultima con fecha <= hoy -> define ProdIMax1..4 (R-005).
        mockMvc.perform(get(BASE + "/{prodCod}/importes-vigentes", PROD_COD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prodCod").value(PROD_COD))
                .andExpect(jsonPath("$.prodIMax1").value(100.00))
                .andExpect(jsonPath("$.prodIMax2").value(200.00))
                .andExpect(jsonPath("$.prodIMax3").value(300.00))
                .andExpect(jsonPath("$.prodIMax4").value(400.00));

        mockMvc.perform(put(BASE + "/{prodCod}", PROD_COD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCompleto("IT producto mod", "2000-01-01T00:00:00",
                                "2999-01-01T00:00:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prodDesc").value("IT producto mod"));

        mockMvc.perform(delete(BASE + "/{prodCod}", PROD_COD))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("alta duplicada -> 409 PK_DUPLICADA (R-031)")
    void duplicadoDevuelve409() throws Exception {
        String cuerpo = cuerpoCompleto("IT dup", "2000-01-01T00:00:00", "2999-01-01T00:00:00");

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PK_DUPLICADA"));
    }

    @Test
    @DisplayName("vigencias con la misma fecha -> 409 VIGENCIA_DUPLICADA (R-034)")
    void vigenciaDuplicadaDevuelve409() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCompleto("IT vig dup", "2000-01-01T00:00:00",
                                "2000-01-01T00:00:00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VIGENCIA_DUPLICADA"));
    }

    @Test
    @DisplayName("modificar inexistente -> 404 NO_ENCONTRADO (R-032)")
    void modificarInexistenteDevuelve404() throws Exception {
        mockMvc.perform(put(BASE + "/{prodCod}", PROD_COD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCompleto("IT fantasma", "2000-01-01T00:00:00",
                                "2999-01-01T00:00:00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("cuerpo invalido (sin prodCod) -> 400 VALIDACION")
    void validacionDevuelve400() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prodDesc":"sin pk","vigencias":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
    }
}
