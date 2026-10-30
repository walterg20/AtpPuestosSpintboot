package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code PPrim_Turnos} → tabla física {@code PPRIM_TU} (spec 03-login §3.1.1,
 * plan §5.3; ampliada por el módulo {@code 06-turnos}, spec §2.1, T-TUR-A1).
 *
 * <p>La PK física es compuesta ({@code SucNroId}, {@code SucTipo}, {@code TurNroId}) — R-LOG-034
 * (la PK real difiere de la que documentaba el análisis previo).
 *
 * <p><strong>Mapa completo de la tabla</strong> (12 columnas, verificado contra {@code Rentas} el
 * 2026-10-23). Antes de este módulo sólo se mapeaban {@code TurEstado} y {@code TurCPCId}, porque
 * el login sólo leía esos dos valores; el resto se agrega ahora porque el módulo {@code 06-turnos}
 * <strong>escribe</strong> la fila (alta y cierre).
 *
 * <table>
 *   <caption>Correspondencia atributo ↔ columna física</caption>
 *   <tr><th>Atributo</th><th>Columna</th><th>Tipo físico</th></tr>
 *   <tr><td>{@code id.sucNroId}</td><td>{@code SucNroId}</td><td>{@code smallint NOT NULL}</td></tr>
 *   <tr><td>{@code id.sucTipo}</td><td>{@code SucTipo}</td><td>{@code char(3) NOT NULL}</td></tr>
 *   <tr><td>{@code id.turNroId}</td><td>{@code TurNroId}</td><td>{@code int NOT NULL}</td></tr>
 *   <tr><td>{@code turUsuario}</td><td>{@code TurUsuario}</td><td>{@code char(10) NOT NULL}</td></tr>
 *   <tr><td>{@code turFchApe}</td><td>{@code TurFchApe}</td><td>{@code datetime NOT NULL}</td></tr>
 *   <tr><td>{@code turFchCie}</td><td>{@code TurFchCie}</td><td>{@code datetime NOT NULL}</td></tr>
 *   <tr><td>{@code turEstado}</td><td>{@code TurEstado}</td><td>{@code char(1) NOT NULL}</td></tr>
 *   <tr><td>{@code turHraApe}</td><td>{@code TurHraApe}</td><td>{@code datetime NOT NULL}</td></tr>
 *   <tr><td>{@code turHraCie}</td><td>{@code TurHraCie}</td><td>{@code datetime NOT NULL}</td></tr>
 *   <tr><td>{@code turCpcId}</td><td>{@code TurCPCId}</td><td>{@code smallint NULL}</td></tr>
 *   <tr><td>{@code turFchPago}</td><td>{@code TurFchPago}</td><td>{@code datetime NULL}</td></tr>
 *   <tr><td>{@code snSinNum}</td><td>{@code SNSinNum}</td><td>{@code decimal(10,0) NULL}</td></tr>
 * </table>
 *
 * <p><strong>Sentinela {@code 1753-01-01T00:00:00}</strong>: {@code TurFchCie} y {@code TurHraCie}
 * son {@code NOT NULL} sin valor por defecto; el legacy las deja "vacías" y SQL Server las
 * materializa con el mínimo del tipo {@code datetime}. El alta escribe ese valor explícitamente
 * (R-TUR-008, ASM-TUR-03).
 */
@Entity
@Table(name = "PPRIM_TU")
public class Turno {

    /** PK compuesta: sucursal, tipo de sucursal y número de turno. */
    @EmbeddedId
    private TurnoId id;

    /** Usuario que abrió el turno ({@code char(10) NOT NULL}). */
    @ColumnTransformer(read = "RTRIM(TurUsuario)")
    @Column(name = "TurUsuario", nullable = false, length = 10)
    private String turUsuario;

    /** Fecha de apertura ({@code datetime NOT NULL}); el legacy escribe {@code serverdate()}. */
    @Column(name = "TurFchApe", nullable = false)
    private LocalDateTime turFchApe;

