package ar.gob.chaco.atp.puestoscamineros.nucleo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

/**
 * <strong>T-103</strong> (regresión en la suite verde): el listado de productos debe seguir
 * cargando las vigencias con un {@code left join fetch} (una sola consulta) y no con la
 * inicialización {@code LAZY} de la asociación (N+1).
 *
 * <p>Comprobación estructural por reflexión: no necesita base de datos y corre siempre. La
 * evidencia cuantitativa (número de consultas) vive en {@code ProductoListadoN1IT}, que se
 * ejecuta contra la base real con {@code ATP_DB_IT=true}.
 */
class ProductoRepositoryConsultaTest {

    @Test
    @DisplayName("T-103: findAllConVigencias() hace left join fetch de las vigencias")
    void listarTodasHaceFetchJoinDeVigencias() throws Exception {
        Method metodo = ProductoRepository.class.getMethod("findAllConVigencias");
        Query query = metodo.getAnnotation(Query.class);

        assertThat(query).as("findAllConVigencias() debe declarar un @Query explicito").isNotNull();
        assertThat(query.value().toLowerCase())
                .as("el listado debe traer las vigencias en la misma consulta (sin N+1)")
                .contains("left join fetch p.vigencias");
        assertThat(query.value().toLowerCase()).contains("distinct");
    }

    @Test
    @DisplayName("T-103: el listado filtrado por anulados tambien hace fetch join")
    void listarFiltradoHaceFetchJoinDeVigencias() throws Exception {
        Method metodo = ProductoRepository.class.getMethod(
                "findByProdAnulaOrderByProdDescAsc", Integer.class);
        Query query = metodo.getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value().toLowerCase())
                .as("el listado filtrado debe traer las vigencias en la misma consulta (sin N+1)")
                .contains("left join fetch p.vigencias");
        assertThat(query.value().toLowerCase()).contains("distinct");
    }
}
