package ar.gob.chaco.atp.puestoscamineros.repository;

import ar.gob.chaco.atp.puestoscamineros.domain.ActaProducto;
import ar.gob.chaco.atp.puestoscamineros.domain.ActaProductoId;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio del detalle de productos de un Acta → tabla {@code F22761}
 * (spec {@code 09-formularios-acta} §3.4, plan §3.4, T-ACTA-112).
 *
 * <p>Suma los impuestos del detalle ({@code F276RImpIB} / {@code F276RImpCM}), que son el
 * origen de los totales derivados {@code F276ImIBTot} y {@code F276ImCMTot} (D-ACTA-07): esas
 * dos columnas <strong>no existen</strong> en {@code F2276}, son fórmulas de la KB.
 */
public interface ActaProductoRepository extends JpaRepository<ActaProducto, ActaProductoId> {

    /**
     * Detalle de un Acta, ordenado por producto.
     *
     * @param f276Id número de Acta
     * @param sucTipo tipo de sucursal
     * @param sucNroId sucursal
     * @return las filas del detalle
     */
    @Query("select d from ActaProducto d where d.f276Id = :f276Id and d.sucTipo = :sucTipo "
            + "and d.sucNroId = :sucNroId order by d.prodCod asc")
    List<ActaProducto> findByActa(@Param("f276Id") Integer f276Id, @Param("sucTipo") String sucTipo,
            @Param("sucNroId") Integer sucNroId);

    /**
     * Detalle de varios Actas de la misma sucursal/tipo (evita el N+1 del listado).
     *
     * @param f276Ids números de Acta
     * @param sucTipo tipo de sucursal
     * @param sucNroId sucursal
     * @return las filas del detalle de todos los Actas indicados
     */
    @Query("select d from ActaProducto d where d.f276Id in :f276Ids and d.sucTipo = :sucTipo "
            + "and d.sucNroId = :sucNroId order by d.f276Id asc, d.prodCod asc")
    List<ActaProducto> findByActas(@Param("f276Ids") Collection<Integer> f276Ids,
            @Param("sucTipo") String sucTipo, @Param("sucNroId") Integer sucNroId);

    /**
     * Suma del impuesto de ingresos brutos del detalle de un Acta.
     *
     * @param f276Id número de Acta
     * @param sucTipo tipo de sucursal
     * @param sucNroId sucursal
     * @return la suma o {@code null} si el Acta no tiene detalle
     */
    @Query("select sum(d.f276RImpIB) from ActaProducto d where d.f276Id = :f276Id "
            + "and d.sucTipo = :sucTipo and d.sucNroId = :sucNroId")
    BigDecimal sumImpuestoIb(@Param("f276Id") Integer f276Id, @Param("sucTipo") String sucTipo,
            @Param("sucNroId") Integer sucNroId);

    /**
     * Suma del impuesto de cosecha mecánica del detalle de un Acta.
     *
     * @param f276Id número de Acta
     * @param sucTipo tipo de sucursal
     * @param sucNroId sucursal
     * @return la suma o {@code null} si el Acta no tiene detalle
     */
    @Query("select sum(d.f276RImpCM) from ActaProducto d where d.f276Id = :f276Id "
            + "and d.sucTipo = :sucTipo and d.sucNroId = :sucNroId")
    BigDecimal sumImpuestoCm(@Param("f276Id") Integer f276Id, @Param("sucTipo") String sucTipo,
            @Param("sucNroId") Integer sucNroId);

    /**
     * Borra el detalle de un Acta (uso interno del servicio).
     *
     * @param f276Id número de Acta
     * @param sucTipo tipo de sucursal
     * @param sucNroId sucursal
     */
    void deleteByF276IdAndSucTipoAndSucNroId(@Param("f276Id") Integer f276Id,
            @Param("sucTipo") String sucTipo, @Param("sucNroId") Integer sucNroId);
}
