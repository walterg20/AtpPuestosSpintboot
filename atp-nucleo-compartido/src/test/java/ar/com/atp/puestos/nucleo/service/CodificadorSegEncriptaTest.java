package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * T-104 / R-LOG-002 / R-LOG-003: reproducción exacta del procedimiento {@code Seg_Encripta}.
 *
 * <p>Los vectores de verificación están transcritos de la spec (§3.2) y <strong>no</strong> se
 * copian de la implementación: si el codificador cambia, el test falla.
 */
class CodificadorSegEncriptaTest {

    private final CodificadorSegEncripta codificador = new CodificadorSegEncripta();

    @Test
    @DisplayName("R-LOG-002: reproduce los 7 vectores de verificacion de la spec")
    void reproduceLosVectoresDeLaSpec() {
        assertThat(codificador.codificar("ABC123")).isEqualTo("@?>PON");
        assertThat(codificador.codificar("1234567890")).isEqualTo("PONMLKJIHQ");
        assertThat(codificador.codificar("YOLOFFG")).isEqualTo("(252;;:");
        assertThat(codificador.codificar("YOLOFFG   ")).isEqualTo("(252;;:bbb");
        assertThat(codificador.codificar("SCARRASCO ")).isEqualTo(".>@//@.>2b");
        assertThat(codificador.codificar("Z")).isEqualTo("a");
        assertThat(codificador.codificar(" ")).isEqualTo("b");
    }

    @Test
    @DisplayName("R-LOG-001: la normalizacion a mayusculas ocurre antes de codificar")
    void normalizaAMayusculas() {
        assertThat(codificador.codificar("abc123")).isEqualTo(codificador.codificar("ABC123"));
        assertThat(codificador.codificar("yoloffg   ")).isEqualTo("(252;;:bbb");
    }

    @Test
    @DisplayName("R-LOG-002: los caracteres fuera del alfabeto se descartan")
    void descartaLosCaracteresFueraDelAlfabeto() {
        // La enye no pertenece al alfabeto: se descarta y la salida queda mas corta.
        assertThat(codificador.codificar("ÑUEZ")).hasSize(3);
        assertThat(codificador.codificar("ÑUEZ")).isEqualTo(codificador.codificar("UEZ"));
    }

    @Test
    @DisplayName("R-LOG-002: el recorrido se limita a las 10 posiciones del legacy")
    void recorreComoMaximoDiezPosiciones() {
        // 12 caracteres: solo se recorren los 10 primeros.
        assertThat(codificador.codificar("ABCDEFGHIJKL")).isEqualTo(codificador.codificar("ABCDEFGHIJ"));
    }

    @Test
    @DisplayName("R-LOG-003: la codificacion es reversible")
    void laCodificacionEsReversible() {
        assertThat(codificador.decodificar("(252;;:bbb")).isEqualTo("YOLOFFG   ");
        assertThat(codificador.decodificar("@?>PON")).isEqualTo("ABC123");
        assertThat(codificador.decodificar(codificador.codificar("SCARRASCO ")))
                .isEqualTo("SCARRASCO ");
    }

    @Test
    @DisplayName("R-LOG-006: la comparaicion ignora el relleno de espacios final")
    void laComparacionIgnoraElRellenoFinal() {
        assertThat(codificador.coincideCon("@?>PON", "@?>PON  ")).isTrue();
        assertThat(codificador.coincideCon("@?>PON  ", "@?>PON")).isTrue();
        assertThat(codificador.coincideCon("@?>PON", "@?>PON")).isTrue();
        assertThat(codificador.coincideCon("@?>PON", "@?>POM")).isFalse();
        assertThat(codificador.coincideCon(null, "@?>PON")).isFalse();
        assertThat(codificador.coincideCon("@?>PON", null)).isFalse();
    }

    @Test
    @DisplayName("R-LOG-002: la entrada nula o vacia no produce excepcion")
    void laEntradaVaciaNoProduceExcepcion() {
        assertThat(codificador.codificar(null)).isEmpty();
        assertThat(codificador.codificar("")).isEmpty();
        assertThat(codificador.decodificar(null)).isEmpty();
        assertThat(codificador.decodificar("")).isEmpty();
    }
}
