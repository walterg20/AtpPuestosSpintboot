package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code Calles} → tabla física {@code CALLES} (spec §4.3 / §4.5).
 *
 * <p>PK {@code CallCod} ({@code int}). Incluye las columnas legacy {@code CallAlt1/2/3}
 * (ausentes en la KB, {@code char(50)} opcionales). Todas las columnas {@code char(n)} se leen
 * con {@code RTRIM} (R-002 / R-036).
 */
@Entity
@Table(name = "CALLES")
public class Calle {

    @Id
    @Column(name = "CallCod", nullable = false)
    private Integer callCod;

    @ColumnTransformer(read = "RTRIM(CallNom)")
    @Column(name = "CallNom", length = 50)
    private String callNom;

    /** Columna legacy (no KB), opcional. */
    @ColumnTransformer(read = "RTRIM(CallAlt1)")
    @Column(name = "CallAlt1", length = 50)
    private String callAlt1;

    /** Columna legacy (no KB), opcional. */
    @ColumnTransformer(read = "RTRIM(CallAlt2)")
    @Column(name = "CallAlt2", length = 50)
    private String callAlt2;

    /** Columna legacy (no KB), opcional. */
    @ColumnTransformer(read = "RTRIM(CallAlt3)")
    @Column(name = "CallAlt3", length = 50)
    private String callAlt3;

    public Calle() {
        // requerido por JPA
    }

    public Calle(Integer callCod, String callNom) {
        this.callCod = callCod;
        this.callNom = callNom;
    }

    public Integer getCallCod() {
        return callCod;
    }

    public void setCallCod(Integer callCod) {
        this.callCod = callCod;
    }

    public String getCallNom() {
        return callNom;
    }

    public void setCallNom(String callNom) {
        this.callNom = callNom;
    }

    public String getCallAlt1() {
        return callAlt1;
    }

    public void setCallAlt1(String callAlt1) {
        this.callAlt1 = callAlt1;
    }

    public String getCallAlt2() {
        return callAlt2;
    }

    public void setCallAlt2(String callAlt2) {
        this.callAlt2 = callAlt2;
    }

    public String getCallAlt3() {
        return callAlt3;
    }

    public void setCallAlt3(String callAlt3) {
        this.callAlt3 = callAlt3;
    }
}
