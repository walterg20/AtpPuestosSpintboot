package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.com.atp.puestos.nucleo.web.dto.ProductoDto;
import ar.com.atp.puestos.nucleo.web.dto.ProductoVigenciaDto;
import ar.com.atp.puestos.nucleo.web.dto.ProductoVigenciaEscrituraDto;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * <strong>T-111</strong> (D-38) contra la base real {@code Rentas}: la lectura de
 * {@code PRODUCT1.ProdImpu1..4} (columna fisica {@code money}, <strong>escala 4</strong>) se
 * normaliza al tipo logico {@code NUMERIC(12,2)} (<strong>escala 2</strong>) en el borde de lectura.
 *
 * <p>Sin esa normalizacion el driver entrega {@code 12.0000} y el
 * {@code @Digits(integer = 10, fraction = 2)} del DTO de escritura (R-PROD-08) lo
 * <strong>rechaza</strong>: el ida y vuelta leer -&gt; editar -&gt; guardar se rompe en la API y en
 * la pantalla Thymeleaf.
 *
 * <p>La prueba <strong>elige las filas por consulta</strong> (no las supone):
 * <ul>
 *   <li>una fila real con {@code ProdImpu3} (o {@code ProdImpu4}) en {@code NULL};</li>
 *   <li>una fila real con importes no nulos (escala 4).</li>
 * </ul>
 *
 * <p>Verifica el valor <strong>antes</strong> (columna fisica, escala 4) y <strong>despues</strong>
 * (DTO de lectura, escala 2), que el importe ausente sigue ausente, que el valor normalizado
 * <strong>pasa</strong> la validacion del DTO de escritura y que la <strong>base queda sin
 * cambios</strong> (la normalizacion es del borde de lectura; la transaccion revierte todo).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-111 - Escala de los importes leidos (money escala 4 -> NUMERIC(12,2))")
class ProductoImporteEscalaIT {

    /** Filas reales con al menos uno de los dos importes "de salida" en NULL. */
    private static final String SQL_BUSQUEDA_NULOS =
            "SELECT TOP 20 ProdCod, ProdFchVig, ProdImpu1, ProdImpu2, ProdImpu3, ProdImpu4 "
                    + "FROM PRODUCT1 "
                    + "WHERE ProdImpu3 IS NULL OR ProdImpu4 IS NULL "
                    + "ORDER BY ProdCod ASC, ProdFchVig ASC";

    /** Filas reales con importes no nulos. */
    private static final String SQL_BUSQUEDA_NO_NULOS =
            "SELECT TOP 20 ProdCod, ProdFchVig, ProdImpu1, ProdImpu2, ProdImpu3, ProdImpu4 "
                    + "FROM PRODUCT1 "
                    + "WHERE ProdImpu1 IS NOT NULL AND ProdImpu2 IS NOT NULL "
                    + "AND ProdFchVig > '1900-01-01' "
                    + "ORDER BY ProdCod ASC, ProdFchVig ASC";

    /** Filas reales cuyo importe tiene mas de 2 decimales (consecuencia documentada en D-38). */
    private static final String SQL_BUSQUEDA_MAS_DECIMALES =
            "SELECT TOP 20 ProdCod, ProdFchVig, ProdImpu1, ProdImpu2, ProdImpu3, ProdImpu4 "
                    + "FROM PRODUCT1 "
                    + "WHERE ProdImpu1 <> ROUND(ProdImpu1, 2) "
                    + "OR ProdImpu2 <> ROUND(ProdImpu2, 2) "
                    + "OR ProdImpu3 <> ROUND(ProdImpu3, 2) "
                    + "OR ProdImpu4 <> ROUND(ProdImpu4, 2) "
                    + "ORDER BY ProdCod ASC, ProdFchVig ASC";

