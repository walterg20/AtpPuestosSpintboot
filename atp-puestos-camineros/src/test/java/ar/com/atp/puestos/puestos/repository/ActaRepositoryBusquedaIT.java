package ar.com.atp.puestos.puestos.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("09-formularios-acta - busqueda con Sort (regresion B0)")
class ActaRepositoryBusquedaIT {

    private static final Integer SUCURSAL = 15;
    private static final String TIPO = "PUE";
    private static final Integer TURNO = 8;

    @Autowired
    private ActaRepository actaRepository;

    @Test
    @DisplayName("B0: buscar con Sort.by(\"f276Id\") no lanza y devuelve filas")
    void conSortF276Id() {
        var pagina = actaRepository.buscar(TIPO, SUCURSAL, TURNO, "A", "A", null, null, null,
                null, null, PageRequest.of(0, 10, Sort.by("f276Id")));
        assertThat(pagina).isNotNull();
        assertThat(pagina.getContent()).isNotEmpty();
        assertThat(pagina.getContent().stream().map(a -> a.getF276Id()).toList()).isSorted();
    }

    @Test
    @DisplayName("B0: buscar con Sort.by(4 campos) no lanza y devuelve filas")
    void conSortCuatroCampos() {
        var pagina = actaRepository.buscar(TIPO, SUCURSAL, TURNO, "A", "A", null, null, null,
                null, null, PageRequest.of(0, 10,
                        Sort.by("turNroId", "sucNroId", "sucTipo", "f276Id")));
        assertThat(pagina).isNotNull();
        assertThat(pagina.getContent()).isNotEmpty();
    }
}