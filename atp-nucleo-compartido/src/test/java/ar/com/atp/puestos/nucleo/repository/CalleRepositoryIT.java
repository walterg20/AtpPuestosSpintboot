package ar.com.atp.puestos.nucleo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.nucleo.domain.Calle;
import java.util.List;
import java.util.Objects;
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
 * T-404 (plan.md §5): paridad de lectura de la tabla legacy {@code Calles} (R-001, R-020).
 * Solo se ejecuta con {@code ATP_DB_IT=true}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-404 - Calles (Calle)")
class CalleRepositoryIT {

    @Autowired
    private CalleRepository repository;

    @Test
    @DisplayName("findAll() lee Calles y aplica RTRIM a CallNom (R-002)")
    void findAllTrimeaNombre() {
        List<Calle> calles = repository.findAll();

        assertThat(calles).allSatisfy(c -> {
            assertThat(c.getCallCod()).isNotNull();
            if (c.getCallNom() != null) {
                assertThat(c.getCallNom()).isEqualTo(c.getCallNom().stripTrailing());
            }
        });
    }

    @Test
    @DisplayName("findByCallNomContainingIgnoreCaseOrderByCallNomAsc busca por fragmento")
    void buscaParcialIgnorandoMayusculas() {
        List<Calle> todas = repository.findAll();
        String nombre = todas.stream()
                .map(Calle::getCallNom)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(n -> n.length() >= 3)
                .findFirst()
                .orElse(null);
        if (nombre == null) {
            return;
        }

        String fragmento = nombre.substring(0, 3);
        List<Calle> halladas = repository.findByCallNomContainingIgnoreCaseOrderByCallNomAsc(fragmento);

        assertThat(halladas).isNotEmpty();
        assertThat(halladas).allSatisfy(c ->
                assertThat(c.getCallNom().toUpperCase()).contains(fragmento.toUpperCase()));
    }
}