    private static ValidatorFactory validatorFactory;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void abrirValidador() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void cerrarValidador() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    @Test
    @DisplayName("T-111: la fila real con importe NULL se lee con escala 2 y el NULL sigue NULL")
    void laFilaConImporteNuloSeLeeNormalizada() {
        Object[] fila = primeraFila(SQL_BUSQUEDA_NULOS);
        Integer prodCod = ((Number) fila[0]).intValue();
        LocalDateTime fecha = aLocalDateTime(fila[1]);

        System.out.println("[T-111] fila real leida: PRODUCT1(ProdCod=" + prodCod
                + ", ProdFchVig=" + fecha + ") -> ProdImpu1=" + fila[2]
                + ", ProdImpu2=" + fila[3]
                + ", ProdImpu3=" + fila[4]
                + ", ProdImpu4=" + fila[5]);

        ProductoVigenciaDto lectura = vigenciaLeida(prodCod, fecha);

        // El valor no nulo viene con escala 2 (antes: escala 4 del `money`).
        assertThat(lectura.prodImpu1())
                .as("ProdImpu1 de PRODUCT1(%s, %s)", prodCod, fecha)
                .isNotNull();
        assertThat(lectura.prodImpu1().scale())
                .as("el importe leido debe venir con escala 2 (tipo logico NUMERIC(12,2))")
                .isEqualTo(2);
        assertThat(lectura.prodImpu1())
                .as("el valor numerico no cambia: solo la escala")
                .isEqualByComparingTo((BigDecimal) fila[2]);
        assertThat(lectura.prodImpu2().scale()).isEqualTo(2);

        // El importe ausente sigue ausente en la lectura (la vista lo normaliza a 0: R-PROD-20).
        if (fila[4] == null) {
            assertThat(lectura.prodImpu3())
                    .as("ProdImpu3 en NULL debe seguir en null en el DTO de lectura")
                    .isNull();
        }
        if (fila[5] == null) {
            assertThat(lectura.prodImpu4())
                    .as("ProdImpu4 en NULL debe seguir en null en el DTO de lectura")
                    .isNull();
        }
    }

    @Test
    @DisplayName("T-111: el importe real leido pasa la validacion del DTO de escritura")
    void elImporteRealLeidoPasaLaValidacionDelDtoDeEscritura() {
        Object[] fila = primeraFila(SQL_BUSQUEDA_NO_NULOS);
        Integer prodCod = ((Number) fila[0]).intValue();
        LocalDateTime fecha = aLocalDateTime(fila[1]);

        ProductoVigenciaDto lectura = vigenciaLeida(prodCod, fecha);

        // El DTO de escritura que armaria la pantalla con lo leido (el ausente va como 0).
        ProductoVigenciaEscrituraDto escritura = new ProductoVigenciaEscrituraDto(
                lectura.prodFchVig(),
                lectura.prodImpu1(),
                lectura.prodImpu2(),
                ceroSiNulo(lectura.prodImpu3()),
                ceroSiNulo(lectura.prodImpu4()));

        Validator validator = validatorFactory.getValidator();
        Set<String> rutas = validator.validate(escritura).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
        System.out.println("[T-111] violaciones del DTO de escritura de la vigencia "
                + prodCod + " / " + fecha + ": " + rutas);
        assertThat(rutas)
                .as("el importe leido (escala 2) no puede violar @Digits(fraction = 2)")
                .isEmpty();
    }

    @Test
    @DisplayName("T-111: la base queda sin cambios (la normalizacion es del borde de lectura)")
    void laBaseQuedaSinCambios() {
        Object[] fila = primeraFila(SQL_BUSQUEDA_NO_NULOS);
        Integer prodCod = ((Number) fila[0]).intValue();
        LocalDateTime fecha = aLocalDateTime(fila[1]);

        // Se lee (y normaliza) por el servicio...
        ProductoVigenciaDto lectura = vigenciaLeida(prodCod, fecha);
        assertThat(lectura.prodImpu1().scale()).isEqualTo(2);

        // ...y la columna fisica sigue intacta: la escala 4 del `money` no se reescribe.
        Object[] despues = primeraFila(SQL_BUSQUEDA_NO_NULOS, prodCod, fecha);
        assertThat(((BigDecimal) despues[2]).scale())
                .as("la columna fisica conserva la escala 4 del `money`: la normalizacion no escribe")
                .isEqualTo(4);
        assertThat((BigDecimal) despues[2])
                .as("el valor de la columna no cambio")
                .isEqualByComparingTo((BigDecimal) fila[2]);
    }

    @Test
    @DisplayName("T-111 (D-38): las filas con mas de 2 decimales se redondean al centavo (HALF_UP)")
    void lasFilasConMasDecimalesSeRedondeanAlCentavo() {
        @SuppressWarnings("unchecked")
        List<Object[]> candidatas = entityManager.createNativeQuery(SQL_BUSQUEDA_MAS_DECIMALES)
                .getResultList();
        if (candidatas.isEmpty()) {
            // No hay filas con mas decimales en esta base: la consecuencia de D-38 no se
            // puede observar, pero tampoco rompe nada.
            System.out.println("[T-111] la base no tiene filas con mas de 2 decimales");
            return;
        }
        Object[] fila = candidatas.get(0);
        Integer prodCod = ((Number) fila[0]).intValue();
        LocalDateTime fecha = aLocalDateTime(fila[1]);

        ProductoVigenciaDto lectura = vigenciaLeida(prodCod, fecha);

        System.out.println("[T-111] fila con mas de 2 decimales: PRODUCT1(ProdCod=" + prodCod
                + ", ProdFchVig=" + fecha + ") -> ProdImpu1=" + fila[2]
                + " | leido=" + lectura.prodImpu1());

        assertThat(lectura.prodImpu1().scale())
                .as("el importe leido queda con escala 2")
                .isEqualTo(2);
        assertThat(lectura.prodImpu1())
                .as("el importe leido es el redondeo HALF_UP del valor de la columna")
                .isEqualByComparingTo(((BigDecimal) fila[2]).setScale(2, RoundingMode.HALF_UP));
    }

