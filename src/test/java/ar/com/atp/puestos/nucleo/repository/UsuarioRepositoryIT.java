package ar.gob.chaco.atp.puestoscamineros.nucleo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Usuario;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.CodificadorSegEncripta;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * T-304 / R-LOG-003 / R-LOG-004: lectura de {@code SCRTY} contra la base {@code Rentas}
 * (sólo lectura). Se ejecuta sólo con {@code ATP_DB_IT=true}.
 *
 * <p>Verifica la paridad del mapeo ({@code @Table(name="SCRTY")}, PK {@code UsuId}), la búsqueda
 * insensible a mayúsculas (504 de los 552 {@code UsuId} contienen {@code b} minúscula) y que los
 * 552 identificadores se descifran sin excepción.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-304 - SCRTY (Usuario): lectura de solo acceso")
class UsuarioRepositoryIT {

    private final CodificadorSegEncripta codificador = new CodificadorSegEncripta();

    @Autowired
    private UsuarioRepository repository;

    @Test
    @DisplayName("R-LOG-004: un UsuId con 'b' minuscula se encuentra (comparacion con UPPER)")
    void encuentraUnUsuIdConBMinuscula() {
        List<Usuario> usuarios = repository.findAll();
        Optional<Usuario> conBMinuscula = usuarios.stream()
                .filter(u -> u.getUsuId() != null && u.getUsuId().contains("b"))
                .findFirst();

        assertThat(conBMinuscula).isPresent();
        Usuario usuario = conBMinuscula.get();

        Optional<Usuario> encontrado = repository.findByUsuId(usuario.getUsuId());
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getUsuId()).isEqualTo(usuario.getUsuId());
    }

    @Test
    @DisplayName("R-LOG-004: la busqueda no distingue mayusculas")
    void laBusquedaNoDistingueMayusculas() {
        List<Usuario> usuarios = repository.findAll();
        Usuario usuario = usuarios.stream()
                .filter(u -> u.getUsuId() != null && u.getUsuId().contains("b"))
                .findFirst()
                .orElseThrow();

        Optional<Usuario> encontrado = repository.findByUsuId(usuario.getUsuId().toUpperCase());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getUsuId()).isEqualTo(usuario.getUsuId());
    }

    @Test
    @DisplayName("R-LOG-003: los 552 UsuId se descifran sin excepcion")
    void losUsuIdSeDescifranSinExcepcion() {
        List<Usuario> usuarios = repository.findAll();

        assertThat(usuarios).hasSize(552);
        assertThat(usuarios).allSatisfy(usuario -> {
            String enClaro = codificador.decodificar(usuario.getUsuId());
            assertThat(enClaro).isNotEmpty();
            assertThat(enClaro).isEqualTo(enClaro.toUpperCase(java.util.Locale.ROOT));
        });
    }

    @Test
    @DisplayName("spec 3.2.1: 549 UsuId decodifican a 10 caracteres y 3 a 9 (se perdio la enye)")
    void laCantidadDeUsuIdDecodificadosReproduceElDatoDeLaSpec() {
        List<Usuario> usuarios = repository.findAll();

        long deDiez = usuarios.stream()
                .filter(u -> codificador.decodificar(u.getUsuId()).length() == 10)
                .count();
        long deNueve = usuarios.stream()
                .filter(u -> codificador.decodificar(u.getUsuId()).length() == 9)
                .count();

        assertThat(deDiez).isEqualTo(549);
        assertThat(deNueve).isEqualTo(3);
    }

    @Test
    @DisplayName("R-LOG-005: hay 2 cuentas sin clave almacenada (solo UsuHash)")
    void hayCuentasSinClaveAlmacenada() {
        long sinClave = repository.findAll().stream()
                .filter(u -> u.getUsuClave() == null || u.getUsuClave().isBlank())
                .count();

        assertThat(sinClave).isEqualTo(2);
    }

    @Test
    @DisplayName("R-LOG-037: hay un usuario con UsuIngreso nulo (1 de las 552 filas)")
    void hayUnUsuarioConUsuIngresoNulo() {
        long nulos = repository.findAll().stream()
                .filter(u -> u.getUsuIngreso() == null)
                .count();

        assertThat(nulos).isEqualTo(1);
    }
}
