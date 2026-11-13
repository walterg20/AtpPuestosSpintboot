package ar.gob.chaco.atp.puestoscamineros.nucleo.repository;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Comision;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.ComisionId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link Comision} → tabla {@code COMISION} (sólo lectura).
 *
 * <p><strong>T-132 / T-133 / D-LOG-20</strong>: los atributos <em>calculados</em> del legacy
 * ({@code CPCPerNyA} de los integrantes y {@code CPCResNyA} del responsable) <strong>no son
 * columnas</strong> (G-DATA 2026-10-22: 0 coincidencias en toda {@code Rentas}). Por eso las
 * consultas hacen el <strong>JOIN a {@code PERSONAS}</strong> que los resuelve:
 *
 * <ul>
 *   <li>{@code Seg_BusComi} ⇒ JOIN {@code COMISIO1} + JOIN {@code PERSONAS} por
 *       {@code CPCPerId}, con el orden {@code defined by CPCPerNyA} (⇒ gana la fila de <strong>mayor
 *       nombre y apellido</strong>) — R-LOG-036;</li>
 *   <li>{@code Seg_BusResponsable} ⇒ JOIN {@code PERSONAS} por {@code CPCResId}, con el orden
 *       {@code defined by CPCCierre} — R-LOG-035.</li>
 * </ul>
 *
 * <p>El descarte de la comisión: si {@code CPCFchHas} es <strong>igual</strong> al día actual y
 * {@code CPCHorHas} (un <strong>número</strong> en formato {@code HHMM}) es anterior a la hora
 * actual, la comisión se descarta (R-LOG-038). El descarte <strong>sólo</strong> aplica cuando
 * {@code CPCFchHas = hoy}.
 */
public interface ComisionRepository extends JpaRepository<Comision, ComisionId> {

    /**
     * Comisiones vigentes en las que la persona es <strong>integrante</strong> ({@code Seg_BusComi}).
     *
     * <p>Condiciones: {@code CPCFchDes <= hoy <= CPCFchHas}, {@code CPCCierre = 'N'} y el descarte
     * por hora vigente. El orden reproduce el {@code defined by CPCPerNyA} del legacy: gana la
     * comisión cuyo integrante tiene el <strong>mayor</strong> {@code PERSONAS.PerNomyApe}; el
     * {@code CPCId} descendente es el desempate determinista que el legacy no define (SA-LOG-15).
     *
     * @param perId persona integrante
     * @param hoy inicio del día actual
     * @param hora hora actual en formato {@code HHMM} (número)
     * @return las comisiones candidatas (la primera es la que gana); vacía si no hay ninguna
     */
    @Query("select c from Comision c "
            + "join ComisionIntegrante i on i.id.pueCod = c.id.pueCod and i.id.cpcId = c.id.cpcId "
            + "join Persona p on p.perId = i.id.cpcPerId "
            + "where exists (select x.id from ComisionIntegrante x "
            + "  where x.id.pueCod = c.id.pueCod and x.id.cpcId = c.id.cpcId "
            + "  and x.id.cpcPerId = :perId) "
            + "and c.cpcFchDes <= :hoy and c.cpcFchHas >= :hoy and c.cpcCierre = 'N' "
            + "and not (c.cpcFchHas = :hoy and c.cpcHorHas is not null and c.cpcHorHas < :hora) "
            + "order by p.perNomyApe desc, c.id.cpcId desc")
    List<Comision> findComisionesVigentesComoIntegrante(@Param("perId") Integer perId,
            @Param("hoy") LocalDateTime hoy, @Param("hora") Integer hora);

    /**
     * Comisiones vigentes en las que la persona es <strong>responsable</strong> (segunda búsqueda
     * de {@code Seg_BusComi}).
     *
     * <p>Mismas condiciones de fecha y cierre que la búsqueda como integrante. El orden
     * {@code defined by CPCCierre} del legacy se explicita (con el {@code CPCId} descendente como
     * desempate determinista).
     *
     * @param perId persona responsable
     * @param hoy inicio del día actual
     * @param hora hora actual en formato {@code HHMM} (número)
     * @return las comisiones candidatas (la primera es la que gana); vacía si no hay ninguna
     */
    @Query("select c from Comision c "
            + "where c.cpcResId = :perId "
            + "and c.cpcFchDes <= :hoy and c.cpcFchHas >= :hoy and c.cpcCierre = 'N' "
            + "and not (c.cpcFchHas = :hoy and c.cpcHorHas is not null and c.cpcHorHas < :hora) "
            + "order by c.cpcCierre desc, c.id.cpcId desc")
    List<Comision> findComisionesVigentesComoResponsable(@Param("perId") Integer perId,
            @Param("hoy") LocalDateTime hoy, @Param("hora") Integer hora);