    // ------------------------------------------------------------------ helpers

    /**
     * La pantalla renderiza el importe ya normalizado: el input de la grilla muestra
     * {@code value="12.00"} y no {@code value="12.0000"} (la escala 4 del {@code money}).
     */
    @Test
    @DisplayName("T-111: la pantalla renderiza el importe con escala 2")
    void laPantallaRenderizaElImporteConEscala2() throws Exception {
        Object[] fila = primeraFila(SQL_BUSQUEDA_NO_NULOS);
        Integer prodCod = ((Number) fila[0]).intValue();
        LocalDateTime fecha = aLocalDateTime(fila[1]);
        ProductoVigenciaDto lectura = vigenciaLeida(prodCod, fecha);

        String html = mockMvc.perform(get("/productos").param("prodCod", String.valueOf(prodCod)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String filaHtml = filaDeVigencia(html, fecha.toLocalDate());
        assertThat(filaHtml)
                .as("la fila de la vigencia %s / %s debe estar renderizada", prodCod, fecha)
                .isNotNull();
        String input = inputDeImporte(filaHtml, "prodImpu1");
        assertThat(input)
                .as("el input de ProdImpu1 debe renderizar el importe con escala 2")
                .isNotNull()
                .contains("value=\"" + lectura.prodImpu1().toPlainString() + "\"");
        assertThat(html)
                .as("la pantalla no debe renderizar el importe con la escala 4 del `money`")
                .doesNotContain("value=\"12.0000\"");
    }

    /** Primera fila del resultado de una consulta (elegida por consulta, no supuesta). */
    private Object[] primeraFila(String sql) {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = entityManager.createNativeQuery(sql).getResultList();
        assertThat(filas).as("la consulta debe devolver al menos una fila real").isNotEmpty();
        return filas.get(0);
    }

    /** Primera fila del resultado de una consulta, acotada a una PK (para releer la columna). */
    private Object[] primeraFila(String sql, Integer prodCod, LocalDateTime fecha) {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = entityManager.createNativeQuery(sql).getResultList();
        return filas.stream()
                .filter(f -> ((Number) f[0]).intValue() == prodCod
                        && aLocalDateTime(f[1]).toLocalDate().equals(fecha.toLocalDate()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No se encontro la fila " + prodCod + " / " + fecha + " al releerla"));
    }

    /** La vigencia pedida, tal como la expone el servicio (borde de lectura). */
    private ProductoVigenciaDto vigenciaLeida(Integer prodCod, LocalDateTime fecha) {
        ProductoDto dto = productoService.obtenerProducto(prodCod);
        return dto.vigencias().stream()
                .filter(v -> v.prodFchVig().toLocalDate().equals(fecha.toLocalDate()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "La vigencia " + prodCod + " / " + fecha + " no vino en el DTO de lectura"));
    }

    private static BigDecimal ceroSiNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    /**
     * Devuelve el tag {@code <input>} del importe indicado dentro de una fila de la grilla, o
     * {@code null} si no esta. El orden de los atributos no es estable, asi que se busca por
     * {@code id}.
     */
    private static String inputDeImporte(String filaHtml, String campo) {
        Matcher matcher = Pattern.compile("<input[^>]*id=\"vigencias_\\d+_" + campo + "\"[^>]*>")
                .matcher(filaHtml);
        return matcher.find() ? matcher.group() : null;
    }

    /**
     * Devuelve el fragmento HTML de la fila de la vigencia con la fecha dada, o {@code null} si no
     * esta. Se identifica por la fecha (y no por el indice) porque el indice depende del orden de
     * la coleccion, que no es parte del contrato.
     */
    private static String filaDeVigencia(String html, LocalDate fecha) {
        for (String chunk : html.split("<tr data-vigencia=\"true\">")) {
            if (chunk.contains("prodFchVig") && chunk.contains("value=\"" + fecha + "\"")) {
                return chunk;
            }
        }
        return null;
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
