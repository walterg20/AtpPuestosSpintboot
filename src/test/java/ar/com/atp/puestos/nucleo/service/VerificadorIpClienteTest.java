package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * T-111 / R-LOG-013: el chequeo de compatibilidad de la IP con el prefijo de red del puesto.
 *
 * <p>El test documenta la <strong>semántica intencional</strong> elegida (SA-LOG-01): el prefijo
 * configurado debe matchear la IP y un prefijo vacío <strong>no restringe</strong>. La semántica
 * literal de {@code IndexOf} del runtime GeneXus 18 sigue sin confirmar (SA-LOG-01 abierto).
 */
class VerificadorIpClienteTest {

    private final VerificadorIpCliente verificador = new VerificadorIpCliente();

    @Test
    @DisplayName("R-LOG-013: la IP que empieza con el prefijo es compatible")
    void laIpQueEmpiezaConElPrefijoEsCompatible() {
        assertThat(verificador.esCompatible("10.13.26.55", "10.13.26")).isTrue();
        assertThat(verificador.esCompatible("100.100.100.7", "100.100.100")).isTrue();
    }

    @Test
    @DisplayName("R-LOG-013: la IP que no empieza con el prefijo NO es compatible")
    void laIpQueNoEmpiezaConElPrefijoNoEsCompatible() {
        assertThat(verificador.esCompatible("192.168.0.10", "10.13.26")).isFalse();
        assertThat(verificador.esCompatible("10.13.2.6", "10.13.26")).isFalse();
        assertThat(verificador.esCompatible(null, "10.13.26")).isFalse();
    }

    @Test
    @DisplayName("R-LOG-013: un prefijo vacio NO restringe el acceso (11 de los 14 puestos)")
    void unPrefijoVacioNoRestringe() {
        assertThat(verificador.esCompatible("192.168.0.10", "")).isTrue();
        assertThat(verificador.esCompatible("192.168.0.10", "   ")).isTrue();
        assertThat(verificador.esCompatible("192.168.0.10", null)).isTrue();
        assertThat(verificador.esCompatible(null, null)).isTrue();
    }
}
