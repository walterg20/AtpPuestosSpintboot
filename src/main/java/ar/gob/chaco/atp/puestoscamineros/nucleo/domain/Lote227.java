package ar.gob.chaco.atp.puestoscamineros.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code F2276_Lotes} → tabla física {@code LOTES227} (spec {@code 06-turnos}
 * §2.2, ASM-TUR-01, R-TUR-013).
 *
 * <p>El lote agrupa los formularios 2276 de un turno. Para el cierre del turno sólo interesan dos
 * atributos:
 *
 * <ul>
 *   <li>{@code LF276Estad} ({@code char(1) NOT NULL}): {@code 'C'} = lote cerrado;</li>
 *   <li>{@code LF276FchHa} ({@code datetime}): fecha hasta la que el lote está vigente.</li>
 * </ul>
 *
 * <p>El vínculo con el formulario es {@code F2276.LF276ID = LOTES227.LF276ID} (confirmado por el
 * analista de la KB: ASM-TUR-01).
 */
@Entity
@Table(name = "LOTES227")
public class Lote227 {

    /** PK: número de lote ({@code int}). */
    @Id
    @Column(name = "LF276ID")
    private Integer lf276Id;

    /** Estado del lote ({@code char(1) NOT NULL}): {@code 'P'} = pendiente, {@code 'C'} = cerrado. */
    @ColumnTransformer(read = "RTRIM(LF276Estad)")
    @Column(name = "LF276Estad", nullable = false, length = 1)
    private String lf276Estad;

    /** Fecha hasta la que el lote está vigente ({@code datetime}). */
    @Column(name = "LF276FchHa")
    private LocalDateTime lf276FchHa;

    public Lote227() {
        // requerido por JPA
    }

    public Lote227(Integer lf276Id) {
        this.lf276Id = lf276Id;
    }

    public Integer getLf276Id() {
        return lf276Id;
    }

    public void setLf276Id(Integer lf276Id) {
        this.lf276Id = lf276Id;
    }

    public String getLf276Estad() {
        return lf276Estad;
    }

    public void setLf276Estad(String lf276Estad) {
        this.lf276Estad = lf276Estad;
    }

    public LocalDateTime getLf276FchHa() {
        return lf276FchHa;
    }

    public void setLf276FchHa(LocalDateTime lf276FchHa) {
        this.lf276FchHa = lf276FchHa;
    }
}
