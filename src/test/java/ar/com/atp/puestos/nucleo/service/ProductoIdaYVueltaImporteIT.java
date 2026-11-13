package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoEscrituraDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoVigenciaDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoVigenciaEscrituraDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.producto.ProductoForm;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.producto.ProductoMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
 * <strong>T-311</strong>: ida y vuelta <strong>completo</strong> de los importes de vigencia contra
 * la base real {@code Rentas}, a partir de la normalizacion de escala de <strong>D-38</strong>
 * (implementada en {@code nucleo/service/ImporteNormalizador.java}, T-111).
 *
 * <p>La cadena que se verifica, de punta a punta:
 *
 * <ol>
 *   <li><strong>GET</strong> de un producto con vigencias reales ⇒ el DTO de lectura (servicio) y
 *       el JSON de la API devuelven los importes con <strong>escala 2</strong> (el tipo logico
 *       {@code NUMERIC(12,2)} de la KB), no con la escala 4 de la columna fisica {@code money};</li>
 *   <li>ese mismo valor, con la forma en que lo devuelve la lectura, arma un
 *       {@link ProductoEscrituraDto} <strong>valido</strong>: sin violaciones de
 *       {@code @Digits(integer = 10, fraction = 2)} (integracion de la caracterizacion de
 *       {@code ProductoImporteEscalaIT});</li>
 *   <li>el <strong>PUT</strong> de modificacion con ese cuerpo <strong>no</strong> responde
 *       {@code 400 VALIDACION} (antes de D-38 respondia 400);</li>
 *   <li>tras la escritura, la vigencia <strong>queda persistida</strong> en {@code PRODUCT1} con el
 *       importe leido (comparacion del valor persistido contra el leido);</li>
 *   <li>el caso de una de las <strong>2 filas reales</strong> con mas de 2 decimales: el importe
 *       crudo {@code 0.0190} se lee {@code 0.02} y se persiste {@code 0.0200}, que es el
 *       <strong>redondeo documentado en D-38</strong> ({@code setScale(2, HALF_UP)}).</li>
 * </ol>
 *
 * <p><strong>Como se arma el cuerpo de la escritura.</strong> Por la <strong>forma que devuelve la
 * lectura</strong>: {@code ProductoDto} -&gt; {@code ProductoForm} -&gt; {@code ProductoEscrituraDto}
 * (los importes ausentes se normalizan a {@code 0} en la vista, R-PROD-20): es exactamente el
 * camino que recorre la pantalla Thymeleaf, de modo que la prueba verifica el contrato real.
 *
 * <p><strong>Rollback.</strong> La clase es {@code @Transactional}: cada prueba revierte su
 * transaccion al terminar, de modo que la base {@code Rentas} queda sin cambios. El conteo de filas
 * de {@code PRODUCTO} y {@code PRODUCT1} se toma <strong>fuera</strong> de la transaccion (conexion
 * propia, autocommit) en {@code @BeforeAll} y en {@code @AfterAll}: si el rollback fallara, la
 * suite quedaria en rojo. Corre solo con {@code ATP_DB_IT=true} (perfil {@code dev}: base real).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("T-311 - Ida y vuelta completo de los importes de vigencia (D-38)")
class ProductoIdaYVueltaImporteIT {

    private static final String BASE_API = "/api/v1/productos";

    /** Los importes tal como viajan en el JSON, en el orden en que aparecen. */
    private static final Pattern REGEX_IMPORTE_JSON =
            Pattern.compile("\"prodImpu([1-4])\":(-?\\d+(?:\\.\\d+)?)");

    /**
     * Producto real con vigencias reales, <strong>elegido por consulta</strong>: al menos 2
     * vigencias, todos los importes presentes y ninguno con mas de 2 decimales.
     */
    private static final String SQL_PRODUCTO_CON_VIGENCIAS =
            "SELECT TOP 1 p.ProdCod "
                    + "FROM PRODUCTO p JOIN PRODUCT1 v ON v.ProdCod = p.ProdCod "
                    + "WHERE LTRIM(RTRIM(p.ProdDesc)) <> '' "
                    + "GROUP BY p.ProdCod "
                    + "HAVING COUNT(*) >= 2 "
                    + "   AND SUM(CASE WHEN v.ProdImpu3 IS NULL OR v.ProdImpu4 IS NULL "
                    + "                THEN 1 ELSE 0 END) = 0 "
                    + "   AND SUM(CASE WHEN v.ProdImpu1 <> ROUND(v.ProdImpu1, 2) "
                    + "                 OR v.ProdImpu2 <> ROUND(v.ProdImpu2, 2) "
                    + "                 OR v.ProdImpu3 <> ROUND(v.ProdImpu3, 2) "
                    + "                 OR v.ProdImpu4 <> ROUND(v.ProdImpu4, 2) "
                    + "                THEN 1 ELSE 0 END) = 0 "
                    + "ORDER BY p.ProdCod";

