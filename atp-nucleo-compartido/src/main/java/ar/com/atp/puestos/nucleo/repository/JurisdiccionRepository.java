package ar.com.atp.puestos.nucleo.repository;

import ar.com.atp.puestos.nucleo.domain.Jurisdiccion;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de {@link Jurisdiccion} → tabla {@code Jurisdic} (R-001, R-020). */
public interface JurisdiccionRepository extends JpaRepository<Jurisdiccion, Integer> {
}
