package ar.gob.chaco.atp.puestoscamineros.nucleo.repository;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Formulario2276;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Lote227;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de sólo lectura sobre los formularios 2276 del legacy (spec {@code 06-turnos} §2.2).
 *
 * <p>Cubre las dos reglas de negocio que necesitan {@code F2276}:
 *
 * <ul>
 *   <li><strong>R-TUR-013</strong> — detección de formularios abiertos: un formulario del turno cuyo
 *       lote ({@code LOTES227}) tiene {@code LF276Estad = 'C'} y {@code LF276FchHa <= hoy}.</li>
 *   <li><strong>R-TUR-015</strong> — totalización para el SN del volante de pago.</li>
 * </ul>
 *
 * <p>La totalización se hace con SQL nativo porque {@code F276ImIBTot} y {@code F276ImCMTot}
 * <strong>no son columnas</strong> de {@code F2276}: son atributos calculados de la KB
 * ({@code sum(F276RImpIB)} y {@code sum(F276RImpCM)} sobre el detalle {@code F22761}, y sólo si
 * {@code F276GenWeb <> 'A'}). La consulta reproduce esas dos fórmulas.
 */
public interface Formulario2276Repositorio extends JpaRepository<Formulario2276, Integer> {

    /**
     * Cuenta los formularios del turno cuyo lote está cerrado y vencido (R-TUR-013).
     *
     * <p>Equivale al {@code PPrim_EstadoDeForm} del legacy: {@code &LoteAbierto = 1} si existe
     * alguna fila con {@code LF276Estad = 'C'} y {@code today() >= LF276FchHa}.
     *
     * @param sucNroId sucursal del turno
     * @param sucTipo tipo de sucursal del turno
     * @param turNroId número de turno
     * @param hoy inicio del día actual (el legacy compara con {@code today()})
     * @return la cantidad de formularios abiertos (0 = se puede cerrar el turno)
     */
    @Query("select count(l) from Lote227 l where l.lf276Estad = 'C' and l.lf276FchHa <= :hoy "
            + "and l.lf276Id in (select f.lf276Id from Formulario2276 f "
            + "  where f.sucNroId = :sucNroId and f.sucTipo = :sucTipo and f.turNroId = :turNroId)")
    long contarFormulariosAbiertos(@Param("sucNroId") Integer sucNroId,
            @Param("sucTipo") String sucTipo, @Param("turNroId") Integer turNroId,
            @Param("hoy") LocalDateTime hoy);

    /**
     * Totaliza los importes de los formularios del turno (R-TUR-015).
     *
     * <p>Reproduce el {@code For Each} de {@code TurnoGenerarSN}: se excluyen los formularios
     * anulados ({@code F276Estad = 'A'}) y los importes de ingresos brutos y cosecha mecánica se
     * obtienen sumando el detalle ({@code F22761}) de cada formulario, igual que las fórmulas
     * {@code F276ImIBTot} y {@code F276ImCMTot} de la KB (que además sólo aplican si
     * {@code F276GenWeb <> 'A'}).
     *
     * @param sucNroId sucursal del turno
     * @param sucTipo tipo de sucursal del turno
     * @param turNroId número de turno
     * @return una fila con los seis totales (ver {@code TotalesFormulario2276#deFila})
     */
    @Query(value = "SELECT "
            + "ISNULL(SUM(f.F276ImpuFl), 0) AS totFletes, "
            + "ISNULL(SUM(f.F276AdCMTo), 0) AS totAdCM, "
            + "ISNULL(SUM(f.F276AdicFl), 0) AS totAdFletes, "
            + "ISNULL(SUM(f.F276AdIBTo), 0) AS totAdIB, "
            + "ISNULL((SELECT SUM(d.F276RImpIB) FROM F22761 d "
            + "   INNER JOIN F2276 f2 ON f2.F276Id = d.F276Id "
            + "   AND f2.SucTipo = d.SucTipo AND f2.SucNroId = d.SucNroId "
            + "   WHERE f2.SucNroId = :sucNroId AND f2.SucTipo = :sucTipo "
            + "   AND f2.TurNroId = :turNroId "
            + "   AND (f2.F276Estad IS NULL OR f2.F276Estad <> 'A') "
            + "   AND (f2.F276GenWeb IS NULL OR f2.F276GenWeb <> 'A')), 0) AS totIB, "
            + "ISNULL((SELECT SUM(d.F276RImpCM) FROM F22761 d "
            + "   INNER JOIN F2276 f2 ON f2.F276Id = d.F276Id "
            + "   AND f2.SucTipo = d.SucTipo AND f2.SucNroId = d.SucNroId "
            + "   WHERE f2.SucNroId = :sucNroId AND f2.SucTipo = :sucTipo "
            + "   AND f2.TurNroId = :turNroId "
            + "   AND (f2.F276Estad IS NULL OR f2.F276Estad <> 'A') "
            + "   AND (f2.F276GenWeb IS NULL OR f2.F276GenWeb <> 'A')), 0) AS totCM "
            + "FROM F2276 f "
            + "WHERE f.SucNroId = :sucNroId AND f.SucTipo = :sucTipo AND f.TurNroId = :turNroId "
            + "AND (f.F276Estad IS NULL OR f.F276Estad <> 'A')",
            nativeQuery = true)
    List<Object[]> totalizarTurno(@Param("sucNroId") Integer sucNroId,
            @Param("sucTipo") String sucTipo, @Param("turNroId") Integer turNroId);
    List<Formulario2276> findBySucNroIdAndSucTipoAndTurNroIdOrderByF276IdAsc(Integer sucNroId, String sucTipo, Integer turNroId);

    /**
     * Igual que {@link #findBySucNroIdAndSucTipoAndTurNroIdOrderByF276IdAsc(Integer, String, Integer)}
     * pero con paginación (Fase 3 — solapa LISTADO ACTAS).
     *
     * @param sucNroId sucursal del turno
     * @param sucTipo   tipo de sucursal del turno
     * @param turNroId  número de turno
     * @param pageable  paginación pedida por la vista
     * @return página de formularios ordenados por {@code F276Id} asc
     */
    Page<Formulario2276> findBySucNroIdAndSucTipoAndTurNroIdOrderByF276IdAsc(Integer sucNroId, String sucTipo, Integer turNroId, Pageable pageable);
}