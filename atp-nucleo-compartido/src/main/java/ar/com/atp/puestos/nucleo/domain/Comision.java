package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code Comisiones} (raíz) → tabla física {@code COMISION} (spec 03-login
 * §3.1.1, plan §5.3).
 *
 * <p>La PK física es compuesta ({@code PueCod}, {@code CPCId}) — R-LOG-034. El login lee las 6
 * columnas de vigencia/cierre: {@code CPCFchDes} y {@code CPCFchHas} ({@code datetime}),
 * {@code CPCHorDes} y {@code CPCHorHas} ({@code int} — la hora es un <strong>número</strong> en
 * formato {@code HHMM}, no una fecha-hora), {@code CPCCierre} ({@code char(1)}) y {@code CPCResId}
 * ({@code int}, el responsable).
 *
 * <p><strong>{@code CPCResNyA} NO es una columna</strong>: es un atributo <em>calculado</em> de la
 * KB (G-DATA 2026-10-22: 0 coincidencias en toda la base). El nombre del responsable se resuelve
 * por <strong>JOIN a {@code PERSONAS}</strong> ({@code PerNomyApe}) en el repositorio
 * (D-LOG-20, R-LOG-035).
 *
 * <p><strong>Módulo {@code 06-turnos}</strong>: se agregan {@code CPCEstado} ({@code char(1)}) y
 * {@code CPCFchCie} ({@code datetime}), que el alta de turno consulta para rechazar la apertura
 * cuando la comisión está cerrada (R-TUR-010). El resto de las columnas huérfanas
 * ({@code CPCFeLog}, {@code CPCUsuLog}, {@code CPCSnNum}, {@code CPCFePag}) sigue sin mapear: el
 * módulo no las escribe.
 */
@Entity
@Table(name = "COMISION")
public class Comision {

    /** PK compuesta: puesto caminerero y número de comisión. */
    @EmbeddedId
    private ComisionId id;

    /** Comienzo de la vigencia ({@code datetime}). */
    @Column(name = "CPCFchDes")
    private LocalDateTime cpcFchDes;

    /** Fin de la vigencia ({@code datetime}). */
    @Column(name = "CPCFchHas")
    private LocalDateTime cpcFchHas;

    /** Hora de inicio ({@code int}, formato {@code HHMM}). */
    @Column(name = "CPCHorDes")
    private Integer cpcHorDes;

    /** Hora de fin ({@code int}, formato {@code HHMM}). */
    @Column(name = "CPCHorHas")
    private Integer cpcHorHas;

    /** {@code 'N'} = abierta, {@code 'S'} = cerrada. */
    @ColumnTransformer(read = "RTRIM(CPCCierre)")
    @Column(name = "CPCCierre", length = 1)
    private String cpcCierre;

    /** Persona responsable de la comisión ({@code int}); {@code 0} si no hay responsable. */
    @Column(name = "CPCResId")
    private Integer cpcResId;

    /**
     * Estado de la comisión ({@code char(1)}): {@code 'A'}, {@code 'P'} o {@code 'C'}.
     *
     * <p>Es el valor que el legacy compara con {@code 'C'} para impedir la apertura de turnos en
     * un puesto caminerero (R-TUR-010, D-TUR-002: si la comisión no existe o el estado es
     * {@code NULL}, el alta <strong>se permite</strong>).
     */
    @ColumnTransformer(read = "RTRIM(CPCEstado)")
    @Column(name = "CPCEstado", length = 1)
    private String cpcEstado;

    /** Fecha de cierre de la comisión ({@code datetime}). */
    @Column(name = "CPCFchCie")
    private LocalDateTime cpcFchCie;

    /**
     * Fecha de log de la comisión ({@code datetime}) — columna huérfana {@code CPCFeLog} (no existe
     * en la KB). Se mapea, <strong>no se escribe</strong> (SA-COM-01). Sólo lectura.
     */
    @Column(name = "CPCFeLog")
    private LocalDateTime cpcFeLog;

    /**
     * Usuario de log de la comisión ({@code char(10)}) — columna huérfana {@code CPCUsuLog}. Se
     * mapea, <strong>no se escribe</strong> (SA-COM-01). Sólo lectura.
     */
    @Column(name = "CPCUsuLog", length = 10)
    private String cpcUsuLog;

    /**
     * Número SN de la comisión ({@code decimal}) — columna huérfana {@code CPCSnNum}. Se mapea,
     * <strong>no se escribe</strong> (SA-COM-01). Sólo lectura.
     */
    @Column(name = "CPCSnNum")
    private BigDecimal cpcSnNum;

    /**
     * Fecha de pago de la comisión ({@code datetime}) — columna huérfana {@code CPCFePag}. Se
     * mapea, <strong>no se escribe</strong> (SA-COM-01). Sólo lectura.
     */
    @Column(name = "CPCFePag")
    private LocalDateTime cpcFePag;

    public Comision() {
        // requerido por JPA
    }

    public Comision(ComisionId id) {
        this.id = id;
    }

    public ComisionId getId() {
        return id;
    }

    public void setId(ComisionId id) {
        this.id = id;
    }

    /** Puesto caminerero de la comisión. */
    public Integer getPueCod() {
        return id == null ? null : id.getPueCod();
    }

    /** Número de comisión. */
    public Integer getCpcId() {
        return id == null ? null : id.getCpcId();
    }

    public LocalDateTime getCpcFchDes() {
        return cpcFchDes;
    }

    public void setCpcFchDes(LocalDateTime cpcFchDes) {
        this.cpcFchDes = cpcFchDes;
    }

    public LocalDateTime getCpcFchHas() {
        return cpcFchHas;
    }

    public void setCpcFchHas(LocalDateTime cpcFchHas) {
        this.cpcFchHas = cpcFchHas;
    }

    public Integer getCpcHorDes() {
        return cpcHorDes;
    }

    public void setCpcHorDes(Integer cpcHorDes) {
        this.cpcHorDes = cpcHorDes;
    }

    public Integer getCpcHorHas() {
        return cpcHorHas;
    }

    public void setCpcHorHas(Integer cpcHorHas) {
        this.cpcHorHas = cpcHorHas;
    }

    public String getCpcCierre() {
        return cpcCierre;
    }

    public void setCpcCierre(String cpcCierre) {
        this.cpcCierre = cpcCierre;
    }

    public Integer getCpcResId() {
        return cpcResId;
    }

    public void setCpcResId(Integer cpcResId) {
        this.cpcResId = cpcResId;
    }

    public String getCpcEstado() {
        return cpcEstado;
    }

    public void setCpcEstado(String cpcEstado) {
        this.cpcEstado = cpcEstado;
    }

    public LocalDateTime getCpcFchCie() {
        return cpcFchCie;
    }

    public void setCpcFchCie(LocalDateTime cpcFchCie) {
        this.cpcFchCie = cpcFchCie;
    }

    /** Sólo lectura: columna huérfana {@code CPCFeLog} (SA-COM-01). */
    public LocalDateTime getCpcFeLog() {
        return cpcFeLog;
    }

    /** Sólo lectura: columna huérfana {@code CPCUsuLog} (SA-COM-01). */
    public String getCpcUsuLog() {
        return cpcUsuLog;
    }

    /** Sólo lectura: columna huérfana {@code CPCSnNum} (SA-COM-01). */
    public BigDecimal getCpcSnNum() {
        return cpcSnNum;
    }

    /** Sólo lectura: columna huérfana {@code CPCFePag} (SA-COM-01). */
    public LocalDateTime getCpcFePag() {
        return cpcFePag;
    }
}
