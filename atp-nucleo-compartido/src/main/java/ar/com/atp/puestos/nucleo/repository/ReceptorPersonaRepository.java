package ar.com.atp.puestos.nucleo.repository;

import ar.com.atp.puestos.nucleo.domain.ReceptorPersona;
import ar.com.atp.puestos.nucleo.domain.ReceptorPersonaId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link ReceptorPersona} → tabla {@code RECEPTO1} (sólo lectura).
 *
 * <p>El detalle de la asignación persona ↔ receptoría. La PK es compuesta
 * ({@code ReceId}, {@code PerId}) — R-LOG-034.
 */
public interface ReceptorPersonaRepository extends JpaRepository<ReceptorPersona, ReceptorPersonaId> {

    /**
     * Asignaciones de una persona, en el orden natural del legacy (la primera gana).
     *
     * @param perId persona
     * @return las asignaciones ordenadas por {@code ReceId}
     */
    @Query("select rp from ReceptorPersona rp where rp.id.perId = :perId order by rp.id.receId asc")
    List<ReceptorPersona> findByPerId(@Param("perId") Integer perId);
}
