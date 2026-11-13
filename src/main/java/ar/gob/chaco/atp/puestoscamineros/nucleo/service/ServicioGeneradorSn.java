package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.SnSinDev;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.TotalesFormulario2276;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Turno;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.TurnoId;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.FeriadoRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.Formulario2276Repositorio;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.SnSinDevRepositorio;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.TurnoRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generación del SN del volante de pago al cerrar un turno de receptoría (spec {@code 06-turnos}
 * R-TUR-014..017, T-TUR-B5).
 *
 * <p>Reproduce el procedimiento legacy {@code TurnoGenerarSN}:
 *
 * <ol>
 *   <li>totaliza los {@code F2276} del turno <strong>excluyendo los anulados</strong>
 *       ({@code F276Estad <> 'A'}): {@code TotIB}, {@code TotFletes}, {@code TotCM},
 *       {@code TotAdCM}, {@code TotAdFletes} y {@code TotAdIB} (R-TUR-015);</li>
 *   <li>si el turno no tiene monto, <strong>no</strong> genera SN y devuelve éxito (R-TUR-015);</li>
 *   <li>si tiene monto, inserta la fila en {@code SNSINDEV} y deja el número generado referenciado
 *       en {@code PPRIM_TU.SNSinNum} (R-TUR-016).</li>
 * </ol>
 *
 * <p><strong>Rollback</strong>: si la inserción del SN falla, el método propaga la excepción y la
 * transacción del cierre completo se revierte, con lo que el turno queda abierto (R-TUR-014). Por
 * eso el método <strong>no</strong> captura la excepción: el {@code rollback} del legacy
 * ({@code rollback} / {@code &Error = true}) es exactamente eso.
 */
@Service
public class ServicioGeneradorSn {

    /** Código de formulario del volante de pago 2276 (R-TUR-016). */
    public static final int COD_FORM_VOLANTE_PAGO = 126;

    /** CUIT emisor: RENTAS (R-TUR-016). */
    public static final BigDecimal CUIT_RENTAS = new BigDecimal("33999176769");

    /** Tipo de liquidación: INTERNET (R-TUR-016). */
    public static final int TIPO_LIQUIDACION_INTERNET = 2;

    /** Identificador del volante: MISVOLPAGO (R-TUR-016). */
    public static final int ID_VOLANTE_PAGO = 6;

    /** Días hábiles de vencimiento para receptoría (R-TUR-017). */
    public static final int DIAS_HABILES_VENCIMIENTO_REC = 2;

    /** Días de vencimiento para el resto de los tipos (R-TUR-017). */
    public static final int DIAS_VENCIMIENTO_OTROS = 15;

    private final Formulario2276Repositorio formularioRepositorio;
    private final SnSinDevRepositorio snRepositorio;
    private final TurnoRepositorio turnoRepositorio;
    private final FeriadoRepository feriadoRepository;

    public ServicioGeneradorSn(Formulario2276Repositorio formularioRepositorio,
            SnSinDevRepositorio snRepositorio, TurnoRepositorio turnoRepositorio,
            FeriadoRepository feriadoRepository) {
        this.formularioRepositorio = formularioRepositorio;
        this.snRepositorio = snRepositorio;
        this.turnoRepositorio = turnoRepositorio;
        this.feriadoRepository = feriadoRepository;
    }

