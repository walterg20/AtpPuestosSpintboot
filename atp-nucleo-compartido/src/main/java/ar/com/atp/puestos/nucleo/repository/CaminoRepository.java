package ar.com.atp.puestos.nucleo.repository;

import ar.com.atp.puestos.nucleo.domain.Camino;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de {@link Camino} → tabla {@code CAMINOS} (R-001, R-020). */
public interface CaminoRepository extends JpaRepository<Camino, String> {
}
