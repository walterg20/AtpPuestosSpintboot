package ar.com.atp.puestos.nucleo.repository;

import ar.com.atp.puestos.nucleo.domain.ComisionIntegrante;
import ar.com.atp.puestos.nucleo.domain.ComisionIntegranteId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de {@link ComisionIntegrante} → tabla {@code COMISIO1} (sólo lectura).
 *
 * <p>Nivel de detalle de las comisiones: los integrantes. La PK es compuesta
 * ({@code PueCod}, {@code CPCId}, {@code CPCPerId}) — R-LOG-034.
 */
public interface ComisionIntegranteRepository
        extends JpaRepository<ComisionIntegrante, ComisionIntegranteId> {
}
