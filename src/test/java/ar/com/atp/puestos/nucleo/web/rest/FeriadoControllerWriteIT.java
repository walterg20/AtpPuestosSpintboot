package ar.gob.chaco.atp.puestoscamineros.nucleo.web.rest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.FeriadoRepository;
import java.time.LocalDateTime;
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
 * T-407 (plan.md §5): ABM de {@code Feriado} de extremo a extremo contra la base {@code Rentas}
 * (R-030, R-031, R-033, R-037). Se ejecuta solo con {@code ATP_DB_IT=true} y revierte todo al
 * terminar cada prueba ({@link Transactional}), por lo que nunca deja datos de prueba. Usa fechas
 * lejanas para no colisionar con datos reales y omite la prueba si la PK ya existe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-407 - ABM de Feriados (MockMvc, HTTP real)")
class FeriadoControllerWriteIT {

    private static final String FECHA = "2999-01-01T00:00:00";
    private static final String OTRA = "2999-12-31T00:00:00";
    private static final String BASE = "/api/v1/feriados";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FeriadoRepository feriadoRepository;

    @BeforeEach
    void soloSiNoExiste() {
        Assumptions.assumeFalse(
                feriadoRepository.existsById(LocalDateTime.of(2999, 1, 1, 0, 0)),
                "La fecha de prueba 2999-01-01 ya existe en la base");
    }

    @Test
    @DisplayName("ciclo crear -> listar -> modificar -> eliminar (R-033)")
    void cicloCompleto() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ferFecha":"2999-01-01T00:00:00","ferDescripcion":"IT alta","ferTipo":"A"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("2999-01-01T00:00")))
                .andExpect(jsonPath("$.ferDescripcion").value("IT alta"))
                .andExpect(jsonPath("$.ferUsuario").value("SISTEMA"))
                .andExpect(jsonPath("$.ferFchReg", notNullValue()));

        mockMvc.perform(get(BASE)
                        .param("desde", "2999-01-01T00:00:00")
                        .param("hasta", "2999-01-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ferDescripcion").value("IT alta"));

        mockMvc.perform(put(BASE + "/{ferFecha}", FECHA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ferFecha":"2999-01-01T00:00:00","ferDescripcion":"IT mod","ferTipo":"B"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ferDescripcion").value("IT mod"))
                .andExpect(jsonPath("$.ferTipo").value("B"));

        mockMvc.perform(delete(BASE + "/{ferFecha}", FECHA))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("alta duplicada -> 409 PK_DUPLICADA (R-031)")
    void duplicadoDevuelve409() throws Exception {
        String cuerpo = """
                {"ferFecha":"2999-01-01T00:00:00","ferDescripcion":"IT dup","ferTipo":"A"}
                """;

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PK_DUPLICADA"));
    }

    @Test
    @DisplayName("modificar inexistente -> 404 NO_ENCONTRADO (R-032)")
    void modificarInexistenteDevuelve404() throws Exception {
        mockMvc.perform(put(BASE + "/{ferFecha}", OTRA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ferFecha":"2999-12-31T00:00:00","ferDescripcion":"IT fantasma"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("cuerpo invalido -> 400 VALIDACION")
    void validacionDevuelve400() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ferDescripcion":"sin fecha"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
    }
}
