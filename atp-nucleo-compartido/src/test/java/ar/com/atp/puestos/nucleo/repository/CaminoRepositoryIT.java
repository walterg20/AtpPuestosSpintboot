package ar.com.atp.puestos.nucleo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.nucleo.domain.Camino;
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
 * T-404 (plan.md §5): paridad de lectura de la tabla legacy {@code CAMINOS} (R-001, R-020).
 * Solo se ejecuta con {@code ATP_DB_IT=true}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-404 - CAMINOS (Camino)")
class CaminoRepositoryIT {

    @Autowired
    private CaminoRepository repository;

    @Test
    @DisplayName("findAll() lee CAMINOS, PK String no nula y RTRIM a CanRuta")
    void findAll() {
        List<Camino> caminos = repository.findAll();

        assertThat(caminos).allSatisfy(c -> {
            assertThat(c.getCamID()).isNotBlank();
            if (c.getCanRuta() != null) {
                assertThat(c.getCanRuta()).isEqualTo(c.getCanRuta().stripTrailing());
            }
        });
        assertThat(repository.count()).isEqualTo(caminos.size());
    }

    @Test
    @DisplayName("findById() encuentra una PK existente")
    void findById() {
        List<Camino> todos = repository.findAll();
        if (todos.isEmpty()) {
            return;
        }
        String pk = todos.get(0).getCamID();
        assertThat(repository.findById(pk)).isPresent();
    }
}
