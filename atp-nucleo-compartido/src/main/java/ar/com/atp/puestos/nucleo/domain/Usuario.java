package ar.com.atp.puestos.nucleo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Transacción legacy {@code SCRTY} → tabla física {@code SCRTY} (spec 03-login §3.1.1, plan §5.1).
 *
 * <p>El login sólo lee **4** de las 16 columnas físicas (D-LOG-14): {@code UsuId} (PK, {@code char(10)},
 * con el identificador <em>cifrado</em> por {@code Seg_Encripta}), {@code UsuClave} ({@code char(10)},
 * la clave también cifrada), {@code UsuNomYApe} ({@code char(35)}) y {@code UsuIngreso}
 * ({@code smallint}, nullable ⇒ {@code Integer}).
 *
 * <p>Las columnas {@code char(n)} se leen con {@code RTRIM} (regla de paridad de BD: {@code char(n)}
 * fijo se lee sin el relleno). {@code UsuHash} (PBKDF2) queda <strong>fuera</strong> de alcance
 * (SA-LOG-03): el legacy no la consulta para autenticar.
 *
 * <p>El módulo es de <strong>sólo lectura</strong>: no hay alta ni modificación de usuarios.
 */
@Entity
@Table(name = "SCRTY")
public class Usuario {

    /** Identificador cifrado ({@code char(10)}). Es la PK física. */
    @Id
    @Column(name = "UsuId", nullable = false, length = 10)
    private String usuId;

    /** Clave cifrada ({@code char(10)}); vacía en las 2 cuentas que sólo tienen {@code UsuHash}. */
    @ColumnTransformer(read = "RTRIM(UsuClave)")
    @Column(name = "UsuClave", length = 10)
    private String usuClave;

    /** Nombre y apellido ({@code char(35)}) → {@code Context.UsuarioNombre}. */
    @ColumnTransformer(read = "RTRIM(UsuNomYApe)")
    @Column(name = "UsuNomYApe", length = 35)
    private String usuNomYApe;

    /** {@code 1} ⇒ el usuario debe cambiar su clave (código 5). {@code null} en 1 de las 552 filas. */
    @Column(name = "UsuIngreso")
    private Integer usuIngreso;

    public Usuario() {
        // requerido por JPA
    }

    public Usuario(String usuId, String usuClave, String usuNomYApe, Integer usuIngreso) {
        this.usuId = usuId;
        this.usuClave = usuClave;
        this.usuNomYApe = usuNomYApe;
        this.usuIngreso = usuIngreso;
    }

    public String getUsuId() {
        return usuId;
    }

    public void setUsuId(String usuId) {
        this.usuId = usuId;
    }

    public String getUsuClave() {
        return usuClave;
    }

    public void setUsuClave(String usuClave) {
        this.usuClave = usuClave;
    }

    public String getUsuNomYApe() {
        return usuNomYApe;
    }

    public void setUsuNomYApe(String usuNomYApe) {
        this.usuNomYApe = usuNomYApe;
    }

    public Integer getUsuIngreso() {
        return usuIngreso;
    }

    public void setUsuIngreso(Integer usuIngreso) {
        this.usuIngreso = usuIngreso;
    }
}
