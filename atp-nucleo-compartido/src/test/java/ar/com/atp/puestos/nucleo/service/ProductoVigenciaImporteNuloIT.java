package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.com.atp.puestos.nucleo.domain.ProductoVigencia;
import ar.com.atp.puestos.nucleo.repository.ProductoVigenciaRepository;
import ar.com.atp.puestos.nucleo.web.dto.ProductoDto;
import ar.com.atp.puestos.nucleo.web.dto.ProductoEscrituraDto;
import ar.com.atp.puestos.nucleo.web.dto.ProductoVigenciaDto;
import ar.com.atp.puestos.nucleo.web.dto.ProductoVigenciaEscrituraDto;
import ar.com.atp.puestos.nucleo.web.mvc.producto.ProductoForm;
import ar.com.atp.puestos.nucleo.web.mvc.producto.ProductoMapper;
import ar.com.atp.puestos.nucleo.web.mvc.producto.ProductoVigenciaForm;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
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
 * <strong>T-302</strong>: una vigencia real cuyo {@code ProdImpu3} (o {@code ProdImpu4}) esta en
 * {@code NULL} se muestra como {@code 0} y se enviaria como {@code 0} (R-PROD-20).
 *
 * <p>La base {@code Rentas} tiene 293 filas reales de {@code PRODUCT1} con {@code ProdImpu3} /
 * {@code ProdImpu4} en {@code NULL}. Esta prueba <strong>elige la fila por consulta</strong>
 * (no la supone) y verifica la cadena completa:
 *
 * <ol>
 *   <li><strong>BD</strong>: la consulta nativa confirma que la columna esta en {@code NULL};</li>
 *   <li><strong>entidad</strong>: la lectura JPA devuelve {@code null} (no hay normalizacion en la
 *       capa de persistencia);</li>
 *   <li><strong>DTO de lectura</strong>: {@code NucleoMapper} <strong>no</strong> normaliza, por
 *       lo que el importe sigue siendo {@code null} en el DTO;</li>
 *   <li><strong>vista</strong>: {@code ProductoMapper.aForm} normaliza a {@code 0}: el formulario
 *       que se renderiza muestra {@code 0} (y el HTML renderizado lo confirma);</li>
 *   <li><strong>escritura</strong>: {@code ProductoMapper.aEscritura} vuelve a normalizar a
 *       {@code 0}, de modo que el DTO de escritura <strong>no</strong> viola {@code @NotNull} en
 *       esos campos: la pantalla no devuelve {@code 400} por el importe ausente.</li>
 * </ol>
 *
 * <p><strong>Hallazgo colateral (caracterizado y corregido por T-111 / D-38)</strong>:
 * las columnas fisicas {@code ProdImpu1..5} de {@code PRODUCT1} son de tipo {@code money} (escala 4),
 * mientras que el tipo logico declarado por la KB es {@code NUMERIC(12,2)}. Al releerlas, el driver
 * entregaba {@code BigDecimal} con escala 4 ({@code 12.0000}), que el DTO de escritura rechazaba por
 * {@code @Digits(integer = 10, fraction = 2)}: el ida y vuelta leer -&gt; editar -&gt; guardar se
 * rompia. <strong>T-111</strong> normaliza la escala en el borde de lectura, de modo que la
 * caracterizacion de este test paso a ser la del <strong>comportamiento corregido</strong> (ver
 * {@link #losImportesNoNulosSeLeenConEscala2YElDtoDeEscrituraLosAcepta()}).
 *
 * <p>Solo lectura contra la base real ({@code SELECT}); se ejecuta con {@code ATP_DB_IT=true} y
 * revierte todo al terminar ({@link Transactional}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-302 - Vigencia real con importe NULL: la vista la muestra y la envia como 0")
class ProductoVigenciaImporteNuloIT {

    /** Filas reales con al menos uno de los dos importes "de salida" en NULL. */
    private static final String SQL_BUSQUEDA =
            "SELECT TOP 20 ProdCod, ProdFchVig, ProdImpu1, ProdImpu2, ProdImpu3, ProdImpu4 "
                    + "FROM PRODUCT1 "
                    + "WHERE ProdImpu3 IS NULL OR ProdImpu4 IS NULL "
                    + "ORDER BY ProdCod ASC, ProdFchVig ASC";

    /** Cuantificacion de las filas reales con cada columna en NULL. */
    private static final String SQL_CUENTO_IMPU3 =
            "SELECT COUNT(*) FROM PRODUCT1 WHERE ProdImpu3 IS NULL";

    private static final String SQL_CUENTO_IMPU4 =
            "SELECT COUNT(*) FROM PRODUCT1 WHERE ProdImpu4 IS NULL";

    private static ValidatorFactory validatorFactory;
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ProductoVigenciaRepository vigenciaRepository;

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
    @DisplayName("T-302: la base tiene filas de PRODUCT1 con ProdImpu3/ProdImpu4 en NULL")
    void laBaseTieneVigenciasConImporteNulo() {
        long conImpu3 = ((Number) entityManager.createNativeQuery(SQL_CUENTO_IMPU3)
                .getSingleResult()).longValue();
        long conImpu4 = ((Number) entityManager.createNativeQuery(SQL_CUENTO_IMPU4)
                .getSingleResult()).longValue();

        assertThat(conImpu3)
                .as("filas reales de PRODUCT1 con ProdImpu3 IS NULL")
                .isPositive();
        assertThat(conImpu4)
                .as("filas reales de PRODUCT1 con ProdImpu4 IS NULL")
                .isPositive();
    }

    @Test
    @DisplayName("T-302: la vigencia con importe NULL se muestra y se envia como 0 (R-PROD-20)")
    void laVigenciaConImporteNuloSeMuestraYSeEnviaComoCero() throws Exception {
        // ---------------------------------------------------------- 1) elegir la fila por consulta
        @SuppressWarnings("unchecked")
        List<Object[]> candidatas = entityManager.createNativeQuery(SQL_BUSQUEDA).getResultList();
        assertThat(candidatas)
                .as("debe existir al menos una fila real de PRODUCT1 con un importe en NULL")
                .isNotEmpty();

        Object[] fila = candidatas.get(0);
        Integer prodCod = ((Number) fila[0]).intValue();
        LocalDateTime fecha = aLocalDateTime(fila[1]);
        boolean nuloImpu3 = fila[4] == null;
        boolean nuloImpu4 = fila[5] == null;
        assertThat(nuloImpu3 || nuloImpu4)
                .as("la fila elegida debe tener al menos un importe en NULL")
                .isTrue();

        Set<String> propiedadesNulas = new HashSet<>();
        if (nuloImpu3) {
            propiedadesNulas.add("prodImpu3");
        }
        if (nuloImpu4) {
            propiedadesNulas.add("prodImpu4");
        }

        System.out.println("[T-302] fila real leida: PRODUCT1(ProdCod=" + prodCod
                + ", ProdFchVig=" + fecha + ") -> ProdImpu1=" + fila[2]
                + ", ProdImpu2=" + fila[3]
                + ", ProdImpu3=" + fila[4]
                + ", ProdImpu4=" + fila[5]);

        // ---------------------------------------------------------- 2) la entidad JPA
        ProductoVigencia entidad = vigenciaRepository
                .findById_ProdCodOrderById_ProdFchVigAsc(prodCod)
                .stream()
                .filter(v -> v.getProdFchVig().toLocalDate().equals(fecha.toLocalDate()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No se encontro la vigencia " + prodCod + " / " + fecha));

        if (nuloImpu3) {
            assertThat(entidad.getProdImpu3())
                    .as("ProdImpu3 de PRODUCT1(%s, %s) debe seguir en NULL en la entidad", prodCod, fecha)
                    .isNull();
        }
        if (nuloImpu4) {
            assertThat(entidad.getProdImpu4())
                    .as("ProdImpu4 de PRODUCT1(%s, %s) debe seguir en NULL en la entidad", prodCod, fecha)
                    .isNull();
        }

        // ---------------------------------------------------------- 3) el DTO de lectura NO normaliza
        ProductoDto dto = productoService.obtenerProducto(prodCod);
        ProductoVigenciaDto vigenciaDto = dto.vigencias().stream()
                .filter(v -> v.prodFchVig().toLocalDate().equals(fecha.toLocalDate()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "La vigencia " + prodCod + " / " + fecha + " no vino en el DTO de lectura"));
        if (nuloImpu3) {
            assertThat(vigenciaDto.prodImpu3())
                    .as("el DTO de lectura conserva el NULL (la normalizacion es de la vista)")
                    .isNull();
        }
        if (nuloImpu4) {
            assertThat(vigenciaDto.prodImpu4())
                    .as("el DTO de lectura conserva el NULL (la normalizacion es de la vista)")
                    .isNull();
        }

        // ---------------------------------------------------------- 4) la vista lo muestra como 0
        ProductoForm form = ProductoMapper.aForm(dto, productoService.importesVigentes(prodCod));
        ProductoVigenciaForm vigenciaForm = form.getVigencias().stream()
                .filter(f -> f.getProdFchVig().equals(fecha.toLocalDate()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "La vigencia " + prodCod + " / " + fecha + " no vino en el formulario"));
        if (nuloImpu3) {
            assertThat(vigenciaForm.getProdImpu3())
                    .as("la vista debe mostrar el importe nulo como 0")
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
        if (nuloImpu4) {
            assertThat(vigenciaForm.getProdImpu4())
                    .as("la vista debe mostrar el importe nulo como 0")
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        // ---------------------------------------------------------- 5) lo enviaria como 0
        ProductoEscrituraDto escritura = ProductoMapper.aEscritura(form);
        ProductoVigenciaEscrituraDto vigenciaEscritura = escritura.vigencias().stream()
                .filter(v -> v.prodFchVig().toLocalDate().equals(fecha.toLocalDate()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "La vigencia " + prodCod + " / " + fecha + " no vino en el DTO de escritura"));
        if (nuloImpu3) {
            assertThat(vigenciaEscritura.prodImpu3())
                    .as("el DTO de escritura debe enviar 0, no null")
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
        if (nuloImpu4) {
            assertThat(vigenciaEscritura.prodImpu4())
                    .as("el DTO de escritura debe enviar 0, no null")
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        // El importe ausente, ya normalizado a 0, NO genera violacion (@NotNull/@Digits): la
        // pantalla no responde 400 por ese campo. Se acota la asercion a los campos normalizados:
        // los importes NO nulos de la misma vigencia llegan ya con escala 2 (T-111), de modo que
        // tampoco pueden violar @Digits.
        Validator validator = validatorFactory.getValidator();
        Set<String> rutasConViolacion = validator.validate(vigenciaEscritura).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
        System.out.println("[T-302] violaciones del DTO de escritura de la vigencia "
                + prodCod + " / " + fecha + ": " + rutasConViolacion);
        assertThat(rutasConViolacion)
                .as("los importes normalizados a 0 no pueden generar violacion de validacion")
                .doesNotContainAnyElementsOf(propiedadesNulas);

        // ---------------------------------------------------------- 6) el HTML renderizado muestra 0
        String html = mockMvc.perform(get("/productos").param("prodCod", String.valueOf(prodCod)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String filaHtml = filaDeVigencia(html, fecha.toLocalDate());
        assertThat(filaHtml)
                .as("la fila de la vigencia %s / %s debe estar renderizada", prodCod, fecha)
                .isNotNull();
        if (nuloImpu3) {
            String input = inputDeImporte(filaHtml, "prodImpu3");
            assertThat(input)
                    .as("el input de ProdImpu3 debe renderizarse con value=\"0\"")
                    .isNotNull()
                    .contains("value=\"0\"");
        }
        if (nuloImpu4) {
            String input = inputDeImporte(filaHtml, "prodImpu4");
            assertThat(input)
                    .as("el input de ProdImpu4 debe renderizarse con value=\"0\"")
                    .isNotNull()
                    .contains("value=\"0\"");
        }
    }

    /**
     * <strong>T-111 / D-38</strong>: las columnas fisicas {@code ProdImpu1..5} de
     * {@code PRODUCT1} son {@code money} (escala 4), pero el tipo logico de la KB es
     * {@code NUMERIC(12,2)}. La lectura normaliza la escala en el borde de lectura, de modo que el
     * importe llega al DTO de escritura con escala 2 y <strong>no</strong> viola
     * {@code @Digits(integer = 10, fraction = 2)}.
     *
     * <p><strong>Antes de T-111</strong> este test caracterizaba el bug (el importe con escala 4
     * <em>si</em> era rechazado). Con la correccion, la caracterizacion es la del comportamiento
     * vigente: la ida y vuelta leer -&gt; editar -&gt; guardar ya no se rompe.
     */
    @Test
    @DisplayName("T-111: los importes NO nulos llegan con escala 2 y el DTO de escritura los acepta")
    void losImportesNoNulosSeLeenConEscala2YElDtoDeEscrituraLosAcepta() {
        @SuppressWarnings("unchecked")
        List<Object[]> candidatas = entityManager.createNativeQuery(SQL_BUSQUEDA).getResultList();
        Object[] fila = candidatas.get(0);
        Integer prodCod = ((Number) fila[0]).intValue();
        LocalDateTime fecha = aLocalDateTime(fila[1]);

        ProductoDto dto = productoService.obtenerProducto(prodCod);
        ProductoVigenciaDto vigenciaDto = dto.vigencias().stream()
                .filter(v -> v.prodFchVig().toLocalDate().equals(fecha.toLocalDate()))
                .findFirst()
                .orElseThrow();

        // La lectura normaliza la escala del `money` (4 decimales) al tipo logico (2 decimales).
        BigDecimal impu1 = vigenciaDto.prodImpu1();
        assertThat(impu1)
                .as("ProdImpu1 de la vigencia real %s / %s", prodCod, fecha)
                .isNotNull();
        assertThat(impu1.scale())
                .as("el DTO de lectura entrega el importe con escala 2 (NUMERIC(12,2))")
                .isEqualTo(2);
        assertThat(impu1)
                .as("el valor numerico no cambia: solo la escala")
                .isEqualByComparingTo((BigDecimal) fila[2]);

        ProductoForm form = ProductoMapper.aForm(dto, productoService.importesVigentes(prodCod));
        ProductoEscrituraDto escritura = ProductoMapper.aEscritura(form);
        ProductoVigenciaEscrituraDto vigenciaEscritura = escritura.vigencias().stream()
                .filter(v -> v.prodFchVig().toLocalDate().equals(fecha.toLocalDate()))
                .findFirst()
                .orElseThrow();
        Set<String> rutas = validatorFactory.getValidator().validate(vigenciaEscritura).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
        System.out.println("[T-302] violaciones del DTO de escritura de la vigencia "
                + prodCod + " / " + fecha + ": " + rutas);
        assertThat(rutas)
                .as("el importe leido con escala 2 no puede violar @Digits(fraction = 2)")
                .doesNotContain("prodImpu1");
        if (vigenciaDto.prodImpu2() != null) {
            assertThat(rutas).doesNotContain("prodImpu2");
        }
    }

    /**
     * Devuelve el tag {@code <input>} del importe indicado dentro de una fila de la grilla, o
     * {@code null} si no esta. El orden de los atributos no es estable (Thymeleaf emite
     * {@code th:value} antes que {@code th:attr}), asi que se busca por {@code id} y se devuelve
     * el tag completo.
     */
    private static String inputDeImporte(String filaHtml, String campo) {
        Matcher matcher = Pattern.compile("<input[^>]*id=\"vigencias_\\d+_" + campo + "\"[^>]*>")
                .matcher(filaHtml);
        return matcher.find() ? matcher.group() : null;
    }

    /**
     * Devuelve el fragmento HTML de la fila de la vigencia con la fecha dada, o {@code null} si
     * no esta. Se identifica por la fecha (y no por el indice) porque el indice depende del
     * orden de la coleccion, que no es parte del contrato.
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
