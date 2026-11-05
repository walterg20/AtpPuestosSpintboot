package ar.com.atp.puestos.puestos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.com.atp.puestos.puestos.domain.ContadorFormulario;
import ar.com.atp.puestos.puestos.repository.ContadorFormularioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Suite unitaria de la numeración de Actas (spec {@code 09-formularios-acta} §3.11, T-ACTA-129).
 *
 * <p>Replica el {@code F2276_GenNroForm} del legacy: si la fila de {@code GES_SUCN} existe, se
 * incrementa su contador y se devuelve el valor nuevo; si <strong>no</strong> existe, se inserta
 * con {@code SucUltNum = 1} y {@code SucNuId = 'MAN'} y se devuelve {@code 1}.
 */
@ExtendWith(MockitoExtension.class)
class ServicioNumeracionActaTest {

    private static final Integer SUCURSAL = 5;
    private static final String TIPO = "PUE";

    @Mock
    private ContadorFormularioRepositorio contadorRepositorio;

    private ServicioNumeracionActa servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioNumeracionActa(contadorRepositorio);
    }

    @Test
    @DisplayName("R-ACTA-036: si el contador existe se incrementa y se devuelve el valor nuevo")
    void elContadorExiste() {
        when(contadorRepositorio.incrementarAtomico(SUCURSAL, TIPO, 2276)).thenReturn(1);
        when(contadorRepositorio.valorActual(SUCURSAL, TIPO, 2276)).thenReturn(15664);

        int numero = servicio.siguienteNumeroActa(SUCURSAL, TIPO);

        assertThat(numero).isEqualTo(15664);
        verify(contadorRepositorio).incrementarAtomico(SUCURSAL, TIPO, 2276);
    }

    @Test
    @DisplayName("R-ACTA-036: si el contador no existe se inserta con SucUltNum=1 y se devuelve 1")
    void elContadorNoExiste() {
        when(contadorRepositorio.incrementarAtomico(SUCURSAL, TIPO, 2276)).thenReturn(0);

        int numero = servicio.siguienteNumeroActa(SUCURSAL, TIPO);

        assertThat(numero).isEqualTo(1);
        ArgumentCaptor<ContadorFormulario> contador = ArgumentCaptor.forClass(ContadorFormulario.class);
        verify(contadorRepositorio).save(contador.capture());
        ContadorFormulario guardada = contador.getValue();
        assertThat(guardada.getSucNroId()).isEqualTo(SUCURSAL);
        assertThat(guardada.getSucTipo()).isEqualTo(TIPO);
        assertThat(guardada.getSucNuForm()).isEqualTo(2276);
        assertThat(guardada.getSucNuId()).isEqualTo("MAN");
        assertThat(guardada.getSucUltNum()).isEqualTo(1);
    }

    @Test
    @DisplayName("R-ACTA-036: el proximo numero sugerido no incrementa el contador")
    void elProximoNumeroNoIncrementa() {
        when(contadorRepositorio.valorActual(SUCURSAL, TIPO, 2276)).thenReturn(15663);

        Integer sugerido = servicio.proximoNumeroActa(SUCURSAL, TIPO);

        assertThat(sugerido).isEqualTo(15664);
        verify(contadorRepositorio, org.mockito.Mockito.never())
                .incrementarAtomico(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("R-ACTA-036: sin fila de contador el proximo numero sugerido es 1")
    void elProximoNumeroSinContador() {
        when(contadorRepositorio.valorActual(SUCURSAL, TIPO, 2276)).thenReturn(null);

        assertThat(servicio.proximoNumeroActa(SUCURSAL, TIPO)).isEqualTo(1);
    }

    @Test
    @DisplayName("R-ACTA-036: sin sucursal ni tipo no hay proximo numero")
    void elProximoNumeroSinContexto() {
        assertThat(servicio.proximoNumeroActa(null, null)).isNull();
        assertThat(servicio.proximoNumeroActa(SUCURSAL, null)).isNull();
    }

    @Test
    @DisplayName("D-ACTA-19: el tipo de sucursal se recorta antes de consultar el contador")
    void elTipoDeSucursalSeRecorta() {
        when(contadorRepositorio.incrementarAtomico(SUCURSAL, TIPO, 2276)).thenReturn(1);
        when(contadorRepositorio.valorActual(SUCURSAL, TIPO, 2276)).thenReturn(5);

        assertThat(servicio.siguienteNumeroActa(SUCURSAL, " PUE ")).isEqualTo(5);
    }

    @Test
    @DisplayName("R-ACTA-036: el numero de formulario de las Actas es 2276")
    void elNumeroDeFormulario() {
        assertThat(ServicioNumeracionActa.NUMERO_FORMULARIO_ACTAS).isEqualTo(2276);
        assertThat(ServicioNumeracionActa.VALOR_POR_DEFECTO_SUC_NU_ID).isEqualTo("MAN");
        verify(contadorRepositorio, org.mockito.Mockito.never())
                .incrementarAtomico(org.mockito.ArgumentMatchers.any(), any(),
                        org.mockito.ArgumentMatchers.any());
    }
}
