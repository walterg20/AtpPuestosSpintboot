package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code Puestos} → tabla física {@code PUEPPRIM} (spec 03-login §3.1.1,
 * plan §5.3).
 *
 * <p>El login lee las 3 columnas: {@code PueCod} (PK, {@code smallint}), {@code PueDesc}
 * ({@code char(20)}, la descripción del puesto) y {@code PueIPRed} ({@code varchar(40)}, el
 * <strong>prefijo de red</strong> contra el que se compara la IP del cliente).
 *
 * <p>11 de los 14 puestos tienen el prefijo vacío o nulo ⇒ el chequeo de IP no restringe
 * (SA-LOG-01 / SA-LOG-06: el prefijo se compara como <strong>prefijo de texto</strong>).
 */
@Entity
@Table(name = "PUEPPRIM")
public class Puesto {

    /** Código del puesto caminerero ({@code smallint}). */
    @Id
    @Column(name = "PueCod", nullable = false)
    private Integer pueCod;

    /** Descripción del puesto ({@code char(20)}) → {@code Context.SucursalNombre}. */
    @ColumnTransformer(read = "RTRIM(PueDesc)")
    @Column(name = "PueDesc", length = 20)
    private String pueDesc;

    /** Prefijo de red habilitada para el puesto ({@code varchar(40)}); vacío en 11 de 14. */
    @Column(name = "PueIPRed", length = 40)
    private String pueIPRed;

    public Puesto() {
        // requerido por JPA
    }

    public Puesto(Integer pueCod, String pueDesc, String pueIPRed) {
        this.pueCod = pueCod;
        this.pueDesc = pueDesc;
        this.pueIPRed = pueIPRed;
    }

    public Integer getPueCod() {
        return pueCod;
    }

    public void setPueCod(Integer pueCod) {
        this.pueCod = pueCod;
    }

    public String getPueDesc() {
        return pueDesc;
    }

    public void setPueDesc(String pueDesc) {
        this.pueDesc = pueDesc;
    }

    public String getPueIPRed() {
        return pueIPRed;
    }

    public void setPueIPRed(String pueIPRed) {
        this.pueIPRed = pueIPRed;
    }
}
