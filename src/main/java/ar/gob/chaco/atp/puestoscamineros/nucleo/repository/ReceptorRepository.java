package ar.gob.chaco.atp.puestoscamineros.nucleo.repository;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Receptor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link Receptor} → tabla {@code RECEPTOR} (sólo lectura).
 *
 * <p><strong>T-103 / T-133</strong>: reproduce la consulta lógica de {@code Seg_BusRece}
 * (spec 03-login §3.9): {@code RECEPTO1} donde {@code PerId = :perId} <strong>JOIN
 * {@code RECEPTOR}</strong> por {@code ReceId} para obtener {@code ReceDsc}.
 *
 * <p>El legacy ordena por {@code PerId} (el {@code defined by RecUsuFchD} está comentado) ⇒ gana
 * <strong>la primera</strong> fila por persona. Como el índice físico de {@code RECEPTO1} es
 * ({@code ReceId}, {@code PerId}), el orden natural dentro de un mismo {@code PerId} es por
 * {@code ReceId}; se explicita para que el resultado sea determinista.
 */
public interface ReceptorRepository extends JpaRepository<Receptor, Integer> {

    /**
     * Receptorías asignadas a una persona, en el orden del legacy.
     *
     * @param perId persona
     * @return la lista de receptorías (la primera es la que gana); vacía si no hay ninguna
     */
    @Query("select rec from ReceptorPersona rp join Receptor rec on rec.receId = rp.id.receId "
            + "where rp.id.perId = :perId order by rp.id.receId asc")
    List<Receptor> findReceptoresDePersona(@Param("perId") Integer perId);
}
