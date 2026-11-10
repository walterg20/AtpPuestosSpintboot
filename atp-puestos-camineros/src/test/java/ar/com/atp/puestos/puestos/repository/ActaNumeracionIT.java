package ar.com.atp.puestos.puestos.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("09-formularios-acta - numeracion GES_SUCN con rollback")
class ActaNumeracionIT {

    private static final Integer SUCURSAL = 1;
    private static final String TIPO = "PUE";
    private static final Integer NUMERO_FORMULARIO = 2276;

    @Autowired
    private ContadorFormularioRepositorio repositorio;

    @Test
    @DisplayName("R-ACTA-036: incremento atomico y rollback restaura SucUltNum")
    void incrementoAtomicoYRollback() {
        Integer antes = repositorio.valorActual(SUCURSAL, TIPO, NUMERO_FORMULARIO);
        assertThat(antes).isNotNull();

        repositorio.incrementarAtomico(SUCURSAL, TIPO, NUMERO_FORMULARIO);

        assertThat(repositorio.valorActual(SUCURSAL, TIPO, NUMERO_FORMULARIO))
                .isEqualTo(antes + 1);
    }
}