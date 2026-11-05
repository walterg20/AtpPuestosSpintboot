package ar.com.atp.puestos.puestos.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;
import ar.com.atp.puestos.nucleo.web.mvc.GestorContextoSesion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@WithMockUser(username = "tester", roles = "USER")
@DisplayName("10-popup-localidades - PopupLocalidadesIT contra Rentas")
class PopupLocalidadesIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockHttpSession sesionConContexto() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(GestorContextoSesion.CLAVE,
                ContextoSesion.completo("TESTER", "Tester", 1, 1, 7, 5, "Puesto5", "PUE",
                        1, "Resp"));
        return session;
    }

    private static int contar(String html, String aguja) {
        int total = 0;
        int indice = html.indexOf(aguja);
        while (indice >= 0) {
            total++;
            indice = html.indexOf(aguja, indice + aguja.length());
        }
        return total;
    }

    @Test
    @DisplayName("10-popup-localidades: /localidades renderiza las 30 filas con FK huerfana con provincia vacia")
    void renderizaFilasHuerfanasConProvinciaVacia() throws Exception {
        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/localidades")
                                .param("size", "1000")
                                .session(sesionConContexto()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        assertThat(contar(html, "<td></td>")).isEqualTo(30);
    }

    @Test
    @DisplayName("10-popup-localidades: el conteo total coincide con SELECT COUNT(*) FROM LOCALIDA")
    void conteoTotalCoincide() throws Exception {
        long totalDb = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM LOCALIDA", Long.class);

        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/localidades")
                                .param("size", "1000")
                                .session(sesionConContexto()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("Total elementos: <span>" + totalDb + "</span>");
    }
}
