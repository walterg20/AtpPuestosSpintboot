package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.com.atp.puestos.nucleo.domain.Comision;
import ar.com.atp.puestos.nucleo.domain.ComisionId;
import ar.com.atp.puestos.nucleo.domain.Persona;
import ar.com.atp.puestos.nucleo.domain.Puesto;
import ar.com.atp.puestos.nucleo.domain.Receptor;
import ar.com.atp.puestos.nucleo.domain.Turno;
import ar.com.atp.puestos.nucleo.domain.TurnoId;
import ar.com.atp.puestos.nucleo.domain.Usuario;
import ar.com.atp.puestos.nucleo.repository.ComisionRepository;
import ar.com.atp.puestos.nucleo.repository.PersonaRepository;
import ar.com.atp.puestos.nucleo.repository.PuestoRepository;
import ar.com.atp.puestos.nucleo.repository.ReceptorRepository;
import ar.com.atp.puestos.nucleo.repository.TurnoRepositorio;
import ar.com.atp.puestos.nucleo.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * T-107 / T-108 / T-303: el orden de las comprobaciones de {@link ServicioAcceso} y los códigos
 * alcanzables (spec 03-login §3.3).
 *
 * <p>El codificador y el verificador de IP <strong>reales</strong> se usan (no mocks): lo que se
 * aísla es el acceso a la base. Los repositorios se mockean.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServicioAccesoTest {

    /** Identificador de prueba. */
    private static final String USUARIO = "YOLOFFG";

    /** Clave de prueba (5 caracteres: se rellena a 10 como en el legacy). */
    private static final String CLAVE = "CLAVE";

    private static final int PER_ID = 77;
    private static final int PUE_COD = 10;
    private static final int CPC_ID = 123;
    private static final int RES_ID = 88;

    private static final CodificadorSegEncripta CODIFICADOR = new CodificadorSegEncripta();

    private final VerificadorIpCliente verificadorIp = new VerificadorIpCliente();

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private ComisionRepository comisionRepository;
    @Mock
    private ReceptorRepository receptorRepository;
    @Mock
    private TurnoRepositorio turnoRepository;
    @Mock
    private PuestoRepository puestoRepository;

    private ServicioAcceso servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioAcceso(usuarioRepository, personaRepository, comisionRepository,
                receptorRepository, turnoRepository, puestoRepository, CODIFICADOR, verificadorIp);
    }

    // ------------------------------------------------------------------ credenciales

    @Test
    @DisplayName("R-LOG-004: un usuario inexistente devuelve 2 y no se busca la persona")
    void unUsuarioInexistenteDevuelve2() {
        when(usuarioRepository.findByUsuId(anyString())).thenReturn(Optional.empty());

        ResultadoAcceso resultado = autenticar(PerfilAcceso.INTERNO, "PUE", "10.13.26.1");

        assertThat(resultado.codigo()).isEqualTo(2);
        verify(personaRepository, never()).findByPerCodUsu(anyString());
    }

    @Test
    @DisplayName("R-LOG-005: un UsuClave vacia devuelve 2 y no se busca la persona")
    void unaClaveVaciaDevuelve2() {
        when(usuarioRepository.findByUsuId(anyString()))
                .thenReturn(Optional.of(new Usuario("X", "     ", "NOMBRE", 0)));

        ResultadoAcceso resultado = autenticar(PerfilAcceso.INTERNO, "PUE", "10.13.26.1");

        assertThat(resultado.codigo()).isEqualTo(2);
        verify(personaRepository, never()).findByPerCodUsu(anyString());
    }

    @Test
    @DisplayName("R-LOG-006: una clave incorrecta devuelve 2 y no se busca la persona")
    void unaClaveIncorrectaDevuelve2() {
        when(usuarioRepository.findByUsuId(anyString()))
                .thenReturn(Optional.of(new Usuario("X", claveAlmacenada(), "NOMBRE", 0)));

        ResultadoAcceso resultado = autenticar(PerfilAcceso.INTERNO, "PUE", "10.13.26.1",
                "OTRACLAVE");

        assertThat(resultado.codigo()).isEqualTo(2);
        verify(personaRepository, never()).findByPerCodUsu(anyString());
    }

    @Test
    @DisplayName("R-LOG-007: UsuIngreso = 1 devuelve 5 y no se busca la persona")
    void usuarioQueDebeCambiarLaClaveDevuelve5() {
        when(usuarioRepository.findByUsuId(anyString()))
                .thenReturn(Optional.of(new Usuario("X", claveAlmacenada(), "NOMBRE", 1)));

        ResultadoAcceso resultado = autenticar(PerfilAcceso.INTERNO, "PUE", "10.13.26.1");

        assertThat(resultado.codigo()).isEqualTo(5);
        verify(personaRepository, never()).findByPerCodUsu(anyString());
    }

    @Test
    @DisplayName("R-LOG-037: un UsuIngreso nulo NO es el codigo 5")
    void usuarioIngresoNuloNoEsElCodigo5() {
        when(usuarioRepository.findByUsuId(anyString()))
                .thenReturn(Optional.of(new Usuario("X", claveAlmacenada(), "NOMBRE", null)));
        when(personaRepository.findByPerCodUsu(anyString())).thenReturn(List.of());

        ResultadoAcceso resultado = autenticar(PerfilAcceso.INTERNO, "PUE", "10.13.26.1");

        assertThat(resultado.codigo()).isEqualTo(3);
    }

    @Test
    @DisplayName("R-LOG-008: un usuario sin persona devuelve 3")
    void unUsuarioSinPersonaDevuelve3() {
        when(usuarioRepository.findByUsuId(anyString()))
                .thenReturn(Optional.of(new Usuario("X", claveAlmacenada(), "NOMBRE", 0)));
        when(personaRepository.findByPerCodUsu(anyString())).thenReturn(List.of());

        ResultadoAcceso resultado = autenticar(PerfilAcceso.INTERNO, "PUE", "10.13.26.1");

        assertThat(resultado.codigo()).isEqualTo(3);
        assertThat(resultado.contexto()).isNull();
    }

    // ------------------------------------------------------------------ rama PUE

    @Test
    @DisplayName("R-LOG-009/016: PUE con comision activa devuelve 0 y el contexto con 10 campos")
    void pueConComisionDevuelve0YElContextoCompleto() {
        prepararUsuarioYPersona();
        prepararComisionVigente(true);

        ResultadoAcceso resultado = autenticar(PerfilAcceso.INTERNO, "PUE", "10.13.26.1");

        assertThat(resultado.codigo()).isEqualTo(0);
        assertThat(resultado.sucursal()).isEqualTo(PUE_COD);
        assertThat(resultado.comision()).isEqualTo(CPC_ID);

        ContextoSesion contexto = resultado.contexto();
        assertThat(contexto).isNotNull();
        assertThat(contexto.usuario()).isEqualTo(USUARIO);
        assertThat(contexto.usuarioNombre()).isEqualTo("YOLOFFG NOMBRE");
        assertThat(contexto.personaId()).isEqualTo(PER_ID);
        assertThat(contexto.comision()).isEqualTo(CPC_ID);
        assertThat(contexto.turno()).isEqualTo(7);
        assertThat(contexto.sucursal()).isEqualTo(PUE_COD);
        assertThat(contexto.sucursalNombre()).isEqualTo("PUESTO DIEZ");
        assertThat(contexto.tipo()).isEqualTo("PUE");
        assertThat(contexto.responsable()).isEqualTo(RES_ID);
        assertThat(contexto.responsableNombre()).isEqualTo("RESPONSABLE UNO");
        // Los 3 campos que el login no carga quedan en su valor por defecto.
        assertThat(contexto.form2276()).isNull();
        assertThat(contexto.f276Id()).isNull();
        assertThat(contexto.ultimoMenu()).isNull();
    }

    @Test
    @DisplayName("R-LOG-009: PUE sin comision activa devuelve 4")
    void pueSinComisionDevuelve4() {
        prepararUsuarioYPersona();
        when(comisionRepository.findComisionesVigentesComoIntegrante(eq(PER_ID), any(), any()))
                .thenReturn(List.of());
        when(comisionRepository.findComisionesVigentesComoResponsable(eq(PER_ID), any(), any()))
                .thenReturn(List.of());
        when(comisionRepository.countComisionesCerradasComoResponsable(PER_ID)).thenReturn(0L);

        ResultadoAcceso resultado = autenticar(PerfilAcceso.INTERNO, "PUE", "10.13.26.1");

        assertThat(resultado.codigo()).isEqualTo(4);
        assertThat(resultado.contexto()).isNull();
    }

    @Test
    @DisplayName("R-LOG-015: PUE sin comision pero con historia de comision devuelve 0 y contexto parcial")
    void fueResponsableDeUnaComisionDevuelve0YContextoParcial() {
        prepararUsuarioYPersona();
        when(comisionRepository.findComisionesVigentesComoIntegrante(eq(PER_ID), any(), any()))
                .thenReturn(List.of());
        when(comisionRepository.findComisionesVigentesComoResponsable(eq(PER_ID), any(), any()))
                .thenReturn(List.of());
        when(comisionRepository.countComisionesCerradasComoResponsable(PER_ID)).thenReturn(3L);

        ResultadoAcceso resultado = autenticar(PerfilAcceso.INTERNO, "PUE", "10.13.26.1");

        assertThat(resultado.codigo()).isEqualTo(0);
        ContextoSesion contexto = resultado.contexto();
        assertThat(contexto).isNotNull();
        assertThat(contexto.usuario()).isEqualTo(USUARIO);
        assertThat(contexto.usuarioNombre()).isEqualTo("YOLOFFG NOMBRE");
        assertThat(contexto.personaId()).isEqualTo(PER_ID);
        assertThat(contexto.tipo()).isEqualTo("PUE");
        // Los 6 campos restantes quedan en su valor por defecto.
        assertThat(contexto.comision()).isZero();
        assertThat(contexto.turno()).isZero();
        assertThat(contexto.sucursal()).isZero();
        assertThat(contexto.sucursalNombre()).isEmpty();
        assertThat(contexto.responsable()).isZero();
        assertThat(contexto.responsableNombre()).isEmpty();
    }

    @Test
    @DisplayName("R-LOG-013: una IP que no pertenece a la red del puesto devuelve 6")
    void unaIpIncompatibleDevuelve6() {
        prepararUsuarioYPersona();
        prepararComisionVigente(true);

        ResultadoAcceso resultado = autenticar(PerfilAcceso.INTERNO, "PUE", "192.168.0.10");

        assertThat(resultado.codigo()).isEqualTo(6);
        assertThat(resultado.contexto()).isNull();
    }

    @Test
    @DisplayName("R-LOG-014: sin turno el contexto deja el turno en 0")
    void sinTurnoElTurnoQuedaEn0() {
        prepararUsuarioYPersona();
        prepararComisionVigente(false);

        ResultadoAcceso resultado = autenticar(PerfilAcceso.INTERNO, "PUE", "10.13.26.1");

        assertThat(resultado.codigo()).isEqualTo(0);
        assertThat(resultado.contexto().turno()).isZero();
    }

    @Test
    @DisplayName("R-LOG-010: sin responsable la comision conserva los valores por defecto")
    void sinResponsableLaComisionConservaLosValoresPorDefecto() {
        prepararUsuarioYPersona();
        Comision comision = new Comision(new ComisionId(PUE_COD, CPC_ID));
        comision.setCpcCierre("N");
        when(comisionRepository.findComisionesVigentesComoIntegrante(eq(PER_ID), any(), any()))
                .thenReturn(List.of(comision));
        when(comisionRepository.findComisionConResponsable(PUE_COD, CPC_ID))
                .thenReturn(List.<Object[]>of(new Object[] {comision, null}));
        when(puestoRepository.findById(PUE_COD))
                .thenReturn(Optional.of(new Puesto(PUE_COD, "PUESTO DIEZ", "")));
        when(turnoRepository.findTurnosActivos(PUE_COD, "PUE", CPC_ID)).thenReturn(List.of());

        ResultadoAcceso resultado = autenticar(PerfilAcceso.INTERNO, "PUE", "10.13.26.1");

        assertThat(resultado.codigo()).isEqualTo(0);
        assertThat(resultado.contexto().responsable()).isZero();
        assertThat(resultado.contexto().responsableNombre()).isEqualTo("Sin Comisión");
    }

    // ------------------------------------------------------------------ rama REC

    @Test
    @DisplayName("R-LOG-011/016: REC con receptoria devuelve 0 y el contexto con 10 campos")
    void recConReceptoriaDevuelve0YElContextoCompleto() {
        prepararUsuarioYPersona();
        when(receptorRepository.findReceptoresDePersona(PER_ID))
                .thenReturn(List.of(new Receptor(5, "RECEPTORIA CINCO")));
        when(turnoRepository.findTurnosActivos(5, "REC", 0)).thenReturn(List.of());

        ResultadoAcceso resultado = autenticar(PerfilAcceso.INTERNO, "REC", "10.13.26.1");

        assertThat(resultado.codigo()).isEqualTo(0);
        assertThat(resultado.sucursal()).isEqualTo(5);
        assertThat(resultado.comision()).isZero();

        ContextoSesion contexto = resultado.contexto();
        assertThat(contexto.sucursal()).isEqualTo(5);
        assertThat(contexto.sucursalNombre()).isEqualTo("RECEPTORIA CINCO");
        assertThat(contexto.tipo()).isEqualTo("REC");
        // En la rama de receptoria no se resuelve responsable.
        assertThat(contexto.responsable()).isZero();
        assertThat(contexto.responsableNombre()).isEmpty();
    }

    @Test
    @DisplayName("R-LOG-011: REC sin receptoria devuelve 4 en el perfil INTERNO")
    void recSinReceptoriaDevuelve4EnElPerfilInterno() {
        prepararUsuarioYPersona();
        when(receptorRepository.findReceptoresDePersona(PER_ID)).thenReturn(List.of());

        ResultadoAcceso resultado = autenticar(PerfilAcceso.INTERNO, "REC", "10.13.26.1");

        assertThat(resultado.codigo()).isEqualTo(4);
        assertThat(resultado.contexto()).isNull();
    }

    @Test
    @DisplayName("R-LOG-029: en el perfil WS, REC sin receptoria devuelve 0 con sucursal 0 y comision 0")
    void wsRecSinReceptoriaDevuelve0ConSucursal0YComision0() {
        prepararUsuarioYPersona();
        when(receptorRepository.findReceptoresDePersona(PER_ID)).thenReturn(List.of());

        ResultadoAcceso resultado = autenticar(PerfilAcceso.WS, "REC", null);

        assertThat(resultado.codigo()).isZero();
        assertThat(resultado.sucursal()).isZero();
        assertThat(resultado.comision()).isZero();
        assertThat(resultado.contexto()).isNull();
    }

    @Test
    @DisplayName("R-LOG-028: el perfil WS no hace chequeo de IP (el codigo 6 no es producible)")
    void elPerfilWsNoHaceChequeoDeIp() {
        prepararUsuarioYPersona();
        prepararComisionVigente(true);

        ResultadoAcceso resultado = autenticar(PerfilAcceso.WS, "PUE", "192.168.0.10");

        assertThat(resultado.codigo()).isZero();
        assertThat(resultado.sucursal()).isEqualTo(PUE_COD);
        assertThat(resultado.comision()).isEqualTo(CPC_ID);
    }

    @Test
    @DisplayName("R-LOG-028: el perfil WS no carga contexto de sesion")
    void elPerfilWsNoCargaContextoDeSesion() {
        prepararUsuarioYPersona();
        prepararComisionVigente(true);

        ResultadoAcceso resultado = autenticar(PerfilAcceso.WS, "PUE", "10.13.26.1");

        assertThat(resultado.contexto()).isNull();
    }

    @Test
    @DisplayName("R-LOG-019: el login no lanza excepciones ante credenciales invalidas")
    void elLoginNoLanzaExcepciones() {
        when(usuarioRepository.findByUsuId(anyString())).thenReturn(Optional.empty());

        ResultadoAcceso resultado = autenticar(PerfilAcceso.INTERNO, "PUE", null);

        assertThat(resultado.codigo()).isEqualTo(2);
        assertThat(resultado.mensaje()).isEqualTo("Usuario/Contraseña Incorrecto");
    }

    @Test
    @DisplayName("R-LOG-018: el codigo 1 no es producible (el legacy solo asigna 0, 2, 3, 4, 5 y 6)")
    void elCodigo1NoEsProducible() {
        when(usuarioRepository.findByUsuId(anyString())).thenReturn(Optional.empty());
        assertThat(autenticar(PerfilAcceso.INTERNO, "PUE", null).codigo()).isEqualTo(2);

        when(usuarioRepository.findByUsuId(anyString()))
                .thenReturn(Optional.of(new Usuario("X", claveAlmacenada(), "NOMBRE", 0)));
        assertThat(autenticar(PerfilAcceso.INTERNO, "PUE", null, "OTRA").codigo()).isEqualTo(2);

        when(usuarioRepository.findByUsuId(anyString()))
                .thenReturn(Optional.of(new Usuario("X", claveAlmacenada(), "NOMBRE", 1)));
        assertThat(autenticar(PerfilAcceso.INTERNO, "PUE", null).codigo()).isEqualTo(5);

        when(usuarioRepository.findByUsuId(anyString()))
                .thenReturn(Optional.of(new Usuario("X", claveAlmacenada(), "NOMBRE", 0)));
        when(personaRepository.findByPerCodUsu(anyString())).thenReturn(List.of());
        assertThat(autenticar(PerfilAcceso.INTERNO, "PUE", null).codigo()).isEqualTo(3);

        // Ninguno de los caminos devuelve 1.
        assertThat(MensajesAcceso.esConocido(1)).isTrue();
    }

    // ------------------------------------------------------------------ helpers

    private ResultadoAcceso autenticar(PerfilAcceso perfil, String tipo, String ip) {
        return autenticar(perfil, tipo, ip, CLAVE);
    }

    private ResultadoAcceso autenticar(PerfilAcceso perfil, String tipo, String ip, String clave) {
        Credencial credencial = new Credencial(USUARIO, clave, tipo);
        return servicio.autenticar(perfil, credencial, new DatosPeticion(ip));
    }

    /** Usuario con clave correcta y persona asociada. */
    private void prepararUsuarioYPersona() {
        when(usuarioRepository.findByUsuId(anyString()))
                .thenReturn(Optional.of(new Usuario("X", claveAlmacenada(), "YOLOFFG NOMBRE", 0)));
        when(personaRepository.findByPerCodUsu(anyString()))
                .thenReturn(List.of(new Persona(PER_ID, "PERSONA PRINCIPAL", "YOLOFFG   ")));
    }

    /** Comision vigente (integrante), con responsable y puesto configurado. */
    private void prepararComisionVigente(boolean conTurno) {
        Comision comision = new Comision(new ComisionId(PUE_COD, CPC_ID));
        comision.setCpcResId(RES_ID);
        comision.setCpcCierre("N");

        when(comisionRepository.findComisionesVigentesComoIntegrante(eq(PER_ID), any(), any()))
                .thenReturn(List.of(comision));
        when(comisionRepository.findComisionConResponsable(PUE_COD, CPC_ID))
                .thenReturn(List.<Object[]>of(
                        new Object[] {comision, new Persona(RES_ID, "RESPONSABLE UNO", null)}));
        when(puestoRepository.findById(PUE_COD))
                .thenReturn(Optional.of(new Puesto(PUE_COD, "PUESTO DIEZ", "10.13.26")));
        when(turnoRepository.findTurnosActivos(PUE_COD, "PUE", CPC_ID))
                .thenReturn(conTurno ? List.of(turno(7)) : List.of());
    }

    private static Turno turno(int numero) {
        Turno turno = new Turno(new TurnoId(PUE_COD, "PUE", numero));
        turno.setTurEstado("A");
        turno.setTurCpcId(CPC_ID);
        return turno;
    }

    /** La clave tal como la guarda el legacy: el texto codificado y rellenado a 10. */
    private static String claveAlmacenada() {
        return CODIFICADOR.codificar(CLAVE + " ".repeat(10 - CLAVE.length()));
    }
}
