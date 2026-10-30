package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code Receptores} → tabla física {@code RECEPTOR} (spec 03-login §3.1.1,
 * plan §5.3).
 *
 * <p>El login lee {@code ReceId} (PK, {@code smallint}) y {@code ReceDsc} ({@code char(30)}, la
 * descripción de la receptoría) → {@code Context.SucursalNombre} de la rama de receptoría.
 *
 * <p>{@code AreaId} y {@code ReceNLiq} quedan sin mapear: el login no los lee.
 */
@Entity
@Table(name = "RECEPTOR")
public class Receptor {

    /** Código de la receptoría ({@code smallint}). */
    @Id
    @Column(name = "ReceId", nullable = false)
    private Integer receId;

    /** Descripción de la receptoría ({@code char(30)}). */
    @ColumnTransformer(read = "RTRIM(ReceDsc)")
    @Column(name = "ReceDsc", length = 30)
    private String receDsc;

    public Receptor() {
        // requerido por JPA
    }

    public Receptor(Integer receId, String receDsc) {
        this.receId = receId;
        this.receDsc = receDsc;
    }

    public Integer getReceId() {
        return receId;
    }

    public void setReceId(Integer receId) {
        this.receId = receId;
    }

    public String getReceDsc() {
        return receDsc;
    }

    public void setReceDsc(String receDsc) {
        this.receDsc = receDsc;
    }
}
