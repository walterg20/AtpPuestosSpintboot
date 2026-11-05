package ar.com.atp.puestos.puestos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Versiones del sistema → tabla física {@code VERSIONE}
 * (spec {@code 09-formularios-acta} §2.5, plan §2.5, T-ACTA-106).
 *
 * <p><strong>Mapa completo</strong>: las <strong>5 columnas físicas</strong> verificadas contra
 * {@code Rentas} el 2026-11-04 (G-DATA). La PK es {@code VerApliId} ({@code char(3)}).
 *
 * <p><strong>Uso</strong>: origen de {@code F276FchVers} / {@code F276Vers} — el procedimiento
 * legacy {@code BuscarVersionActual} toma la fila con {@code VerApliId = 'SWP'} (ordenada por
 * {@code VerDescr}) y arma {@code VerVersion + "." + VerRevisio + "." + VerBuild}
 * (R-ACTA-045).
 */
@Entity
@Table(name = "VERSIONE")
public class VersionSistema {

    /** Aplicación ({@code VerApliId}, {@code char(3) NOT NULL}) ⇒ {@code 'SWP'} para Puestos. */
    @Id
    @ColumnTransformer(read = "RTRIM(VerApliId)")
    @Column(name = "VerApliId", nullable = false, length = 3)
    private String verApliId;

    /** Descripción ({@code VerDescr}, {@code varchar(40) NOT NULL}). */
    @ColumnTransformer(read = "RTRIM(VerDescr)")
    @Column(name = "VerDescr", nullable = false, length = 40)
    private String verDescr;

    /** Versión ({@code VerVersion}, {@code smallint NOT NULL}). */
    @Column(name = "VerVersion", nullable = false)
    private Integer verVersion;

    /** Revisión ({@code VerRevisio}, {@code smallint NOT NULL}). */
    @Column(name = "VerRevisio", nullable = false)
    private Integer verRevisio;

    /** Build ({@code VerBuild}, {@code decimal(10,0)}). */
    @Column(name = "VerBuild", precision = 10, scale = 0)
    private BigDecimal verBuild;

    public VersionSistema() {
        // requerido por JPA
    }

    public String getVerApliId() { return verApliId; }
    public void setVerApliId(String verApliId) { this.verApliId = verApliId; }
    public String getVerDescr() { return verDescr; }
    public void setVerDescr(String verDescr) { this.verDescr = verDescr; }
    public Integer getVerVersion() { return verVersion; }
    public void setVerVersion(Integer verVersion) { this.verVersion = verVersion; }
    public Integer getVerRevisio() { return verRevisio; }
    public void setVerRevisio(Integer verRevisio) { this.verRevisio = verRevisio; }
    public BigDecimal getVerBuild() { return verBuild; }
    public void setVerBuild(BigDecimal verBuild) { this.verBuild = verBuild; }
}
