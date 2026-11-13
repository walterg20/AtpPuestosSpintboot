package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * <strong>T-112</strong>: <strong>actualización diferencial</strong> de las vigencias (<strong>D-39</strong>)
 * contra la base real {@code Rentas}.
 *
 * <p>El defecto que se cierra: la modificación de un producto hacía {@code clear()} + reinserción de
 * las filas hijas, con lo que las columnas legacy de auditoría {@code ProdUsu}, {@code ProdFeLog} y
 * {@code ProdImpu5} quedaban en {@code NULL} (se borraban y recreaban las filas). Con D-39:
 *
 * <ul>
 *   <li>las vigencias <strong>existentes</strong> con la misma {@code ProdFchVig} se <strong>actualizan
 *       in situ</strong> y <strong>conservan</strong> {@code ProdUsu}, {@code ProdFeLog} y
 *       {@code ProdImpu5};</li>
 *   <li>la vigencia existente que <strong>no</strong> viene en el body se <strong>elimina</strong>;</li>
 *   <li>la vigencia <strong>nueva</strong> se <strong>inserta</strong> (con las columnas legacy en
 *       {@code NULL}: la auditoría de las filas nuevas es <strong>SA-P11</strong>, de {@code 03-login});</li>
 *   <li>el conjunto final de vigencias es <strong>exactamente</strong> el enviado (R-PROD-21 / SA-P08).</li>
 * </ul>
 *
 * <p><strong>Rollback.</strong> La clase es {@code @Transactional}: cada prueba revierte su
 * transaccion al terminar, de modo que la base {@code Rentas} queda sin cambios. El conteo de filas
 * de {@code PRODUCTO} y {@code PRODUCT1} se toma <strong>fuera</strong> de la transaccion (conexion
 * propia, autocommit) en {@code @BeforeAll} y en {@code @AfterAll}: si el rollback fallara, la suite
 * quedaria en rojo. Corre solo con {@code ATP_DB_IT=true} (perfil {@code dev}: base real).
 *
 * <p>El producto y sus vigencias se <strong>eligen por consulta</strong>: no se suponen ni se fijan
 * en el codigo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("T-112 - Actualizacion diferencial de las vigencias (D-39)")
class ProductoVigenciaDiferencialIT {

    private static final String BASE_API = "/api/v1/productos";

    /**
     * Producto real con al menos 3 vigencias, todas con los 4 importes presentes, al menos 2 con
     * {@code ProdUsu} y {@code ProdFeLog} y al menos 1 con {@code ProdImpu5}: es el escenario minimo
     * para comprobar que las columnas legacy se conservan.
     */
    private static final String SQL_PRODUCTO_ELEGIDO =
            "SELECT TOP 1 p.ProdCod "
                    + "FROM PRODUCTO p JOIN PRODUCT1 v ON v.ProdCod = p.ProdCod "
                    + "GROUP BY p.ProdCod "
                    + "HAVING COUNT(*) >= 3 "
                    + "   AND SUM(CASE WHEN v.ProdUsu IS NOT NULL AND v.ProdFeLog IS NOT NULL "
                    + "                THEN 1 ELSE 0 END) >= 2 "
                    + "   AND SUM(CASE WHEN v.ProdImpu5 IS NOT NULL THEN 1 ELSE 0 END) >= 1 "
                    + "   AND SUM(CASE WHEN v.ProdImpu1 IS NULL OR v.ProdImpu2 IS NULL "
                    + "                 OR v.ProdImpu3 IS NULL OR v.ProdImpu4 IS NULL "
                    + "                THEN 1 ELSE 0 END) = 0 "
                    + "ORDER BY p.ProdCod";

    /** Los datos de la raiz del producto (se reenvian tal cual: el PUT no debe alterarlos). */
    private static final String SQL_PRODUCTO =
            "SELECT LTRIM(RTRIM(ProdDesc)) AS ProdDesc, ProdTipCal, TPPriID, "
                    + "LTRIM(RTRIM(UMedNroId)) AS UMedNroId, LTRIM(RTRIM(ProdAcop)) AS ProdAcop, "
                    + "ProdAnula "
                    + "FROM PRODUCTO WHERE ProdCod = :prodCod";

