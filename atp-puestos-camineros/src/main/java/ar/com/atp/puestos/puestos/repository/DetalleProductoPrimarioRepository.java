package ar.com.atp.puestos.puestos.repository;

import ar.com.atp.puestos.puestos.domain.DetalleProductoPrimario;
import ar.com.atp.puestos.puestos.domain.DetalleProductoPrimarioId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DetalleProductoPrimarioRepository extends JpaRepository<DetalleProductoPrimario, DetalleProductoPrimarioId> {

    @Query("select d from DetalleProductoPrimario d where d.id.conCuit = :conCuit and d.id.dppSucu = :dppSucu and d.id.calPerFisc = :calPerFisc and d.id.dppSecu = :dppSecu and d.id.dppNroForm = :dppNroForm")
    Optional<DetalleProductoPrimario> findByConCuitAndDppSucuAndCalPerFiscAndDppSecuAndDppNroForm(
            @Param("conCuit") BigDecimal conCuit, @Param("dppSucu") Short dppSucu,
            @Param("calPerFisc") Integer calPerFisc, @Param("dppSecu") Short dppSecu,
            @Param("dppNroForm") String dppNroForm);

    @Query("select d from DetalleProductoPrimario d where d.id.dppNroForm = :nroForm")
    List<DetalleProductoPrimario> findByDppNroForm(@Param("nroForm") String nroForm);
}
