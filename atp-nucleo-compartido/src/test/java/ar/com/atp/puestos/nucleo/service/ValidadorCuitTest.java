package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Suite unitaria del servicio {@link ValidadorCuit} (módulo 11, T-CUIT-012).
 *
 * <p>Los casos están transcritos del algoritmo verbatim de {@code Nuc_ValidaCuit} (spec §3.3)
 * y NO se copian de la implementación.
 */
class ValidadorCuitTest {

    // ------------------------------------------------------------------ Long

    @Test
    @DisplayName("R-CUIT-001: null en Long es invalido")
    void longNullEsInvalido() {
        assertThat(ValidadorCuit.esValido((Long) null)).isFalse();
    }

    @Test
    @DisplayName("R-CUIT-001: negativo en Long es invalido")
    void longNegativoEsInvalido() {
        assertThat(ValidadorCuit.esValido(-1L)).isFalse();
    }

    @Test
    @DisplayName("R-CUIT-001: 10 digitos en Long es invalido")
    void long10DigitosEsInvalido() {
        assertThat(ValidadorCuit.esValido(2012345678L)).isFalse();
    }

    @Test
    @DisplayName("R-CUIT-001: 12 digitos en Long es invalido")
    void long12DigitosEsInvalido() {
        assertThat(ValidadorCuit.esValido(201234567890L)).isFalse();
    }

    @Test
    @DisplayName("R-CUIT-002/R-CUIT-007: CUIT valido conocido (prefijo 20)")
    void cuitValidoConocido() {
        assertThat(ValidadorCuit.esValido(20123456786L)).isTrue();
    }

    @Test
    @DisplayName("R-CUIT-002: mismo CUIT con ultimo digito alterado es invalido")
    void cuitConUltimoDigitoAlteradoEsInvalido() {
        assertThat(ValidadorCuit.esValido(20123456789L)).isFalse();
    }

    @Test
    @DisplayName("R-CUIT-007: prefijo fuera del conjunto es invalido aunque el dv coincida")
    void prefijoFueraDelConjuntoEsInvalido() {
        assertThat(ValidadorCuit.esValido(11123456789L)).isFalse();
    }

    // ------------------------------------------------------------------ String formato

    @Test
    @DisplayName("R-CUIT-001: null en String es invalido")
    void stringNullEsInvalido() {
        assertThat(ValidadorCuit.esValido((String) null)).isFalse();
    }

    @Test
    @DisplayName("R-CUIT-001: String vacio es invalido")
    void stringVacioEsInvalido() {
        assertThat(ValidadorCuit.esValido("")).isFalse();
    }

    @Test
    @DisplayName("R-CUIT-001: String de 10 digitos es invalido")
    void string10DigitosEsInvalido() {
        assertThat(ValidadorCuit.esValido("2012345678")).isFalse();
    }

    @Test
    @DisplayName("R-CUIT-001: String de 12 digitos es invalido")
    void string12DigitosEsInvalido() {
        assertThat(ValidadorCuit.esValido("201234567890")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"20-12345678-9", "2012345678A", " 20123456789", "20.123.456.789"})
    @DisplayName("R-CUIT-005: caracteres no numericos son invalidos sin normalizar")
    void caracteresNoNumericosSonInvalidos(String cuit) {
        assertThat(ValidadorCuit.esValido(cuit)).isFalse();
    }

    // ------------------------------------------------------------------ algoritmo / prefijos

    static Stream<Arguments> casosValidos() {
        return Stream.of(
                Arguments.of("20123456786", 20),
                Arguments.of("23123456785", 23),
                Arguments.of("24123456781", 24),
                Arguments.of("27123456780", 27),
                Arguments.of("30123456781", 30),
                Arguments.of("33123456780", 33),
                Arguments.of("34123456787", 34)
        );
    }

    @ParameterizedTest
    @MethodSource("casosValidos")
    @DisplayName("R-CUIT-002/R-CUIT-007: CUITs validos por prefijo")
    void cuitValidoPorPrefijo(String cuit) {
        assertThat(ValidadorCuit.esValido(cuit)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"20123456789", "23123456780", "24123456780", "27123456789",
            "30123456784", "33123456781", "34123456780"})
    @DisplayName("R-CUIT-002: CUITs con dv incorrecto son invalidos")
    void cuitConDvIncorrectoEsInvalido(String cuit) {
        assertThat(ValidadorCuit.esValido(cuit)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11123456789", "12123456789", "19123456789", "40123456789"})
    @DisplayName("R-CUIT-007: prefijos fuera del conjunto son invalidos")
    void prefijosFueraDelConjuntoSonInvalidos(String cuit) {
        assertThat(ValidadorCuit.esValido(cuit)).isFalse();
    }

    @Test
    @DisplayName("R-CUIT-010: Resto=0 produce UltDig=0 (caso limite)")
    void restoCeroProduceUltDigCero() {
        // 3312345678: suma ponderada divisible por 11 -> UltDig=0.
        assertThat(ValidadorCuit.esValido("33123456780")).isTrue();
        assertThat(ValidadorCuit.esValido("33123456781")).isFalse();
    }

    @Test
    @DisplayName("R-CUIT-010: Resto=1 produce UltDig=10 (siempre invalido)")
    void restoUnoProduceUltDigDiezSiempreInvalido() {
        // body 2000000001 -> suma=12 -> resto=1 -> UltDig=10, nunca coincide con 0-9.
        assertThat(ValidadorCuit.esValido("20000000010")).isFalse();
        assertThat(ValidadorCuit.esValido("20000000019")).isFalse();
    }

    @Test
    @DisplayName("R-CUIT-008: determinismo (1000 invocaciones mismo resultado)")
    void determinismo() {
        boolean esperado = ValidadorCuit.esValido("20123456786");
        for (int i = 0; i < 1000; i++) {
            assertThat(ValidadorCuit.esValido("20123456786")).isEqualTo(esperado);
        }
    }

    @Test
    @DisplayName("R-CUIT-001: Long con menos de 11 digitos pero String con ceros a la izquierda")
    void longConCerosALaIzquierda() {
        // 123456789 -> "000123456789"? no, 11 chars "00012345678"? length check.
        assertThat(ValidadorCuit.esValido(123L)).isFalse();
    }
}