    /** Todas las filas hijas del producto, con sus columnas legacy, ordenadas por fecha. */
    private static final String SQL_VIGENCIAS =
            "SELECT ProdFchVig, ProdImpu1, ProdImpu2, ProdImpu3, ProdImpu4, ProdImpu5, "
                    + "RTRIM(ProdUsu) AS ProdUsu, ProdFeLog "
                    + "FROM PRODUCT1 WHERE ProdCod = :prodCod ORDER BY ProdFchVig";

    /** Conteo de filas de las dos tablas del ABM (se toma fuera de la transaccion de prueba). */
    private static final String SQL_CUENTOS =
            "SELECT (SELECT COUNT(*) FROM PRODUCTO), (SELECT COUNT(*) FROM PRODUCT1)";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    /** Conteo de filas al abrir la clase (estado comiteado de la base). */
    private long[] conteoInicial;

    @BeforeAll
    void preparar() {
        conteoInicial = conteos();
        System.out.println("[T-112] conteo inicial (fuera de la transaccion): PRODUCTO="
                + conteoInicial[0] + ", PRODUCT1=" + conteoInicial[1]);
    }

    @AfterAll
    void verificarBaseSinCambios() {
        long[] conteoFinal = conteos();
        System.out.println("[T-112] conteo final (fuera de la transaccion): PRODUCTO="
                + conteoFinal[0] + ", PRODUCT1=" + conteoFinal[1]);
        assertThat(conteoFinal)
                .as("la base debe quedar sin cambios: el @Transactional revierte todo")
                .containsExactly(conteoInicial[0], conteoInicial[1]);
    }

    // ------------------------------------------------- (a) se conservan las columnas legacy

    @Test
    @DisplayName("T-112: las vigencias existentes conservan ProdUsu, ProdFeLog y ProdImpu5")
    void lasVigenciasExistentesConservanLasColumnasLegacy() throws Exception {
        Escenario escenario = escenario();
        Resultado resultado = ejecutarPut(escenario);

        assertThat(resultado.status())
                .as("el PUT debe aceptarse: %s", resultado.cuerpoRespuesta())
                .isEqualTo(200);

        Map<LocalDateTime, Fila> despues = filasPorFecha(escenario.prodCod());

        for (Enviada enviada : escenario.enviadas()) {
            if (enviada.fecha().equals(escenario.nueva().fecha())) {
                continue; // la vigencia nueva no existia antes: no hay columnas legacy que conservar
            }
            Fila antes = escenario.antes().get(enviada.fecha());
            Fila ahora = despues.get(enviada.fecha());
            assertThat(ahora)
                    .as("la vigencia %s debe seguir existiendo en PRODUCT1", enviada.fecha())
                    .isNotNull();
            assertThat(antes)
                    .as("la vigencia %s debe haber existido antes", enviada.fecha())
                    .isNotNull();
            System.out.println("[T-112] vigencia " + enviada.fecha()
                    + " -> antes: ProdUsu=" + antes.usu() + ", ProdFeLog=" + antes.felog()
                    + ", ProdImpu5=" + antes.impu5()
                    + " | despues: ProdUsu=" + ahora.usu() + ", ProdFeLog=" + ahora.felog()
                    + ", ProdImpu5=" + ahora.impu5());

            assertThat(ahora.usu())
                    .as("ProdUsu debe conservarse (vigencia %s)", enviada.fecha())
                    .isEqualTo(antes.usu());
            assertThat(ahora.felog())
                    .as("ProdFeLog debe conservarse (vigencia %s)", enviada.fecha())
                    .isEqualTo(antes.felog());
            mismoImporte(antes.impu5(), ahora.impu5(),
                    "ProdImpu5 debe conservarse (vigencia " + enviada.fecha() + ")");
        }
    }

    // ------------------------- (b) se elimina la quitada, (c) se inserta la nueva, (d) el conjunto

