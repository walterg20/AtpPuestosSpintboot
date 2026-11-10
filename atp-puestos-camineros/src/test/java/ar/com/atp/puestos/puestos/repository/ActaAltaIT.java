package ar.com.atp.puestos.puestos.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("09-formularios-acta - alta de Acta con rollback")
class ActaAltaIT {

    private static final Integer SUCURSAL = 15;
    private static final String TIPO = "PUE";
    private static final Integer TURNO = 8;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("R-ACTA-045/047/048/049: alta completa persiste y el rollback deja la BD intacta")
    void altaCompletaConRollback() {
        Integer numero = jdbcTemplate.queryForObject(
                "SELECT ISNULL(MAX(F276Id), 0) + 1 FROM F2276 WHERE SucNroId = ? AND SucTipo = ?",
                Integer.class, SUCURSAL, TIPO);

        Integer cpcPerId = jdbcTemplate.queryForObject(
                "SELECT TOP 1 CPCPerId FROM F2276 WHERE SucNroId = ? AND SucTipo = ?",
                Integer.class, SUCURSAL, TIPO);
        if (cpcPerId == null) {
            cpcPerId = 1;
        }
        Integer prodCod = jdbcTemplate.queryForObject(
                "SELECT TOP 1 ProdCod FROM PRODUCTO WHERE ProdAnula = 0", Integer.class);

        long antes2276 = count("F2276", SUCURSAL, TIPO);
        long antes22761 = count("F22761", SUCURSAL, TIPO);
        long antesPase = count("F2276PAS", SUCURSAL, TIPO);

        LocalDateTime ahora = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO F2276 (F276Id, SucTipo, SucNroId, CPCPerId, F276GenWeb, F276Operad, "
                        + "F276Estad, F276Multa, F276ForPag, F276Clasif, F276ActaTi, F276FchMov, "
                        + "F276FeGra, F276Vers, F276FormTi, F276ImpuFl, F276AdicFl, F276AdIBTo, "
                        + "F276AdCMTo, F276KmFle, F276Chasis, F276Acopla, F276TCpbte, F276NCpbte, "
                        + "F276CuitVe, F276VenRS, F276VenDom, F276CuitDe, F276DestRS, F276DestDo, "
                        + "F276CuitTr, F276TranRS, F276TranDo, TurNroId) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                numero, TIPO, SUCURSAL, cpcPerId, "A", "TEST", "C", BigDecimal.ZERO, "EFE",
                "S", "F", ahora, ahora, "1.0.0", 3, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, "ABC123", "DEF456", 1, "123", null,
                "RS", "DOM", null, "RS2", "DOM2", null, "RS3", "DOM3", TURNO);

        jdbcTemplate.update(
                "INSERT INTO F22761 (F276Id, SucTipo, SucNroId, ProdCod, F276RCant, F276RFijo, "
                        + "F276RSPre, F276RPreCM, F276RAlicu, F276RMarCM, F276RImpIB, F276RImpCM, "
                        + "F276RZona) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                numero, TIPO, SUCURSAL, prodCod, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN,
                BigDecimal.ZERO, BigDecimal.ONE, "N", BigDecimal.ZERO, BigDecimal.ZERO, " ");

        jdbcTemplate.update(
                "INSERT INTO F2276PAS (F276Id, SucTipo, SucNroId, F276PPaseI, F276PFecha, "
                        + "F276PUsuar, F276PaseST, F276PaseSN, F276PaseCo) VALUES (?,?,?,?,?,?,?,?,?)",
                numero, TIPO, SUCURSAL, 1, ahora, "TEST", TIPO, SUCURSAL, 1);

        assertThat(count("F2276", SUCURSAL, TIPO)).isEqualTo(antes2276 + 1);
        assertThat(count("F22761", SUCURSAL, TIPO)).isEqualTo(antes22761 + 1);
        assertThat(count("F2276PAS", SUCURSAL, TIPO)).isEqualTo(antesPase + 1);
    }

    private long count(String tabla, Integer suc, String tipo) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tabla + " WHERE SucNroId = ? AND SucTipo = ?",
                Long.class, suc, tipo);
    }
}