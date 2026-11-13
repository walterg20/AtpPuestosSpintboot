package ar.gob.chaco.atp.puestoscamineros.repository;

import ar.gob.chaco.atp.puestoscamineros.domain.ContadorFormulario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio del contador de numeración → tabla {@code GES_SUCN}
 * (spec {@code 09-formularios-acta} §3.4, plan §3.4, T-ACTA-114).
 *
 * <p><strong>Incremento atómico</strong> (D-ACTA-19): el legacy lee la fila, suma uno y la
 * escribe <strong>sin bloquearla</strong>; dos altas simultáneas podrían obtener el mismo
 * {@code F276Id} y violar la PK {@code (F276Id, SucTipo, SucNroId)}. El {@code UPDATE ... SET
 * SucUltNum = SucUltNum + 1} delega el incremento al motor y elimina la carrera.
 *
 * <p>La lectura del valor se hace con una consulta <strong>nativa</strong> para no devolver un
 * valor cacheado por el contexto de persistencia (la actualización nativa no lo invalida).
 */
public interface ContadorFormularioRepositorio
        extends JpaRepository<ContadorFormulario, ContadorFormulario.ContadorId> {

    /**
     * Incrementa el contador de forma atómica.
     *
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @param sucNuForm número de formulario ({@code 2276} para Actas)
     * @return la cantidad de filas afectadas ({@code 0} si la fila no existe)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE GES_SUCN SET SucUltNum = SucUltNum + 1 "
            + "WHERE SucNroId = :sucNroId AND SucTipo = :sucTipo AND SucNuForm = :sucNuForm",
            nativeQuery = true)
    int incrementarAtomico(@Param("sucNroId") Integer sucNroId, @Param("sucTipo") String sucTipo,
            @Param("sucNuForm") Integer sucNuForm);

    /**
     * Valor actual del contador (lectura nativa, sin caché del contexto de persistencia).
     *
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @param sucNuForm número de formulario
     * @return el último número emitido o {@code null} si la fila no existe
     */
    @Query(value = "SELECT SucUltNum FROM GES_SUCN WHERE SucNroId = :sucNroId "
            + "AND SucTipo = :sucTipo AND SucNuForm = :sucNuForm", nativeQuery = true)
    Integer valorActual(@Param("sucNroId") Integer sucNroId, @Param("sucTipo") String sucTipo,
            @Param("sucNuForm") Integer sucNuForm);

    /**
     * Fila del contador de la sucursal.
     *
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @param sucNuForm número de formulario
     * @return la fila o {@link Optional#empty()}
     */
    @Query("select c from ContadorFormulario c where c.sucNroId = :sucNroId "
            + "and c.sucTipo = :sucTipo and c.sucNuForm = :sucNuForm")
    Optional<ContadorFormulario> buscar(@Param("sucNroId") Integer sucNroId,
            @Param("sucTipo") String sucTipo, @Param("sucNuForm") Integer sucNuForm);
}
