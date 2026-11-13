package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Producto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.ProductoVigencia;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.ProductoRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.ProductoVigenciaRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ImportesVigentesDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoVigenciaDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoVigenciaEscrituraDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <strong>T-111</strong> (D-38): los importes leidos de {@code PRODUCT1} se normalizan al tipo
 * logico {@code NUMERIC(12,2)} en el <strong>borde de lectura</strong> (entidad -&gt; DTO).
 *
 * <p><strong>Por que</strong>: las columnas fisicas {@code ProdImpu1..5} de {@code PRODUCT1} son
 * {@code money} (escala 4 en SQL Server) mientras que el tipo logico de la KB es
 * {@code NUMERIC(12,2)} (escala 2). El driver entrega {@code 12.0000} y el
 * {@code @Digits(integer = 10, fraction = 2)} del DTO de escritura (R-PROD-08) lo
 * <strong>rechaza</strong> =&gt; el ida y vuelta leer -&gt; editar -&gt; guardar se rompe en la API y
 * en la pantalla Thymeleaf.
 *
 * <p>Verifica:
 * <ul>
 *   <li>el helper compartido {@link ImporteNormalizador} produce {@code 12.00} a partir de
 *       {@code 12.0000} (y redondea {@code HALF_UP});</li>
 *   <li>el valor normalizado <strong>pasa</strong> la validacion del DTO de escritura, mientras
 *       que el valor crudo (escala 4) la <strong>incumple</strong> (el bug, caracterizado);</li>
 *   <li>un importe ausente ({@code null}) <strong>sigue ausente</strong> en la lectura (la
 *       normalizacion a {@code 0} es de la vista, R-PROD-20);</li>
 *   <li>los dos caminos de lectura (el mapper y {@code importesVigentes}) devuelven escala 2.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T-111 - Normalizacion de la escala de los importes leidos (D-38)")
class ProductoImporteEscalaTest {

    private static ValidatorFactory validatorFactory;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProductoVigenciaRepository vigenciaRepository;

    @InjectMocks
    private ProductoService productoService;

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

