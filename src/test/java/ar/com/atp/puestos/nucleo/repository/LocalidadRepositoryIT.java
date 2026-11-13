package ar.gob.chaco.atp.puestoscamineros.nucleo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Localidad;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Provincia;
import ar.gob.chaco.atp.puestoscamineros.support.SqlCollation;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
 * T-404 (plan.md §5): paridad de lectura de la tabla legacy {@code Localida} y su FK logica
 * {@code ProvCod} (R-003, R-020, R-021). Solo se ejecuta con {@code ATP_DB_IT=true}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-404 - Localida (Localidad)")
class LocalidadRepositoryIT {

    @Autowired
    private LocalidadRepository repository;

    @Autowired
    private ProvinciaRepository provinciaRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    @Test
    @DisplayName("findAll() lee Localida y aplica RTRIM a LocNom (R-002)")
    void findAllTrimeaNombre() {
        List<Localidad> localidades = repository.findAll();

        assertThat(localidades).allSatisfy(l -> {
            assertThat(l.getLocCod()).isNotNull();
            if (l.getLocNom() != null) {
                assertThat(l.getLocNom()).isEqualTo(l.getLocNom().stripTrailing());
            }
        });
    }

    @Test
    @DisplayName("findByProvincia_ProvCodOrderByLocNomAsc filtra por la FK logica ProvCod (R-003/R-021)")
    void filtrarPorProvincia() {
        List<Localidad> todas = repository.findAll();
        Integer provCod = todas.stream()
                .map(Localidad::getProvincia)
                .filter(Objects::nonNull)
                .map(Provincia::getProvCod)
                .findFirst()
                .orElse(null);
        if (provCod == null) {
            return;
        }

        List<Localidad> deProvincia = repository.findByProvincia_ProvCodOrderByLocNomAsc(provCod);

        Set<Integer> esperado = todas.stream()
                .filter(l -> l.getProvincia() != null && provCod.equals(l.getProvincia().getProvCod()))
                .map(Localidad::getLocCod)
                .collect(Collectors.toSet());
        Set<Integer> obtenido = deProvincia.stream()
                .map(Localidad::getLocCod)
                .collect(Collectors.toSet());

        assertThat(obtenido).isEqualTo(esperado);

        List<String> nombres = deProvincia.stream()
                .map(Localidad::getLocNom)
                .filter(Objects::nonNull)
                .map(String::trim)
                .collect(Collectors.toList());
        assertThat(nombres).isSortedAccordingTo(SqlCollation.spanish());
    }

    @Test
    @DisplayName("findAllConProvincia no descarta filas con FK logica huerfana y no lanza excepcion (R-024)")
    void listarTodasTolerandoFkHuerfana() {
        List<Localidad> tablaCompleta = repository.findAll();
        Set<Integer> pkDeLaTabla = tablaCompleta.stream()
                .map(Localidad::getLocCod)
                .collect(Collectors.toSet());

        // ProvCod realmente existentes en PROVINCI (verdad fisica).
        Set<Integer> provCodsExistentes = provinciaRepository.findAll().stream()
                .map(Provincia::getProvCod)
                .collect(Collectors.toSet());

        // Filas cuya FK logica no resuelve contra PROVINCI (verdad fisica, spec 4.3).
        long huerfanasEsperadas = tablaCompleta.stream()
                .filter(l -> l.getProvincia() == null
                        || !provCodsExistentes.contains(l.getProvincia().getProvCod()))
                .count();

        // NO debe lanzar EntityNotFoundException al inicializar la provincia huerfana.
        List<Localidad> todas = repository.findAllConProvincia();

        // 1) Ninguna fila se descarta (paridad con la tabla fisica).
        assertThat(todas.stream().map(Localidad::getLocCod).collect(Collectors.toSet()))
                .isEqualTo(pkDeLaTabla);
        assertThat(todas).hasSameSizeAs(pkDeLaTabla);

        // 2) La provincia se puede leer sin excepcion en todas las filas: las que tienen
        //    provincia resoluble la exponen con nombre, las huerfanas quedan sin nombre.
        List<Localidad> conProvincia = todas.stream()
                .filter(l -> {
                    Provincia prov = l.getProvincia();
                    return prov != null && prov.getProvNombre() != null && !prov.getProvNombre().isBlank();
                })
                .toList();
        List<Localidad> huerfanas = todas.stream()
                .filter(l -> {
                    Provincia prov = l.getProvincia();
                    return prov == null || prov.getProvNombre() == null || prov.getProvNombre().isBlank();
                })
                .toList();

        assertThat(conProvincia.size() + huerfanas.size()).isEqualTo(todas.size());
        assertThat(conProvincia).isNotEmpty();

        // 3) Las filas huerfanas expuestas son exactamente las de FK no resoluble (R-024).
        assertThat(huerfanas).isNotEmpty();
        assertThat(huerfanas).hasSize((int) huerfanasEsperadas);
        assertThat(huerfanas).allSatisfy(l -> {
            Provincia prov = l.getProvincia();
            if (prov != null) {
                assertThat(provCodsExistentes).doesNotContain(prov.getProvCod());
            }
        });
        assertThat(conProvincia).allSatisfy(l ->
                assertThat(provCodsExistentes).contains(l.getProvincia().getProvCod()));
    }

