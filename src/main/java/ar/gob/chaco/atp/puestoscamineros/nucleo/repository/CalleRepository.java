package ar.gob.chaco.atp.puestoscamineros.nucleo.repository;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Calle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de {@link Calle} → tabla {@code CALLES} (R-001, R-020). */
public interface CalleRepository extends JpaRepository<Calle, Integer> {

    /** Calles cuyo nombre contiene {@code texto} (búsqueda parcial, sin distinguir mayúsculas). */
    List<Calle> findByCallNomContainingIgnoreCaseOrderByCallNomAsc(String texto);
}
