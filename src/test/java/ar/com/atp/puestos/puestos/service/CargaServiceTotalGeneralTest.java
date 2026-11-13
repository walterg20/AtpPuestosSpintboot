package ar.gob.chaco.atp.puestoscamineros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ar.gob.chaco.atp.puestoscamineros.domain.DetalleProductoPrimario;
import ar.gob.chaco.atp.puestoscamineros.repository.DetalleProductoPrimarioRepository;
import ar.gob.chaco.atp.puestoscamineros.repository.PaseGuiaRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CargaServiceTotalGeneralTest {

    private final CargaService service =
            new CargaService(mock(DetalleProductoPrimarioRepository.class),
                    mock(PaseGuiaRepository.class));

    @Test
    void sumaSeisComponentesConValoresConocidos() {
        DetalleProductoPrimario d = new DetalleProductoPrimario();
        d.setDppAdCMTot(new BigDecimal("10.5"));
        d.setDppAdIBTot(new BigDecimal("20.25"));
        d.setDppAdicFle(new BigDecimal("5.0"));
        d.setDppImCMTot(new BigDecimal("3.0"));
        d.setDppImIBTot(new BigDecimal("1.5"));
        d.setDppImpuFle(new BigDecimal("0.75"));

        assertThat(service.calcularTotalGeneral(d))
                .isEqualByComparingTo(new BigDecimal("41.0"));
    }

    @Test
    void todosNullDevuelveCero() {
        DetalleProductoPrimario d = new DetalleProductoPrimario();

        assertThat(service.calcularTotalGeneral(d))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void detalleNullDevuelveCero() {
        assertThat(service.calcularTotalGeneral(null))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void valoresParcialesSumaSoloNoNull() {
        DetalleProductoPrimario d = new DetalleProductoPrimario();
        d.setDppAdCMTot(new BigDecimal("10.5"));
        d.setDppAdicFle(new BigDecimal("5.0"));
        d.setDppImpuFle(new BigDecimal("0.75"));

        assertThat(service.calcularTotalGeneral(d))
                .isEqualByComparingTo(new BigDecimal("16.25"));
    }
}
