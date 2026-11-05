package ar.com.atp.puestos.puestos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Contador persistente de numeración de formularios → tabla física {@code GES_SUCN}
 * (spec {@code 09-formularios-acta} §2.4.1, plan §2.4.1, T-ACTA-104).
 *
 * <p><strong>Mapa completo</strong>: las <strong>5 columnas físicas</strong> verificadas contra
 * {@code Rentas} el 2026-11-04 (G-DATA). La PK es ({@code SucNroId}, {@code SucTipo},
 * {@code sucNuForm}) — constraint {@code PK__GES_SUCN__1DB825BC}. Sin {@code IDENTITY} y sin claves
 * foráneas declaradas.
 *
 * <p><strong>Uso</strong>: es la fuente <strong>autoritativa</strong> de la numeración de Actas
 * (D-ACTA-19 / D-ACTA-21). El legacy lee la fila, devuelve {@code SucUltNum + 1} y escribe ese
 * valor; aquí el incremento es <strong>atómico</strong> (ver
 * {@code ContadorFormularioRepositorio#incrementarAtomico}) porque el legacy no bloquea la fila y
 * dos altas simultáneas podrían violar la PK {@code (F276Id, SucTipo, SucNroId)}.
 *
 * <p><strong>No confundir</strong> con {@link RangoManualFormulario} ({@code FORMSUCU}):{@code
 * GES_SUCN} es el <em>contador</em>; {@code FORMSUCU} registra los <em>rangos recibidos</em> y
 * sólo interviene en la detección de números manuales (D-ACTA-20).
 */
@Entity
@Table(name = "GES_SUCN")
@IdClass(ContadorFormulario.ContadorId.class)
public class ContadorFormulario {

    /** Número de sucursal ({@code SucNroId}, {@code smallint NOT NULL}). */
    @Id
    @Column(name = "SucNroId", nullable = false)
    private Integer sucNroId;

    /** Tipo de sucursal ({@code SucTipo}, {@code char(3) NOT NULL}). */
    @Id
    @ColumnTransformer(read = "RTRIM(SucTipo)")
    @Column(name = "SucTipo", nullable = false, length = 3)
    private String sucTipo;

    /**
     * Número de formulario ({@code SucNuForm}, {@code smallint NOT NULL}) ⇒ {@code 2276}.
     *
     * <p>El nombre físico es {@code SucNuForm} (verificado contra {@code sys.columns} el
     * 2026-11-04); el plan §2.4.1 lo transcribe en minúscula, lo que no cambia nada porque la
     * colación de {@code Rentas} no distingue mayúsculas de minúsculas.
     */
    @Id
    @Column(name = "SucNuForm", nullable = false)
    private Integer sucNuForm;

    /** Identificador del contador ({@code SucNuId}, {@code char(3) NOT NULL}) ⇒ {@code 'MAN'}. */
    @ColumnTransformer(read = "RTRIM(SucNuId)")
    @Column(name = "SucNuId", nullable = false, length = 3)
    private String sucNuId;

    /** Último número emitido ({@code SucUltNum}, {@code int NOT NULL}). */
    @Column(name = "SucUltNum", nullable = false)
    private Integer sucUltNum;

    public ContadorFormulario() {
        // requerido por JPA
    }

    public Integer getSucNroId() { return sucNroId; }
    public void setSucNroId(Integer sucNroId) { this.sucNroId = sucNroId; }
    public String getSucTipo() { return sucTipo; }
    public void setSucTipo(String sucTipo) { this.sucTipo = sucTipo; }
    public Integer getSucNuForm() { return sucNuForm; }
    public void setSucNuForm(Integer sucNuForm) { this.sucNuForm = sucNuForm; }
    public String getSucNuId() { return sucNuId; }
    public void setSucNuId(String sucNuId) { this.sucNuId = sucNuId; }
    public Integer getSucUltNum() { return sucUltNum; }
    public void setSucUltNum(Integer sucUltNum) { this.sucUltNum = sucUltNum; }

    /** Clave lógica del contador ({@code @IdClass}). */
    public static class ContadorId implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer sucNroId;
        private String sucTipo;
        private Integer sucNuForm;

        public ContadorId() {
            // requerido por JPA
        }

        public ContadorId(Integer sucNroId, String sucTipo, Integer sucNuForm) {
            this.sucNroId = sucNroId;
            this.sucTipo = sucTipo;
            this.sucNuForm = sucNuForm;
        }

        public Integer getSucNroId() { return sucNroId; }
        public void setSucNroId(Integer sucNroId) { this.sucNroId = sucNroId; }
        public String getSucTipo() { return sucTipo; }
        public void setSucTipo(String sucTipo) { this.sucTipo = sucTipo; }
        public Integer getSucNuForm() { return sucNuForm; }
        public void setSucNuForm(Integer sucNuForm) { this.sucNuForm = sucNuForm; }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ContadorId other)) {
                return false;
            }
            return Objects.equals(sucNroId, other.sucNroId)
                    && Objects.equals(sucTipo, other.sucTipo)
                    && Objects.equals(sucNuForm, other.sucNuForm);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sucNroId, sucTipo, sucNuForm);
        }

        @Override
        public String toString() {
            return "ContadorId[sucNroId=" + sucNroId + ", sucTipo=" + sucTipo + ", sucNuForm="
                    + sucNuForm + "]";
        }
    }
}
