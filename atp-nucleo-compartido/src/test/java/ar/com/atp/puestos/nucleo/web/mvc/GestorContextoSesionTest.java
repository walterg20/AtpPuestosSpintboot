package ar.com.atp.puestos.nucleo.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * T-109 (parte de vistas) — El contexto de sesión se persiste bajo la clave {@code "context"},
 * con paridad con {@code SetContext} / {@code LoadContext} del legacy (R-LOG-017).
 */
class GestorContextoSesionTest {

    private static final ContextoSesion CONTEXTO = ContextoSesion.completo("YOLOFFG",
            "Yolanda Officer", 12, 345, 7, 10, "Puesto 10", "PUE", 44, "Responsable Nombre");

    @Test
    @DisplayName("T-109: el contexto se guarda en la sesión bajo la clave 'context'")
    void elContextoSeGuardaEnLaSesion() {
        GestorContextoSesion gestor = new GestorContextoSesion();
        MockHttpServletRequest request = new MockHttpServletRequest();

        gestor.guardar(request, CONTEXTO);

        assertThat(request.getSession(false).getAttribute("context")).isEqualTo(CONTEXTO);
        assertThat(GestorContextoSesion.CLAVE).isEqualTo("context");
    }

    @Test
    @DisplayName("T-109: el contexto se recupera íntegro de la sesión")
    void elContextoSeRecuperaDeLaSesion() {
        GestorContextoSesion gestor = new GestorContextoSesion();
        MockHttpServletRequest request = new MockHttpServletRequest();
        gestor.guardar(request, CONTEXTO);

        ContextoSesion recuperado = gestor.recuperar(request);

        assertThat(recuperado).isEqualTo(CONTEXTO);
        assertThat(recuperado.usuario()).isEqualTo("YOLOFFG");
        assertThat(recuperado.usuarioNombre()).isEqualTo("Yolanda Officer");
        assertThat(recuperado.personaId()).isEqualTo(12);
        assertThat(recuperado.comision()).isEqualTo(345);
        assertThat(recuperado.turno()).isEqualTo(7);
        assertThat(recuperado.sucursal()).isEqualTo(10);
        assertThat(recuperado.sucursalNombre()).isEqualTo("Puesto 10");
        assertThat(recuperado.tipo()).isEqualTo("PUE");
        assertThat(recuperado.responsable()).isEqualTo(44);
        assertThat(recuperado.responsableNombre()).isEqualTo("Responsable Nombre");
    }

    @Test
    @DisplayName("T-109: sin sesión o sin atributo, se devuelve el contexto vacío")
    void sinSesionSeDevuelveElContextoVacio() {
        GestorContextoSesion gestor = new GestorContextoSesion();
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(gestor.recuperar(request)).isEqualTo(ContextoSesion.vacio());

        MockHttpServletRequest conAtributoAjeno = new MockHttpServletRequest();
        conAtributoAjeno.getSession(true).setAttribute("context", "no soy un contexto");
        assertThat(gestor.recuperar(conAtributoAjeno)).isEqualTo(ContextoSesion.vacio());
    }
}
