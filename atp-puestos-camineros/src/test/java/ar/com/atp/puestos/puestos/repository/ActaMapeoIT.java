package ar.com.atp.puestos.puestos.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.puestos.domain.Acta;
import ar.com.atp.puestos.puestos.domain.ActaPase;
import ar.com.atp.puestos.puestos.domain.ActaProducto;
import ar.com.atp.puestos.puestos.domain.ContadorFormulario;
import ar.com.atp.puestos.puestos.domain.RangoManualFormulario;
import ar.com.atp.puestos.puestos.domain.VersionSistema;
import jakarta.persistence.Column;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Módulo {@code 09-formularios-acta} — paridad del mapeo JPA contra el esquema físico de
 * {@code Rentas} (spec §3.10, G-DATA 2026-11-04). Se ejecuta sólo con {@code ATP_DB_IT=true}.
 *
 * <p>Verifica que las <strong>54/15/9/5/8/5</strong> columnas mapeadas por las entidades
 * coinciden <strong>carácter a carácter</strong> con {@code INFORMATION_SCHEMA.COLUMNS}, y que
 * los {@code char(n)} se leen recortados (R-ACTA-050, R-ACTA-052).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("09-formularios-acta - paridad de columnas con INFORMATION_SCHEMA")
class ActaMapeoIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ActaRepository actaRepository;

    @Test
    @DisplayName("R-ACTA-050: F2276 tiene las 54 columnas mapeadas por la entidad Acta")
    void laTablaF2276() {
        assertParidad("F2276", Acta.class, 54);
    }

    @Test
    @DisplayName("R-ACTA-050: F22761 tiene las 15 columnas mapeadas por la entidad ActaProducto")
    void laTablaF22761() {
        assertParidad("F22761", ActaProducto.class, 15);
    }

    @Test
    @DisplayName("R-ACTA-050: F2276PAS tiene las 9 columnas mapeadas por la entidad ActaPase")
    void laTablaF2276Pas() {
        assertParidad("F2276PAS", ActaPase.class, 9);
    }

    @Test
    @DisplayName("R-ACTA-050: GES_SUCN tiene las 5 columnas mapeadas por ContadorFormulario")
    void laTablaGesSucn() {
        assertParidad("GES_SUCN", ContadorFormulario.class, 5);
    }

    @Test
    @DisplayName("R-ACTA-050: FORMSUCU tiene las 8 columnas mapeadas por RangoManualFormulario")
    void laTablaFormSucu() {
        assertParidad("FORMSUCU", RangoManualFormulario.class, 8);
    }

    @Test
    @DisplayName("R-ACTA-050: VERSIONE tiene las 5 columnas mapeadas por VersionSistema")
    void laTablaVersione() {
        assertParidad("VERSIONE", VersionSistema.class, 5);
    }

    @Test
    @DisplayName("R-ACTA-052: los char(n) se leen recortados (F276Operad)")
    void losCaracteresFijosSeLeenRecortados() {
        List<String> rellenos = jdbcTemplate.queryForList(
                "SELECT TOP 1 F276Operad FROM F2276 WHERE F276Operad IS NOT NULL "
                        + "AND DATALENGTH(F276Operad) > LEN(RTRIM(F276Operad))",
                String.class);
        assertThat(rellenos).isNotEmpty();

        String esperado = rellenos.get(0).trim();
        String leido = jdbcTemplate.queryForObject(
                "SELECT TOP 1 RTRIM(F276Operad) FROM F2276 WHERE F276Operad = ?",
                String.class, rellenos.get(0));

        assertThat(leido).isEqualTo(esperado);
        assertThat(leido).doesNotEndWith(" ");
    }

    @Test
    @DisplayName("R-ACTA-052: la entidad lee el char(n) con RTRIM")
    void laEntidadRecortaLosCaracteresFijos() {
        Acta acta = actaRepository.buscar("PUE", 15, 8, "A", "A", null, null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 1)).getContent().get(0);

        assertThat(acta.getF276Operad()).isNotNull();
        assertThat(acta.getF276Operad()).isEqualTo(acta.getF276Operad().trim());
    }

    @Test
    @DisplayName("R-ACTA-050: los atributos que NO son columnas no se mapean")
    void losAtributosFormulaNoSeMapean() {
        Set<String> columnas = columnasDe(Acta.class);

        assertThat(columnas).doesNotContain("F276Total", "F276AdiTot", "F276ImpTotal",
                "F276ImIBTot", "F276ImCMTot", "F276ProCan", "F276GenMan", "F276CodBar",
                "FCalFlete", "F276VenLoc", "F276VenProv", "F276DestLoc", "F276DestProv",
                "F276TranLoc", "F276TranProv");
    }

    // ------------------------------------------------------------- helpers

    private void assertParidad(String tabla, Class<?> entidad, int cantidadEsperada) {
        Set<String> fisicas = new TreeSet<>(jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ?",
                String.class, tabla));
        Set<String> mapeadas = new TreeSet<>(columnasDe(entidad));

        assertThat(mapeadas).hasSize(cantidadEsperada);
        assertThat(fisicas).hasSize(cantidadEsperada);
        assertThat(mapeadas).isEqualTo(fisicas);
    }

    /** Nombres físicos declarados en los {@code @Column} de la entidad. */
    private Set<String> columnasDe(Class<?> entidad) {
        return Arrays.stream(entidad.getDeclaredFields())
                .filter(campo -> campo.isAnnotationPresent(Column.class))
                .map(campo -> campo.getAnnotation(Column.class).name())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
