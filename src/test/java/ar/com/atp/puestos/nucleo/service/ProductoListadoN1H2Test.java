package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Producto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.ProductoVigencia;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.ProductoRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoDto;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * <strong>T-103</strong> (evidencia aut&#243;noma, sin base real): el listado de productos con
 * {@code left join fetch} resuelve <strong>una</strong> consulta y devuelve <strong>exactamente</strong>
 * el mismo contenido que la ruta con N+1.
 *
 * <p><strong>Naturaleza del fixture</strong>: el esquema se crea <em>en memoria</em> (H2) a partir
 * de las entidades, con {@code ddl-auto=create-drop} acotado a esta clase de prueba. <strong>No</strong>
 * es DDL del proyecto, <strong>no</strong> es una migraci&#243;n, <strong>no</strong> hay
 * {@code schema.sql}/{@code data.sql}/Flyway/Liquibase y <strong>no</strong> se toca la base real
 * {@code Rentas}: la ruta canónica contra la base real es {@code ProductoListadoN1IT} (gated por
 * {@code ATP_DB_IT=true}).
 *
 * <p>Lo que se verifica:
 * <ol>
 *   <li>la ruta antigua ({@code findAll()} + inicializaci&#243;n LAZY) necesita una consulta por
 *       producto (N+1);</li>
 *   <li>la ruta nueva ({@code findAllConVigencias()}) necesita <strong>una</strong> consulta;</li>
 *   <li>la ra&#237;z <strong>no</strong> aparece duplicada (el {@code distinct} de HQL deduplica en
 *       memoria en Hibernate 6) y el JSON resultante es id&#233;ntico al de la ruta anterior.</li>
 * </ol>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        // T-129: el nucleo fija el dialecto de SQL Server (para que el arranque contra `Rentas`
        // no emita HHH000511). Esta fixture corre contra H2 en memoria => necesita su dialecto.
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
@DisplayName("T-103 - Listado de Productos sin N+1 (H2 en memoria, fixture efimera)")
class ProductoListadoN1H2Test {

    private static final LocalDateTime F1 = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDateTime F2 = LocalDateTime.of(2026, 6, 1, 0, 0);
    private static final LocalDateTime F3 = LocalDateTime.of(2026, 12, 1, 0, 0);

    @Autowired
    private ProductoRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SessionFactory sessionFactory;

    @BeforeEach
    void datosDePrueba() {
        // 3 productos: 3 vigencias, 2 vigencias y ninguno (el caso "sin vigencias" es real: 16 de 244).
        // Los dos primeros NO estan anulados; el tercero si (para ejercitar el filtro).
        repository.save(producto(1, "ALGODON", 0, 3));
        repository.save(producto(2, "MAIZ", 0, 2));
        repository.save(producto(3, "SIN VIGENCIAS", 1, 0));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("T-103: findAllConVigencias() = 1 consulta; la ruta con N+1 = 1 por producto")
    void listar_UsaUnaSolaConsulta() {
        Statistics stats = sessionFactory.getStatistics();

        // Ruta antigua: findAll() + inicializacion LAZY de cada coleccion.
        stats.clear();
        List<Producto> conN1 = repository.findAll();
        conN1.forEach(p -> p.getVigencias().size());
        long consultasConN1 = stats.getPrepareStatementCount();

        // Ruta nueva: un solo left join fetch.
        entityManager.clear();
        stats.clear();
        List<Producto> conFetch = repository.findAllConVigencias();
        long consultasConFetch = stats.getPrepareStatementCount();

        assertThat(conN1).hasSize(3);
        assertThat(conFetch).hasSize(3);
        assertThat(consultasConFetch).as("una sola consulta para toda la coleccion").isEqualTo(1L);
        assertThat(consultasConN1).as("la ruta con N+1 necesita una consulta por producto")
                .isGreaterThan(conN1.size());
    }

    @Test
    @DisplayName("T-103: la raiz no aparece duplicada y el JSON es identico al de la ruta con N+1")
    void listar_ProduceElMismoJsonQueLaRutaConN1() {
        List<Producto> conN1 = repository.findAll();
        conN1.forEach(p -> p.getVigencias().size());

        entityManager.clear();
        List<Producto> conFetch = repository.findAllConVigencias();

        assertThat(conFetch).hasSize(3);
        assertThat(dtosPorCodigo(conFetch)).isEqualTo(dtosPorCodigo(conN1));
    }

    @Test
    @DisplayName("T-103: el listado filtrado por anulados tambien usa 1 consulta y el mismo JSON")
    void listarFiltrado_UsaUnaSolaConsultaYElMismoJson() {
        Statistics stats = sessionFactory.getStatistics();

        List<Producto> conN1 = repository.findByProdAnulaOrderByProdDescAsc(0);
        conN1.forEach(p -> p.getVigencias().size());

        entityManager.clear();
        stats.clear();
        List<Producto> conFetch = repository.findByProdAnulaOrderByProdDescAsc(0);
        long consultas = stats.getPrepareStatementCount();

        assertThat(conFetch).hasSize(2);
        assertThat(consultas).isEqualTo(1L);
        assertThat(dtosPorCodigo(conFetch)).isEqualTo(dtosPorCodigo(conN1));
    }

    // ------------------------------------------------------------ helpers

    private static Producto producto(int codigo, String descripcion, int anulado, int vigencias) {
        Producto producto = new Producto(codigo, descripcion);
        producto.setProdTipCal(1);
        producto.setTpPriID(1);
        producto.setProdAnula(anulado);
        producto.setUMedNroId("UN");
        producto.setProdAcop("N");
        LocalDateTime[] fechas = {F1, F2, F3};
        for (int i = 0; i < vigencias; i++) {
            ProductoVigencia vigencia = new ProductoVigencia(fechas[i]);
            vigencia.setProdImpu1(new BigDecimal("100.00"));
            vigencia.setProdImpu2(new BigDecimal("200.00"));
            vigencia.setProdImpu3(new BigDecimal("300.00"));
            vigencia.setProdImpu4(new BigDecimal("400.00"));
            producto.addVigencia(vigencia);
        }
        return producto;
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
