package ar.com.atp.puestos.nucleo.repository;

import ar.com.atp.puestos.nucleo.domain.Localidad;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio de {@link Localidad} → tabla {@code Localida} (R-003, R-020, R-021). */
public interface LocalidadRepository extends JpaRepository<Localidad, Integer> {

    /** Localidades de una provincia (filtro por la FK lógica {@code ProvCod}). */
    List<Localidad> findByProvincia_ProvCodOrderByLocNomAsc(Integer provCod);

    /**
     * Todas las localidades con la provincia resuelta <strong>en la misma consulta</strong> (R-025).
     *
     * <p>El {@code left join fetch} es lo que hace tolerante la consulta a la FK lógica huérfana:
     * {@code Localida} tiene 30 filas con {@code ProvCod = 0}, valor inexistente en {@code PROVINCI}
     * (spec §4.3). Con una asociación LAZY sin fetch, Hibernate crea un proxy no nulo para esas
     * filas y su inicialización lanza {@code EntityNotFoundException} ⇒ {@code 500 ERROR_INTERNO}
     * (R-024). Con {@code left join fetch} la asociación queda en {@code null} para las filas
     * huérfanas y ninguna fila se descarta.
     */
    @Query("select l from Localidad l left join fetch l.provincia order by l.locNom asc")
    List<Localidad> findAllConProvincia();

    /**
     * Localidades de una provincia con la provincia resuelta en la misma consulta (R-021, R-025).
     *
     * <p>El filtro se aplica sobre la provincia asociada: las filas con {@code ProvCod} inexistente
     * quedan excluidas del resultado (mismo comportamiento que el INNER JOIN del método derivado
     * {@link #findByProvincia_ProvCodOrderByLocNomAsc}).
     */
    @Query("select l from Localidad l left join fetch l.provincia p "
            + "where p.provCod = :provCod order by l.locNom asc")
    List<Localidad> findByProvCodConProvincia(@Param("provCod") Integer provCod);

    /**
     * Una localidad por PK con la provincia resuelta en la misma consulta (R-020, R-024).
     *
     * <p>Un {@code findById} plano dejaría la provincia como proxy LAZY; al leerla para una
     * localidad con FK huérfana fallaría con {@code EntityNotFoundException} ⇒ {@code 500}.
     */
    @Query("select l from Localidad l left join fetch l.provincia where l.locCod = :locCod")
    Optional<Localidad> findByIdConProvincia(@Param("locCod") Integer locCod);
}