    /** Fecha de cierre ({@code datetime NOT NULL}); vacío = {@code 1753-01-01 00:00:00}. */
    @Column(name = "TurFchCie", nullable = false)
    private LocalDateTime turFchCie;

    /** Estado del turno ({@code char(1)}): {@code 'A'} = abierto, {@code 'C'} = cerrado. */
    @ColumnTransformer(read = "RTRIM(TurEstado)")
    @Column(name = "TurEstado", nullable = false, length = 1)
    private String turEstado;

    /** Hora de apertura ({@code datetime NOT NULL}); el legacy escribe {@code servernow()}. */
    @Column(name = "TurHraApe", nullable = false)
    private LocalDateTime turHraApe;

    /** Hora de cierre ({@code datetime NOT NULL}); vacío = {@code 1753-01-01 00:00:00}. */
    @Column(name = "TurHraCie", nullable = false)
    private LocalDateTime turHraCie;

    /** Comisión del turno ({@code smallint NULL}); {@code 0} cuando no va atado a una (D-TUR-003). */
    @Column(name = "TurCPCId")
    private Integer turCpcId;

    /** Fecha de pago del turno ({@code datetime NULL}); el legacy la deja en {@code NULL}. */
    @Column(name = "TurFchPago")
    private LocalDateTime turFchPago;

    /** Número identificatorio del SN del volante de pago ({@code decimal(10,0) NULL}); sólo REC. */
    @Column(name = "SNSinNum", precision = 10, scale = 0)
    private BigDecimal snSinNum;

    public Turno() {
        // requerido por JPA
    }

    public Turno(TurnoId id) {
        this.id = id;
    }

    public TurnoId getId() {
        return id;
    }

    public void setId(TurnoId id) {
        this.id = id;
    }

    /** Sucursal ({@code smallint}). */
    public Integer getSucNroId() {
        return id == null ? null : id.getSucNroId();
    }

    /** Tipo de sucursal ({@code char(3)}): {@code PUE} o {@code REC} (con relleno). */
    public String getSucTipo() {
        return id == null ? null : id.getSucTipo();
    }

    /** Número de turno ({@code int}). */
    public Integer getTurNroId() {
        return id == null ? null : id.getTurNroId();
    }

    public String getTurUsuario() {
        return turUsuario;
    }

    public void setTurUsuario(String turUsuario) {
        this.turUsuario = turUsuario;
    }

    public LocalDateTime getTurFchApe() {
        return turFchApe;
    }

    public void setTurFchApe(LocalDateTime turFchApe) {
        this.turFchApe = turFchApe;
    }

    public LocalDateTime getTurFchCie() {
        return turFchCie;
    }

    public void setTurFchCie(LocalDateTime turFchCie) {
        this.turFchCie = turFchCie;
    }

    public String getTurEstado() {
        return turEstado;
    }

    public void setTurEstado(String turEstado) {
        this.turEstado = turEstado;
    }

    public LocalDateTime getTurHraApe() {
        return turHraApe;
    }

    public void setTurHraApe(LocalDateTime turHraApe) {
        this.turHraApe = turHraApe;
    }

    public LocalDateTime getTurHraCie() {
        return turHraCie;
    }

    public void setTurHraCie(LocalDateTime turHraCie) {
        this.turHraCie = turHraCie;
    }

    public Integer getTurCpcId() {
        return turCpcId;
    }

    public void setTurCpcId(Integer turCpcId) {
        this.turCpcId = turCpcId;
    }

    public LocalDateTime getTurFchPago() {
        return turFchPago;
    }

    public void setTurFchPago(LocalDateTime turFchPago) {
        this.turFchPago = turFchPago;
    }

    public BigDecimal getSnSinNum() {
        return snSinNum;
    }

    public void setSnSinNum(BigDecimal snSinNum) {
        this.snSinNum = snSinNum;
    }
}