    /** Filas reales con mas de 2 decimales (consecuencia documentada en D-38). */
    private static final String SQL_FILA_MAS_DECIMALES =
            "SELECT TOP 1 ProdCod, ProdFchVig, ProdImpu1, ProdImpu2, ProdImpu3, ProdImpu4 "
                    + "FROM PRODUCT1 "
                    + "WHERE ProdImpu1 <> ROUND(ProdImpu1, 2) "
                    + "OR ProdImpu2 <> ROUND(ProdImpu2, 2) "
                    + "OR ProdImpu3 <> ROUND(ProdImpu3, 2) "
                    + "OR ProdImpu4 <> ROUND(ProdImpu4, 2) "
                    + "ORDER BY ProdCod ASC, ProdFchVig ASC";

    /** Importes (y columnas legacy) de una vigencia, tal como quedaron en la columna fisica. */
    private static final String SQL_VIGENCIA =
            "SELECT ProdImpu1, ProdImpu2, ProdImpu3, ProdImpu4, ProdImpu5, ProdUsu, ProdFeLog "
                    + "FROM PRODUCT1 WHERE ProdCod = :prodCod AND ProdFchVig = :prodFchVig";

    /** Conteo de filas de las dos tablas del ABM (se toma fuera de la transaccion de prueba). */
    private static final String SQL_CUENTOS =
            "SELECT (SELECT COUNT(*) FROM PRODUCTO), (SELECT COUNT(*) FROM PRODUCT1)";

    private static ValidatorFactory validatorFactory;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    /** Lector JSON (el arbol de Jackson normaliza los ceros finales: la escala se mira en el texto). */
    private ObjectMapper lector;

    /** Conteo de filas al abrir la clase (estado comiteado de la base). */
    private long[] conteoInicial;

