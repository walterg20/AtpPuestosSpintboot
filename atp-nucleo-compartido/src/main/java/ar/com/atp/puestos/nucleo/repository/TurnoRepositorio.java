package ar.com.atp.puestos.nucleo.repository;

import ar.com.atp.puestos.nucleo.domain.Turno;
import ar.com.atp.puestos.nucleo.domain.TurnoId;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link Turno} → tabla {@code PPRIM_TU} (lectura y escritura).
 *
 * <p><strong>T-103</strong>: reproduce la consulta lógica de {@code PPrim_BusTur} (spec 03-login
 * §3.9): {@code PPRIM_TU} donde {@code SucNroId = :sucNroId}, {@code SucTipo = :tipo},
 * {@code TurEstado = 'A'} y ({@code TurCPCId = :cpcId} <strong>o</strong> {@code :tipo = 'REC'}).
 *
 * <p>Sin {@code Order} ni {@code defined by} en el legacy ⇒ gana <strong>la primera</strong> fila
 * que cumpla. El índice físico es ({@code SucNroId}, {@code SucTipo}, {@code TurNroId}) ⇒ el orden
 * natural dentro del filtro es por {@code TurNroId}; se explicita para que el resultado sea
 * determinista.
 *
 * <p><strong>Módulo {@code 06-turnos}</strong>: se agregan el listado paginado de la sucursal
 * (R-TUR-001..003) y el máximo de {@code TurNroId} (R-TUR-006).
 */
public interface TurnoRepositorio extends JpaRepository<Turno, TurnoId> {

    /**
     * Turnos activos de la sucursal.
     *
     * @param sucNroId sucursal (puesto o receptoría)
     * @param tipo tipo de acceso ({@code PUE} / {@code REC})
     * @param cpcId comisión del turno (irrelevante para {@code REC})
     * @return la lista de turnos candidatos (el primero es el que gana); vacía si no hay
     */
    @Query("select t from Turno t where t.id.sucNroId = :sucNroId "
            + "and t.id.sucTipo = :tipo and t.turEstado = 'A' "
            + "and (t.turCpcId = :cpcId or :tipo = 'REC') order by t.id.turNroId asc")
    java.util.List<Turno> findTurnosActivos(@Param("sucNroId") Integer sucNroId,
            @Param("tipo") String tipo, @Param("cpcId") Integer cpcId);

    /**
     * Listado de los turnos de la sucursal, ordenado por la clave primaria y paginado
     * (R-TUR-001, R-TUR-003).
     *
     * <p>El orden es {@code SucNroId, SucTipo, TurNroId}: dentro del filtro por sucursal y tipo
     * queda el orden por {@code TurNroId}, que es el índice físico de la tabla.
     *
     * @param sucNroId sucursal del usuario ({@code Context.Sucursal})
     * @param sucTipo tipo de sucursal del usuario ({@code Context.Tipo})
     * @param pageable paginación (el módulo usa páginas de 10 filas, como el legacy)
     * @return la página de turnos
     */
    @Query("select t from Turno t where t.id.sucNroId = :sucNroId and t.id.sucTipo = :sucTipo "
            + "order by t.id.sucNroId asc, t.id.sucTipo asc, t.id.turNroId asc")
    Page<Turno> listarPorSucursal(@Param("sucNroId") Integer sucNroId,
            @Param("sucTipo") String sucTipo, Pageable pageable);

    /**
     * Máximo {@code TurNroId} de la sucursal/tipo (R-TUR-006, {@code PPrim_NumTur}).
     *
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @return el máximo, o {@code null} si la sucursal no tiene turnos
     */
    @Query("select max(t.id.turNroId) from Turno t "
            + "where t.id.sucNroId = :sucNroId and t.id.sucTipo = :sucTipo")
    Integer maximoTurNroId(@Param("sucNroId") Integer sucNroId, @Param("sucTipo") String sucTipo);

    /** Lectura por clave primaria compuesta (R-TUR-012, paso 1). */
    Optional<Turno> findById(TurnoId id);

    /**
     * Turnos <strong>abiertos</strong> de la sucursal y tipo del contexto (R-TURNO-002, R-TURNO-005).
     *
     * <p>Reproduce el {@code ControlWhere} de {@code TurnosListadoAbiertos}: {@code TurEstado = 'A'},
     * {@code SucNroId = :sucNroId}, {@code SucTipo = :sucTipo}, ordenado por {@code SucNroId,
     * SucTipo, TurNroId}.
     *
     * @param sucNroId sucursal del contexto
     * @param sucTipo tipo de sucursal del contexto
     * @param pageable paginación (10 filas por defecto)
     * @return la página de turnos abiertos
     */
    @Query("select t from Turno t where t.id.sucNroId = :sucNroId "
            + "and t.id.sucTipo = :sucTipo and t.turEstado = 'A' "
            + "order by t.id.sucNroId asc, t.id.sucTipo asc, t.id.turNroId asc")
    Page<Turno> listarAbiertosPorSucursal(@Param("sucNroId") Integer sucNroId,
            @Param("sucTipo") String sucTipo, Pageable pageable);
}
