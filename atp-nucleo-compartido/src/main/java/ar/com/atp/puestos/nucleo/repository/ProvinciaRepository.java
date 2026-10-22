package ar.com.atp.puestos.nucleo.repository;

import ar.com.atp.puestos.nucleo.domain.Provincia;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de {@link Provincia} → tabla {@code PROVINCI} (R-001, R-020). */
public interface ProvinciaRepository extends JpaRepository<Provincia, Integer> {
}
