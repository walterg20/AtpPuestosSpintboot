package ar.com.atp.puestos.nucleo.repository;

import ar.com.atp.puestos.nucleo.domain.SnSinDev;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de {@link SnSinDev} → tabla {@code SNSINDEV} (módulo {@code 06-turnos}, R-TUR-016).
 *
 * <p>La PK {@code SNSinNum} es {@code IDENTITY} en la base, de modo que el número identificatorio
 * lo asigna el motor y el servicio no lo escribe.
 */
public interface SnSinDevRepositorio extends JpaRepository<SnSinDev, BigDecimal> {
}