    @Test
    @DisplayName("T-112: la vigencia quitada se elimina, la nueva se inserta y el conjunto es el enviado")
    void elConjuntoFinalEsExactamenteElEnviado() throws Exception {
        Escenario escenario = escenario();
        Resultado resultado = ejecutarPut(escenario);

        assertThat(resultado.status())
                .as("el PUT debe aceptarse: %s", resultado.cuerpoRespuesta())
                .isEqualTo(200);

        Map<LocalDateTime, Fila> despues = filasPorFecha(escenario.prodCod());

        // (b) la vigencia quitada ya no esta.
        assertThat(despues)
                .as("la vigencia quitada (%s) debe eliminarse de PRODUCT1", escenario.quitada().fecha())
                .doesNotContainKey(escenario.quitada().fecha());

        // (c) la vigencia nueva esta, con las columnas legacy en NULL (SA-P11).
        Fila nueva = despues.get(escenario.nueva().fecha());
        assertThat(nueva)
                .as("la vigencia nueva (%s) debe insertarse en PRODUCT1", escenario.nueva().fecha())
                .isNotNull();
        assertThat(nueva.usu()).as("ProdUsu de la vigencia nueva").isNull();
        assertThat(nueva.felog()).as("ProdFeLog de la vigencia nueva").isNull();
        assertThat(nueva.impu5()).as("ProdImpu5 de la vigencia nueva").isNull();

        // (d) el conjunto final es exactamente el enviado (R-PROD-21 / SA-P08).
        Set<LocalDateTime> fechasEnviadas = new LinkedHashSet<>();
        escenario.enviadas().forEach(e -> fechasEnviadas.add(e.fecha()));
        assertThat(despues.keySet())
                .as("las filas de PRODUCT1 del producto deben ser exactamente las enviadas")
                .containsExactlyInAnyOrderElementsOf(fechasEnviadas);
        assertThat(despues).as("el numero de filas del producto").hasSize(fechasEnviadas.size());

        // Los importes enviados quedaron persistidos.
        for (Enviada enviada : escenario.enviadas()) {
            Fila fila = despues.get(enviada.fecha());
            assertThat(fila.impu1()).as("ProdImpu1 de %s", enviada.fecha())
                    .isEqualByComparingTo(enviada.impu1());
            assertThat(fila.impu2()).as("ProdImpu2 de %s", enviada.fecha())
                    .isEqualByComparingTo(enviada.impu2());
            assertThat(fila.impu3()).as("ProdImpu3 de %s", enviada.fecha())
                    .isEqualByComparingTo(enviada.impu3());
            assertThat(fila.impu4()).as("ProdImpu4 de %s", enviada.fecha())
                    .isEqualByComparingTo(enviada.impu4());
        }

        // La respuesta conserva el conjunto enviado (misma forma observable del contrato).
        JsonNode respuesta = objectMapper.readTree(resultado.cuerpoRespuesta());
        List<LocalDateTime> fechasRespuesta = new ArrayList<>();
        respuesta.get("vigencias").forEach(n -> fechasRespuesta.add(LocalDateTime.parse(n.get("prodFchVig").asText())));
        assertThat(fechasRespuesta)
                .as("la respuesta debe traer las vigencias en el orden enviado")
                .containsExactlyElementsOf(fechasEnviadas);
    }

    // ------------------------------------------------------------- helpers

    /** Fila real de {@code PRODUCT1} con sus columnas legacy. */
    private record Fila(LocalDateTime fecha, BigDecimal impu1, BigDecimal impu2, BigDecimal impu3,
            BigDecimal impu4, BigDecimal impu5, String usu, LocalDateTime felog) {
    }

    /** Vigencia tal como viaja en el body de escritura. */
    private record Enviada(LocalDateTime fecha, BigDecimal impu1, BigDecimal impu2, BigDecimal impu3,
            BigDecimal impu4) {
    }

