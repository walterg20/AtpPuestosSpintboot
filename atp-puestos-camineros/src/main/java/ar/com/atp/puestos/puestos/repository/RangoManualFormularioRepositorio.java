package ar.com.atp.puestos.puestos.repository;

import ar.com.atp.puestos.puestos.domain.RangoManualFormulario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de los rangos manuales → tabla {@code FORMSUCU}
 * (spec {@code 09-formularios-acta} §3.4, plan §3.4, T-ACTA-115).
 *
 * <p>Sólo interviene en la detección de <strong>números manuales</strong> (D-ACTA-20): el rango
 * manual es <strong>[{@code ForRecIni}, {@code ForRecIni} + {@code ForRecCant})</strong>, tal como
 * lo verifican {@code ProPri_VerManual} y {@code PPrim_VerificaGenMan}.
 */
public interface RangoManualFormularioRepositorio
        extends JpaRepository<RangoManualFormulario, RangoManualFormulario.RangoId> {

    /**
     * Rangos manuales de la sucursal para un formulario, ordenados por inicio de rango.
     *
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @param sucNuForm número de formulario ({@code 2276} para Actas)
     * @return los rangos registrados
     */
    @Query("select r from RangoManualFormulario r where r.sucNroId = :sucNroId "
            + "and r.sucTipo = :sucTipo and r.sucNuForm = :sucNuForm order by r.forRecIni asc")
    List<RangoManualFormulario> findBySucursalYFormulario(@Param("sucNroId") Integer sucNroId,
            @Param("sucTipo") String sucTipo, @Param("sucNuForm") Integer sucNuForm);

    /**
     * ¿El número cae dentro de un rango manual? (equivalente de {@code PPrim_VerificaGenMan}).
     *
     * @param sucNroId sucursal
     * @param sucTipo tipo de sucursal
     * @param sucNuForm número de formulario
     * @param f276Id número a verificar
     * @return la cantidad de rangos que contienen el número ({@code 0} = no es manual)
     */
    @Query("select count(r) from RangoManualFormulario r where r.sucNroId = :sucNroId "
            + "and r.sucTipo = :sucTipo and r.sucNuForm = :sucNuForm "
            + "and r.forRecIni <= :f276Id and r.forRecIni + r.forRecCant > :f276Id")
    long contarSiEsManual(@Param("sucNroId") Integer sucNroId, @Param("sucTipo") String sucTipo,
            @Param("sucNuForm") Integer sucNuForm, @Param("f276Id") Integer f276Id);
}
