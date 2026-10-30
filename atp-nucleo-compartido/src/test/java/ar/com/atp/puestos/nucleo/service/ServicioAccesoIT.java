package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.nucleo.domain.Persona;
import ar.com.atp.puestos.nucleo.domain.ReceptorPersona;
import ar.com.atp.puestos.nucleo.domain.Usuario;
import ar.com.atp.puestos.nucleo.repository.PersonaRepository;
import ar.com.atp.puestos.nucleo.repository.ReceptorPersonaRepository;
import ar.com.atp.puestos.nucleo.repository.UsuarioRepository;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * T-304 / T-305: el login contra la base {@code Rentas} (sólo lectura; la transacción se revierte
 * al terminar). Se ejecuta sólo con {@code ATP_DB_IT=true}.
 *
 * <p>Las credenciales válidas se obtienen <strong>descifrando</strong> el propio dato almacenado
 * (el mecanismo {@code Seg_Encripta} es reversible), de modo que la prueba no versiona ningún
 * secreto (G3).
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-304/T-305 - Login contra la base Rentas")
class ServicioAccesoIT {

    /** Las 8 tablas del contexto de acceso: el login no escribe en ninguna. */
    private static final List<String> TABLAS = List.of(
            "SCRTY", "PERSONAS", "PUEPPRIM", "RECEPTOR", "RECEPTO1", "COMISION", "COMISIO1",
            "PPRIM_TU");

    private final CodificadorSegEncripta codificador = new CodificadorSegEncripta();

    @Autowired
    private ServicioAcceso servicioAcceso;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private ReceptorPersonaRepository receptorPersonaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("R-LOG-004/006: un usuario real con clave correcta no devuelve 2")
    void unUsuarioRealConClaveCorrectaNoDevuelve2() {
        Usuario usuario = usuarioUtilizable().orElse(null);
        Assumptions.assumeTrue(usuario != null, "no hay un usuario utilizable en la base");

        ResultadoAcceso resultado = autenticar(PerfilAcceso.WS, usuario, "PUE");

        assertThat(resultado.codigo()).isNotEqualTo(2);
        assertThat(resultado.codigo()).isNotEqualTo(3);
    }

    @Test
    @DisplayName("R-LOG-006: una clave incorrecta devuelve 2")
    void unaClaveIncorrectaDevuelve2() {
        Usuario usuario = usuarioUtilizable().orElse(null);
        Assumptions.assumeTrue(usuario != null, "no hay un usuario utilizable en la base");

        ResultadoAcceso resultado = servicioAcceso.autenticar(PerfilAcceso.WS,
                new Credencial(codificador.decodificar(usuario.getUsuId()), "CLAVEERRADA", "PUE"),
                DatosPeticion.sinIp());

        assertThat(resultado.codigo()).isEqualTo(2);
    }

    @Test
    @DisplayName("R-LOG-005: un usuario sin clave almacenada devuelve 2")
    void unUsuarioSinClaveAlmacenadaDevuelve2() {
        Usuario usuario = usuarioRepository.findAll().stream()
                .filter(u -> u.getUsuClave() == null || u.getUsuClave().isBlank())
                .findFirst()
                .orElse(null);
        Assumptions.assumeTrue(usuario != null, "no hay ninguna cuenta sin clave");

        ResultadoAcceso resultado = servicioAcceso.autenticar(PerfilAcceso.WS,
                new Credencial(codificador.decodificar(usuario.getUsuId()), "CLAVE", "PUE"),
                DatosPeticion.sinIp());

        assertThat(resultado.codigo()).isEqualTo(2);
    }

    @Test
    @DisplayName("R-LOG-007: un usuario con UsuIngreso = 1 devuelve 5")
    void unUsuarioQueDebeCambiarLaClaveDevuelve5() {
        Usuario usuario = usuarioRepository.findAll().stream()
                .filter(u -> Integer.valueOf(1).equals(u.getUsuIngreso()))
                .filter(u -> u.getUsuClave() != null && !u.getUsuClave().isBlank())
                .filter(u -> reversible(u.getUsuId()) && reversible(u.getUsuClave()))
                .findFirst()
                .orElse(null);
        Assumptions.assumeTrue(usuario != null, "no hay ningun usuario con UsuIngreso = 1");

        ResultadoAcceso resultado = autenticar(PerfilAcceso.WS, usuario, "PUE");

        assertThat(resultado.codigo()).isEqualTo(5);
    }

