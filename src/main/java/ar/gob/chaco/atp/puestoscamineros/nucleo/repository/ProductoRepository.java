package ar.gob.chaco.atp.puestoscamineros.nucleo.repository;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio de {@link Producto} → tabla {@code PRODUCTO} (R-004, R-020). */
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    /**
     * Productos filtrados por estado de anulación ({@code ProdAnula}), ordenados por descripción.
     *
     * <p>Se usa {@code @Query} explícito: la derivación por nombre falla porque {@code ProdDesc}
     * contiene la palabra reservada {@code Desc} y el parser lo interpreta como dirección.
     *
     * <p><strong>T-103</strong>: el {@code left join fetch} de las vigencias elimina el N+1
     * (244 productos ⇒ ~245 consultas). No cambia la forma del JSON: el {@code distinct} de HQL
     * deduplica la raíz en memoria (Hibernate 6) y la lista de vigencias de cada producto conserva
     * el orden ascendente por {@code ProdFchVig} que fija el {@code @OrderBy} de la asociación.
     */
    @Query("select distinct p from Producto p left join fetch p.vigencias "
            + "where p.prodAnula = :prodAnula order by p.prodDesc asc")
    List<Producto> findByProdAnulaOrderByProdDescAsc(@Param("prodAnula") Integer prodAnula);

    /**
     * Todos los productos con sus vigencias cargadas <strong>en la misma consulta</strong> (T-103).
     *
     * <p>Es el equivalente del {@code findAll()} para el listado del ABM: sin el
     * {@code left join fetch}, la asociación {@code LAZY} se inicializa con una consulta por
     * producto (N+1). El {@code distinct} evita que la raíz aparezca repetida por cada vigencia
     * (Hibernate 6 deduplica la raíz en memoria), de modo que el listado conserva exactamente las
     * mismas filas y el mismo orden que el {@code findAll()} original.
     */
    @Query("select distinct p from Producto p left join fetch p.vigencias")
    List<Producto> findAllConVigencias();
}
