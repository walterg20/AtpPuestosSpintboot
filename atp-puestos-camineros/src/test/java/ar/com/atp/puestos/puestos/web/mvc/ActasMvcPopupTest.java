package ar.com.atp.puestos.puestos.web.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.GestorContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.ProveedorNavegacion;
import ar.com.atp.puestos.puestos.service.ActaService;
import ar.com.atp.puestos.puestos.service.model.ActaPreparada;
import ar.com.atp.puestos.puestos.service.model.ActaProductoDisponible;
import ar.com.atp.puestos.puestos.service.model.ResultadoAltaActa;
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
class ActasMvcPopupTest {

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
        lenient().when(actaService.guardar(any(), any()))
                .thenReturn(new ResultadoAltaActa(false, "Debe seleccionar algun producto", null));
    }

    @Test
    @DisplayName("GET /actas/nueva expone los 3 flags del popup")
    void getNuevaExponeFlags() throws Exception {
        mockMvc.perform(get("/actas/nueva"))
                .andExpect(status().isOk())
                .andExpect(view().name("puestos/carga-manual-actas"))
                .andExpect(model().attributeExists("locCodVenDerivado"))
                .andExpect(model().attributeExists("locCodDesDerivado"))
                .andExpect(model().attributeExists("locCodTranDerivado"));
    }

    @Test
    @DisplayName("GET /actas/nueva renderiza el icono en los 3 fieldsets (habilitado)")
    void getNuevaRenderizaIconoHabilitado() throws Exception {
        String html = html(get("/actas/nueva"));
        assertThat(html).contains("⇧");
        // 3 ocurrencias del icono
        int count = 0;
        int idx = html.indexOf("⇧");
        while (idx >= 0) {
            count++;
            idx = html.indexOf("⇧", idx + 1);
        }
        assertThat(count).isEqualTo(3);
        assertThat(html).contains("localidad-popup-trigger");
        // estado habilitado por defecto (flag false)
        assertThat(html).contains("is-enabled");
        assertThat(html).contains("data-popup-enabled=\"true\"");
    }

    @Test
    @DisplayName("POST /actas/guardar con localidad derivada renderiza el icono deshabilitado")
    void postGuardarRenderizaIconoDeshabilitado() throws Exception {
        String html = html(post("/actas/guardar").with(csrf())
                .param("f276ActaTi", "P")
                .param("f276Clasif", "S")
                .param("f276FchMov", "2020-01-15")
                .param("f276CuitVe", "20123456789")
                .param("f276CuitDe", "20123456790")
                .param("f276CuitTr", "20123456791")
                .param("f276TCpbte", "1")
                .param("f276NCpbte", "0000000001")
                .param("f276Chasis", "AB123CD")
                .param("f276Acopla", "EF456GH")
                .param("f276KmFle", "150")
                .param("f276VenCLo", "5")
                .param("f276DestCL", "6")
                .param("f276TranCL", "7")
                .param("productos[0].prodCod", "101")
                .param("productos[0].f276RCant", "2"));

        assertThat(html).contains("⇧");
        assertThat(html).contains("is-disabled");
        assertThat(html).contains("data-popup-enabled=\"false\"");
        assertThat(html).contains("disabled=\"disabled\"");
    }

    private String html(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder)
            throws Exception {
        return mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
