package ar.gob.chaco.atp.puestoscamineros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.domain.DetalleProductoPrimario;
import ar.gob.chaco.atp.puestoscamineros.domain.PaseGuia;
import ar.gob.chaco.atp.puestoscamineros.repository.DetalleProductoPrimarioRepository;
import ar.gob.chaco.atp.puestoscamineros.repository.PaseGuiaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CargaServiceTest {

    @Mock
    private DetalleProductoPrimarioRepository detalleRepository;

    @Mock
    private PaseGuiaRepository paseRepository;

    private CargaService service;

    private static final BigDecimal CUIT = new BigDecimal("20123456789");
    private static final Short SUCU = (short) 1;
    private static final Integer PER_FISC = 2024;
    private static final Short SECU = (short) 1;
    private static final String NRO_FORM = "1234567890";

    @BeforeEach
    void setUp() {
        service = new CargaService(detalleRepository, paseRepository);
    }

    private DetalleProductoPrimario cabecera(String clasif, Short marcAnu) {
        DetalleProductoPrimario d = new DetalleProductoPrimario();
        d.setConCuit(CUIT);
        d.setDppSucu(SUCU);
        d.setCalPerFisc(PER_FISC);
        d.setDppSecu(SECU);
        d.setDppNroForm(NRO_FORM);
        d.setDppClasif(clasif);
        d.setDppMarcAnu(marcAnu);
        return d;
    }

    private void stubPrevios(long countC, int totalPases) {
        when(paseRepository.countByConCuitAndDppSucuAndCalPerFiscAndDppSecuAndDppNroFormAndDppPaseEstAndSucTipo(
                CUIT, SUCU, PER_FISC, SECU, NRO_FORM, "C", "PUE")).thenReturn(countC);
        when(paseRepository.findByConCuitAndDppSucuAndCalPerFiscAndDppSecuAndDppNroForm(
                CUIT, SUCU, PER_FISC, SECU, NRO_FORM)).thenReturn(
                java.util.Collections.nCopies(totalPases, null));
    }

    @Test
    @DisplayName("marcarGuia: formulario no encontrado lanza NotFoundException")
    void formularioNoEncontrado() {
        when(detalleRepository.findById(any())).thenReturn(Optional.empty());
        try {
            service.marcarGuia(CUIT, SUCU, PER_FISC, SECU, NRO_FORM, "OP", "PUE", (short) 5,
                    ContextoSesion.vacio());
            org.junit.jupiter.api.Assertions.fail("esperaba NotFoundException");
        } catch (ar.gob.chaco.atp.puestoscamineros.shared.error.NotFoundException ex) {
            assertThat(ex.getCode()).isEqualTo("NO_ENCONTRADO");
        }
    }

    @Test
    @DisplayName("marcarGuia: guia anulada (DPPMarcAnu=1) devuelve MSG_ANULADA sin registrar pase")
    void guiaAnulada() {
        DetalleProductoPrimario cab = cabecera("N", (short) 1);
        when(detalleRepository.findById(any())).thenReturn(Optional.of(cab));

        ResultadoMarcado r = service.marcarGuia(CUIT, SUCU, PER_FISC, SECU, NRO_FORM, "OP", "PUE", (short) 5,
                ContextoSesion.vacio());

        assertThat(r.exito()).isFalse();
        assertThat(r.duplicado()).isZero();
        assertThat(r.mensaje()).isEqualTo(CargaService.MSG_ANULADA);
        verify(detalleRepository, never()).save(any());
        verify(paseRepository, never()).save(any());
    }

    @Test
    @DisplayName("marcarGuia: guia interna (DPPClasif='I') devuelve MSG_INTERNA sin registrar pase")
    void guiaInterna() {
        DetalleProductoPrimario cab = cabecera("I", (short) 0);
        when(detalleRepository.findById(any())).thenReturn(Optional.of(cab));

        ResultadoMarcado r = service.marcarGuia(CUIT, SUCU, PER_FISC, SECU, NRO_FORM, "OP", "PUE", (short) 5,
                ContextoSesion.vacio());

        assertThat(r.exito()).isFalse();
        assertThat(r.duplicado()).isZero();
        assertThat(r.mensaje()).isEqualTo(CargaService.MSG_INTERNA);
        verify(detalleRepository, never()).save(any());
        verify(paseRepository, never()).save(any());
    }

    @Test
    @DisplayName("marcarGuia: exito con 0 pases previos -> duplicado=1")
    void exitoCeroPrevios() {
        DetalleProductoPrimario cab = cabecera("N", (short) 0);
        when(detalleRepository.findById(any())).thenReturn(Optional.of(cab));
        stubPrevios(0, 0);

        ResultadoMarcado r = service.marcarGuia(CUIT, SUCU, PER_FISC, SECU, NRO_FORM, "OP", "PUE", (short) 5,
                ContextoSesion.completo("OP", "Op", 1, 7, 1, 5, "P5", "PUE", 1, "Resp"));

        assertThat(r.exito()).isTrue();
        assertThat(r.duplicado()).isEqualTo(1);
        assertThat(r.mensaje()).isEqualTo(CargaService.MSG_EXITO);
        verify(detalleRepository).save(cab);
        verify(paseRepository).save(any(PaseGuia.class));
    }

    @Test
    @DisplayName("marcarGuia: exito con 1 pase previo (C,PUE) -> duplicado=2")
    void exitoUnPrevio() {
        DetalleProductoPrimario cab = cabecera("N", (short) 0);
        when(detalleRepository.findById(any())).thenReturn(Optional.of(cab));
        stubPrevios(1, 1);

        ResultadoMarcado r = service.marcarGuia(CUIT, SUCU, PER_FISC, SECU, NRO_FORM, "OP", "PUE", (short) 5,
                ContextoSesion.vacio());

        assertThat(r.exito()).isTrue();
        assertThat(r.duplicado()).isEqualTo(2);
    }

    @Test
    @DisplayName("marcarGuia: exito con 2 pases previos (C,PUE) -> duplicado=3")
    void exitoDosPrevios() {
        DetalleProductoPrimario cab = cabecera("N", (short) 0);
        when(detalleRepository.findById(any())).thenReturn(Optional.of(cab));
        stubPrevios(2, 2);

        ResultadoMarcado r = service.marcarGuia(CUIT, SUCU, PER_FISC, SECU, NRO_FORM, "OP", "PUE", (short) 5,
                ContextoSesion.vacio());

        assertThat(r.exito()).isTrue();
        assertThat(r.duplicado()).isEqualTo(3);
    }

    @Test
    @DisplayName("marcarGuia: cuenta solo pases C/PUE; otros no incrementan duplicado")
    void exitoPaseNoConfirmadoNoIncrementa() {
        DetalleProductoPrimario cab = cabecera("N", (short) 0);
        when(detalleRepository.findById(any())).thenReturn(Optional.of(cab));
        // countBy devuelve 0 aunque existan 3 pases totales no-C/PUE
        stubPrevios(0, 3);

        ResultadoMarcado r = service.marcarGuia(CUIT, SUCU, PER_FISC, SECU, NRO_FORM, "OP", "PUE", (short) 5,
                ContextoSesion.vacio());

        assertThat(r.exito()).isTrue();
        assertThat(r.duplicado()).isEqualTo(1);
    }

    @Test
    @DisplayName("buscarFormularios: blank devuelve lista vacia")
    void buscarBlank() {
        assertThat(service.buscarFormularios(null)).isEmpty();
        assertThat(service.buscarFormularios("   ")).isEmpty();
    }

    @Test
    @DisplayName("buscarFormularios: delega al repositorio con trim")
    void buscarDelega() {
        when(detalleRepository.findByDppNroForm("123")).thenReturn(List.of());
        service.buscarFormularios(" 123 ");
        verify(detalleRepository).findByDppNroForm("123");
    }
}