    @BeforeAll
    void preparar() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        lector = objectMapper.copy().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        conteoInicial = conteos();
        System.out.println("[T-311] conteo inicial (fuera de la transaccion): PRODUCTO="
                + conteoInicial[0] + ", PRODUCT1=" + conteoInicial[1]);
    }

    @AfterAll
    void verificarBaseSinCambios() {
        long[] conteoFinal = conteos();
        System.out.println("[T-311] conteo final (fuera de la transaccion): PRODUCTO="
                + conteoFinal[0] + ", PRODUCT1=" + conteoFinal[1]);
        assertThat(conteoFinal)
                .as("la base debe quedar sin cambios: el @Transactional revierte todo")
                .containsExactly(conteoInicial[0], conteoInicial[1]);
    }

    // ------------------------------------------------------------- 1) lectura con escala 2

    @Test
    @DisplayName("T-311: el GET de un producto con vigencias reales devuelve los importes con escala 2")
    void laLecturaDevuelveLosImportesConEscala2() throws Exception {
        Integer prodCod = productoConVigenciasReales();

        // El JSON de la API: los importes que viajan deben venir con escala 2.
        MvcResult r = mockMvc.perform(get(BASE_API + "/{prodCod}", prodCod))
                .andExpect(status().isOk())
                .andReturn();
        String json = r.getResponse().getContentAsString();
        System.out.println("[T-311] JSON crudo del GET: " + json);

        JsonNode raiz = lector.readTree(json);
        JsonNode vigencias = raiz.get("vigencias");
        assertThat(vigencias).as("el producto %s debe traer sus vigencias", prodCod).isNotEmpty();

        // El literal del JSON conserva la escala: se compara con el valor del DTO de lectura.
        List<String> literales = importesLiterales(json);
        List<String> esperados = productoService.obtenerProducto(prodCod).vigencias().stream()
                .flatMap(v -> Stream.of(v.prodImpu1(), v.prodImpu2(), v.prodImpu3(), v.prodImpu4())
                        .filter(Objects::nonNull)
                        .map(BigDecimal::toPlainString))
                .toList();
        System.out.println("[T-311] importes en el JSON: " + literales);
        assertThat(literales)
                .as("el JSON debe traer los importes con escala 2 (los mismos del DTO de lectura)")
                .isEqualTo(esperados);
        literales.forEach(literal -> assertThat(literal)
                .as("todo importe del JSON debe tener exactamente 2 decimales")
                .matches("-?\\d+\\.\\d{2}"));

        // El DTO de lectura del servicio: lo mismo, en el borde de lectura.
        ProductoDto lectura = productoService.obtenerProducto(prodCod);
        assertThat(lectura.vigencias()).as("el DTO de lectura debe traer las vigencias").isNotEmpty();
        for (ProductoVigenciaDto vigencia : lectura.vigencias()) {
            for (BigDecimal importe : List.of(vigencia.prodImpu1(), vigencia.prodImpu2(),
                    vigencia.prodImpu3(), vigencia.prodImpu4())) {
                if (importe == null) {
                    continue;
                }
                assertThat(importe.scale())
                        .as("el DTO de lectura debe entregar el importe con escala 2 (llego %s)",
                                importe)
                        .isEqualTo(2);
            }
        }
    }

    // ------------------------------------------- 2) el DTO de escritura armado es valido

    @Test
    @DisplayName("T-311: el importe leido arma un ProductoEscrituraDto sin violaciones de @Digits")
    void elImporteLeidoArmaUnDtoDeEscrituraValido() throws Exception {
        Integer prodCod = productoConVigenciasReales();
        ProductoDto lectura = productoService.obtenerProducto(prodCod);
        ProductoEscrituraDto escritura = ProductoMapper.aEscritura(
                ProductoMapper.aForm(lectura, productoService.importesVigentes(prodCod)));

        Set<String> rutas = validatorFactory.getValidator().validate(escritura).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
        System.out.println("[T-311] violaciones del ProductoEscrituraDto del producto " + prodCod
                + ": " + rutas);
        assertThat(rutas)
                .as("el cuerpo armado con la lectura no puede violar @Digits(fraction = 2) "
                        + "ni @NotNull")
                .isEmpty();

        // Evidencia de por que antes de D-38 la API respondia 400: el valor CRUDO de la columna
        // `money` (escala 4) si viola @Digits; el valor LEIDO (escala 2) no.
        Object[] cruda = filaCruda(prodCod, fechaDe(lectura, 0));
        BigDecimal crudo = (BigDecimal) cruda[0];
        assertThat(crudo.scale())
                .as("la columna fisica `money` devuelve escala 4")
                .isEqualTo(4);
        ProductoVigenciaEscrituraDto conValorCrudo = new ProductoVigenciaEscrituraDto(
                fechaDe(lectura, 0), crudo, crudo, crudo, crudo);
        Set<String> rutasCrudo = validatorFactory.getValidator().validate(conValorCrudo).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
        System.out.println("[T-311] violaciones con el valor CRUDO de la columna (escala 4): "
                + rutasCrudo);
        assertThat(rutasCrudo)
                .as("el valor crudo (escala 4) es el que la API rechazaba antes de D-38")
                .contains("prodImpu1");
    }

    // ----------------------------------------------- 3) el PUT de modificacion no es 400

    @Test
    @DisplayName("T-311: el PUT de modificacion con las vigencias leidas NO responde 400 VALIDACION")
    void elPutDeModificacionNoResponde400Validacion() throws Exception {
        Integer prodCod = productoConVigenciasReales();
        IdaYVuelta ida = idaYVuelta(prodCod);

        System.out.println("[T-311] PUT /api/v1/productos/" + prodCod + " -> HTTP " + ida.status());
        System.out.println("[T-311] cuerpo enviado: " + ida.cuerpoJson());

        assertThat(ida.status())
                .as("la modificacion con las vigencias leidas no puede ser 400 VALIDACION "
                        + "(respuesta: %s)", ida.cuerpoRespuesta())
                .isNotEqualTo(400);
        assertThat(ida.status())
                .as("la modificacion debe aceptarse: %s", ida.cuerpoRespuesta())
                .isEqualTo(200);

        // El producto devuelto conserva el conjunto de vigencias enviado (R-PROD-21).
        JsonNode respuesta = lector.readTree(ida.cuerpoRespuesta());
        assertThat(respuesta.get("vigencias").size())
                .as("la respuesta debe traer todas las vigencias del producto")
                .isEqualTo(ida.lectura().vigencias().size());
    }

    // --------------------------------------- 4) la vigencia queda persistida con el importe leido

    @Test
    @DisplayName("T-311: tras el PUT, la vigencia queda persistida con el importe leido")
    void laVigenciaQuedaPersistidaConElImporteLeido() throws Exception {
        Integer prodCod = productoConVigenciasReales();
        ProductoDto lecturaPrevio = productoService.obtenerProducto(prodCod);
        ProductoVigenciaDto vigenciaPrevia = lecturaPrevio.vigencias().get(0);

        // Estado de la fila en la columna fisica, ANTES de la escritura.
        Object[] antes = filaCruda(prodCod, vigenciaPrevia.prodFchVig());

        IdaYVuelta ida = idaYVuelta(prodCod);
        assertThat(ida.status()).as("el PUT debe aceptarse").isEqualTo(200);

        // Lo que se persistio, releido de la columna fisica.
        Object[] persistida = filaCruda(prodCod, vigenciaPrevia.prodFchVig());
        BigDecimal impu1Persistido = (BigDecimal) persistida[0];

        System.out.println("[T-311] persistido: PRODUCT1(ProdCod=" + prodCod + ", ProdFchVig="
                + vigenciaPrevia.prodFchVig() + ") -> ProdImpu1=" + impu1Persistido
                + " (leido=" + vigenciaPrevia.prodImpu1() + ")");
        System.out.println("[T-311] columnas legacy de la fila -> antes: ProdImpu5=" + antes[4]
                + ", ProdUsu=" + antes[5] + ", ProdFeLog=" + antes[6]
                + " | despues: ProdImpu5=" + persistida[4] + ", ProdUsu=" + persistida[5]
                + ", ProdFeLog=" + persistida[6]);

        assertThat(impu1Persistido)
                .as("el importe persistido debe ser el mismo valor que el leido")
                .isEqualByComparingTo(vigenciaPrevia.prodImpu1());
        assertThat(impu1Persistido.setScale(2, RoundingMode.HALF_UP))
                .as("el valor persistido, normalizado, es el leido")
                .isEqualByComparingTo(vigenciaPrevia.prodImpu1());
    }

    // ------------------------- 5) la fila real con mas de 2 decimales (redondeo de D-38)

    @Test
    @DisplayName("T-311: la fila real con mas de 2 decimales se redondea al centavo (D-38)")
    void laFilaConMasDeDosDecimalesSeRedondeaAlCentavo() throws Exception {
        // La fila real, elegida por consulta.
        @SuppressWarnings("unchecked")
        List<Object[]> candidatas = entityManager.createNativeQuery(SQL_FILA_MAS_DECIMALES)
                .getResultList();
        assertThat(candidatas)
                .as("debe existir al menos una fila real de PRODUCT1 con mas de 2 decimales")
                .isNotEmpty();
        Object[] fila = candidatas.get(0);
        Integer prodCod = ((Number) fila[0]).intValue();
        LocalDateTime fecha = aLocalDateTime(fila[1]);
        BigDecimal crudo = (BigDecimal) fila[2];

        System.out.println("[T-311] fila real con mas de 2 decimales: PRODUCT1(ProdCod=" + prodCod
                + ", ProdFchVig=" + fecha + ") -> ProdImpu1=" + crudo
                + " | esperado HALF_UP=" + crudo.setScale(2, RoundingMode.HALF_UP));

        // (a) lectura: el importe llega redondeado al centavo.
        ProductoDto lectura = productoService.obtenerProducto(prodCod);
        ProductoVigenciaDto vigencia = lectura.vigencias().stream()
                .filter(v -> v.prodFchVig().toLocalDate().equals(fecha.toLocalDate()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "La vigencia " + prodCod + " / " + fecha + " no vino en el DTO de lectura"));

        assertThat(vigencia.prodImpu1().scale())
                .as("el importe leido queda con escala 2")
                .isEqualTo(2);
        assertThat(vigencia.prodImpu1())
                .as("el importe leido es el redondeo HALF_UP del valor crudo (D-38)")
                .isEqualByComparingTo(crudo.setScale(2, RoundingMode.HALF_UP));

        // (b) ida y vuelta: se envia el valor leido y la API lo acepta.
        IdaYVuelta ida = idaYVuelta(prodCod);
        System.out.println("[T-311] PUT del producto " + prodCod + " (fila con mas decimales) -> HTTP "
                + ida.status());
        assertThat(ida.status())
                .as("el PUT con la fila redondeada no puede ser 400 VALIDACION (%s)",
                        ida.cuerpoRespuesta())
                .isEqualTo(200);

        ProductoVigenciaEscrituraDto enviada = ida.escritura().vigencias().stream()
                .filter(v -> v.prodFchVig().toLocalDate().equals(fecha.toLocalDate()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "La vigencia " + prodCod + " / " + fecha + " no se envio"));
        System.out.println("[T-311] enviado: prodFchVig=" + enviada.prodFchVig()
                + ", prodImpu1=" + enviada.prodImpu1());
        assertThat(enviada.prodImpu1())
                .as("lo enviado es el valor leido (ya redondeado)")
                .isEqualByComparingTo(crudo.setScale(2, RoundingMode.HALF_UP));

        // (c) persistido: el valor redondeado queda en la columna.
        Object[] persistida = filaCruda(prodCod, fecha);
        BigDecimal impu1Persistido = (BigDecimal) persistida[0];
        System.out.println("[T-311] persistido: PRODUCT1(ProdCod=" + prodCod + ", ProdFchVig="
                + fecha + ") -> ProdImpu1=" + impu1Persistido);
        assertThat(impu1Persistido)
                .as("el importe persistido es el valor enviado (redondeado al centavo)")
                .isEqualByComparingTo(crudo.setScale(2, RoundingMode.HALF_UP));
        assertThat(impu1Persistido)
                .as("el importe persistido no es el valor crudo: D-38 redondea al centavo")
                .isNotEqualByComparingTo(crudo);
    }

    // ------------------------------------------------------------- helpers

    /** Resultado del ida y vuelta: lectura -&gt; DTO de escritura -&gt; PUT. */
    private record IdaYVuelta(Integer prodCod, ProductoDto lectura, ProductoEscrituraDto escritura,
            String cuerpoJson, int status, String cuerpoRespuesta) {
    }

    /**
     * El ida y vuelta real: se lee el producto por el servicio, se arma el cuerpo de la escritura
     * con la <strong>forma que devuelve la lectura</strong> (el camino de la pantalla) y se envia
     * un {@code PUT} de modificacion por la API REST.
     */
    private IdaYVuelta idaYVuelta(Integer prodCod) throws Exception {
        ProductoDto lectura = productoService.obtenerProducto(prodCod);
        ProductoForm form = ProductoMapper.aForm(lectura, productoService.importesVigentes(prodCod));
        ProductoEscrituraDto escritura = ProductoMapper.aEscritura(form);
        String json = objectMapper.writeValueAsString(escritura);
        MvcResult r = mockMvc.perform(put(BASE_API + "/{prodCod}", prodCod)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn();
        return new IdaYVuelta(prodCod, lectura, escritura, json,
                r.getResponse().getStatus(), r.getResponse().getContentAsString());
    }

    /** El {@code ProdCod} real elegido por consulta (no supuesto). */
    private Integer productoConVigenciasReales() {
        Object codigo = entityManager.createNativeQuery(SQL_PRODUCTO_CON_VIGENCIAS)
                .getSingleResult();
        Integer prodCod = ((Number) codigo).intValue();
        System.out.println("[T-311] producto real elegido por consulta: ProdCod=" + prodCod);
        return prodCod;
    }

    /** Los valores de la columna fisica de una vigencia (tras el flush de la escritura). */
    private Object[] filaCruda(Integer prodCod, LocalDateTime fecha) {
        entityManager.flush();
        entityManager.clear();
        @SuppressWarnings("unchecked")
        List<Object[]> filas = entityManager.createNativeQuery(SQL_VIGENCIA)
                .setParameter("prodCod", prodCod)
                .setParameter("prodFchVig", Timestamp.valueOf(fecha))
                .getResultList();
        assertThat(filas)
                .as("la vigencia %s / %s debe existir en PRODUCT1", prodCod, fecha)
                .hasSize(1);
        return filas.get(0);
    }

    /** La fecha de la vigencia n-esima del DTO de lectura. */
    private static LocalDateTime fechaDe(ProductoDto dto, int indice) {
        return dto.vigencias().get(indice).prodFchVig();
    }

    /**
     * Los importes <strong>literales</strong> del JSON, en el orden en que viajan. Se lee el texto
     * y no el arbol de Jackson porque el arbol normaliza los ceros finales ({@code 18.00} -&gt;
     * {@code 18}) y lo que hay que verificar es la escala que viaja.
     */
    private static List<String> importesLiterales(String json) {
        List<String> literales = new ArrayList<>();
        Matcher matcher = REGEX_IMPORTE_JSON.matcher(json);
        while (matcher.find()) {
            literales.add(matcher.group(2));
        }
        return literales;
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

    /** Convierte el valor devuelto por la consulta nativa a {@link LocalDateTime}. */
    private static LocalDateTime aLocalDateTime(Object valor) {
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
}
