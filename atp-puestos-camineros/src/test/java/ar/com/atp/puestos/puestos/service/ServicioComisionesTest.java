package ar.com.atp.puestos.puestos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import ar.com.atp.puestos.nucleo.domain.Comision;
import ar.com.atp.puestos.nucleo.domain.ComisionId;
import ar.com.atp.puestos.nucleo.domain.Persona;
import ar.com.atp.puestos.nucleo.repository.ComisionIntegranteRepository;
import ar.com.atp.puestos.nucleo.repository.ComisionRepository;
import ar.com.atp.puestos.puestos.service.dto.comision.ComisionFilaView;
import ar.com.atp.puestos.puestos.service.dto.comision.ComisionGeneralView;
import ar.com.atp.puestos.puestos.service.dto.comision.IntegranteView;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ServicioComisionesTest {

    @Mock
    private ComisionRepository comisionRepository;
    @Mock
    private ComisionIntegranteRepository comisionIntegranteRepository;

    private ServicioComisiones service;

    @BeforeEach
    void setUp() {
        service = new ServicioComisiones(comisionRepository, comisionIntegranteRepository);
    }

    @Test
    void decodificarEstadoCerrado() {
        assertThat(DecodificadorEstadoComision.decodificar("S")).isEqualTo("Cerrado");
    }

    @Test
    void decodificarEstadoAbierto() {
        assertThat(DecodificadorEstadoComision.decodificar("N")).isEqualTo("Abierto");
    }

    @Test
    void decodificarEstadoNull() {
        assertThat(DecodificadorEstadoComision.decodificar(null)).isEqualTo("Abierto");
    }

    @Test
    void listarAplicaFiltroPueCodYPersonaId() {
        Comision c = comision(1, 5, 10, "N");
        Persona p = new Persona(42, "RESPONSABLE", null);
        when(comisionRepository.findComisionesConResponsable(eq(5), eq(42), any()))
                .thenReturn((Page<Object[]>) new PageImpl<Object[]>(List.<Object[]>of(new Object[] { c, p }),
                        PageRequest.of(0, 10), 1));

        Page<ComisionFilaView> resultado = service.listar(5, 42, 0);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).responsable()).isEqualTo("RESPONSABLE");
    }

    @Test
    void listarOmitePersonaIdCuandoEsCero() {
        Comision c = comision(2, 5, 10, "N");
        when(comisionRepository.findComisionesConResponsable(eq(5), eq(null), any()))
                .thenReturn((Page<Object[]>) new PageImpl<Object[]>(List.<Object[]>of(new Object[] { c, null }),
                        PageRequest.of(0, 10), 1));

        Page<ComisionFilaView> resultado = service.listar(5, 0, 0);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).responsable()).isEqualTo("");
    }

    @Test
    void listarOmitePersonaIdCuandoEsNull() {
        Comision c = comision(3, 5, 10, "S");
        when(comisionRepository.findComisionesConResponsable(eq(5), eq(null), any()))
                .thenReturn((Page<Object[]>) new PageImpl<Object[]>(List.<Object[]>of(new Object[] { c, null }),
                        PageRequest.of(0, 10), 1));

        Page<ComisionFilaView> resultado = service.listar(5, null, 0);

        assertThat(resultado.getContent().get(0).estado()).isEqualTo("Cerrado");
    }

    @Test
    void listarOrdenaPorCpcIdDesc() {
        Comision c1 = comision(5, 5, 10, "N");
        Comision c2 = comision(3, 5, 10, "N");
        when(comisionRepository.findComisionesConResponsable(eq(5), eq(null), any()))
                .thenReturn((Page<Object[]>) new PageImpl<Object[]>(
                        List.<Object[]>of(new Object[] { c1, null }, new Object[] { c2, null }),
                        PageRequest.of(0, 10), 2));

        Page<ComisionFilaView> resultado = service.listar(5, null, 0);

        assertThat(resultado.getContent()).extracting(ComisionFilaView::cpcId)
                .containsExactly(5, 3);
    }

    @Test
    void horaFormateadaConCeroALaIzquierda() {
        Comision c = comision(1, 5, 10, "N");
        c.setCpcHorDes(5);
        c.setCpcHorHas(1230);
        when(comisionRepository.findComisionesConResponsable(eq(5), eq(null), any()))
                .thenReturn((Page<Object[]>) new PageImpl<Object[]>(List.<Object[]>of(new Object[] { c, null }),
                        PageRequest.of(0, 10), 1));

        ComisionFilaView fila = service.listar(5, null, 0).getContent().get(0);

        assertThat(fila.horaInicio()).isEqualTo("0005");
        assertThat(fila.horaFin()).isEqualTo("1230");
    }

    @Test
    void fechaFormateadaDdMmYy() {
        Comision c = comision(1, 5, 10, "N");
        c.setCpcFchDes(LocalDateTime.of(2025, 3, 7, 0, 0));
        c.setCpcFchHas(null);
        when(comisionRepository.findComisionesConResponsable(eq(5), eq(null), any()))
                .thenReturn((Page<Object[]>) new PageImpl<Object[]>(List.<Object[]>of(new Object[] { c, null }),
                        PageRequest.of(0, 10), 1));

        ComisionFilaView fila = service.listar(5, null, 0).getContent().get(0);

        assertThat(fila.fechaInicio()).isEqualTo("07/03/25");
        assertThat(fila.fechaFin()).isEqualTo("");
    }

    @Test
    void listarIntegrantesVacio() {
        when(comisionIntegranteRepository.findIntegrantesConNombre(eq(5), eq(10)))
                .thenReturn(List.<Object[]>of());

        List<IntegranteView> resultado = service.listarIntegrantes(5, 10);

        assertThat(resultado).isEmpty();
    }

    @Test
    void listarIntegrantesMapeaNombre() {
        ar.com.atp.puestos.nucleo.domain.ComisionIntegrante i =
                new ar.com.atp.puestos.nucleo.domain.ComisionIntegrante(
                        new ar.com.atp.puestos.nucleo.domain.ComisionIntegranteId(5, 10, 7));
        Persona p = new Persona(7, "  JUAN PEREZ  ", null);
        when(comisionIntegranteRepository.findIntegrantesConNombre(eq(5), eq(10)))
                .thenReturn(List.<Object[]>of(new Object[] { i, p }));

        List<IntegranteView> resultado = service.listarIntegrantes(5, 10);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).cpcPerId()).isEqualTo(7);
        assertThat(resultado.get(0).perNomyApe()).isEqualTo("JUAN PEREZ");
    }

    @Test
    void obtenerDetalleExistente() {
        Comision c = comision(1, 5, 10, "N");
        c.setCpcResId(42);
        Persona p = new Persona(42, "RESPONSABLE", null);
        when(comisionRepository.findComisionConResponsable(eq(5), eq(10)))
                .thenReturn(List.<Object[]>of(new Object[] { c, p }));
        when(comisionRepository.findPueDescByPueCod(eq(5)))
                .thenReturn(java.util.Optional.of("Puesto 5"));

        ComisionGeneralView detalle = service.obtenerDetalle(5, 10);

        assertThat(detalle).isNotNull();
        assertThat(detalle.pueDesc()).isEqualTo("Puesto 5");
        assertThat(detalle.responsable()).isEqualTo("RESPONSABLE");
        assertThat(detalle.estado()).isEqualTo("Abierto");
        assertThat(detalle.cpcResId()).isEqualTo(42);
    }

    @Test
    void obtenerDetalleInexistente() {
        when(comisionRepository.findComisionConResponsable(eq(5), eq(999)))
                .thenReturn(List.<Object[]>of());

        ComisionGeneralView detalle = service.obtenerDetalle(5, 999);

        assertThat(detalle).isNull();
    }

    @Test
    void obtenerDetallePueDescVacioSiNoHayFila() {
        Comision c = comision(1, 5, 10, "S");
        when(comisionRepository.findComisionConResponsable(eq(5), eq(10)))
                .thenReturn(List.<Object[]>of(new Object[] { c, null }));
        when(comisionRepository.findPueDescByPueCod(eq(5)))
                .thenReturn(java.util.Optional.empty());

        ComisionGeneralView detalle = service.obtenerDetalle(5, 10);

        assertThat(detalle.pueDesc()).isEqualTo("");
    }

    private static Comision comision(int cpcId, int pueCod, int horDes, String cierre) {
        Comision c = new Comision(new ComisionId(pueCod, cpcId));
        c.setCpcHorDes(horDes);
        c.setCpcCierre(cierre);
        return c;
    }
}
