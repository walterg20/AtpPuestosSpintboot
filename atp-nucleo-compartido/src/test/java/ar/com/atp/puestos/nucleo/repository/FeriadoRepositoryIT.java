package ar.com.atp.puestos.nucleo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.nucleo.domain.Feriado;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * T-404 (plan.md §5): paridad de lectura de la tabla legacy {@code Feriados} (R-006, R-021).
 * Verifica implicitamente el mapeo de columnas R-205 ({@code FerDescrip}, {@code FerFecha}).
 * Solo se ejecuta con {@code ATP_DB_IT=true}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-404 - Feriados (Feriado)")
class FeriadoRepositoryIT {

    @Autowired
    private FeriadoRepository repository;

    @Test
    @DisplayName("findAll() lee Feriados mapeando FerDescrip -> ferDescripcion (R-205)")
    void findAll() {
        List<Feriado> feriados = repository.findAll();

        assertThat(feriados).allSatisfy(f -> assertThat(f.getFerFecha()).isNotNull());
        if (!feriados.isEmpty()) {
            assertThat(feriados).anySatisfy(f -> assertThat(f.getFerDescripcion()).isNotNull());
        }
    }

    @Test
    @DisplayName("findByFerFechaBetween... devuelve el rango ordenado ascendentemente (R-021)")
    void rangoOrdenado() {
        LocalDateTime desde = LocalDateTime.of(1900, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2999, 12, 31, 23, 59);

        List<Feriado> rango = repository.findByFerFechaBetweenOrderByFerFechaAsc(desde, hasta);

        assertThat(rango).allSatisfy(f -> assertThat(f.getFerFecha()).isBetween(desde, hasta));
        assertThat(rango).isSortedAccordingTo(Comparator.comparing(Feriado::getFerFecha));

        // hasSameSizeAs(findAll()) no aplica: hay registros con FerFecha nula o fuera del rango
        // 1900..2999 (p. ej. fechas centinela). Se compara contra la misma semantica del predicado.
        long esperado = repository.findAll().stream()
                .filter(f -> f.getFerFecha() != null)
                .filter(f -> !f.getFerFecha().isBefore(desde) && !f.getFerFecha().isAfter(hasta))
                .count();
        assertThat(rango).hasSize((int) esperado);
    }
}
