package ar.gob.chaco.atp.puestoscamineros.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ar.gob.chaco.atp.puestoscamineros.domain.Acta;
import ar.gob.chaco.atp.puestoscamineros.domain.ActaProducto;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Módulo {@code 09-formularios-acta} — lectura de {@code F2276} y {@code F22761} contra la base
 * {@code Rentas} (sólo lectura). Se ejecuta sólo con {@code ATP_DB_IT=true}.
 *
 * <p>Verifica que el filtro base del listado reproduce el {@code ControlWhere} del legacy
 * ({@code TurNroId}/{@code SucNroId}/{@code SucTipo}, {@code F276Estad <> 'A'},
 * {@code F276GenWeb = 'A'}) y que el orden es el {@code ControlOrder}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("09-formularios-acta - F2276 y F22761: lectura de solo acceso")
class ActaRepositoryIT {

    /** Sucursal con actas del turno 8 (Puestos Camineros). */
    private static final Integer SUCURSAL = 15;
    private static final String TIPO = "PUE";
    private static final Integer TURNO = 8;

    @Autowired
    private ActaRepository actaRepository;

    @Autowired
    private ActaProductoRepository actaProductoRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("R-ACTA-003: el listado trae exactamente las filas del ControlWhere del legacy")
    void elListadoReproduceElControlWhere() {
        Page<Acta> pagina = actaRepository.buscar(TIPO, SUCURSAL, TURNO, "A", "A", null, null,
                null, null, null, PageRequest.of(0, 10));

        Long esperado = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM F2276 WHERE SucNroId = ? AND SucTipo = ? AND TurNroId = ? "
                        + "AND F276Estad <> 'A' AND F276GenWeb = 'A'",
                Long.class, SUCURSAL, TIPO, TURNO);

        assertThat(esperado).isNotNull().isPositive();
        assertThat(pagina.getTotalElements()).isEqualTo(esperado);
        assertThat(pagina.getContent()).allSatisfy(acta -> {
            assertThat(acta.getSucNroId()).isEqualTo(SUCURSAL);
            assertThat(acta.getSucTipo()).isEqualTo(TIPO);
            assertThat(acta.getTurNroId()).isEqualTo(TURNO);
            assertThat(acta.getF276GenWeb()).isEqualTo("A");
            assertThat(acta.getF276Estad()).isNotEqualTo("A");
        });
    }

    @Test
    @DisplayName("R-ACTA-004: el listado viene ordenado por F276Id dentro del turno")
    void elListadoVieneOrdenadoPorNumeroDeActa() {
        Page<Acta> pagina = actaRepository.buscar(TIPO, SUCURSAL, TURNO, "A", "A", null, null,
                null, null, null, PageRequest.of(0, 10));

        List<Integer> numeros = pagina.getContent().stream().map(Acta::getF276Id).toList();
        assertThat(numeros).isSorted();
    }

    @Test
    @DisplayName("R-ACTA-011: el filtro por numero de Acta es exacto")
    void elFiltroPorNumeroDeActa() {
        Integer numero = actaRepository.buscar(TIPO, SUCURSAL, TURNO, "A", "A", null, null, null,
                null, null, PageRequest.of(0, 1)).getContent().get(0).getF276Id();

        Page<Acta> filtrado = actaRepository.buscar(TIPO, SUCURSAL, TURNO, "A", "A", numero, null,
                null, null, null, PageRequest.of(0, 10));

        assertThat(filtrado.getTotalElements()).isEqualTo(1L);
        assertThat(filtrado.getContent().get(0).getF276Id()).isEqualTo(numero);
    }

    @Test
    @DisplayName("R-ACTA-047: el detalle de F22761 se lee con sus importes")
    void elDetalleSeLee() {
        Integer numero = actaRepository.buscar(TIPO, SUCURSAL, TURNO, "A", "A", null, null, null,
                null, null, PageRequest.of(0, 1)).getContent().get(0).getF276Id();

        List<ActaProducto> detalle = actaProductoRepository.findByActa(numero, TIPO, SUCURSAL);

        assertThat(detalle).isNotEmpty();
        assertThat(detalle).allSatisfy(linea -> {
            assertThat(linea.getF276Id()).isEqualTo(numero);
            assertThat(linea.getSucTipo()).isEqualTo(TIPO);
            assertThat(linea.getSucNroId()).isEqualTo(SUCURSAL);
            assertThat(linea.getProdCod()).isNotNull();
            assertThat(linea.getF276RAlicu()).isEqualByComparingTo(BigDecimal.ONE);
        });
    }

    @Test
    @DisplayName("R-ACTA-043: el conteo de actas pendientes se ejecuta contra la base real")
    void elConteoDeActasPendientesSeEjecuta() {
        long pendientes = actaRepository.contarActasPendientes(SUCURSAL, TIPO, TURNO);

        assertThat(pendientes).isGreaterThanOrEqualTo(0L);
    }
}
