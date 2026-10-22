package ar.com.atp.puestos.nucleo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.nucleo.domain.Area;
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
 * T-404 (plan.md §5): paridad de lectura de la tabla legacy {@code Areas} (R-001, R-020).
 * Verifica que la PK {@code AreaId} se mapea como {@code BigDecimal} (G-DATA, plan §2.2).
 * Solo se ejecuta con {@code ATP_DB_IT=true}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-404 - Areas (Area)")
class AreaRepositoryIT {

    @Autowired
    private AreaRepository repository;

    @Test
    @DisplayName("findAll() lee Areas con PK numerica (BigDecimal) no nula")
    void findAll() {
        List<Area> areas = repository.findAll();

        assertThat(areas).allSatisfy(a -> {
            assertThat(a.getAreaId()).isNotNull();
            if (a.getAreaDesc() != null) {
                assertThat(a.getAreaDesc()).isEqualTo(a.getAreaDesc().stripTrailing());
            }
        });
        assertThat(repository.count()).isEqualTo(areas.size());
    }

    @Test
    @DisplayName("findById() encuentra una PK BigDecimal existente")
    void findById() {
        List<Area> todas = repository.findAll();
        if (todas.isEmpty()) {
            return;
        }
        assertThat(repository.findById(todas.get(0).getAreaId())).isPresent();
    }
}