    @Test
    @DisplayName("R-LOG-008: un usuario sin persona devuelve 3")
    void unUsuarioSinPersonaDevuelve3() {
        Set<String> idsConPersona = idsDeUsuariosConPersona();
        Usuario usuario = usuarioRepository.findAll().stream()
                .filter(u -> reversible(u.getUsuId()) && reversible(u.getUsuClave()))
                .filter(u -> !idsConPersona.contains(codificador.decodificar(u.getUsuId()).trim()))
                .findFirst()
                .orElse(null);
        Assumptions.assumeTrue(usuario != null, "todos los usuarios tienen persona");

        // Con la clave CORRECTA del usuario (descifrada del propio dato): el chequeo de la clave
        // pasa y el flujo llega a la resolución de la persona, que es lo que se quiere verificar.
        ResultadoAcceso resultado = autenticar(PerfilAcceso.WS, usuario, "PUE");

        assertThat(resultado.codigo()).isEqualTo(3);
    }

    @Test
    @DisplayName("R-LOG-029: en el canal WS, un usuario REC sin receptoria devuelve 0 con 0 y 0")
    void unUsuarioRecSinReceptoriaDevuelve0ConSucursal0YComision0() {
        Set<Integer> personasConReceptoria = receptorPersonaRepository.findAll().stream()
                .map(ReceptorPersona::getPerId)
                .collect(Collectors.toSet());
        Map<String, Integer> personaPorUsuario = new LinkedHashMap<>();
        for (Persona persona : personaRepository.findAll()) {
            if (persona.getPerCodUsu() != null && !persona.getPerCodUsu().isBlank()) {
                personaPorUsuario.put(persona.getPerCodUsu().trim(), persona.getPerId());
            }
        }

        Usuario usuario = usuarioRepository.findAll().stream()
                .filter(u -> u.getUsuClave() != null && !u.getUsuClave().isBlank())
                .filter(u -> reversible(u.getUsuId()) && reversible(u.getUsuClave()))
                .filter(u -> {
                    Integer perId = personaPorUsuario.get(codificador.decodificar(u.getUsuId()).trim());
                    return perId != null && !personasConReceptoria.contains(perId);
                })
                .findFirst()
                .orElse(null);
        Assumptions.assumeTrue(usuario != null, "no hay ningun usuario REC sin receptoria");

        ResultadoAcceso resultado = autenticar(PerfilAcceso.WS, usuario, "REC");

        assertThat(resultado.codigo()).isZero();
        assertThat(resultado.sucursal()).isZero();
        assertThat(resultado.comision()).isZero();
    }

    @Test
    @DisplayName("T-305: el login NO escribe en la base (conteos identicos antes y despues)")
    void elLoginNoEscribeEnLaBase() {
        Usuario usuario = usuarioUtilizable().orElse(null);
        Assumptions.assumeTrue(usuario != null, "no hay un usuario utilizable en la base");

        Map<String, Integer> antes = conteos();
        autenticar(PerfilAcceso.INTERNO, usuario, "PUE");
        Map<String, Integer> despues = conteos();

        assertThat(despues).isEqualTo(antes);
    }

    // ------------------------------------------------------------------ helpers

    private ResultadoAcceso autenticar(PerfilAcceso perfil, Usuario usuario, String tipo) {
        return servicioAcceso.autenticar(perfil,
                new Credencial(codificador.decodificar(usuario.getUsuId()),
                        codificador.decodificar(usuario.getUsuClave()), tipo),
                DatosPeticion.sinIp());
    }

    /** Usuario con clave utilizable y persona asociada. */
    private Optional<Usuario> usuarioUtilizable() {
        Set<String> idsConPersona = idsDeUsuariosConPersona();
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getUsuClave() != null && !u.getUsuClave().isBlank())
                .filter(u -> reversible(u.getUsuId()) && reversible(u.getUsuClave()))
                .filter(u -> idsConPersona.contains(codificador.decodificar(u.getUsuId()).trim()))
                .findFirst();
    }

    /** Identificadores (en claro) de los usuarios que tienen persona. */
    private Set<String> idsDeUsuariosConPersona() {
        Set<String> ids = new LinkedHashSet<>();
        for (Persona persona : personaRepository.findAll()) {
            if (persona.getPerCodUsu() != null && !persona.getPerCodUsu().isBlank()) {
                ids.add(persona.getPerCodUsu().trim());
            }
        }
        return ids;
    }

    /** El dato cifrado es reversible sin perdidas (sin caracteres fuera del alfabeto). */
    private boolean reversible(String codificado) {
        return codificado != null && !codificado.isBlank()
                && codificador.coincideCon(
                        codificador.codificar(codificador.decodificar(codificado)), codificado);
    }

    /** Conteo de filas de las 8 tablas del contexto de acceso. */
    private Map<String, Integer> conteos() {
        Map<String, Integer> conteos = new LinkedHashMap<>();
        for (String tabla : TABLAS) {
            conteos.put(tabla, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tabla,
                    Integer.class));
        }
        return conteos;
    }
}
