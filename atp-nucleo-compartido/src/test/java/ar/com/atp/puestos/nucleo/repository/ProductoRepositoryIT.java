package ar.com.atp.puestos.nucleo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.nucleo.domain.Producto;
import ar.com.atp.puestos.nucleo.domain.ProductoVigencia;
import ar.com.atp.puestos.support.SqlCollation;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
 * T-404 (plan.md §5): paridad de lectura del nivel 0 ({@code Producto}) y nivel 1
 * ({@code Product1} / {@code ProductoVigencia}) (R-004, R-020). Solo se ejecuta con
 * {@code ATP_DB_IT=true}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-404 - Producto + Product1 (Producto/ProductoVigencia)")
class ProductoRepositoryIT {

    @Autowired
    private ProductoRepository repository;

    @Autowired
    private ProductoVigenciaRepository vigenciaRepository;

    @Test
    @DisplayName("findByProdAnulaOrderByProdDescAsc filtra no anulados y ordena por descripcion")
    void noAnuladosOrdenados() {
        List<Producto> activos = repository.findByProdAnulaOrderByProdDescAsc(0);

        assertThat(activos).allSatisfy(p -> assertThat(p.getProdAnula()).isEqualTo(0));

        Set<Integer> esperado = repository.findAll().stream()
                .filter(p -> Integer.valueOf(0).equals(p.getProdAnula()))
                .map(Producto::getProdCod)
                .collect(Collectors.toSet());
        Set<Integer> obtenido = activos.stream()
                .map(Producto::getProdCod)
                .collect(Collectors.toSet());
        assertThat(obtenido).isEqualTo(esperado);

        List<String> descripciones = activos.stream()
                .map(Producto::getProdDesc)
                .filter(Objects::nonNull)
                .map(String::trim)
                .collect(Collectors.toList());
        assertThat(descripciones).isSortedAccordingTo(SqlCollation.spanish());
    }

    @Test
    @DisplayName("nivel 1: vigencias ordenadas por ProdFchVig asc (R-004)")
    void vigenciasOrdenadas() {
        List<Producto> productos = repository.findAll();
        Integer prodCod = productos.stream()
                .map(Producto::getProdCod)
                .findFirst()
                .orElse(null);
        if (prodCod == null) {
            return;
        }

        List<ProductoVigencia> vigencias =
                vigenciaRepository.findById_ProdCodOrderById_ProdFchVigAsc(prodCod);
        assertThat(vigencias).isSortedAccordingTo(
                Comparator.comparing(ProductoVigencia::getProdFchVig));

        repository.findById(prodCod).ifPresent(p -> {
            List<LocalDateTime> fechas = p.getVigencias().stream()
                    .map(ProductoVigencia::getProdFchVig)
                    .collect(Collectors.toList());
            assertThat(fechas).isSortedAccordingTo(Comparator.naturalOrder());
        });
    }
}
