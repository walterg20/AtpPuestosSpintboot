package ar.com.atp.puestos.puestos.repository;

import ar.com.atp.puestos.puestos.domain.VersionSistema;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de las versiones del sistema → tabla {@code VERSIONE}
 * (spec {@code 09-formularios-acta} §3.4, plan §3.4, T-ACTA-116).
 *
 * <p>El legacy {@code BuscarVersionActual} toma la fila con {@code VerApliId = 'SWP'} ordenada por
 * {@code VerDescr}.
 */
public interface VersionSistemaRepositorio extends JpaRepository<VersionSistema, String> {

    /**
     * Versión de una aplicación, ordenada por descripción.
     *
     * @param verApliId aplicación ({@code 'SWP'} para Puestos Camineros)
     * @return las versiones registradas (a lo sumo una por aplicación)
     */
    @Query("select v from VersionSistema v where v.verApliId = :verApliId order by v.verDescr asc")
    List<VersionSistema> buscarPorAplicacion(@Param("verApliId") String verApliId);
}
