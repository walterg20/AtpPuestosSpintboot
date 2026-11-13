package ar.gob.chaco.atp.puestoscamineros.nucleo.repository;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Persona;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link Persona} → tabla {@code PERSONAS} (sólo lectura).
 *
 * <p><strong>T-103</strong>: reproduce la consulta lógica de {@code Seg_BusPer} (spec 03-login
 * §3.9): {@code PERSONAS} donde {@code PerCodUsu = :usuario}, con el orden
 * {@code defined by PerNomyApe} ⇒ gana la fila de <strong>mayor nombre y apellido</strong> entre
 * las que coinciden. Sin coincidencia ⇒ la lista vacía (el legacy devuelve {@code -1}).
 *
 * <p>El identificador se compara con {@code UPPER()} en ambos lados para no depender de la
 * collation (mismo criterio que {@code UsuarioRepository.findByUsuId}, D-LOG-05).
 */
public interface PersonaRepository extends JpaRepository<Persona, Integer> {

    /**
     * Personas cuyo identificador de usuario en claro coincide con el dado.
     *
     * @param usuario identificador en claro, en mayúsculas y con relleno a 10
     * @return la lista ordenada por {@code PerNomyApe} descendente (el primero es el que gana)
     */
    @Query("select p from Persona p where upper(p.perCodUsu) = upper(:usuario) "
            + "order by p.perNomyApe desc")
    List<Persona> findByPerCodUsu(@Param("usuario") String usuario);
}
