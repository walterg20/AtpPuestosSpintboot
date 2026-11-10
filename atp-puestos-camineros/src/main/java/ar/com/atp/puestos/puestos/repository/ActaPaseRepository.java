package ar.com.atp.puestos.puestos.repository;

import ar.com.atp.puestos.puestos.domain.ActaPase;
import ar.com.atp.puestos.puestos.domain.ActaPaseId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de los pases de un Acta → tabla {@code F2276PAS}
 * (spec {@code 09-formularios-acta} §3.4, plan §3.4, T-ACTA-113).
 *
 * <p>El índice correlativo del pase ({@code F276PPaseI}) lo calcula el legacy recorriendo los
 * pases existentes del Acta ({@code F2276_RegPasePuestoV2}).
 */
public interface ActaPaseRepository extends JpaRepository<ActaPase, ActaPaseId> {

    /**
     * Pases de un Acta, ordenados por índice correlativo.
     *
     * @param f276Id número de Acta
     * @param sucTipo tipo de sucursal
     * @param sucNroId sucursal
     * @return los pases del Acta
     */
    @Query("select p from ActaPase p where p.f276Id = :f276Id and p.sucTipo = :sucTipo "
            + "and p.sucNroId = :sucNroId order by p.f276PPaseI asc")
    List<ActaPase> findByActa(@Param("f276Id") Integer f276Id, @Param("sucTipo") String sucTipo,
            @Param("sucNroId") Integer sucNroId);

    /**
     * Índice correlativo máximo de los pases de un Acta ({@code null} si no hay pases).
     *
     * @param f276Id número de Acta
     * @param sucTipo tipo de sucursal
     * @param sucNroId sucursal
     * @return el índice máximo o {@code null}
     */
    @Query("select max(p.f276PPaseI) from ActaPase p where p.f276Id = :f276Id "
            + "and p.sucTipo = :sucTipo and p.sucNroId = :sucNroId")
    Integer maxIndicePase(@Param("f276Id") Integer f276Id, @Param("sucTipo") String sucTipo,
            @Param("sucNroId") Integer sucNroId);
}
