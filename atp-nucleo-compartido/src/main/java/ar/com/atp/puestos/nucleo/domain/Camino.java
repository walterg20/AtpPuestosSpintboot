package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code CAMINOS} → tabla física {@code CAMINOS} (spec §4.3 / §4.5).
 *
 * <p>PK {@code CamID} ({@code char(3)}). {@code CanRuta} es {@code char(100)} fija: se lee con
 * {@code RTRIM} para eliminar el relleno (R-002 / R-011).
 */
@Entity
@Table(name = "CAMINOS")
public class Camino {

    @Id
    @Column(name = "CamID", nullable = false, length = 3)
    private String camID;

    @ColumnTransformer(read = "RTRIM(CanRuta)")
    @Column(name = "CanRuta", length = 100)
    private String canRuta;

    public Camino() {
        // requerido por JPA
    }

    public Camino(String camID, String canRuta) {
        this.camID = camID;
        this.canRuta = canRuta;
    }

    public String getCamID() {
        return camID;
    }

    public void setCamID(String camID) {
        this.camID = camID;
    }

    public String getCanRuta() {
        return canRuta;
    }

    public void setCanRuta(String canRuta) {
        this.canRuta = canRuta;
    }
}
