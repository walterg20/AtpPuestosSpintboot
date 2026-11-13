package ar.gob.chaco.atp.puestoscamineros.shared.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.gob.chaco.atp.puestoscamineros.nucleo.web.rest.ExplodingController;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.rest.MetodoUnicoController;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

/**
 * T-409 (R-206): las excepciones no controladas deben quedar registradas en el log del servidor con
 * nivel {@code ERROR} y su traza, sin que el cliente reciba el tipo de excepción, la traza ni ningún
 * detalle interno.
 *
 * <p>Cubre los dos extremos del requisito: (a) el log del servidor registra el error y (b) el cuerpo
 * de la respuesta sigue siendo el uniforme {@code { code:"ERROR_INTERNO", message:… }} de R-023.
 *
 * <p>T-411 (R-208): el aborto del cliente (decisión del cliente, no fallo del servidor) se registra
 * a nivel {@code INFO} <strong>sin</strong> traza y no produce un {@code 500 ERROR_INTERNO}.
 *
 * <p>T-415 (R-209): la ruta inexistente responde {@code 404 NO_ENCONTRADO} y el método no
 * soportado responde {@code 405 METODO_NO_SOPORTADO}, ambos con el cuerpo uniforme y
 * <strong>sin</strong> generar ningún evento {@code ERROR} en el log (la señal de R-206 queda
 * limpia: no son fallos del servidor).
 */
