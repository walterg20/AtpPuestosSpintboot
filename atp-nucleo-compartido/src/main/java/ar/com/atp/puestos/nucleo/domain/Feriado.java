package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code Feriados} → tabla física {@code FERIADOS} (spec §4.3 / §4.5).
 *
 * <p>PK natural {@code FerFecha} ({@code datetime} → {@link LocalDateTime}, ASM-03). El atributo
 * lógico {@code FerDescripcion} se mapea a la columna física truncada {@code FerDescrip}
 * ({@code char(100) NOT NULL}, trim R-002 / R-006). Incluye auditoría {@code FerFchReg} y
 * {@code FerUsuario} ({@code NOT NULL}) y {@code FerTipo} opcional.
 */
@Entity
@Table(name = "FERIADOS")
public class Feriado {

    @Id
    @Column(name = "FerFecha", nullable = false)
    private LocalDateTime ferFecha;

    /** Atributo lógico {@code FerDescripcion} → columna física {@code FerDescrip} (truncada a 10). */
    @ColumnTransformer(read = "RTRIM(FerDescrip)")
    @Column(name = "FerDescrip", nullable = false, length = 100)
    private String ferDescripcion;

    /** Auditoría: fecha de registro ({@code datetime NOT NULL}). */
    @Column(name = "FerFchReg", nullable = false)
    private LocalDateTime ferFchReg;

    /** Auditoría: usuario que registró ({@code char(10) NOT NULL}). */
    @ColumnTransformer(read = "RTRIM(FerUsuario)")
    @Column(name = "FerUsuario", nullable = false, length = 10)
    private String ferUsuario;

    @ColumnTransformer(read = "RTRIM(FerTipo)")
    @Column(name = "FerTipo", length = 1)
    private String ferTipo;

    public Feriado() {
        // requerido por JPA
    }

    public Feriado(LocalDateTime ferFecha, String ferDescripcion) {
        this.ferFecha = ferFecha;
        this.ferDescripcion = ferDescripcion;
    }

    public LocalDateTime getFerFecha() {
        return ferFecha;
    }

    public void setFerFecha(LocalDateTime ferFecha) {
        this.ferFecha = ferFecha;
    }

    public String getFerDescripcion() {
        return ferDescripcion;
    }

    public void setFerDescripcion(String ferDescripcion) {
        this.ferDescripcion = ferDescripcion;
    }

    public LocalDateTime getFerFchReg() {
        return ferFchReg;
    }

    public void setFerFchReg(LocalDateTime ferFchReg) {
        this.ferFchReg = ferFchReg;
    }

    public String getFerUsuario() {
        return ferUsuario;
    }

    public void setFerUsuario(String ferUsuario) {
        this.ferUsuario = ferUsuario;
    }

    public String getFerTipo() {
        return ferTipo;
    }

    public void setFerTipo(String ferTipo) {
        this.ferTipo = ferTipo;
    }
}
