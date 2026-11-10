package ar.com.atp.puestos.puestos.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.puestos.domain.ContadorFormulario;
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
 * Módulo {@code 09-formularios-acta} — contador de numeración {@code GES_SUCN} contra la base
 * {@code Rentas}. Se ejecuta sólo con {@code ATP_DB_IT=true}.
 *
 * <p><strong>La escritura se revierte</strong>: el test corre dentro de la transacción del
 * {@code @DataJpaTest}, que hace {@code rollback} al terminar. La base de producción queda sin
 * cambios.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("09-formularios-acta - GES_SUCN: incremento atomico del contador (rollback)")
class ContadorFormularioRepositorioIT {

    /** Sucursal con contador de Actas en {@code GES_SUCN}. */
    private static final Integer SUCURSAL = 1;
    private static final String TIPO = "PUE";
    private static final Integer NUMERO_FORMULARIO = 2276;

    @Autowired
    private ContadorFormularioRepositorio repositorio;

    @Test
    @DisplayName("D-ACTA-19: el incremento es atomico y devuelve el valor siguiente")
    void elIncrementoDevuelveElValorSiguiente() {
        Integer antes = repositorio.valorActual(SUCURSAL, TIPO, NUMERO_FORMULARIO);
        assertThat(antes).isNotNull();

        int filas = repositorio.incrementarAtomico(SUCURSAL, TIPO, NUMERO_FORMULARIO);

        assertThat(filas).isEqualTo(1);
        assertThat(repositorio.valorActual(SUCURSAL, TIPO, NUMERO_FORMULARIO))
                .isEqualTo(antes + 1);
    }

    @Test
    @DisplayName("D-ACTA-19: dos incrementos consecutivos devuelven n+1 y n+2")
    void dosIncrementosConsecutivos() {
        Integer antes = repositorio.valorActual(SUCURSAL, TIPO, NUMERO_FORMULARIO);

        repositorio.incrementarAtomico(SUCURSAL, TIPO, NUMERO_FORMULARIO);
        Integer primero = repositorio.valorActual(SUCURSAL, TIPO, NUMERO_FORMULARIO);
        repositorio.incrementarAtomico(SUCURSAL, TIPO, NUMERO_FORMULARIO);
        Integer segundo = repositorio.valorActual(SUCURSAL, TIPO, NUMERO_FORMULARIO);

        assertThat(primero).isEqualTo(antes + 1);
        assertThat(segundo).isEqualTo(antes + 2);
    }

    @Test
    @DisplayName("D-ACTA-19: la fila del contador se lee por su clave primaria compuesta")
    void laFilaDelContadorSeLee() {
        Optional<ContadorFormulario> contador = repositorio.buscar(SUCURSAL, TIPO,
                NUMERO_FORMULARIO);

        assertThat(contador).isPresent();
        ContadorFormulario fila = contador.get();
        assertThat(fila.getSucNroId()).isEqualTo(SUCURSAL);
        assertThat(fila.getSucTipo()).isEqualTo(TIPO);
        assertThat(fila.getSucNuForm()).isEqualTo(NUMERO_FORMULARIO);
        assertThat(fila.getSucUltNum()).isNotNull();
    }

    @Test
    @DisplayName("SA-ACTA-14: el identificador del contador es 'MAN' en las filas de Actas")
    void elIdentificadorDelContador() {
        repositorio.buscar(SUCURSAL, TIPO, NUMERO_FORMULARIO).ifPresent(fila ->
                assertThat(fila.getSucNuId()).isEqualTo("MAN"));
    }
}
