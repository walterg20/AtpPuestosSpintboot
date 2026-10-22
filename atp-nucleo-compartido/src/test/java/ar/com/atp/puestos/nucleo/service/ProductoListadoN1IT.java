package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.nucleo.domain.Producto;
import ar.com.atp.puestos.nucleo.repository.ProductoRepository;
import ar.com.atp.puestos.nucleo.web.dto.ProductoDto;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * <strong>T-103</strong>: el listado de productos no debe disparar N+1.
 *
 * <p>Con la asociacion {@code @OneToMany(LAZY)} y un {@code findAll()} plano, cargar las vigencias
 * de cada producto cuesta una consulta por producto: con los 244 productos reales de
 * {@code PRODUCTO} eso son ~245 consultas. Con el {@code left join fetch} de
 * {@link ProductoRepository#findAllConVigencias()} la lista completa se resuelve en
 * <strong>una</strong> consulta.
 *
 * <p>La prueba ademas verifica que el <strong>contenido</strong> es identico al de la ruta
 * anterior (misma forma de JSON): se comparan los DTOs que produce el mapper para ambas rutas,
 * indexados por {@code prodCod} (el endpoint no declara {@code ORDER BY}; el orden lo fija la
 * capa de vista, R-PROD-25 / D-33, asi que no es parte del contrato).
 *
 * <p>Solo lectura contra la base real ({@code SELECT}); se ejecuta con {@code ATP_DB_IT=true}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-103 - Listado de Productos sin N+1 (una consulta) y con JSON identico")
class ProductoListadoN1IT {

    @Autowired
    private ProductoRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SessionFactory sessionFactory;

    /** Estadisticas de Hibernate: permiten contar las consultas SQL realmente emitidas. */
    private Statistics estadisticas() {
        return sessionFactory.getStatistics();
    }

    @Test
    @DisplayName("T-103: findAllConVigencias() resuelve todo en 1 consulta (antes: 1 por producto)")
    void listarConVigencias_UsaUnaSolaConsulta() {
        Statistics stats = estadisticas();

        // Ruta anterior: findAll() + inicializacion LAZY de cada coleccion.
        stats.clear();
        List<Producto> conN1 = repository.findAll();
        conN1.forEach(p -> p.getVigencias().size());
        long consultasConN1 = stats.getPrepareStatementCount();

        // Ruta nueva: un solo left join fetch.
        entityManager.clear();
        stats.clear();
        List<Producto> conFetch = repository.findAllConVigencias();
        long consultasConFetch = stats.getPrepareStatementCount();

        assertThat(conN1).isNotEmpty();
        assertThat(conFetch).hasSameSizeAs(conN1);
        assertThat(consultasConFetch).isEqualTo(1L);
        // Evidencia del N+1 que se elimino: la ruta anterior necesita una consulta por producto.
        assertThat(consultasConN1).isGreaterThan(conN1.size());
    }

    @Test
    @DisplayName("T-103: el listado con filtro de anulados tambien resuelve en 1 consulta")
    void listarFiltrado_UsaUnaSolaConsulta() {
        Statistics stats = estadisticas();

        stats.clear();
        List<Producto> filtrado = repository.findByProdAnulaOrderByProdDescAsc(0);
        long consultas = stats.getPrepareStatementCount();

        assertThat(filtrado).isNotEmpty();
        assertThat(consultas).isEqualTo(1L);
        assertThat(filtrado).allSatisfy(p -> assertThat(p.getProdAnula()).isZero());
        // Las vigencias vienen cargadas: no hay inicializacion LAZY pendiente.
        assertThat(filtrado).allSatisfy(p ->
                assertThat(org.hibernate.Hibernate.isInitialized(p.getVigencias())).isTrue());
    }

    @Test
    @DisplayName("T-103: el JSON resultante es identico al de la ruta con N+1")
    void listar_ProduceElMismoJsonQueLaRutaConN1() {
        // Ruta antigua (N+1): findAll() + carga LAZY de las vigencias.
        List<Producto> conN1 = repository.findAll();
        conN1.forEach(p -> p.getVigencias().size());

        // Ruta nueva: un solo left join fetch.
        entityManager.clear();
        List<Producto> conFetch = repository.findAllConVigencias();

        assertThat(dtosPorCodigo(conFetch)).isEqualTo(dtosPorCodigo(conN1));
    }

    @Test
    @DisplayName("T-103: el listado con filtro de anulados produce el mismo JSON que antes")
    void listarFiltrado_ProduceElMismoJsonQueLaRutaConN1() {
        List<Producto> conN1 = repository.findByProdAnulaOrderByProdDescAsc(0);
        conN1.forEach(p -> p.getVigencias().size());

        entityManager.clear();
        List<Producto> conFetch = repository.findByProdAnulaOrderByProdDescAsc(0);

        assertThat(dtosPorCodigo(conFetch)).isEqualTo(dtosPorCodigo(conN1));
    }

    /** Indexa los DTOs por {@code prodCod}: el endpoint no declara {@code ORDER BY}. */
    private static Map<Integer, ProductoDto> dtosPorCodigo(List<Producto> productos) {
        Map<Integer, ProductoDto> porCodigo = new LinkedHashMap<>();
        for (Producto producto : productos) {
            ProductoDto dto = NucleoMapper.toDto(producto);
            porCodigo.put(dto.prodCod(), dto);
        }
        return porCodigo;
    }
}
