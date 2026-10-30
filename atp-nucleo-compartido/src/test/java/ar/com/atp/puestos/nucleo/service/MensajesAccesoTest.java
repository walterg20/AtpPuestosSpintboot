package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * T-105 / T-303 / R-LOG-018: los 7 pares (código → mensaje) de la tabla única
 * {@link MensajesAcceso}.
 *
 * <p>Los textos esperados están transcritos <strong>carácter a carácter</strong> de la spec
 * (§3.4) — <strong>no</strong> de las constantes de producción — de modo que el test falla si un
 * texto difiere en un solo carácter. Se conservan las erratas del legacy: "esta" sin tilde y
 * "Receptoria" sin tilde.
 */
class MensajesAccesoTest {

    static Stream<Arguments> pares() {
        return Stream.of(
                Arguments.of(0, ""),
                Arguments.of(1, "Usuario/Contraseña Incorrecto"),
                Arguments.of(2, "Usuario/Contraseña Incorrecto"),
                Arguments.of(3, "Este Usuario no esta relacionado con una Persona del sistema"),
                Arguments.of(4, "No existe Receptoria o puesto caminero asignado"),
                Arguments.of(5, "Debe cambiar su clave desde el Sistema Integrado (win)"),
                Arguments.of(6, "Dispositivo no habilitado para el uso en el puesto caminero, "
                        + "contacte con el administrador"));
    }

    @ParameterizedTest(name = "codigo {0} => \"{1}\"")
    @MethodSource("pares")
    @DisplayName("R-LOG-018: el mensaje de cada codigo es verbatim del legacy")
    void losMensajesSonVerbatim(int codigo, String esperado) {
        assertThat(MensajesAcceso.para(codigo)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "codigo {0}")
    @MethodSource("pares")
    @DisplayName("R-LOG-018: los 7 codigos estan en la tabla")
    void losSieteCodigosEstanEnLaTabla(int codigo, String esperado) {
        assertThat(MensajesAcceso.esConocido(codigo)).isTrue();
        assertThat(MensajesAcceso.para(codigo).length()).isEqualTo(esperado.length());
    }

    @Test
    @DisplayName("R-LOG-018: las erratas del legacy se conservan (esta, Receptoria)")
    void lasErratasDelLegacySeConservan() {
        assertThat(MensajesAcceso.para(3)).contains("no esta relacionado");
        assertThat(MensajesAcceso.para(3)).doesNotContain("no está relacionado");
        assertThat(MensajesAcceso.para(4)).contains("Receptoria");
        assertThat(MensajesAcceso.para(4)).doesNotContain("Receptoría");
    }

    @Test
    @DisplayName("R-LOG-018: un codigo fuera de la tabla no produce mensaje ni excepcion")
    void unCodigoFueraDeLaTablaNoProduceMensaje() {
        assertThat(MensajesAcceso.para(7)).isEmpty();
        assertThat(MensajesAcceso.para(-1)).isEmpty();
        assertThat(MensajesAcceso.esConocido(7)).isFalse();
    }
}
