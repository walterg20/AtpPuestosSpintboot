package ar.com.atp.puestos.nucleo.repository;

import ar.com.atp.puestos.nucleo.domain.ProductoVigencia;
import ar.com.atp.puestos.nucleo.domain.ProductoVigenciaId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de {@link ProductoVigencia} → tabla {@code PRODUCT1} (R-004, R-005). */
public interface ProductoVigenciaRepository extends JpaRepository<ProductoVigencia, ProductoVigenciaId> {

    /** Vigencias de un producto, ordenadas por fecha ascendente. */
    List<ProductoVigencia> findById_ProdCodOrderById_ProdFchVigAsc(Integer prodCod);

    /**
     * Última vigencia con {@code ProdFchVig <= hoy} — base de las fórmulas {@code ProdIMax1..4} (R-005).
     * Devuelve {@link Optional#empty()} si no hay ninguna vigencia pasada/actual.
     */
    Optional<ProductoVigencia> findFirstById_ProdCodAndId_ProdFchVigLessThanEqualOrderById_ProdFchVigDesc(
            Integer prodCod, LocalDateTime hoy);
}
