package ar.gob.chaco.atp.puestoscamineros.service;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Producto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.ProductoVigencia;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.ProductoVigenciaId;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Turno;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.TurnoId;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.CalleRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.LocalidadRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.ProductoRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.ProductoVigenciaRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.TurnoRepositorio;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ServicioEstadoDeForm;
import ar.gob.chaco.atp.puestoscamineros.domain.Acta;
import ar.gob.chaco.atp.puestoscamineros.domain.Contribuyente;
import ar.gob.chaco.atp.puestoscamineros.repository.ActaPaseRepository;
import ar.gob.chaco.atp.puestoscamineros.repository.ActaProductoRepository;
import ar.gob.chaco.atp.puestoscamineros.repository.ActaRepository;
import ar.gob.chaco.atp.puestoscamineros.repository.ContribuyenteRepository;
import ar.gob.chaco.atp.puestoscamineros.repository.RangoManualFormularioRepositorio;
import ar.gob.chaco.atp.puestoscamineros.repository.VersionSistemaRepositorio;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaAlta;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaAltaProducto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Base común de las suites unitarias del servicio de Actas: mocks, construcción del servicio y
 * helpers compartidos (spec {@code 09-formularios-acta} §4, T-ACTA-173).
 *
 * <p>Se separa para que cada suite quede por debajo del límite de 500 líneas por archivo (G4).
 */
@ExtendWith(MockitoExtension.class)
abstract class ActaServicioTestBase {

    /** CUIT del vendedor (11 dígitos, dv válido). */
    protected static final Long CUIT = 20123456786L;

    /** CUIT del destinatario (11 dígitos, dv válido). */
    protected static final Long CUIT_DEST = 30123456781L;

    /** CUIT del transportista (11 dígitos, dv válido). */
    protected static final Long CUIT_TRAN = 27123456780L;

    /** Sucursal de la sesión. */
    protected static final Integer SUCURSAL = 5;

    /** Tipo de sucursal de la sesión. */
    protected static final String TIPO = "PUE";

    /** Turno de la sesión. */
    protected static final Integer TURNO = 77;

    @Mock
    protected ActaRepository actaRepository;
    @Mock
    protected ActaProductoRepository actaProductoRepository;
    @Mock
    protected ActaPaseRepository actaPaseRepository;
    @Mock
    protected RangoManualFormularioRepositorio rangoManualRepositorio;
    @Mock
    protected VersionSistemaRepositorio versionSistemaRepositorio;
    @Mock
    protected ProductoRepository productoRepository;
    @Mock
    protected ProductoVigenciaRepository vigenciaRepository;
    @Mock
    protected ContribuyenteRepository contribuyenteRepository;
    @Mock
    protected CalleRepository calleRepository;
    @Mock
    protected LocalidadRepository localidadRepository;
    @Mock
    protected TurnoRepositorio turnoRepositorio;
    @Mock
    protected ServicioEstadoDeForm servicioEstadoDeForm;
    @Mock
    protected ServicioNumeracionActa servicioNumeracionActa;

    /** Servicio bajo prueba. */
    protected ActaService service;

    @BeforeEach
    void setUp() {
        ActaDerivaciones derivaciones = new ActaDerivaciones(productoRepository, vigenciaRepository,
                contribuyenteRepository, calleRepository, localidadRepository, turnoRepositorio,
                versionSistemaRepositorio, rangoManualRepositorio);
        ActaValidador validador = new ActaValidador(derivaciones);
        service = new ActaService(actaRepository, actaProductoRepository, actaPaseRepository,
                rangoManualRepositorio, servicioEstadoDeForm, servicioNumeracionActa, derivaciones,
                validador);
    }

    /** Contexto de sesión del puesto caminero. */
    protected ContextoSesion contexto() {
        return ContextoSesion.completo("operador", "Operador Test", 42, 7, TURNO, SUCURSAL,
                "Sucursal 5", TIPO, 1, "Responsable");
    }

    /** Cabecera de Acta con los valores del turno de prueba. */
    protected Acta acta(Integer numero) {
        Acta acta = new Acta();
        acta.setF276Id(numero);
        acta.setSucTipo(TIPO);
        acta.setSucNroId(SUCURSAL);
        acta.setTurNroId(TURNO);
        acta.setF276GenWeb("A");
        acta.setF276Operad("OPERADOR");
        return acta;
    }

    /** Página de Actas para el listado. */
    protected Page<Acta> pagina(List<Acta> actas) {
        return new PageImpl<>(actas, PageRequest.of(0, 10), actas.size());
    }

    /** Alta de un Acta con los valores válidos del legacy. */
    protected ActaAlta altaValida() {
        return new ActaAlta("F", "S", LocalDate.now(), CUIT, "VENDEDOR SA", "DOM VENDEDOR", 1,
                CUIT_DEST, "DESTINATARIO SA", "DOM DESTINATARIO", 2, CUIT_TRAN, "TRANSPORTISTA SA",
                "DOM TRANSPORTISTA", 3, 1, "12345678", "AB123CD", "EF456GH", new BigDecimal("120.5"),
                List.of(new ActaAltaProducto(10, new BigDecimal("2"), new BigDecimal("100"))));
    }

    /** Alta con motivo, clasificación, CUIT y razón social parametrizados. */
    protected ActaAlta altaCon(String motivo, String clasif, Long cuit, String razonSocial) {
        return new ActaAlta(motivo, clasif, LocalDate.now(), cuit, razonSocial, "D", 1,
                CUIT_DEST, "DESTINATARIO SA", "D", 2, CUIT_TRAN, "TRANSPORTISTA SA", "D", 3, 1,
                "1", "AB123CD", "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(10, BigDecimal.ONE, BigDecimal.ONE)));
    }

    /** Producto con un tipo de cálculo. */
    protected Producto producto(Integer codigo, Integer tipoCal) {
        Producto producto = new Producto(codigo, "MAIZ");
        producto.setProdTipCal(tipoCal);
        return producto;
    }

    /** Vigencia de producto con los cuatro importes. */
    protected ProductoVigencia vigencia(BigDecimal i1, BigDecimal i2, BigDecimal i3, BigDecimal i4) {
        ProductoVigencia vigencia = new ProductoVigencia();
        ProductoVigenciaId id = new ProductoVigenciaId();
        id.setProdCod(10);
        id.setProdFchVig(LocalDate.of(2020, 1, 1).atStartOfDay());
        vigencia.setId(id);
        vigencia.setProdImpu1(i1);
        vigencia.setProdImpu2(i2);
        vigencia.setProdImpu3(i3);
        vigencia.setProdImpu4(i4);
        return vigencia;
    }

    /** Contribuyente con un estado. */
    protected Contribuyente contribuyente(short estado) {
        Contribuyente contribuyente = new Contribuyente();
        contribuyente.setConCuit(new BigDecimal(CUIT));
        contribuyente.setConEstado(estado);
        contribuyente.setConRazSoc("VENDEDOR SA");
        return contribuyente;
    }

    /** Turno con un estado. */
    protected Turno turno(String estado) {
        Turno turno = new Turno(new TurnoId(SUCURSAL, TIPO, TURNO));
        turno.setTurEstado(estado);
        return turno;
    }

    /** Fecha límite de vigencia. */
    protected LocalDateTime hoy() {
        return LocalDate.now().atTime(23, 59, 59);
    }
}
