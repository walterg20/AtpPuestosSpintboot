package ar.gob.chaco.atp.puestoscamineros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.domain.DetalleProductoPrimario;
import ar.gob.chaco.atp.puestoscamineros.domain.DetalleProductoPrimarioId;
import ar.gob.chaco.atp.puestoscamineros.repository.DetalleProductoPrimarioRepository;
import ar.gob.chaco.atp.puestoscamineros.repository.PaseGuiaRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-304 - Marcado de guia contra Rentas (rollback)")
class CargaMarcadoIT {

    @Autowired
    private CargaService cargaService;

    @Autowired
    private DetalleProductoPrimarioRepository detalleRepository;

    @Autowired
    private PaseGuiaRepository paseRepository;

    @Autowired
    private EntityManager entityManager;

    private ContextoSesion contexto() {
        return ContextoSesion.completo("TESTER", "Tester", 1, 0, 0, 1, "Puesto1", "PUE", 0, "Resp");
    }

    private List<DetalleProductoPrimario> buscar(String where) {
        @SuppressWarnings("unchecked")
        List<DetalleProductoPrimario> res = entityManager
                .createQuery("select d from DetalleProductoPrimario d where " + where, DetalleProductoPrimario.class)
                .setMaxResults(5)
                .getResultList();
        return res;
    }

    private long pasesPrevios(DetalleProductoPrimario cab) {
        return paseRepository.countByConCuitAndDppSucuAndCalPerFiscAndDppSecuAndDppNroFormAndDppPaseEstAndSucTipo(
                cab.getConCuit(), cab.getDppSucu(), cab.getCalPerFisc(), cab.getDppSecu(),
                cab.getDppNroForm(), "C", "PUE");
    }

    private long totalPases(DetalleProductoPrimario cab) {
        return paseRepository
                .findByConCuitAndDppSucuAndCalPerFiscAndDppSecuAndDppNroForm(
                        cab.getConCuit(), cab.getDppSucu(), cab.getCalPerFisc(), cab.getDppSecu(),
                        cab.getDppNroForm())
                .size();
    }

    @Test
    @DisplayName("marcado exitoso: inserta DETPROD2 y actualiza DETPRODP")
    void marcadoExitoso() {
        List<DetalleProductoPrimario> candidatas = buscar(
                "(d.dppMarcAnu is null or d.dppMarcAnu <> 1) and (d.dppClasif is null or d.dppClasif <> 'I')");
        assumeTrue(!candidatas.isEmpty(), "no hay guia valida en Rentas");

        DetalleProductoPrimario cab = candidatas.get(0);
        long previos = pasesPrevios(cab);
        long totalAntes = totalPases(cab);

        ResultadoMarcado r = cargaService.marcarGuia(
                cab.getConCuit(), cab.getDppSucu(), cab.getCalPerFisc(), cab.getDppSecu(),
                cab.getDppNroForm(), "OPIT", "PUE", (short) 1, contexto());

        assertThat(r.exito()).isTrue();
        assertThat(r.mensaje()).isEqualTo(CargaService.MSG_EXITO);
        assertThat(r.duplicado()).isEqualTo((int) (previos + 1));

        // insercion en DETPROD2
        assertThat(totalPases(cab)).isEqualTo(totalAntes + 1);

        // actualizacion de cabecera DETPRODP
        DetalleProductoPrimarioId id = new DetalleProductoPrimarioId(
                cab.getConCuit(), cab.getDppSucu(), cab.getCalPerFisc(), cab.getDppSecu(),
                cab.getDppNroForm());
        DetalleProductoPrimario actualizado = detalleRepository.findById(id).orElseThrow();
        assertThat(actualizado.getDppUltPSuN()).isEqualTo((short) 1);
        assertThat(actualizado.getDppUltPSuT()).isEqualTo("PUE");
        assertThat(actualizado.getDppUltPaOp()).isEqualTo("OPIT");
    }

    @Test
    @DisplayName("guia anulada: no registra pase y devuelve MSG_ANULADA")
    void guiaAnulada() {
        List<DetalleProductoPrimario> candidatas = buscar("d.dppMarcAnu = 1");
        assumeTrue(!candidatas.isEmpty(), "no hay guia anulada en Rentas");

        DetalleProductoPrimario cab = candidatas.get(0);
        long totalAntes = totalPases(cab);

        ResultadoMarcado r = cargaService.marcarGuia(
                cab.getConCuit(), cab.getDppSucu(), cab.getCalPerFisc(), cab.getDppSecu(),
                cab.getDppNroForm(), "OPIT", "PUE", (short) 1, contexto());

        assertThat(r.exito()).isFalse();
        assertThat(r.mensaje()).isEqualTo(CargaService.MSG_ANULADA);
        assertThat(totalPases(cab)).isEqualTo(totalAntes);
    }

    @Test
    @DisplayName("guia interna: no registra pase y devuelve MSG_INTERNA")
    void guiaInterna() {
        List<DetalleProductoPrimario> candidatas = buscar("d.dppClasif = 'I'");
        assumeTrue(!candidatas.isEmpty(), "no hay guia interna en Rentas");

        DetalleProductoPrimario cab = candidatas.get(0);
        long totalAntes = totalPases(cab);

        ResultadoMarcado r = cargaService.marcarGuia(
                cab.getConCuit(), cab.getDppSucu(), cab.getCalPerFisc(), cab.getDppSecu(),
                cab.getDppNroForm(), "OPIT", "PUE", (short) 1, contexto());

        assertThat(r.exito()).isFalse();
        assertThat(r.mensaje()).isEqualTo(CargaService.MSG_INTERNA);
        assertThat(totalPases(cab)).isEqualTo(totalAntes);
    }

    @Test
    @DisplayName("guia duplicada: Duplicado > 1 cuando ya habia pases C/PUE")
    void guiaDuplicada() {
        // buscar una guia con al menos un pase previo C/PUE
        DetalleProductoPrimario cab = null;
        for (DetalleProductoPrimario d : buscar("(d.dppMarcAnu is null or d.dppMarcAnu <> 1) and (d.dppClasif is null or d.dppClasif <> 'I')")) {
            if (pasesPrevios(d) >= 1) {
                cab = d;
                break;
            }
        }
        assumeTrue(cab != null, "no hay guia con pase previo C/PUE en Rentas");

        long previos = pasesPrevios(cab);
        ResultadoMarcado r = cargaService.marcarGuia(
                cab.getConCuit(), cab.getDppSucu(), cab.getCalPerFisc(), cab.getDppSecu(),
                cab.getDppNroForm(), "OPIT", "PUE", (short) 1, contexto());

        assertThat(r.exito()).isTrue();
        assertThat(r.duplicado()).isEqualTo((int) (previos + 1));
        assertThat(r.duplicado()).isGreaterThan(1);
    }
}