    /** Escenario armado por consulta: producto real, filas reales y body de escritura. */
    private record Escenario(Integer prodCod, Map<String, Object> raiz, Map<LocalDateTime, Fila> antes,
            List<Enviada> enviadas, Enviada quitada, Enviada nueva) {
    }

    /** Resultado del PUT. */
    private record Resultado(int status, String cuerpoRespuesta) {
    }

    /**
     * Arma el escenario con datos <strong>reales</strong>: elige el producto por consulta, lee sus
     * filas y arma el body (una vigencia modificada, una quitada y una nueva).
     */
    private Escenario escenario() {
        Integer prodCod = productoElegido();
        Map<LocalDateTime, Fila> antes = filasPorFecha(prodCod);
        List<Fila> filas = new ArrayList<>(antes.values());
        assertThat(filas).as("el producto elegido debe tener al menos 3 vigencias").hasSizeGreaterThanOrEqualTo(3);

        // Se quita la segunda vigencia y se agrega una nueva entre la primera y la tercera.
        Fila quitada = filas.get(1);
        Fila modificada = filas.get(0);
        Fila siguiente = filas.get(2);
        LocalDateTime fechaNueva = huecoEntre(modificada.fecha(), siguiente.fecha(), antes.keySet());

        List<Enviada> enviadas = new ArrayList<>();
        enviadas.add(new Enviada(modificada.fecha(),
                escala(modificada.impu1().add(new BigDecimal("1.00"))),
                escala(modificada.impu2()), escala(modificada.impu3()), escala(modificada.impu4())));
        Enviada nueva = new Enviada(fechaNueva,
                new BigDecimal("11.11"), new BigDecimal("22.22"),
                new BigDecimal("33.33"), new BigDecimal("44.44"));
        enviadas.add(nueva);
        for (int i = 2; i < filas.size(); i++) {
            Fila f = filas.get(i);
            enviadas.add(new Enviada(f.fecha(), escala(f.impu1()), escala(f.impu2()),
                    escala(f.impu3()), escala(f.impu4())));
        }

        System.out.println("[T-112] producto real elegido por consulta: ProdCod=" + prodCod);
        System.out.println("[T-112] vigencias antes (" + filas.size() + "): " + antes.keySet());
        System.out.println("[T-112] vigencia quitada: " + quitada.fecha());
        System.out.println("[T-112] vigencia nueva: " + fechaNueva);
        System.out.println("[T-112] vigencias enviadas (" + enviadas.size() + "): "
                + enviadas.stream().map(Enviada::fecha).toList());

        return new Escenario(prodCod, raizDe(prodCod), antes, enviadas,
                new Enviada(quitada.fecha(), escala(quitada.impu1()), escala(quitada.impu2()),
                        escala(quitada.impu3()), escala(quitada.impu4())),
                nueva);
    }

    /** El {@code ProdCod} real elegido por consulta (no supuesto). */
    private Integer productoElegido() {
        Object codigo = entityManager.createNativeQuery(SQL_PRODUCTO_ELEGIDO).getSingleResult();
        return ((Number) codigo).intValue();
    }

    /** Los datos de la raiz del producto, para reenviarlos tal cual. */
    private Map<String, Object> raizDe(Integer prodCod) {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = entityManager.createNativeQuery(SQL_PRODUCTO)
                .setParameter("prodCod", prodCod)
                .getResultList();
        assertThat(filas).as("el producto debe existir").hasSize(1);
        Object[] f = filas.get(0);
        Map<String, Object> raiz = new LinkedHashMap<>();
        raiz.put("prodCod", prodCod);
        raiz.put("prodDesc", f[0]);
        raiz.put("prodTipCal", ((Number) f[1]).intValue());
        raiz.put("tpPriID", ((Number) f[2]).intValue());
        raiz.put("uMedNroId", f[3]);
        raiz.put("prodAcop", f[4]);
        raiz.put("prodAnula", ((Number) f[5]).intValue());
        return raiz;
    }