    /**
     * Genera el SN del volante de pago del turno y lo referencia en {@code PPRIM_TU.SNSinNum}.
     *
     * @param sucNroId sucursal del turno
     * @param sucTipo tipo de sucursal del turno
     * @param turNroId número de turno
     * @return el número identificatorio generado, o {@code null} si el turno no tiene monto y por
     *         lo tanto no corresponde generar SN (R-TUR-015)
     * @throws RuntimeException si la inserción del SN falla (el cierre se revierte)
     */
    @Transactional
    public BigDecimal generar(Integer sucNroId, String sucTipo, Integer turNroId) {
        TotalesFormulario2276 totales = totalizar(sucNroId, sucTipo, turNroId);
        if (totales.sinMonto()) {
            // Sin monto no hay volante de pago: el legacy devuelve éxito sin generar nada.
            return null;
        }

        LocalDate hoy = LocalDate.now();
        SnSinDev sn = new SnSinDev();
        sn.setCodForm(COD_FORM_VOLANTE_PAGO);
        sn.setSnSinCuit(CUIT_RENTAS);
        sn.setSnSinCuo(hoy.getMonthValue());
        sn.setSnSinAnio(hoy.getYear());
        sn.setSnSinTotIB(totales.totalIB());
        sn.setSnSinAdic1(totales.totalAd());
        sn.setSnSinFPres(hoy.atStartOfDay());
        sn.setSnSinDpcia(sucNroId);
        sn.setSnSinFVto(vencimiento(sucTipo, hoy));
        sn.setSnSinTipLe(TIPO_LIQUIDACION_INTERNET);
        sn.setSnSinIdVol(ID_VOLANTE_PAGO);
        sn.setSnSinCorrG(BigDecimal.ZERO);
        sn.setSnSinRPIIm(BigDecimal.ZERO);
        sn.setSnSinPunDe(BigDecimal.ZERO);

        // saveAndFlush: el numero identificatorio es IDENTITY y hace falta ya para referenciarlo.
        SnSinDev guardada = snRepositorio.saveAndFlush(sn);
        BigDecimal numero = guardada.getSnSinNum();

        // Una vez cargado el SN, se relaciona con el turno correspondiente.
        Turno turno = turnoRepositorio.findById(new TurnoId(sucNroId, TurnoMapper.trim(sucTipo), turNroId))
                .orElseThrow(() -> new IllegalStateException(
                        "El turno " + turNroId + " no existe al generar el SN del volante de pago."));
        turno.setSnSinNum(numero);
        turnoRepositorio.save(turno);
        return numero;
    }

    /**
     * Totaliza los importes de los formularios del turno (R-TUR-015).
     *
     * @param sucNroId sucursal del turno
     * @param sucTipo tipo de sucursal del turno
     * @param turNroId número de turno
     * @return los totales (nunca {@code null})
     */
    @Transactional(readOnly = true)
    public TotalesFormulario2276 totalizar(Integer sucNroId, String sucTipo, Integer turNroId) {
        List<Object[]> filas = formularioRepositorio.totalizarTurno(sucNroId, TurnoMapper.trim(sucTipo),
                turNroId);
        if (filas == null || filas.isEmpty()) {
            return new TotalesFormulario2276(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return TotalesFormulario2276.deFila(filas.get(0));
    }

    /**
     * Fecha de vencimiento del SN (R-TUR-017).
     *
     * <p>Para receptoría son 2 días hábiles; para el resto, {@code serverdate() + 15}. El cálculo
     * de días hábiles es el del legacy ({@code Nuc_CalcularVencimientoXDias}): se cuentan hacia
     * adelante los días que <strong>no</strong> están en {@code FERIADOS}, empezando por el propio
     * día de emisión.
     *
     * @param sucTipo tipo de sucursal del turno
     * @param hoy día de emisión
     * @return la fecha de vencimiento (a medianoche)
     */
    public LocalDateTime vencimiento(String sucTipo, LocalDate hoy) {
        if ("REC".equals(TurnoMapper.trim(sucTipo))) {
            return calcularVencimientoDiasHabiles(hoy, DIAS_HABILES_VENCIMIENTO_REC);
        }
        return hoy.plusDays(DIAS_VENCIMIENTO_OTROS).atStartOfDay();
    }

    /**
     * Vencimiento a {@code cantDias} días hábiles (procedimiento legacy
     * {@code Nuc_CalcularVencimientoXDias}).
     *
     * @param desde día de emisión
     * @param cantDias cantidad de días hábiles a contar
     * @return la fecha de vencimiento (a medianoche)
     */
    public LocalDateTime calcularVencimientoDiasHabiles(LocalDate desde, int cantDias) {
        LocalDate fecha = desde;
        int restantes = cantDias;
        while (restantes > 0) {
            if (!feriadoRepository.existsByFerFecha(fecha.atStartOfDay())) {
                restantes--;
            }
            fecha = fecha.plusDays(1);
        }
        return fecha.atStartOfDay();
    }
}
