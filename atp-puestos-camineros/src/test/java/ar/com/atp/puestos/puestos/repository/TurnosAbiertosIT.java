package ar.com.atp.puestos.puestos.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.nucleo.domain.Turno;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("12-flujo-carga-manual-turno - turnos abiertos paginados")
class TurnosAbiertosIT {

    private static final Integer SUCURSAL = 1;
    private static final String TIPO = "PUE";

    @Autowired
    private ar.com.atp.puestos.nucleo.repository.TurnoRepositorio turnoRepositorio;

    @Test
    @DisplayName("R-TURNO-002/005: el listado paginado devuelve exactamente los turnos con TurEstado='A'")
    void listadoPaginadoDeTurnosAbiertos() {
        Page<Turno> pagina = turnoRepositorio.listarAbiertosPorSucursal(SUCURSAL, TIPO,
                PageRequest.of(0, 10));

        assertThat(pagina.getContent()).isNotEmpty();
        assertThat(pagina.getContent()).allSatisfy(t -> {
            assertThat(t.getId().getSucNroId()).isEqualTo(SUCURSAL);
            assertThat(t.getId().getSucTipo().trim()).isEqualTo(TIPO);
            assertThat(t.getTurEstado()).isEqualTo("A");
        });
        List<Integer> numeros = pagina.getContent().stream().map(Turno::getTurNroId).toList();
        assertThat(numeros).isSorted();
        // orden por SucNroId, SucTipo, TurNroId
        assertThat(pagina.getContent().get(0).getId().getTurNroId())
                .isLessThan(pagina.getContent().get(pagina.getContent().size() - 1).getId().getTurNroId());
    }
}
