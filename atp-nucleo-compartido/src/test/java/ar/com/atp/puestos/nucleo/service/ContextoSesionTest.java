package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * T-106 / R-LOG-016: el contexto de sesión replica el SDT {@code Context} del legacy.
 *
 * <p>La lista de nombres se transcribe de la spec (§3.5): los <strong>13</strong> campos del SDT,
 * de los cuales el login carga 10 y deja 3 en su valor por defecto.
 */
class ContextoSesionTest {

    /** Los 13 campos del SDT {@code Context}, en el orden de la spec §3.5. */
    private static final List<String> CAMPOS_DEL_SDT = List.of(
            "usuario",
            "usuarioNombre",
            "personaId",
            "comision",
            "turno",
            "sucursal",
            "sucursalNombre",
            "tipo",
            "responsable",
            "responsableNombre",
            "form2276",
            "f276Id",
            "ultimoMenu");

    @Test
    @DisplayName("T-106: el contexto expone los 13 campos del SDT Context, en orden")
    void elContextoExponeLos13CamposDelSdt() {
        List<String> nombres = Arrays.stream(ContextoSesion.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(nombres).containsExactlyElementsOf(CAMPOS_DEL_SDT);
    }

    @Test
    @DisplayName("R-LOG-016: el contexto vacio deja los 13 campos en su valor por defecto")
    void elContextoVacioDejaLosCamposPorDefecto() {
        ContextoSesion contexto = ContextoSesion.vacio();

        assertThat(contexto.usuario()).isEmpty();
        assertThat(contexto.usuarioNombre()).isEmpty();
        assertThat(contexto.personaId()).isZero();
        assertThat(contexto.comision()).isZero();
        assertThat(contexto.turno()).isZero();
        assertThat(contexto.sucursal()).isZero();
        assertThat(contexto.sucursalNombre()).isEmpty();
        assertThat(contexto.tipo()).isEmpty();
        assertThat(contexto.responsable()).isZero();
        assertThat(contexto.responsableNombre()).isEmpty();
        assertThat(contexto.form2276()).isNull();
        assertThat(contexto.f276Id()).isNull();
        assertThat(contexto.ultimoMenu()).isNull();
    }

    @Test
    @DisplayName("R-LOG-015: el contexto parcial carga 4 campos y deja los demas por defecto")
    void elContextoParcialCargaCuatroCampos() {
        ContextoSesion contexto = ContextoSesion.parcial("YOLOFFG", "YOLOFFG NOMBRE", 77, "PUE");

        assertThat(contexto.usuario()).isEqualTo("YOLOFFG");
        assertThat(contexto.usuarioNombre()).isEqualTo("YOLOFFG NOMBRE");
        assertThat(contexto.personaId()).isEqualTo(77);
        assertThat(contexto.tipo()).isEqualTo("PUE");
        assertThat(contexto.comision()).isZero();
        assertThat(contexto.turno()).isZero();
        assertThat(contexto.sucursal()).isZero();
        assertThat(contexto.sucursalNombre()).isEmpty();
        assertThat(contexto.responsable()).isZero();
        assertThat(contexto.responsableNombre()).isEmpty();
        assertThat(contexto.form2276()).isNull();
        assertThat(contexto.f276Id()).isNull();
        assertThat(contexto.ultimoMenu()).isNull();
    }
}
