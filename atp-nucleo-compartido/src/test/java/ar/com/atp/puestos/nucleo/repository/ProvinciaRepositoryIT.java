package ar.com.atp.puestos.nucleo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.nucleo.domain.Provincia;
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
 * T-404 (plan.md §5): paridad de lectura de la tabla legacy {@code PROVINCI} (R-001, R-002, R-020).
 *
 * <p>Prueba de integracion contra la base {@code Rentas}. Solo se ejecuta con {@code ATP_DB_IT=true};
 * en caso contrario la clase queda deshabilitada y el contexto de Spring ni siquiera arranca.
 * {@code ddl-auto=none} garantiza que Hibernate nunca genera ni valida DDL sobre el esquema real
 * (R-204). Las aserciones son tolerantes a los datos: no dependen de filas concretas.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-404 - PROVINCI (Provincia)")
class ProvinciaRepositoryIT {

    @Autowired
    private ProvinciaRepository repository;

    @Test
    @DisplayName("findAll() lee la tabla sin excepciones y con PK no nula")
    void findAll() {
        List<Provincia> provincias = repository.findAll();

        assertThat(provincias).isNotNull();
        assertThat(provincias).allSatisfy(p -> assertThat(p.getProvCod()).isNotNull());
        assertThat(repository.count()).isEqualTo(provincias.size());
    }

    @Test
    @DisplayName("findById() devuelve la misma fila que findAll() (round-trip de PK)")
    void findById() {
        List<Provincia> todas = repository.findAll();
        if (todas.isEmpty()) {
            return;
        }
        Provincia primera = todas.get(0);
        assertThat(repository.findById(primera.getProvCod())).isPresent();
    }
}