    // ------------------------------------------------------------------ helper compartido

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "12.0000, 12.00",
            "3.8000, 3.80",
            "0.0000, 0.00",
            "-7.2500, -7.25",
            "100.0000, 100.00"
    })
    @DisplayName("T-111: el helper compartido lleva la escala 4 (money) a la escala 2 del tipo logico")
    void elHelperllevaElImporteAEscalaLogica(String crudo, String esperado) {
        BigDecimal resultado = ImporteNormalizador.aEscalaLogica(new BigDecimal(crudo));

        assertThat(resultado).isEqualByComparingTo(esperado);
        assertThat(resultado.scale())
                .as("el importe normalizado debe quedar con escala 2 (NUMERIC(12,2))")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("T-111: el redondeo es HALF_UP (2.6750 -> 2.68)")
    void elRedondeoEsHalfUp() {
        BigDecimal resultado = ImporteNormalizador.aEscalaLogica(new BigDecimal("2.6750"));

        assertThat(resultado).isEqualByComparingTo("2.68");
        assertThat(resultado.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("T-111: el helper conserva el null (no inventa un 0)")
    void elHelperConservaElNull() {
        assertThat(ImporteNormalizador.aEscalaLogica(null)).isNull();
    }

    // ------------------------------------------------------------------ borde de lectura (mapper)

    @Test
    @DisplayName("T-111: el DTO de lectura expone el importe con escala 2")
    void elDtoDeLecturaExponeEscala2() {
        ProductoVigencia vigencia = vigencia(
                LocalDateTime.of(2005, 9, 9, 0, 0),
                new BigDecimal("12.0000"),
                new BigDecimal("3.8000"),
                null,
                null);

        ProductoVigenciaDto dto = NucleoMapper.toDto(vigencia);

        assertThat(dto.prodImpu1()).isEqualByComparingTo("12.00");
        assertThat(dto.prodImpu1().scale()).isEqualTo(2);
        assertThat(dto.prodImpu2()).isEqualByComparingTo("3.80");
        assertThat(dto.prodImpu2().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("T-111: el importe ausente sigue ausente en la lectura (R-PROD-20)")
    void elImporteAusenteSigueAusenteEnLaLectura() {
        ProductoVigencia vigencia = vigencia(
                LocalDateTime.of(2005, 9, 9, 0, 0),
                new BigDecimal("12.0000"),
                new BigDecimal("3.8000"),
                null,
                null);

        ProductoVigenciaDto dto = NucleoMapper.toDto(vigencia);

        assertThat(dto.prodImpu3())
                .as("ProdImpu3 en NULL debe seguir en null en el DTO de lectura (no 0)")
                .isNull();
        assertThat(dto.prodImpu4())
                .as("ProdImpu4 en NULL debe seguir en null en el DTO de lectura (no 0)")
                .isNull();
    }

    // ------------------------------------------------------------------ ida y vuelta validado

    @Test
    @DisplayName("T-111: el importe normalizado PASA la validacion del DTO de escritura")
    void elImporteNormalizadoPasaLaValidacionDelDtoDeEscritura() {
        ProductoVigencia vigencia = vigencia(
                LocalDateTime.of(2005, 9, 9, 0, 0),
                new BigDecimal("12.0000"),
                new BigDecimal("3.8000"),
                null,
                null);

        ProductoVigenciaDto lectura = NucleoMapper.toDto(vigencia);
        // La vista normaliza a 0 el importe ausente (R-PROD-20) antes de enviar.
        ProductoVigenciaEscrituraDto escritura = new ProductoVigenciaEscrituraDto(
                lectura.prodFchVig(),
                lectura.prodImpu1(),
                lectura.prodImpu2(),
                ceroSiNulo(lectura.prodImpu3()),
                ceroSiNulo(lectura.prodImpu4()));

        Set<String> rutas = rutasConViolacion(escritura);

        assertThat(escritura.prodImpu1().scale())
                .as("el valor devuelto por la lectura debe venir con escala 2")
                .isEqualTo(2);
        assertThat(rutas)
                .as("el importe normalizado no puede violar @Digits(fraction = 2): %s", rutas)
                .isEmpty();
    }

    @Test
    @DisplayName("T-111 (caracterizacion del bug): el importe CRUDO de escala 4 INCUMPLE la validacion")
    void elImporteCrudoDeEscala4IncumpleLaValidacion() {
        // Sin la normalizacion (comportamiento previo a T-111) el ida y vuelta se rompia.
        ProductoVigenciaEscrituraDto escritura = new ProductoVigenciaEscrituraDto(
                LocalDateTime.of(2005, 9, 9, 0, 0),
                new BigDecimal("12.0000"),
                new BigDecimal("3.8000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        Set<String> rutas = rutasConViolacion(escritura);

        assertThat(rutas)
                .as("el `money` (escala 4) viola @Digits(fraction = 2): %s", rutas)
                .contains("prodImpu1", "prodImpu2");
    }

    // ------------------------------------------------------------------ importes vigentes

    @Test
    @DisplayName("T-111: importesVigentes tambien devuelve escala 2")
    void importesVigentesDevuelveEscala2() {
        Integer prodCod = 1;
        ProductoVigencia vigente = vigencia(
                LocalDate.now().atTime(12, 0),
                new BigDecimal("12.0000"),
                new BigDecimal("3.8000"),
                null,
                null);
        when(productoRepository.findById(prodCod)).thenReturn(Optional.of(new Producto(prodCod, "ALGODON")));
        when(vigenciaRepository
                .findFirstById_ProdCodAndId_ProdFchVigLessThanEqualOrderById_ProdFchVigDesc(
                        eq(prodCod), any(LocalDateTime.class)))
                .thenAnswer(invocation -> Optional.of(vigente));

        ImportesVigentesDto dto = productoService.importesVigentes(prodCod);

        assertThat(dto.prodIMax1()).isEqualByComparingTo("12.00");
        assertThat(dto.prodIMax1().scale()).isEqualTo(2);
        assertThat(dto.prodIMax2()).isEqualByComparingTo("3.80");
        assertThat(dto.prodIMax2().scale()).isEqualTo(2);
        // R-005: el derivado sin importe aplicable vale 0 (no null).
        assertThat(dto.prodIMax3()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.prodIMax4()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ------------------------------------------------------------------ helpers

    private Set<String> rutasConViolacion(ProductoVigenciaEscrituraDto dto) {
        Validator validator = validatorFactory.getValidator();
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static BigDecimal ceroSiNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static ProductoVigencia vigencia(LocalDateTime fecha, BigDecimal i1, BigDecimal i2,
            BigDecimal i3, BigDecimal i4) {
        ProductoVigencia v = new ProductoVigencia(fecha);
        v.setProdImpu1(i1);
        v.setProdImpu2(i2);
        v.setProdImpu3(i3);
        v.setProdImpu4(i4);
        return v;
    }
}
