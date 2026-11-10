package ar.com.atp.puestos.nucleo.repository;

import ar.com.atp.puestos.nucleo.domain.ComisionIntegrante;
import ar.com.atp.puestos.nucleo.domain.ComisionIntegranteId;
import ar.com.atp.puestos.nucleo.domain.Persona;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link ComisionIntegrante} → tabla {@code COMISIO1} (sólo lectura).
 *
 * <p>Nivel de detalle de las comisiones: los integrantes. La PK es compuesta
 * ({@code PueCod}, {@code CPCId}, {@code CPCPerId}) — R-LOG-034.
 */
public interface ComisionIntegranteRepository
        extends JpaRepository<ComisionIntegrante, ComisionIntegranteId> {

    /**
     * Integrantes de una comisión con el nombre y apellido de la persona (JOIN a {@code PERSONAS}),
     * ordenados por {@code PerNomyApe} (trim) ascendente (R-COM-014, R-COM-015, SA-COM-06).
     */
    @Query("select i, p from ComisionIntegrante i left join Persona p on p.perId = i.id.cpcPerId "
            + "where i.id.pueCod = :pueCod and i.id.cpcId = :cpcId "
            + "order by trim(p.perNomyApe) asc")
    List<Object[]> findIntegrantesConNombre(@Param("pueCod") Integer pueCod,
            @Param("cpcId") Integer cpcId);
}
