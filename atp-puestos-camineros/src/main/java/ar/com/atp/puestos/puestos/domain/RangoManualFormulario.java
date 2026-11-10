package ar.com.atp.puestos.puestos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Rangos manuales de formularios recibidos → tabla física {@code FORMSUCU}
 * (spec {@code 09-formularios-acta} §2.4.2, plan §2.4.2, T-ACTA-105).
 *
 * <p><strong>Mapa completo</strong>: las <strong>8 columnas físicas</strong> verificadas contra
 * {@code Rentas} el 2026-11-04 (G-DATA). La PK es ({@code SucNroId}, {@code SucTipo},
 * {@code SucNuForm}, {@code ForRecIni}) — constraint {@code PK__FORMSUCU__1FA06E2E}.
 *
 * <p><strong>No existen</strong> {@code SucUltNum} ni {@code ForRecFin} en esta tabla (verificado)
 * ⇒ no se mapean.
 *
 * <p><strong>Uso</strong>: <strong>sólo</strong> para la detección de números manuales
 * (D-ACTA-20): la columna "Gen. Manual?" de la grilla ({@code ProPri_VerManual}) y la validación
 * del número informado ({@code PPrim_VerificaGenMan}). El rango manual es
 * <strong>[{@code ForRecIni}, {@code ForRecIni} + {@code ForRecCant})</strong>.
 *
 * <p>La numeración de Actas <strong>no</strong> usa esta tabla: usa el contador
 * {@link ContadorFormulario} ({@code GES_SUCN}).
 */
@Entity
@Table(name = "FORMSUCU")
@IdClass(RangoManualFormulario.RangoId.class)
public class RangoManualFormulario {

    /** Número de sucursal ({@code SucNroId}, {@code smallint NOT NULL}). */
    @Id
    @Column(name = "SucNroId", nullable = false)
    private Integer sucNroId;

    /** Tipo de sucursal ({@code SucTipo}, {@code char(3) NOT NULL}). */
    @Id
    @ColumnTransformer(read = "RTRIM(SucTipo)")
    @Column(name = "SucTipo", nullable = false, length = 3)
    private String sucTipo;

    /** Número de formulario ({@code SucNuForm}, {@code smallint NOT NULL}) ⇒ {@code 2276}. */
    @Id
    @Column(name = "SucNuForm", nullable = false)
    private Integer sucNuForm;

    /** Inicio del rango ({@code ForRecIni}, {@code int NOT NULL}). */
    @Id
    @Column(name = "ForRecIni", nullable = false)
    private Integer forRecIni;

    /** Cantidad del rango ({@code ForRecCant}, {@code int NOT NULL}). */
    @Column(name = "ForRecCant", nullable = false)
    private Integer forRecCant;

    /** Fecha de recepción ({@code ForRecFech}, {@code datetime NOT NULL}). */
    @Column(name = "ForRecFech", nullable = false)
    private LocalDateTime forRecFech;

    /** Usuario que recibió el rango ({@code ForRecUsu}, {@code char(10) NOT NULL}). */
    @ColumnTransformer(read = "RTRIM(ForRecUsu)")
    @Column(name = "ForRecUsu", nullable = false, length = 10)
    private String forRecUsu;

    /** Tipo del rango ({@code ForRecTipo}, {@code char(5)}): {@code 'ACTS'} / {@code 'ACTI'}. */
    @ColumnTransformer(read = "RTRIM(ForRecTipo)")
    @Column(name = "ForRecTipo", length = 5)
    private String forRecTipo;

    public RangoManualFormulario() {
        // requerido por JPA
    }

    public Integer getSucNroId() { return sucNroId; }
    public void setSucNroId(Integer sucNroId) { this.sucNroId = sucNroId; }
    public String getSucTipo() { return sucTipo; }
    public void setSucTipo(String sucTipo) { this.sucTipo = sucTipo; }
    public Integer getSucNuForm() { return sucNuForm; }
    public void setSucNuForm(Integer sucNuForm) { this.sucNuForm = sucNuForm; }
    public Integer getForRecIni() { return forRecIni; }
    public void setForRecIni(Integer forRecIni) { this.forRecIni = forRecIni; }
    public Integer getForRecCant() { return forRecCant; }
    public void setForRecCant(Integer forRecCant) { this.forRecCant = forRecCant; }
    public LocalDateTime getForRecFech() { return forRecFech; }
    public void setForRecFech(LocalDateTime forRecFech) { this.forRecFech = forRecFech; }
    public String getForRecUsu() { return forRecUsu; }
    public void setForRecUsu(String forRecUsu) { this.forRecUsu = forRecUsu; }
    public String getForRecTipo() { return forRecTipo; }
    public void setForRecTipo(String forRecTipo) { this.forRecTipo = forRecTipo; }

    /** Clave lógica del rango ({@code @IdClass}). */
    public static class RangoId implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer sucNroId;
        private String sucTipo;
        private Integer sucNuForm;
        private Integer forRecIni;

        public RangoId() {
            // requerido por JPA
        }

        public RangoId(Integer sucNroId, String sucTipo, Integer sucNuForm, Integer forRecIni) {
            this.sucNroId = sucNroId;
            this.sucTipo = sucTipo;
            this.sucNuForm = sucNuForm;
            this.forRecIni = forRecIni;
        }

        public Integer getSucNroId() { return sucNroId; }
        public void setSucNroId(Integer sucNroId) { this.sucNroId = sucNroId; }
        public String getSucTipo() { return sucTipo; }
        public void setSucTipo(String sucTipo) { this.sucTipo = sucTipo; }
        public Integer getSucNuForm() { return sucNuForm; }
        public void setSucNuForm(Integer sucNuForm) { this.sucNuForm = sucNuForm; }
        public Integer getForRecIni() { return forRecIni; }
        public void setForRecIni(Integer forRecIni) { this.forRecIni = forRecIni; }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RangoId other)) {
                return false;
            }
            return Objects.equals(sucNroId, other.sucNroId)
                    && Objects.equals(sucTipo, other.sucTipo)
                    && Objects.equals(sucNuForm, other.sucNuForm)
                    && Objects.equals(forRecIni, other.forRecIni);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sucNroId, sucTipo, sucNuForm, forRecIni);
        }

        @Override
        public String toString() {
            return "RangoId[sucNroId=" + sucNroId + ", sucTipo=" + sucTipo + ", sucNuForm="
                    + sucNuForm + ", forRecIni=" + forRecIni + "]";
        }
    }
}