@WebMvcTest(controllers = {ExplodingController.class, MetodoUnicoController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    /** Logger de Logback asociado al handler (SLF4J delega en Logback en el classpath de test). */
    private Logger logbackLogger;

    /** Appender en memoria: captura los eventos emitidos por el handler. */
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logbackLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        appender = new ListAppender<>();
        appender.start();
        logbackLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logbackLogger.detachAppender(appender);
        appender.stop();
    }

    /**
     * Peticion del canal REST.
     *
     * <p>El advice negocia la respuesta segun el prefijo de la ruta: {@code /api/} produce JSON
     * (cuerpo uniforme {@code ApiError}) y cualquier otra ruta produce la vista {@code error}.
     * Para ejercitar el camino JSON hace falta una peticion con ruta {@code /api/...}.
     */
    private static HttpServletRequest peticionApi() {
        return new MockHttpServletRequest("GET", "/api/v1/test/explotar");
    }

    /** Extrae el cuerpo {@link ApiError} de la respuesta que el advice devuelve para el canal REST. */
    @SuppressWarnings("unchecked")
    private static ApiError cuerpoDeError(Object respuesta) {
        assertThat(respuesta).isInstanceOf(ResponseEntity.class);
        return ((ResponseEntity<ApiError>) respuesta).getBody();
    }

    @Test
    @DisplayName("(a) el fallback devuelve el cuerpo uniforme sin filtrar tipo ni mensaje interno")
    void handleGenericDevuelveCuerpoUniformeSinDetalleInterno() {
        ApiError error = cuerpoDeError(new GlobalExceptionHandler()
                .handleGeneric(new IllegalStateException("boom-secreto"), peticionApi()));

        assertThat(error.code()).isEqualTo("ERROR_INTERNO");
        assertThat(error.message()).isEqualTo("Ocurrió un error interno no esperado.");
        assertThat(error.message()).doesNotContain("IllegalStateException");
        assertThat(error.message()).doesNotContain("boom-secreto");
    }

    @Test
    @DisplayName("(b) el fallback registra la excepción con nivel ERROR y su traza")
    void handleGenericRegistraLaExcepcionConNivelErrorYTraza() {
        new GlobalExceptionHandler()
                .handleGeneric(new IllegalStateException("boom-secreto"), peticionApi());

        assertThat(appender.list).isNotEmpty();
        ILoggingEvent event = appender.list.get(appender.list.size() - 1);

        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage()).contains("Error interno no controlado");
        assertThat(event.getThrowableProxy()).isNotNull();
        assertThat(event.getThrowableProxy().getClassName())
                .isEqualTo(IllegalStateException.class.getName());
    }

    @Test
    @DisplayName("(c) por HTTP el 500 uniforme no expone el mensaje ni la traza al cliente")
    void errorNoControladoResponde500UniformeSinExponerLaTraza() throws Exception {
        String body = mockMvc.perform(get("/api/v1/test/explotar"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERROR_INTERNO"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("fallo-interno-xyz");
        assertThat(body).doesNotContain("RuntimeException");
        assertThat(body).doesNotContain("at ar.com.atp");
    }

    @Test
    @DisplayName("(d) R-208: el aborto del cliente se registra a nivel INFO y sin traza")    void abortoDelClienteSeRegistraEnInfoSinTraza() {
        new GlobalExceptionHandler().handleClientAbort(
                new AsyncRequestNotUsableException("ServletOutputStream failed to write"));

        assertThat(appender.list).isNotEmpty();
        ILoggingEvent event = appender.list.get(appender.list.size() - 1);

        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        // Sin throwable => sin traza en el log (R-208).
        assertThat(event.getThrowableProxy()).isNull();
        assertThat(event.getFormattedMessage()).contains("R-208");
    }

    @Test
    @DisplayName("(e) R-208: el aborto del cliente (ClientAbortException) también se registra en INFO")
    void abortoDelClienteTomcatSeRegistraEnInfoSinTraza() {
        new GlobalExceptionHandler().handleClientAbort(
                new ClientAbortException("Se ha anulado una conexión establecida."));

        assertThat(appender.list).isNotEmpty();
        ILoggingEvent event = appender.list.get(appender.list.size() - 1);

        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getThrowableProxy()).isNull();
    }

    @Test
    @DisplayName("(f) R-208: por HTTP el aborto del cliente NO responde 500 ERROR_INTERNO")
    void abortoDelClienteNoResponde500ErrorInterno() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/test/abortar"))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse();

        String body = response.getContentAsString();

        assertThat(response.getStatus()).isNotEqualTo(500);
        assertThat(body).doesNotContain("ERROR_INTERNO");
        assertThat(body).doesNotContain("AsyncRequestNotUsableException");
    }

    @Test
    @DisplayName("(g) R-208: el aborto del cliente no genera ningún evento ERROR en el log")
    void abortoDelClienteNoGeneraEventosError() {
        new GlobalExceptionHandler().handleClientAbort(
                new AsyncRequestNotUsableException("ServletOutputStream failed to write"));

        assertThat(appender.list).noneMatch(e -> e.getLevel() == Level.ERROR);
    }

    @Test
    @DisplayName("(h) R-209: una ruta inexistente responde 404 con el cuerpo uniforme")
    void rutaInexistenteResponde404Uniforme() throws Exception {
        appender.list.clear();

        String body = mockMvc.perform(get("/api/v1/ruta-que-no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.*", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Cuerpo uniforme {code, message}: nada de detalles internos ni de ERROR_INTERNO.
        assertThat(body).contains("\"code\":\"NO_ENCONTRADO\"");
        assertThat(body).doesNotContain("ERROR_INTERNO");
        assertThat(body).doesNotContain("NoResourceFoundException");
        assertThat(body).doesNotContain("at ar.com.atp");
    }

    @Test
    @DisplayName("(i) R-209: un método no soportado responde 405 con el cuerpo uniforme")
    void metodoNoSoportadoResponde405Uniforme() throws Exception {
        appender.list.clear();

        String body = mockMvc.perform(post("/api/v1/test/solo-get"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METODO_NO_SOPORTADO"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.*", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("\"code\":\"METODO_NO_SOPORTADO\"");
        assertThat(body).doesNotContain("ERROR_INTERNO");
        assertThat(body).doesNotContain("HttpRequestMethodNotSupportedException");
        assertThat(body).doesNotContain("at ar.com.atp");
    }

    @Test
    @DisplayName("(j) R-209: la ruta inexistente NO genera ningún evento ERROR en el log")
    void rutaInexistenteNoGeneraEventosError() throws Exception {
        appender.list.clear();

        mockMvc.perform(get("/api/v1/ruta-que-no-existe"))
                .andExpect(status().isNotFound());

        // El handler específico se ejecutó (log INFO con la marca de R-209)…
        assertThat(appender.list).anyMatch(e -> e.getLevel() == Level.INFO
                && e.getFormattedMessage().contains("R-209"));
        // …pero la señal de R-206 (ERROR) queda limpia: una ruta inexistente no es un fallo del
        // servidor.
        assertThat(appender.list).noneMatch(e -> e.getLevel() == Level.ERROR);
        assertThat(appender.list).noneMatch(e -> e.getThrowableProxy() != null);
    }

    @Test
    @DisplayName("(k) R-209: el método no soportado NO genera ningún evento ERROR en el log")
    void metodoNoSoportadoNoGeneraEventosError() throws Exception {
        appender.list.clear();

        mockMvc.perform(post("/api/v1/test/solo-get"))
                .andExpect(status().isMethodNotAllowed());

        assertThat(appender.list).anyMatch(e -> e.getLevel() == Level.INFO
                && e.getFormattedMessage().contains("R-209"));
        assertThat(appender.list).noneMatch(e -> e.getLevel() == Level.ERROR);
        assertThat(appender.list).noneMatch(e -> e.getThrowableProxy() != null);
    }

    @Test
    @DisplayName("(l) R-206 no se relaja: una excepción no controlada sigue registrando ERROR con traza")
    void excepcionNoControladaSigueRegistrandoErrorConTraza() throws Exception {
        appender.list.clear();

        mockMvc.perform(get("/api/v1/test/explotar"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERROR_INTERNO"));

        assertThat(appender.list).anyMatch(e -> e.getLevel() == Level.ERROR
                && e.getThrowableProxy() != null
                && RuntimeException.class.getName()
                        .equals(e.getThrowableProxy().getClassName()));
    }
}