    @Test
    @DisplayName("findAllConProvincia resuelve la provincia en una sola consulta, sin N+1 (R-025)")
    void listarTodasEnUnaSolaConsulta() {
        Statistics stats = statistics();
        long consultasAntes = stats.getQueryExecutionCount();
        long sentenciasAntes = stats.getPrepareStatementCount();

        List<Localidad> todas = repository.findAllConProvincia();

        long consultas = stats.getQueryExecutionCount() - consultasAntes;
        long sentencias = stats.getPrepareStatementCount() - sentenciasAntes;

        assertThat(todas).isNotEmpty();
        assertThat(consultas)
                .as("debe ejecutarse una sola consulta HQL, sin N+1 (R-025)")
                .isEqualTo(1L);
        assertThat(sentencias)
                .as("la provincia debe resolverse en el mismo statement SQL (R-025)")
                .isLessThanOrEqualTo(2L);

        // La prueba es significativa solo si el dataset tiene varias provincias:
        // una consulta por provincia (N+1) seria visible como un numero de sentencias > 2.
        assertThat(todas.stream().map(Localidad::getProvincia).filter(Objects::nonNull)
                .map(Provincia::getProvCod).distinct().count())
                .as("el dataset de prueba debe tener mas de una provincia para que R-025 sea significativa")
                .isGreaterThan(1L);
    }

    @Test
    @DisplayName("findByProvCodConProvincia mantiene la paridad del filtro por provincia (R-021)")
    void filtrarPorProvinciaConProvinciaResuelta() {
        Integer provCod = repository.findAll().stream()
                .map(Localidad::getProvincia)
                .filter(Objects::nonNull)
                .map(Provincia::getProvCod)
                .findFirst()
                .orElse(null);
        if (provCod == null) {
            return;
        }

        List<Localidad> derivado = repository.findByProvincia_ProvCodOrderByLocNomAsc(provCod);
        List<Localidad> conJoin = repository.findByProvCodConProvincia(provCod);

        // Mismo conjunto de filas y mismo orden que el metodo derivado (INNER JOIN legacy).
        assertThat(conJoin.stream().map(Localidad::getLocCod).collect(Collectors.toList()))
                .isEqualTo(derivado.stream().map(Localidad::getLocCod).collect(Collectors.toList()));

        // La provincia viene resuelta: leer el nombre no dispara consultas adicionales.
        assertThat(conJoin).allSatisfy(l -> {
            assertThat(l.getProvincia()).isNotNull();
            assertThat(l.getProvincia().getProvNombre()).isNotBlank();
        });

        List<String> nombres = conJoin.stream()
                .map(Localidad::getLocNom)
                .filter(Objects::nonNull)
                .map(String::trim)
                .collect(Collectors.toList());
        assertThat(nombres).isSortedAccordingTo(SqlCollation.spanish());
    }

    @Test
    @DisplayName("findByIdConProvincia devuelve la localidad con la provincia resuelta (R-020/R-024)")
    void obtenerPorIdConProvincia() {
        Integer locCod = repository.findAll().stream()
                .map(Localidad::getLocCod)
                .findFirst()
                .orElse(null);
        if (locCod == null) {
            return;
        }

        var encontrada = repository.findByIdConProvincia(locCod);

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getLocCod()).isEqualTo(locCod);
        assertThat(repository.findByIdConProvincia(-1)).isEmpty();
    }

    @Test
    @DisplayName("R-207: las filas con FK logica huerfana exponen el codigo fisico de la columna (0)")
    void lasFilasHuerfanasExponenElCodigoFisicoDeLaFk() {
        List<Localidad> todas = repository.findAllConProvincia();

        // Mismo criterio de huérfana que usa listarTodasTolerandoFkHuerfana (no se reescribe).
        List<Localidad> huerfanas = todas.stream()
                .filter(l -> {
                    Provincia prov = l.getProvincia();
                    return prov == null || prov.getProvNombre() == null || prov.getProvNombre().isBlank();
                })
                .toList();

        assertThat(huerfanas)
                .as("la BD real tiene 30 filas con ProvCod = 0 (FK logica huerfana)")
                .isNotEmpty();

        // El codigo fisico de la columna Localida.ProvCod se conserva: 0, no null (R-207).
        assertThat(huerfanas).allSatisfy(l -> {
            assertThat(l.getProvCod()).isNotNull();
            assertThat(l.getProvCod()).isEqualTo(0);
        });

        // Las filas con provincia valida exponen el codigo de su provincia.
        List<Localidad> conProvincia = todas.stream()
                .filter(l -> {
                    Provincia prov = l.getProvincia();
                    return prov != null && prov.getProvNombre() != null && !prov.getProvNombre().isBlank();
                })
                .toList();

        assertThat(conProvincia).isNotEmpty();
        assertThat(conProvincia).allSatisfy(l -> {
            assertThat(l.getProvCod()).isNotNull();
            assertThat(l.getProvCod()).isEqualTo(l.getProvincia().getProvCod());
        });
    }
}
