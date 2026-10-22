package ar.com.atp.puestos.nucleo.repository;

import ar.com.atp.puestos.nucleo.domain.Producto;
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
     */
    @Query("select p from Producto p where p.prodAnula = :prodAnula order by p.prodDesc asc")
    List<Producto> findByProdAnulaOrderByProdDescAsc(@Param("prodAnula") Integer prodAnula);
}
