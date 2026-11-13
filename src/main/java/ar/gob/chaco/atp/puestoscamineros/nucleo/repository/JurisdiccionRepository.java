package ar.gob.chaco.atp.puestoscamineros.nucleo.repository;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Jurisdiccion;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de {@link Jurisdiccion} → tabla {@code Jurisdic} (R-001, R-020). */
public interface JurisdiccionRepository extends JpaRepository<Jurisdiccion, Integer> {
}
