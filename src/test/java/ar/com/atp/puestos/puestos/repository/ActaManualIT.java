package ar.gob.chaco.atp.puestoscamineros.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.service.ActaService;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaAlta;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaAltaProducto;
import ar.gob.chaco.atp.puestoscamineros.service.model.ResultadoAltaActa;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("12-flujo-carga-manual-turno - alta manual de Acta")
class ActaManualIT {

    private static final Integer SUCURSAL = 1;
    private static final String TIPO = "PUE";
    private static final Integer TURNO_SELECCIONADO = 2914;
    private static final Integer F276_ID_MANUAL = 13770;

    @Autowired
    private ActaService actaService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private ContextoSesion contexto() {
        return ContextoSesion.completo("TESTER", "Tester", 1, 0, TURNO_SELECCIONADO, SUCURSAL,
                "Sucursal 1", TIPO, 1, "Resp");
    }

    private ActaAlta altaManual(List<Integer> productos) {
        List<ActaAltaProducto> prods = productos.stream()
                .map(c -> new ActaAltaProducto(c, BigDecimal.ONE, BigDecimal.ZERO))
                .toList();
        return new ActaAlta("F", "S", LocalDate.now(), 20123456786L, "VENDEDOR SA", "D", 1,
                30123456781L, "DESTINATARIO SA", "D", 2, 27123456780L, "TRANSPORTISTA SA", "D", 3,
                1, "1", "AB123CD", "EF456GH", new BigDecimal("1"), prods, F276_ID_MANUAL,
                TURNO_SELECCIONADO);
    }

    private long count(String tabla) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tabla + " WHERE SucNroId = ? AND SucTipo = ?",
                Long.class, SUCURSAL, TIPO);
    }

    private Integer sucUltNum() {
        return jdbcTemplate.queryForObject(
                "SELECT SucUltNum FROM GES_SUCN WHERE SucNroId = ? AND SucTipo = ? AND SucNuForm = 2276",
                Integer.class, SUCURSAL, TIPO);
    }

    @Test
    @DisplayName("R-TURNO-012/013: alta manual con 2 productos persiste cabecera, detalle y pase; GES_SUCN sin cambio")
    void altaManualCompleta() {
        long antes2276 = count("F2276");
        long antes22761 = count("F22761");
        long antesPase = count("F2276PAS");
        Integer ultAntes = sucUltNum();

        ResultadoAltaActa resultado = actaService.guardar(altaManual(List.of(1, 2)), contexto(), true);

        assertThat(resultado.ok()).isTrue();
        assertThat(resultado.f276Id()).isEqualTo(F276_ID_MANUAL);

        entityManager.flush();
        assertThat(count("F2276")).isEqualTo(antes2276 + 1);
        assertThat(count("F22761")).isEqualTo(antes22761 + 2);
        assertThat(count("F2276PAS")).isEqualTo(antesPase + 1);

        // cabecera verificada
        Integer turNroId = jdbcTemplate.queryForObject(
                "SELECT TurNroId FROM F2276 WHERE SucNroId = ? AND SucTipo = ? AND F276Id = ?",
                Integer.class, SUCURSAL, TIPO, F276_ID_MANUAL);
        assertThat(turNroId).isEqualTo(TURNO_SELECCIONADO);
        String genWeb = jdbcTemplate.queryForObject(
                "SELECT F276GenWeb FROM F2276 WHERE SucNroId = ? AND SucTipo = ? AND F276Id = ?",
                String.class, SUCURSAL, TIPO, F276_ID_MANUAL);
        assertThat(genWeb.trim()).isEqualTo("A");

        // GES_SUCN no cambio
        assertThat(sucUltNum()).isEqualTo(ultAntes);
    }

    @Test
    @DisplayName("R-TURNO-013: un error intermedio revierte toda la operacion")
    void rollbackEnErrorIntermedio() {
        long antes2276 = count("F2276");
        long antes22761 = count("F22761");
        long antesPase = count("F2276PAS");
        Integer ultAntes = sucUltNum();

        ActaAlta altaInvalida = new ActaAlta("F", "S", LocalDate.now(), 20123456786L, "VENDEDOR SA", "D", 1,
                30123456781L, "DESTINATARIO SA", "D", 2, 27123456780L, "TRANSPORTISTA SA", "D", 3,
                1, "1", "AB123CD", "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(1, BigDecimal.ONE, BigDecimal.ZERO),
                        new ActaAltaProducto(2, BigDecimal.ONE, BigDecimal.ZERO)),
                F276_ID_MANUAL, 999999);
        ResultadoAltaActa r = actaService.guardar(altaInvalida, contexto(), true);
        assertThat(r.ok()).isFalse();

        entityManager.flush();

        assertThat(count("F2276")).isEqualTo(antes2276);
        assertThat(count("F22761")).isEqualTo(antes22761);
        assertThat(count("F2276PAS")).isEqualTo(antesPase);
        assertThat(sucUltNum()).isEqualTo(ultAntes);
    }
}
