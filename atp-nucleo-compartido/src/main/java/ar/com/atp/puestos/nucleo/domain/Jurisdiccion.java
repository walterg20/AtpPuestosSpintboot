package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code Jurisdic} → tabla física {@code Jurisdic} (spec §4.3 / §4.5).
 *
 * <p>PK {@code JurCod} ({@code smallint}); {@code JurNom} es {@code char(20)} fija (trim, R-002).
 */
@Entity
@Table(name = "Jurisdic")
public class Jurisdiccion {

    @Id
    @Column(name = "JurCod", nullable = false)
    private Integer jurCod;

    @ColumnTransformer(read = "RTRIM(JurNom)")
    @Column(name = "JurNom", length = 20)
    private String jurNom;

    public Jurisdiccion() {
        // requerido por JPA
    }

    public Jurisdiccion(Integer jurCod, String jurNom) {
        this.jurCod = jurCod;
        this.jurNom = jurNom;
    }

    public Integer getJurCod() {
        return jurCod;
    }

    public void setJurCod(Integer jurCod) {
        this.jurCod = jurCod;
    }

    public String getJurNom() {
        return jurNom;
    }

    public void setJurNom(String jurNom) {
        this.jurNom = jurNom;
    }
}
