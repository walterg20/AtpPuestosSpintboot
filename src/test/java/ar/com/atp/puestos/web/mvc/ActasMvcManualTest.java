package ar.gob.chaco.atp.puestoscamineros.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.GestorContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc.ProveedorNavegacion;
import ar.gob.chaco.atp.puestoscamineros.service.ActaService;
import ar.gob.chaco.atp.puestoscamineros.service.MensajesActa;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaPreparada;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaProductoDisponible;
import ar.gob.chaco.atp.puestoscamineros.service.model.ResultadoAltaActa;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;

@WebMvcTest(controllers = ActasController.class)
@WithMockUser(username = "tester", roles = "USER")
class ActasMvcManualTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActaService actaService;

    @MockitoBean
    private ProveedorNavegacion proveedorNavegacion;

    @MockitoBean
    private GestorContextoSesion gestorContextoSesion;

    private ContextoSesion contextoConTurno;

    @BeforeEach
    void setUp() {
        contextoConTurno = ContextoSesion.completo("TESTER", "Tester", 1, 1, 7, 5, "Puesto5", "PUE",
                1, "Resp");

        doAnswer(invocacion -> {
            Model model = invocacion.getArgument(0);
            model.addAttribute("contexto", contextoConTurno);
            model.addAttribute("programas", List.of());
            model.addAttribute("rutaActual", "/actas/nueva");
            return null;
        }).when(proveedorNavegacion).poblar(any(), any());

        lenient().when(gestorContextoSesion.recuperar(any())).thenReturn(contextoConTurno);
        lenient().when(actaService.productosDisponibles())
                .thenReturn(List.of(new ActaProductoDisponible(101, "GRANO", 1, "N",
                        new BigDecimal("10.00"), BigDecimal.ZERO)));
        lenient().when(actaService.prepararFormulario(any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(new ActaPreparada(false, null, "A", 123, LocalDate.of(2020, 1, 15)));
        lenient().when(actaService.prepararFormulario(any(),
                org.mockito.ArgumentMatchers.eq(42),
                org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(new ActaPreparada(false, null, null, null, LocalDate.of(2020, 1, 15)));
        lenient().when(actaService.guardar(any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(new ResultadoAltaActa(true, null, 123));
    }

    private String html(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder)
            throws Exception {
        return mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static int cuantas(String html, String aguja) {
        int total = 0;
        int indice = html.indexOf(aguja);
        while (indice >= 0) {
            total++;
            indice = html.indexOf(aguja, indice + aguja.length());
        }
        return total;
    }

    @Test
    @DisplayName("GET /actas/nueva?manual=true&turNroId=42 devuelve 200 y renderiza F276Id editable")
    void getNuevaManualRenderizaNumeroDeActa() throws Exception {
        mockMvc.perform(get("/actas/nueva").param("manual", "true").param("turNroId", "42"))
                .andExpect(status().isOk())
                .andExpect(view().name("puestos/carga-manual-actas"))
                .andExpect(model().attribute("manual", true))
                .andExpect(model().attribute("turNroId", 42));

        String html = html(get("/actas/nueva").param("manual", "true").param("turNroId", "42"));
        assertThat(html).contains("Nro. Acta");
        assertThat(html).contains("name=\"f276Id\"");
        assertThat(html).contains("name=\"manual\"");
        assertThat(html).contains("name=\"turNroId\"");
    }

    @Test
    @DisplayName("POST /actas/guardar modo manual con binding invalido re-renderiza el formulario")
    void postGuardarManualBindingInvalido() throws Exception {
        String html = html(post("/actas/guardar").with(csrf())
                .param("manual", "true")
                .param("turNroId", "42")
                .param("f276KmFle", "no-es-numero"));

        assertThat(html).contains("name=\"manual\"");
        verify(actaService, never()).guardar(any(), any(), eq(true));
    }

    @Test
    @DisplayName("POST /actas/guardar modo manual con numero invalido 0 muestra el mensaje verbatim")
    void postGuardarManualNumeroInvalidoCero() throws Exception {
        when(actaService.guardar(any(), any(), eq(true)))
                .thenReturn(new ResultadoAltaActa(false, MensajesActa.MSG_NRO_FORMULARIO_GENERADO, null));

        org.springframework.util.MultiValueMap<String, String> params = parametrosValidosManual(0);
        String html = html(post("/actas/guardar").with(csrf()).params(params));

        assertThat(html).contains(MensajesActa.MSG_NRO_FORMULARIO_GENERADO);
    }

    @Test
    @DisplayName("POST /actas/guardar modo manual con numero fuera de rango muestra el mensaje verbatim")
    void postGuardarManualNumeroFueraDeRango() throws Exception {
        when(actaService.guardar(any(), any(), eq(true)))
                .thenReturn(new ResultadoAltaActa(false, MensajesActa.MSG_NRO_ACTA_NO_MANUAL, null));

        org.springframework.util.MultiValueMap<String, String> params = parametrosValidosManual(999999);
        String html = html(post("/actas/guardar").with(csrf()).params(params));

        assertThat(html).contains(MensajesActa.MSG_NRO_ACTA_NO_MANUAL);
    }

    @Test
    @DisplayName("POST /actas/guardar modo manual exitoso redirige a /actas con el flash verbatim")
    void postGuardarManualExitoso() throws Exception {
        when(actaService.guardar(any(), any(), eq(true)))
                .thenReturn(new ResultadoAltaActa(true, null, 123));

        org.springframework.util.MultiValueMap<String, String> params = parametrosValidosManual(100);
        mockMvc.perform(post("/actas/guardar").with(csrf()).params(params))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/actas"))
                .andExpect(flash().attribute("mensajeExito", "El acta se ha creado correctamente"));
    }

    private static org.springframework.util.MultiValueMap<String, String> parametrosValidosManual(int f276Id) {
        org.springframework.util.MultiValueMap<String, String> params =
                new org.springframework.util.LinkedMultiValueMap<>();
        params.add("manual", "true");
        params.add("turNroId", "42");
        params.add("f276Id", String.valueOf(f276Id));
        params.add("f276ActaTi", "P");
        params.add("f276Clasif", "S");
        params.add("f276FchMov", "2020-01-15");
        params.add("f276CuitVe", "20123456789");
        params.add("f276CuitDe", "20123456790");
        params.add("f276CuitTr", "20123456791");
        params.add("f276TCpbte", "1");
        params.add("f276NCpbte", "0000000001");
        params.add("f276Chasis", "AB123CD");
        params.add("f276Acopla", "EF456GH");
        params.add("f276KmFle", "150");
        params.add("productos[0].prodCod", "101");
        params.add("productos[0].f276RCant", "2");
        return params;
    }
}
