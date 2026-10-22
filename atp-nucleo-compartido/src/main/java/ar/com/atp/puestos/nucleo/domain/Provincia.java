package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Transacción legacy {@code Provincia} → tabla física {@code PROVINCI} (spec §4.3 / §4.5).
 *
 * <p>PK {@code ProvCod} no autoincremental ({@code smallint}). Incluye la columna legacy
 * {@code ProvCodAfip} ({@code int NOT NULL}, ausente en la KB) con default {@code 0} al alta (R-036).
 */
@Entity
@Table(name = "PROVINCI")
public class Provincia {

    @Id
    @Column(name = "ProvCod", nullable = false)
    private Integer provCod;

    @Column(name = "ProvNombre", nullable = false, length = 40)
    private String provNombre;

    /** Columna legacy (no KB): {@code int NOT NULL} → default {@code 0} al alta (R-036). */
    @Column(name = "ProvCodAfip", nullable = false)
    private Integer provCodAfip = 0;

    public Provincia() {
        // requerido por JPA
    }

    public Provincia(Integer provCod, String provNombre) {
        this.provCod = provCod;
        this.provNombre = provNombre;
    }

    public Integer getProvCod() {
        return provCod;
    }

    public void setProvCod(Integer provCod) {
        this.provCod = provCod;
    }

    public String getProvNombre() {
        return provNombre;
    }

    public void setProvNombre(String provNombre) {
        this.provNombre = provNombre;
    }

    public Integer getProvCodAfip() {
        return provCodAfip;
    }

    public void setProvCodAfip(Integer provCodAfip) {
        this.provCodAfip = provCodAfip;
    }
}
