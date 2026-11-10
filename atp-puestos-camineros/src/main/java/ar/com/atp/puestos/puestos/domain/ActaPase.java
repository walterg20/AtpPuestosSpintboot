package ar.com.atp.puestos.puestos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Pase del puesto caminero sobre un Acta → tabla física {@code F2276PAS}
 * (spec {@code 09-formularios-acta} §2.3, plan §2.3, T-ACTA-103).
 *
 * <p><strong>Mapa completo</strong>: las <strong>9 columnas físicas</strong> verificadas contra
 * {@code Rentas} el 2026-11-04 (G-DATA). La PK es compuesta ({@code SucTipo}, {@code SucNroId},
 * {@code F276Id}, {@code F276PPaseI}) y se declara con {@code @IdClass} ({@link ActaPaseId}).
 *
 * <p><strong>Nota</strong>: {@code F276PaseSD} existe en la KB pero <strong>no</strong> en la BD
 * (verificado: {@code COUNT(*) = 0}) ⇒ no se mapea.
 */
@Entity
@Table(name = "F2276PAS")
@IdClass(ActaPaseId.class)
public class ActaPase {

    /** Tipo de sucursal ({@code SucTipo}, {@code char(3) NOT NULL}). */
    @Id
    @ColumnTransformer(read = "RTRIM(SucTipo)")
    @Column(name = "SucTipo", nullable = false, length = 3)
    private String sucTipo;

    /** Número de sucursal ({@code SucNroId}, {@code smallint NOT NULL}). */
    @Id
    @Column(name = "SucNroId", nullable = false)
    private Integer sucNroId;

    /** Número de Acta ({@code F276Id}, {@code int NOT NULL}). */
    @Id
    @Column(name = "F276Id", nullable = false)
    private Integer f276Id;

    /** Índice correlativo del pase ({@code F276PPaseI}, {@code smallint NOT NULL}). */
    @Id
    @Column(name = "F276PPaseI", nullable = false)
    private Integer f276PPaseI;

    /** Fecha del pase ({@code F276PFecha}, {@code datetime NOT NULL}) ⇒ {@code ServerNow()}. */
    @Column(name = "F276PFecha", nullable = false)
    private LocalDateTime f276PFecha;

    /** Usuario del pase ({@code F276PUsuar}, {@code char(10) NOT NULL}). */
    @ColumnTransformer(read = "RTRIM(F276PUsuar)")
    @Column(name = "F276PUsuar", nullable = false, length = 10)
    private String f276PUsuar;

    /** Tipo del pase ({@code F276PaseST}, {@code char(3)}) ⇒ tipo de la sesión ({@code 'PUE'}). */
    @ColumnTransformer(read = "RTRIM(F276PaseST)")
    @Column(name = "F276PaseST", length = 3)
    private String f276PaseST;

    /** Sucursal del pase ({@code F276PaseSN}, {@code smallint}). */
    @Column(name = "F276PaseSN")
    private Integer f276PaseSN;

    /** Comisión del pase ({@code F276PaseCo}, {@code int}). */
    @Column(name = "F276PaseCo")
    private Integer f276PaseCo;

    public ActaPase() {
        // requerido por JPA
    }

    public String getSucTipo() { return sucTipo; }
    public void setSucTipo(String sucTipo) { this.sucTipo = sucTipo; }
    public Integer getSucNroId() { return sucNroId; }
    public void setSucNroId(Integer sucNroId) { this.sucNroId = sucNroId; }
    public Integer getF276Id() { return f276Id; }
    public void setF276Id(Integer f276Id) { this.f276Id = f276Id; }
    public Integer getF276PPaseI() { return f276PPaseI; }
    public void setF276PPaseI(Integer f276PPaseI) { this.f276PPaseI = f276PPaseI; }
    public LocalDateTime getF276PFecha() { return f276PFecha; }
    public void setF276PFecha(LocalDateTime f276PFecha) { this.f276PFecha = f276PFecha; }
    public String getF276PUsuar() { return f276PUsuar; }
    public void setF276PUsuar(String f276PUsuar) { this.f276PUsuar = f276PUsuar; }
    public String getF276PaseST() { return f276PaseST; }
    public void setF276PaseST(String f276PaseST) { this.f276PaseST = f276PaseST; }
    public Integer getF276PaseSN() { return f276PaseSN; }
    public void setF276PaseSN(Integer f276PaseSN) { this.f276PaseSN = f276PaseSN; }
    public Integer getF276PaseCo() { return f276PaseCo; }
    public void setF276PaseCo(Integer f276PaseCo) { this.f276PaseCo = f276PaseCo; }
}
