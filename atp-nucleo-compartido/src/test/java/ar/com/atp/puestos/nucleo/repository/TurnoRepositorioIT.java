package ar.com.atp.puestos.nucleo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.nucleo.domain.Turno;
import ar.com.atp.puestos.nucleo.domain.TurnoId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Módulo {@code 06-turnos} — lectura de {@code PPRIM_TU}, {@code F2276}, {@code F22761} y
 * {@code LOTES227} contra la base {@code Rentas} (sólo lectura). Se ejecuta sólo con
 * {@code ATP_DB_IT=true}.
 *
 * <p>Verifica la paridad de los nombres físicos, la clave primaria compuesta, el orden de la
 * grilla y — sobre todo — que las consultas JPQL y SQL nativas de {@code 06-turnos} compilan y
 * devuelven los mismos valores que la base.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("06-turnos - PPRIM_TU, F2276, F22761 y LOTES227: lectura de solo acceso")
class TurnoRepositorioIT {

    /** Turno de receptoría con formularios 2276 (sucursal 72, turno 110). */
    private static final Integer SUCURSAL_REC = 72;
    private static final String TIPO_REC = "REC";
    private static final Integer TURNO_REC = 110;

    @Autowired
    private TurnoRepositorio turnoRepositorio;

    @Autowired
    private Formulario2276Repositorio formulario2276Repositorio;

    @Autowired
    private ComisionRepository comisionRepository;

    @Autowired
    private SnSinDevRepositorio snSinDevRepositorio;

    @Test
    @DisplayName("R-TUR-001: el listado de la sucursal viene ordenado por TurNroId")
    void elListadoVieneOrdenadoPorTurNroId() {
        Page<Turno> pagina = turnoRepositorio.listarPorSucursal(SUCURSAL_REC, TIPO_REC,
                PageRequest.of(0, 10));

        assertThat(pagina.getContent()).isNotEmpty();
        List<Integer> numeros = pagina.getContent().stream().map(Turno::getTurNroId).toList();
        assertThat(numeros).isSorted();
        assertThat(pagina.getContent()).allSatisfy(turno -> {
            assertThat(turno.getSucNroId()).isEqualTo(SUCURSAL_REC);
            assertThat(turno.getSucTipo().trim()).isEqualTo(TIPO_REC);
        });
    }

    @Test
    @DisplayName("R-TUR-006: el maximo de TurNroId es el de la ultima fila de la sucursal")
    void elMaximoEsElDeLaUltimaFila() {
        Integer maximo = turnoRepositorio.maximoTurNroId(SUCURSAL_REC, TIPO_REC);

        assertThat(maximo).isNotNull();
        Page<Turno> pagina = turnoRepositorio.listarPorSucursal(SUCURSAL_REC, TIPO_REC,
                PageRequest.of(0, 1));
        Turno primero = pagina.getContent().get(0);
        assertThat(primero.getTurNroId()).isLessThanOrEqualTo(maximo);
    }

    @Test
    @DisplayName("R-TUR-012: el turno se lee por su clave primaria compuesta")
    void elTurnoSeLeePorClavePrimariaCompuesta() {
        Turno turno = turnoRepositorio.listarPorSucursal(SUCURSAL_REC, TIPO_REC, PageRequest.of(0, 1))
                .getContent().get(0);

        Turno leido = turnoRepositorio.findById(turno.getId()).orElseThrow();

        assertThat(leido.getSucNroId()).isEqualTo(turno.getSucNroId());
        assertThat(leido.getTurNroId()).isEqualTo(turno.getTurNroId());
        assertThat(leido.getTurEstado()).isNotBlank();
        assertThat(leido.getTurUsuario()).isNotNull();
        assertThat(leido.getTurFchApe()).isNotNull();
        assertThat(leido.getTurHraApe()).isNotNull();
    }

    @Test
    @DisplayName("R-TUR-015: la totalizacion del turno devuelve los importes de los formularios 2276")
    void laTotalizacionDevuelveLosImportesDeLosFormularios() {
        List<Object[]> filas = formulario2276Repositorio.totalizarTurno(SUCURSAL_REC, TIPO_REC,
                TURNO_REC);

        assertThat(filas).hasSize(1);
        Object[] fila = filas.get(0);
        assertThat(fila).hasSize(6);
        // Los importes son money: la base los devuelve como BigDecimal.
        assertThat(fila[0]).isInstanceOf(BigDecimal.class);
        assertThat((BigDecimal) fila[0]).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        // El total de ingresos brutos del turno 110 es 2453.50 (suma del detalle F22761).
        assertThat((BigDecimal) fila[4]).isEqualByComparingTo("2453.5000");
    }

    @Test
    @DisplayName("R-TUR-013: el conteo de formularios abiertos se ejecuta contra la base real")
    void elConteoDeFormulariosAbiertosSeEjecuta() {
        long abiertos = formulario2276Repositorio.contarFormulariosAbiertos(SUCURSAL_REC, TIPO_REC,
                TURNO_REC, LocalDateTime.now());

        assertThat(abiertos).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("R-TUR-010: el estado de la comision se lee por puesto y numero")
    void elEstadoDeLaComisionSeLee() {
        comisionRepository.findAll().stream().findFirst().ifPresent(comision -> {
            var estado = comisionRepository.estadoComision(comision.getPueCod(),
                    comision.getCpcId());
            assertThat(estado).isNotNull();
        });
    }

    @Test
    @DisplayName("R-TUR-010: una comision inexistente no tiene estado")
    void unaComisionInexistenteNoTieneEstado() {
        assertThat(comisionRepository.estadoComision(-1, -1)).isEmpty();
    }

    @Test
    @DisplayName("R-TUR-016: el mapeo de SNSINDEV es valido (se lee una fila existente)")
    void elMapeoDeSnSinDevEsValido() {
        // La lectura trae todas las columnas mapeadas de la entidad: si alguna nombre fisico
        // estuviera mal, el SELECT fallaria.
        var sn = snSinDevRepositorio.findById(new BigDecimal("2858160")).orElseThrow();

        assertThat(sn.getCodForm()).isEqualTo(126);
        assertThat(sn.getSnSinCuit()).isEqualByComparingTo("33999176769");
        assertThat(sn.getSnSinIdVol()).isEqualTo(6);
        assertThat(sn.getSnSinTipLe()).isEqualTo(2);
        assertThat(sn.getSnSinTotIB()).isNotNull();
        assertThat(sn.getSnSinAdic1()).isNotNull();
        assertThat(sn.getSnSinFPres()).isNotNull();
        assertThat(sn.getSnSinFVto()).isNotNull();
        // Las columnas que el legacy deja en 0 o en la sentinela.
        assertThat(sn.getSnSinPre()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(sn.getSnSin405AC()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(sn.getSnSinFPro()).isEqualTo(ar.com.atp.puestos.shared.time.FechaVacia.FECHA_VACIA);
    }
}
