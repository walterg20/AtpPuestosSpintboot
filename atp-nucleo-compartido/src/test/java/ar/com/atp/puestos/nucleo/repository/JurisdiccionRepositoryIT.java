package ar.com.atp.puestos.nucleo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.nucleo.domain.Jurisdiccion;
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
 * T-404 (plan.md §5): paridad de lectura de la tabla legacy {@code Jurisdic} (R-001, R-020).
 * Solo se ejecuta con {@code ATP_DB_IT=true}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-404 - Jurisdic (Jurisdiccion)")
class JurisdiccionRepositoryIT {

    @Autowired
    private JurisdiccionRepository repository;

    @Test
    @DisplayName("findAll() lee Jurisdic con PK no nula y RTRIM a JurNom")
    void findAll() {
        List<Jurisdiccion> jurisdicciones = repository.findAll();

        assertThat(jurisdicciones).allSatisfy(j -> {
            assertThat(j.getJurCod()).isNotNull();
            if (j.getJurNom() != null) {
                assertThat(j.getJurNom()).isEqualTo(j.getJurNom().stripTrailing());
            }
        });
        assertThat(repository.count()).isEqualTo(jurisdicciones.size());
    }

    @Test
    @DisplayName("findById() encuentra una PK existente")
    void findById() {
        List<Jurisdiccion> todas = repository.findAll();
        if (todas.isEmpty()) {
            return;
        }
        assertThat(repository.findById(todas.get(0).getJurCod())).isPresent();
    }
}
