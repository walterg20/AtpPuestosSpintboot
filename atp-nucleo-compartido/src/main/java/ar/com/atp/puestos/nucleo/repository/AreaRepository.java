package ar.com.atp.puestos.nucleo.repository;

import ar.com.atp.puestos.nucleo.domain.Area;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de {@link Area} → tabla {@code Areas} (R-001, R-020). PK {@code AreaId} = {@link BigDecimal}. */
public interface AreaRepository extends JpaRepository<Area, BigDecimal> {
}