    /**
     * Comisión concreta con el nombre de su responsable ({@code Seg_BusResponsable}).
     *
     * <p>El JOIN a {@code PERSONAS} es <strong>obligatorio</strong>: {@code CPCResNyA} no existe
     * como columna (D-LOG-20). El JOIN es {@code left} para reproducir los valores por defecto del
     * legacy ({@code 0} / {@code Sin Comisión}) cuando la comisión no tiene responsable.
     *
     * @param pueCod puesto caminerero de la comisión
     * @param cpcId número de comisión
     * @return la pareja (comisión, persona responsable o {@code null})
     */
    @Query("select c, p from Comision c left join Persona p on p.perId = c.cpcResId "
            + "where c.id.pueCod = :pueCod and c.id.cpcId = :cpcId")
    List<Object[]> findComisionConResponsable(@Param("pueCod") Integer pueCod,
            @Param("cpcId") Integer cpcId);

    /**
     * ¿La persona fue responsable de alguna comisión <strong>cerrada</strong>?
     *
     * <p>Rama {@code BuscarHistoriaComision} de {@code Seg_ControlAcceso} (R-LOG-015): con
     * {@code CPCCierre = 'S'}.
     *
     * @param perId persona
     * @return la cantidad de comisiones cerradas a cargo de la persona
     */
    @Query("select count(c) from Comision c where c.cpcResId = :perId and c.cpcCierre = 'S'")
    long countComisionesCerradasComoResponsable(@Param("perId") Integer perId);

    /**
     * Estado de la comisión ({@code CPCEstado}) de un puesto y un número de comisión concretos
     * (módulo {@code 06-turnos}, R-TUR-010, T-TUR-A3).
     *
     * <p>El legacy lo lee con el {@code Sub 'ComisionVerEstado'} de {@code TurnosListado}: un
     * {@code For Each} sobre {@code COMISION} por {@code PueCod} y {@code CPCId}. Si la comisión
     * <strong>no existe</strong>, o su estado es {@code NULL}, el alta de turno se permite igual
     * (D-TUR-002).
     *
     * @param pueCod puesto caminerero de la comisión ({@code Context.Sucursal})
     * @param cpcId número de comisión ({@code Context.Comision})
     * @return el estado ({@code 'A'}, {@code 'P'} o {@code 'C'}); vacío si la comisión no existe
     *         o su estado es {@code NULL}
     */
    @Query("select trim(c.cpcEstado) from Comision c "
            + "where c.id.pueCod = :pueCod and c.id.cpcId = :cpcId")
    Optional<String> estadoComision(@Param("pueCod") Integer pueCod, @Param("cpcId") Integer cpcId);

    /**
     * Listado de comisiones con el nombre del responsable (JOIN a {@code PERSONAS}) para el módulo
     * {@code 13-comisiones} (R-COM-001..008).
     *
     * <p>Filtro legacy: {@code PueCod = :pueCod} (cuando no null) y {@code CPCResId = :cpcResId}
     * (cuando no null). Orden {@code CPCId desc}.
     */
    @Query("select c, p from Comision c left join Persona p on p.perId = c.cpcResId "
            + "where (:pueCod is null or c.id.pueCod = :pueCod) "
            + "and (:cpcResId is null or c.cpcResId = :cpcResId)")
    Page<Object[]> findComisionesConResponsable(@Param("pueCod") Integer pueCod,
            @Param("cpcResId") Integer cpcResId, Pageable pageable);

    /**
     * Listado paginado de comisiones con filtro legacy (R-COM-001..008), sin JOIN a Persona.
     */
    @Query("select c from Comision c "
            + "where (:pueCod is null or c.id.pueCod = :pueCod) "
            + "and (:cpcResId is null or c.cpcResId = :cpcResId)")
    Page<Comision> findComisionesFiltradas(@Param("pueCod") Integer pueCod,
            @Param("cpcResId") Integer cpcResId, Pageable pageable);

    /**
     * Resuelve la descripción del puesto caminero ({@code PUEPPRIM.PueDesc}) por código
     * (SA-COM-03: consulta nativa, sin crear entity {@code Puesto}).
     */
    @Query(value = "SELECT PueDesc FROM PUEPPRIM WHERE PueCod = :pueCod", nativeQuery = true)
    Optional<String> findPueDescByPueCod(@Param("pueCod") Integer pueCod);
}
