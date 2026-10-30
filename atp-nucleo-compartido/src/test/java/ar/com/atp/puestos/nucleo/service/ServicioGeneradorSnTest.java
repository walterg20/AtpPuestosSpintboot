package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.com.atp.puestos.nucleo.domain.SnSinDev;
import ar.com.atp.puestos.nucleo.domain.Turno;
import ar.com.atp.puestos.nucleo.domain.TurnoId;
import ar.com.atp.puestos.nucleo.repository.FeriadoRepository;
import ar.com.atp.puestos.nucleo.repository.Formulario2276Repositorio;
import ar.com.atp.puestos.nucleo.repository.SnSinDevRepositorio;
import ar.com.atp.puestos.nucleo.repository.TurnoRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Módulo {@code 06-turnos} — pruebas de la generación del SN del volante de pago (spec R-TUR-015,
 * R-TUR-016, R-TUR-017; T-TUR-E1).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServicioGeneradorSnTest {

    private static final Integer SUCURSAL = 72;
    private static final String TIPO_REC = "REC";
    private static final String TIPO_PUE = "PUE";
    private static final Integer TURNO = 5;

    @Mock
    private Formulario2276Repositorio formularioRepositorio;
    @Mock
    private SnSinDevRepositorio snRepositorio;
    @Mock
    private TurnoRepositorio turnoRepositorio;
    @Mock
    private FeriadoRepository feriadoRepository;

    private ServicioGeneradorSn servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioGeneradorSn(formularioRepositorio, snRepositorio, turnoRepositorio,
                feriadoRepository);
    }

    @Test
    @DisplayName("R-TUR-015: sin monto no se genera SN y el cierre sigue siendo exitoso")
    void sinMontoNoSeGeneraElSn() {
        when(formularioRepositorio.totalizarTurno(SUCURSAL, TIPO_REC, TURNO))
                .thenReturn(Collections.singletonList(fila(0, 0, 0, 0, 0, 0)));

        BigDecimal numero = servicio.generar(SUCURSAL, TIPO_REC, TURNO);

        assertThat(numero).isNull();
        verify(snRepositorio, never()).save(any());
        verify(snRepositorio, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("R-TUR-016: con monto se inserta el SN y se referencia en PPRIM_TU.SNSinNum")
    void conMontoSeInsertaElSnYSeReferenciaEnElTurno() {
        // totFletes=10, totAdCM=1, totAdFletes=2, totAdIB=3, totIB=20, totCM=5
        // => TotalIB = 20 + 10 + 5 = 35 ; TotalAd = 3 + 1 + 2 = 6
        when(formularioRepositorio.totalizarTurno(SUCURSAL, TIPO_REC, TURNO))
                .thenReturn(Collections.singletonList(fila(10, 1, 2, 3, 20, 5)));
        when(snRepositorio.saveAndFlush(any())).thenAnswer(invocacion -> {
            SnSinDev sn = invocacion.getArgument(0);
            sn.setSnSinNum(new BigDecimal("2858160"));
            return sn;
        });
        Turno turno = new Turno(new TurnoId(SUCURSAL, TIPO_REC, TURNO));
        when(turnoRepositorio.findById(any())).thenReturn(Optional.of(turno));

        BigDecimal numero = servicio.generar(SUCURSAL, TIPO_REC, TURNO);

        assertThat(numero).isEqualByComparingTo("2858160");

        ArgumentCaptor<SnSinDev> captor = ArgumentCaptor.forClass(SnSinDev.class);
        verify(snRepositorio).saveAndFlush(captor.capture());
        SnSinDev sn = captor.getValue();
        assertThat(sn.getCodForm()).isEqualTo(126);
        assertThat(sn.getSnSinCuit()).isEqualByComparingTo("33999176769");
        assertThat(sn.getSnSinTotIB()).isEqualByComparingTo("35");
        assertThat(sn.getSnSinAdic1()).isEqualByComparingTo("6");
        assertThat(sn.getSnSinDpcia()).isEqualTo(SUCURSAL);
        assertThat(sn.getSnSinTipLe()).isEqualTo(2);
        assertThat(sn.getSnSinIdVol()).isEqualTo(6);
        assertThat(sn.getSnSinCuo()).isEqualTo(LocalDate.now().getMonthValue());
        assertThat(sn.getSnSinAnio()).isEqualTo(LocalDate.now().getYear());
        assertThat(sn.getSnSinFPres()).isEqualTo(LocalDate.now().atStartOfDay());

        assertThat(turno.getSnSinNum()).isEqualByComparingTo("2858160");
        verify(turnoRepositorio).save(turno);
    }

    @Test
    @DisplayName("R-TUR-017: en REC el vencimiento son 2 dias habiles")
    void enRecElVencimientoSonDosDiasHabiles() {
        LocalDate hoy = LocalDate.of(2026, 3, 12);
        // Ni el 12 ni el 13 de marzo son feriados: el vencimiento es el 14.
        when(feriadoRepository.existsByFerFecha(any())).thenReturn(false);

        LocalDateTime vencimiento = servicio.vencimiento(TIPO_REC, hoy);

        assertThat(vencimiento).isEqualTo(hoy.plusDays(2).atStartOfDay());
    }

    @Test
    @DisplayName("R-TUR-017: los feriados no cuentan como dias habiles")
    void losFeriadosNoCuentanComoDiasHabiles() {
        LocalDate hoy = LocalDate.of(2026, 3, 12);
        // El 13 de marzo es feriado: el 12 y el 14 son los dos dias habiles y el vencimiento
        // es el dia siguiente al ultimo (el 15), igual que en el legacy.
        when(feriadoRepository.existsByFerFecha(eq(hoy.plusDays(1).atStartOfDay())))
                .thenReturn(true);

        LocalDateTime vencimiento = servicio.vencimiento(TIPO_REC, hoy);

        assertThat(vencimiento).isEqualTo(hoy.plusDays(3).atStartOfDay());
    }

    @Test
    @DisplayName("R-TUR-017: en el resto de los tipos el vencimiento es serverdate() + 15")
    void enElRestoElVencimientoEsQuinceDias() {
        LocalDate hoy = LocalDate.of(2026, 3, 12);

        LocalDateTime vencimiento = servicio.vencimiento(TIPO_PUE, hoy);

        assertThat(vencimiento).isEqualTo(hoy.plusDays(15).atStartOfDay());
    }

    @Test
    @DisplayName("R-TUR-015: la totalizacion devuelve ceros cuando el turno no tiene formularios")
    void laTotalizacionDevuelveCerosSinFormularios() {
        when(formularioRepositorio.totalizarTurno(SUCURSAL, TIPO_REC, TURNO)).thenReturn(List.of());

        assertThat(servicio.totalizar(SUCURSAL, TIPO_REC, TURNO).totalIB())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * Fila del agregado en el orden del {@code SELECT} del repositorio: {@code totFletes},
     * {@code totAdCM}, {@code totAdFletes}, {@code totAdIB}, {@code totIB}, {@code totCM}.
     */
    private Object[] fila(int totFletes, int totAdCM, int totAdFletes, int totAdIB, int totIB,
            int totCM) {
        return new Object[] {
                BigDecimal.valueOf(totFletes), BigDecimal.valueOf(totAdCM),
                BigDecimal.valueOf(totAdFletes), BigDecimal.valueOf(totAdIB),
                BigDecimal.valueOf(totIB), BigDecimal.valueOf(totCM) };
    }
}
