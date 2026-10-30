package ar.com.atp.puestos.puestos.repository;

import ar.com.atp.puestos.puestos.domain.PaseGuia;
import ar.com.atp.puestos.puestos.domain.PaseGuiaId;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaseGuiaRepository extends JpaRepository<PaseGuia, PaseGuiaId> {

    @Query("select p from PaseGuia p where p.id.conCuit = :conCuit and p.id.dppSucu = :dppSucu and p.id.calPerFisc = :calPerFisc and p.id.dppSecu = :dppSecu and p.id.dppNroForm = :dppNroForm")
    List<PaseGuia> findByConCuitAndDppSucuAndCalPerFiscAndDppSecuAndDppNroForm(
            @Param("conCuit") BigDecimal conCuit, @Param("dppSucu") Short dppSucu,
            @Param("calPerFisc") Integer calPerFisc, @Param("dppSecu") Short dppSecu,
            @Param("dppNroForm") String dppNroForm);

    @Query("select count(p) from PaseGuia p where p.id.conCuit = :conCuit and p.id.dppSucu = :dppSucu and p.id.calPerFisc = :calPerFisc and p.id.dppSecu = :dppSecu and p.id.dppNroForm = :dppNroForm and p.dppPaseEst = :est and p.sucTipo = :tipo")
    long countByConCuitAndDppSucuAndCalPerFiscAndDppSecuAndDppNroFormAndDppPaseEstAndSucTipo(
            @Param("conCuit") BigDecimal conCuit, @Param("dppSucu") Short dppSucu,
            @Param("calPerFisc") Integer calPerFisc, @Param("dppSecu") Short dppSecu,
            @Param("dppNroForm") String dppNroForm, @Param("est") String dppPaseEst,
            @Param("tipo") String sucTipo);
}