package ar.gob.chaco.atp.puestoscamineros.nucleo.repository;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Area;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de {@link Area} → tabla {@code Areas} (R-001, R-020). PK {@code AreaId} = {@link BigDecimal}. */
public interface AreaRepository extends JpaRepository<Area, BigDecimal> {
}
