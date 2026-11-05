package ar.com.atp.puestos.puestos.repository;

import ar.com.atp.puestos.puestos.domain.Acta;
import ar.com.atp.puestos.puestos.domain.ActaId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link Acta} → tabla {@code F2276}
 * (spec {@code 09-formularios-acta} §3.4, plan §3.4, T-ACTA-111).
 *
 * <p>El listado reproduce el {@code ControlWhere} del legacy ({@code ActaListadoWC.Grid276}) más
 * los filtros de UX (R-ACTA-003, R-ACTA-011):
 *
 * <pre>
 *   TurNroId = &amp;TurNroId ; SucNroId = &amp;SucNroId ; SucTipo = &amp;SucTipo ;
 *   F276Estad &lt;&gt; 'A' ; F276GenWeb = 'A'
 * </pre>
 *
 * <p>El orden es el {@code ControlOrder} del legacy: {@code TurNroId, SucNroId, SucTipo,
 * F276Id} (R-ACTA-004).
 *
 * <p>La consulta trae la <strong>fila cruda</strong>: el total derivado ({@code F276Total}) y la
 * contraparte (CUIT / razón social) los calcula el servicio (D-ACTA-07).
 */
public interface ActaRepository extends JpaRepository<Acta, ActaId> {

    /**
     * Listado paginado de las Actas del turno (R-ACTA-003, R-ACTA-004, R-ACTA-011).
     *
     * @param sucTipo tipo de sucursal de la sesión
     * @param sucNroId sucursal de la sesión
     * @param turNroId turno de la sesión
     * @param f276GenWeb origen de generación ({@code 'A'} = web)
     * @param excluirEstad estado excluido ({@code 'A'} = anulada)
     * @param f276Id número de Acta (exacto) o {@code null}
     * @param cuit CUIT de cualquiera de los tres roles o {@code null}
     * @param razonSocial patrón {@code %texto%} en minúsculas o {@code null}
     * @param desde inicio del rango de {@code F276FchMov} (inclusive) o {@code null}
     * @param hasta fin del rango de {@code F276FchMov} (exclusive) o {@code null}
     * @param pageable paginación
     * @return la página de Actas del turno
     */
    @Query("select a from Acta a "
            + "where a.turNroId = :turNroId "
            + "and a.sucNroId = :sucNroId "
            + "and a.sucTipo = :sucTipo "
            + "and a.f276Estad <> :excluirEstad "
            + "and a.f276GenWeb = :f276GenWeb "
            + "and (:f276Id is null or a.f276Id = :f276Id) "
            + "and (:cuit is null or a.f276CuitVe = :cuit "
            + "     or a.f276CuitDe = :cuit or a.f276CuitTr = :cuit) "
            + "and (:razonSocial is null "
            + "     or lower(a.f276VenRS) like :razonSocial "
            + "     or lower(a.f276DestRS) like :razonSocial "
            + "     or lower(a.f276TranRS) like :razonSocial) "
            + "and (:desde is null or a.f276FchMov >= :desde) "
            + "and (:hasta is null or a.f276FchMov < :hasta)")
    Page<Acta> buscar(@Param("sucTipo") String sucTipo, @Param("sucNroId") Integer sucNroId,
            @Param("turNroId") Integer turNroId, @Param("f276GenWeb") String f276GenWeb,
            @Param("excluirEstad") String excluirEstad, @Param("f276Id") Integer f276Id,
            @Param("cuit") BigDecimal cuit, @Param("razonSocial") String razonSocial,
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
            Pageable pageable);

    /**
     * Máximo {@code F276Id} de la sucursal — <strong>sólo diagnóstico</strong>.
     *
     * <p>No se usa para numerar: la fuente autoritativa es el contador {@code GES_SUCN}
     * (D-ACTA-21, SA-ACTA-15).
     *
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @return el máximo {@code F276Id} o {@code null}
     */
    @Query("select max(a.f276Id) from Acta a where a.sucNroId = :sucNroId "
            + "and a.sucTipo = :sucTipo")
    Integer maxF276Id(@Param("sucNroId") Integer sucNroId, @Param("sucTipo") String sucTipo);

    /**
     * Actas <strong>pendientes</strong> del turno (R-ACTA-043).
     *
     * <p>Una Acta está pendiente cuando su {@code F276Estad} es distinto de {@code 'C'}
     * (confirmada) y de {@code 'A'} (anulada) — <strong>incluido el valor {@code NULL}</strong>
     * (SA-ACTA-04).
     *
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @param turNroId turno
     * @return la cantidad de Actas pendientes
     */
    @Query("select count(a) from Acta a where a.turNroId = :turNroId "
            + "and a.sucNroId = :sucNroId and a.sucTipo = :sucTipo "
            + "and (a.f276Estad is null or (a.f276Estad <> 'C' and a.f276Estad <> 'A'))")
    long contarActasPendientes(@Param("sucNroId") Integer sucNroId,
            @Param("sucTipo") String sucTipo, @Param("turNroId") Integer turNroId);

    /**
     * Actas del turno con un {@code F276Id} concreto — usado por las pruebas de integración para
     * preparar datos de forma determinista.
     *
     * @param f276Id número de Acta
     * @param sucTipo tipo de sucursal
     * @param sucNroId sucursal
     * @return las Actas que coinciden
     */
    @Query("select a from Acta a where a.f276Id = :f276Id and a.sucTipo = :sucTipo "
            + "and a.sucNroId = :sucNroId order by a.turNroId asc")
    List<Acta> findByF276IdAndSucTipoAndSucNroId(@Param("f276Id") Integer f276Id,
            @Param("sucTipo") String sucTipo, @Param("sucNroId") Integer sucNroId);
}
