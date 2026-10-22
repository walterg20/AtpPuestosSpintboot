package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code Localidades} → tabla física {@code Localida} (spec §4.3 / §4.5).
 *
 * <p>PK {@code LocCod} ({@code int}, no autoincremental). La relación
 * {@code Localidades → Provincia} se modela como asociación por {@code ProvCod} (R-003); no hay FK
 * física en la BD, la integridad la garantiza la aplicación. Incluye la columna legacy
 * {@code LocCodAfip} ({@code int NOT NULL}, ausente en la KB) con default {@code 0} al alta (R-036).
 * Las columnas {@code char(n)} se leen con {@code RTRIM} (R-002).
 */
@Entity
@Table(name = "Localida")
public class Localidad {

    @Id
    @Column(name = "LocCod", nullable = false)
    private Integer locCod;

    @ColumnTransformer(read = "RTRIM(LocNom)")
    @Column(name = "LocNom", length = 50)
    private String locNom;

    @ColumnTransformer(read = "RTRIM(LocNomProv)")
    @Column(name = "LocNomProv", length = 19)
    private String locNomProv;

    @Column(name = "LocCodPost")
    private Integer locCodPost;

    /** FK lógica {@code ProvCod} → {@code PROVINCI} (R-003). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProvCod")
    private Provincia provincia;

    /**
     * Valor de la columna {@code Localida.ProvCod} (FK lógica a {@code PROVINCI}).
     *
     * <p><strong>R-207</strong>: la asociación {@link #provincia} no puede representar una FK
     * huérfana —la provincia no existe en {@code PROVINCI}—. Para esas 30 filas la asociación
     * queda en {@code null} y el código físico se pierde. Esta propiedad conserva el valor real
     * de la columna para que el DTO pueda exponerlo (código físico) y el cliente pueda
     * representar el estado huérfano.
     *
     * <p>Desde el punto de vista de la BD es de sólo lectura ({@code insertable = false},
     * {@code updatable = false}): la columna la escribe la asociación {@link #provincia}. En las
     * entidades recién creadas o modificadas —que Hibernate no refresca desde la BD— la
     * aplicación fija el valor para que el DTO devuelva el código pedido.
     */
    @Column(name = "ProvCod", insertable = false, updatable = false)
    private Integer provCod;

    /** Columna legacy (no KB): {@code int NOT NULL} → default {@code 0} al alta (R-036). */
    @Column(name = "LocCodAfip", nullable = false)
    private Integer locCodAfip = 0;

    public Localidad() {
        // requerido por JPA
    }

    public Localidad(Integer locCod, String locNom) {
        this.locCod = locCod;
        this.locNom = locNom;
    }

    public Integer getLocCod() {
        return locCod;
    }

    public void setLocCod(Integer locCod) {
        this.locCod = locCod;
    }

    public String getLocNom() {
        return locNom;
    }

    public void setLocNom(String locNom) {
        this.locNom = locNom;
    }

    public String getLocNomProv() {
        return locNomProv;
    }

    public void setLocNomProv(String locNomProv) {
        this.locNomProv = locNomProv;
    }

    public Integer getLocCodPost() {
        return locCodPost;
    }

    public void setLocCodPost(Integer locCodPost) {
        this.locCodPost = locCodPost;
    }

    public Provincia getProvincia() {
        return provincia;
    }

    public void setProvincia(Provincia provincia) {
        this.provincia = provincia;
    }

    /**
     * Valor de la columna {@code Localida.ProvCod} (R-207).
     *
     * <p>No lo escribe la BD en el {@code INSERT}/{@code UPDATE} (la columna es de sólo lectura
     * para Hibernate): lo usa la aplicación para mantener el DTO alineado con la FK que se está
     * escribiendo.
     */
    public Integer getProvCod() {
        return provCod;
    }

    public void setProvCod(Integer provCod) {
        this.provCod = provCod;
    }

    public Integer getLocCodAfip() {
        return locCodAfip;
    }

    public void setLocCodAfip(Integer locCodAfip) {
        this.locCodAfip = locCodAfip;
    }
}
