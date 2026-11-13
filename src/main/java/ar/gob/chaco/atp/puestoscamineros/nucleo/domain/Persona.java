package ar.gob.chaco.atp.puestoscamineros.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code Personas} → tabla física {@code PERSONAS} (spec 03-login §3.1.1,
 * plan §5.2).
 *
 * <p>El login lee 3 de las 4 columnas físicas: {@code PerId} (PK, {@code int}),
 * {@code PerNomyApe} ({@code char(40)}, el nombre y apellido) y {@code PerCodUsu}
 * ({@code char(10)}, el identificador de usuario <strong>en claro</strong>, en mayúsculas y con
 * relleno). {@code AreaId} queda sin mapear porque el login no la lee.
 *
 * <p><strong>No hay claves foráneas físicas</strong> en la base: la relación entre {@code PERSONAS}
 * y {@code SCRTY} es lógica y la reproduce el repositorio ({@code PerCodUsu} ↔ identificador en
 * claro).
 */
@Entity
@Table(name = "PERSONAS")
public class Persona {

    /** Identificador de la persona ({@code int}). */
    @Id
    @Column(name = "PerId", nullable = false)
    private Integer perId;

    /** Nombre y apellido ({@code char(40)}): es la única columna de nombre de persona de la base. */
    @ColumnTransformer(read = "RTRIM(PerNomyApe)")
    @Column(name = "PerNomyApe", length = 40)
    private String perNomyApe;

    /** Identificador de usuario en claro ({@code char(10)}), en mayúsculas y con relleno. */
    @Column(name = "PerCodUsu", length = 10)
    private String perCodUsu;

    public Persona() {
        // requerido por JPA
    }

    public Persona(Integer perId, String perNomyApe, String perCodUsu) {
        this.perId = perId;
        this.perNomyApe = perNomyApe;
        this.perCodUsu = perCodUsu;
    }

    public Integer getPerId() {
        return perId;
    }

    public void setPerId(Integer perId) {
        this.perId = perId;
    }

    public String getPerNomyApe() {
        return perNomyApe;
    }

    public void setPerNomyApe(String perNomyApe) {
        this.perNomyApe = perNomyApe;
    }

    public String getPerCodUsu() {
        return perCodUsu;
    }

    public void setPerCodUsu(String perCodUsu) {
        this.perCodUsu = perCodUsu;
    }
}
