package ar.com.atp.puestos.puestos.repository;

import ar.com.atp.puestos.puestos.domain.Contribuyente;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContribuyenteRepository extends JpaRepository<Contribuyente, BigDecimal> {
    Contribuyente findByConCuit(BigDecimal conCuit);
}