package ar.com.atp.puestos.nucleo.repository;

import ar.com.atp.puestos.nucleo.domain.Calle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de {@link Calle} → tabla {@code CALLES} (R-001, R-020). */
public interface CalleRepository extends JpaRepository<Calle, Integer> {

    /** Calles cuyo nombre contiene {@code texto} (búsqueda parcial, sin distinguir mayúsculas). */
    List<Calle> findByCallNomContainingIgnoreCaseOrderByCallNomAsc(String texto);
}
