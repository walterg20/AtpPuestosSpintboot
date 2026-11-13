package ar.gob.chaco.atp.puestoscamineros.repository;

import ar.gob.chaco.atp.puestoscamineros.domain.Contribuyente;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContribuyenteRepository extends JpaRepository<Contribuyente, BigDecimal> {
    Contribuyente findByConCuit(BigDecimal conCuit);
}