    /** Las filas reales de {@code PRODUCT1} del producto, con sus columnas legacy. */
    private Map<LocalDateTime, Fila> filasPorFecha(Integer prodCod) {
        entityManager.flush();
        entityManager.clear();
        @SuppressWarnings("unchecked")
        List<Object[]> filas = entityManager.createNativeQuery(SQL_VIGENCIAS)
                .setParameter("prodCod", prodCod)
                .getResultList();
        Map<LocalDateTime, Fila> porFecha = new LinkedHashMap<>();
        for (Object[] f : filas) {
            porFecha.put(fecha(f[0]), new Fila(fecha(f[0]), (BigDecimal) f[1], (BigDecimal) f[2],
                    (BigDecimal) f[3], (BigDecimal) f[4], (BigDecimal) f[5],
                    (String) f[6], f[7] == null ? null : fecha(f[7])));
        }
        return porFecha;
    }

    /** Envia el PUT de modificacion con el body armado y devuelve la respuesta. */
    private Resultado ejecutarPut(Escenario escenario) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>(escenario.raiz());
        List<Map<String, Object>> vigencias = new ArrayList<>();
        for (Enviada e : escenario.enviadas()) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("prodFchVig", e.fecha().toString());
            v.put("prodImpu1", e.impu1());
            v.put("prodImpu2", e.impu2());
            v.put("prodImpu3", e.impu3());
            v.put("prodImpu4", e.impu4());
            vigencias.add(v);
        }
        body.put("vigencias", vigencias);
        String json = objectMapper.writeValueAsString(body);
        System.out.println("[T-112] body enviado: " + json);
        MvcResult r = mockMvc.perform(put(BASE_API + "/{prodCod}", escenario.prodCod())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn();
        return new Resultado(r.getResponse().getStatus(), r.getResponse().getContentAsString());
    }

    /** Comparacion null-safe de dos importes (la columna {@code money} puede estar en NULL). */
    private static void mismoImporte(BigDecimal esperado, BigDecimal actual, String mensaje) {
        if (esperado == null) {
            assertThat(actual).as(mensaje).isNull();
            return;
        }
        assertThat(actual).as(mensaje).isNotNull();
        assertThat(actual.stripTrailingZeros()).as(mensaje).isEqualByComparingTo(esperado.stripTrailingZeros());
    }

    /**
     * Una fecha libre entre dos fechas existentes (para insertar la vigencia nueva en el medio y
     * comprobar que el conjunto final y su orden son los enviados).
     */
    private static LocalDateTime huecoEntre(LocalDateTime desde, LocalDateTime hasta,
            Set<LocalDateTime> existentes) {
        LocalDateTime candidato = hasta.minusDays(1);
        while ((!candidato.isAfter(desde)) || existentes.contains(candidato)) {
            candidato = candidato.minusDays(1);
        }
        return candidato;
    }

    /** Normaliza al tipo logico {@code NUMERIC(12,2)} (D-38): el cuerpo no puede llevar mas de 2 decimales. */
    private static BigDecimal escala(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor.setScale(2, RoundingMode.HALF_UP);
    }

    /** Convierte el valor devuelto por la consulta nativa a {@link LocalDateTime}. */
    private static LocalDateTime fecha(Object valor) {
        if (valor instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (valor instanceof LocalDateTime fecha) {
            return fecha;
        }
        if (valor instanceof java.util.Date fecha) {
            return new Timestamp(fecha.getTime()).toLocalDateTime();
        }
        throw new IllegalStateException("Tipo inesperado para ProdFchVig: " + valor);
    }

    /**
     * Conteo de filas <strong>fuera</strong> de la transaccion de prueba: conexion propia, en
     * autocommit, para ver el estado <em>comiteado</em> de la base.
     */
    private long[] conteos() {
        try (java.sql.Connection conexion = dataSource.getConnection();
                java.sql.Statement st = conexion.createStatement();
                java.sql.ResultSet rs = st.executeQuery(SQL_CUENTOS)) {
            rs.next();
            return new long[] {rs.getLong(1), rs.getLong(2)};
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("No se pudo contar las filas de PRODUCTO/PRODUCT1", e);
        }
    }
}
