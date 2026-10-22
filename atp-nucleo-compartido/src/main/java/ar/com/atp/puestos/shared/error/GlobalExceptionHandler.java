package ar.com.atp.puestos.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduce las excepciones de la aplicación a la respuesta que corresponde al canal que la
 * generó: <strong>JSON uniforme</strong> para la API REST {@code /api/**} y <strong>vista
 * HTML</strong> para las páginas Thymeleaf.
 *
 * <p>Es un único advice global (<em>no</em> acotado por {@code basePackages}) por una razón
 * concreta: las excepciones de <strong>ruta inexistente (404)</strong> y <strong>método no
 * soportado (405)</strong> las lanza el {@code DispatcherServlet}, no un controlador. Un advice
 * acotado a los paquetes de los controladores REST no se activaría para ellas y las respuestas
 * 404/405 dejarían de llevar el cuerpo uniforme {@code {code, message}} que fija R-209.
 *
 * <p>La decisión de canal se toma por el <strong>prefijo de la ruta</strong>: {@code /api/} es
 * API REST (JSON); cualquier otra ruta es una página (HTML). Es una convención estable del
 * proyecto y no depende del header {@code Accept}, que los formularios HTML no envían de forma
 * fiable.
 *
 * <p>Además, R-209 separa las <em>respuestas esperadas</em> (ruta inexistente, método no
 * soportado) de los <em>fallos del servidor</em>: las primeras responden 404/405 con el cuerpo
 * uniforme y se registran a nivel {@code INFO} sin traza; las segundas (R-206) siguen
 * registrándose a nivel {@code ERROR} y traza completa.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /** Prefijo de ruta que identifica al canal REST (JSON). */
    private static final String PREFIJO_API = "/api/";

    /** Log del servidor: las excepciones no controladas quedan registradas con su traza (R-206). */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Recurso inexistente → 404. */
    @ExceptionHandler(NotFoundException.class)
    public Object handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return responder(request, HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage());
    }

    /** Conflicto (PK duplicada) → 409. */
    @ExceptionHandler(ConflictException.class)
    public Object handleConflict(ConflictException ex, HttpServletRequest request) {
        return responder(request, HttpStatus.CONFLICT, ex.getCode(), ex.getMessage());
    }

    /** Violaciones de {@code @Valid} en el body → 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String detail = fieldError != null
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : "cuerpo de la petición inválido";
        return responder(request, HttpStatus.BAD_REQUEST, "VALIDACION", detail);
    }

    /** Body ilegible / tipos incompatibles → 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return responder(request, HttpStatus.BAD_REQUEST, "CUERPO_INVALIDO",
                "El cuerpo de la petición no es válido.");
    }

    /** Parámetro obligatorio ausente → 400. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Object handleMissingParam(MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        return responder(request, HttpStatus.BAD_REQUEST, "PARAMETRO_FALTANTE",
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "'.");
    }

    /** Violaciones de constraints en parámetros → 400. */
    @ExceptionHandler(ConstraintViolationException.class)
    public Object handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        return responder(request, HttpStatus.BAD_REQUEST, "VALIDACION", ex.getMessage());
    }

    /** Restricciones de integridad de la BD (p. ej. PK duplicada) → 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return responder(request, HttpStatus.CONFLICT, "CONFLICTO_DATOS",
                "La operación viola una restricción de integridad.");
    }

    /**
     * Aborto del cliente (R-208).
     *
     * <p>La excepción que llega al handler es
     * {@link AsyncRequestNotUsableException} (Spring 6.1+: la respuesta ya no es utilizable) y su
     * causa es {@link ClientAbortException} (Tomcat: la conexión se aborta). Se cubren las dos.
     *
     * <p>El log va a nivel {@code INFO} y <strong>sin</strong> pasar la excepción como argumento
     * del throwable: así la traza no aparece en el log. El handler no devuelve cuerpo: cuando la
     * respuesta ya fue comiteada no se puede escribir, y cuando no lo está el cliente ya no está
     * escuchando.
     */
    @ExceptionHandler({AsyncRequestNotUsableException.class, ClientAbortException.class})
    public void handleClientAbort(Exception ex) {
        log.info("R-208: el cliente abortó la conexión antes de recibir la respuesta ({}). "
                + "No es un error interno.", ex.getClass().getSimpleName());
    }

    /**
     * Ruta inexistente → 404 (R-209).
     *
     * <p>Verificado empíricamente contra el backend vivo (Spring Framework 6.2.19 / Spring Boot
     * 3.5.16): una ruta no registrada <strong>no</strong> llega como
     * {@link NoHandlerFoundException} sino como {@link NoResourceFoundException}, porque el
     * {@code ResourceHttpRequestHandler} que Spring Boot registra para {@code /**} (recursos
     * estáticos) captura el path y falla al resolverlo. La traza observada fue
     * {@code ResourceHttpRequestHandler.handleRequest -> NoResourceFoundException}.
     * {@link NoHandlerFoundException} se cubre igualmente en el mismo handler porque es la
     * variante que Spring usa si someday se desactiva
     * {@code spring.web.resources.add-mappings} o se habilita
     * {@code spring.mvc.throw-exception-if-no-handler-found}: la semántica (404) es la misma.
     *
     * <p>Como no es un fallo del servidor sino una respuesta esperada, se registra a nivel
     * {@code INFO} <strong>sin</strong> traza (la señal {@code ERROR} de R-206 queda limpia) y el
     * cuerpo es el uniforme {@code {code:"NO_ENCONTRADO", message:…}} sin filtrar detalles
     * internos.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public Object handleRutaNoEncontrada(Exception ex, HttpServletRequest request) {
        log.info("R-209: no existe el recurso solicitado ({}). No es un error interno.",
                rutaDe(ex));
        return responder(request, HttpStatus.NOT_FOUND, "NO_ENCONTRADO",
                "El recurso solicitado no existe.");
    }

    /**
     * Método HTTP no soportado por el recurso → 405 (R-209).
     *
     * <p>Verificado empíricamente contra el backend vivo: la excepción que llega es
     * {@link HttpRequestMethodNotSupportedException}, lanzada por
     * {@code RequestMappingInfoHandlerMapping.handleNoMatch} cuando el path existe pero el verbo
     * no está mapeado (p. ej. {@code POST /api/v1/provincias/{provCod}}, que sólo admite
     * {@code GET}/{@code PUT}/{@code DELETE}).
     *
     * <p>Como es una respuesta esperada y no un fallo del servidor, se registra a nivel
     * {@code INFO} <strong>sin</strong> traza. El cuerpo es el uniforme
     * {@code {code:"METODO_NO_SOPORTADO", message:…}}. Se preserva la semántica HTTP del
     * encabezado {@code Allow} con los verbos admitidos por el recurso.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleMetodoNoSoportado(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request, HttpServletResponse response) {
        String metodo = ex.getMethod() == null ? "-" : ex.getMethod();
        String[] admitidos = ex.getSupportedMethods();
        if (admitidos != null && admitidos.length > 0) {
            response.setHeader(HttpHeaders.ALLOW, String.join(", ", admitidos));
        }
        log.info("R-209: el método {} no está soportado por el recurso solicitado. "
                + "No es un error interno.", metodo);
        return responder(request, HttpStatus.METHOD_NOT_ALLOWED, "METODO_NO_SOPORTADO",
                "El método " + metodo + " no está soportado por este recurso.");
    }

    /**
     * Fallback → 500 con el formato uniforme.
     *
     * <p>R-206: la excepción se registra en el log del servidor con nivel {@code ERROR} y su traza
     * completa; el cuerpo de la respuesta al cliente no expone tipo de excepción, traza ni
     * ningún detalle interno.
     */
    @ExceptionHandler(Exception.class)
    public Object handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error interno no controlado", ex);
        return responder(request, HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Ocurrió un error interno no esperado.");
    }

    /**
     * Construye la respuesta según el canal de la petición.
     *
     * <p>API REST ({@code /api/**}): cuerpo JSON uniforme {@code {code, message}} con el status
     * correspondiente. Página: la vista {@code error} con el mismo código y mensaje, para que la
     * página muestre algo legible en vez de JSON crudo.
     */
    private Object responder(HttpServletRequest request, HttpStatus status, String code, String message) {
        if (esPeticionApi(request)) {
            return ResponseEntity.status(status).body(new ApiError(code, message));
        }
        ModelAndView vista = new ModelAndView("error");
        vista.setStatus(status);
        vista.addObject("status", status.value());
        vista.addObject("codigo", code);
        vista.addObject("mensaje", message);
        vista.addObject("ruta", rutaDe(request));
        return vista;
    }

    /**
     * Indica si la petición pertenece al canal REST.
     *
     * <p>Se compara contra la ruta <em>dentro</em> de la aplicación (sin el context path), para
     * que el criterio no se rompa si el artefacto se despliega bajo un prefijo de contexto.
     */
    private boolean esPeticionApi(HttpServletRequest request) {
        return rutaDe(request).startsWith(PREFIJO_API);
    }

    /** Ruta solicitada dentro de la aplicación (sin context path). */
    private static String rutaDe(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        return uri == null ? "-" : uri;
    }

    /**
     * Ruta solicitada, sólo para el log del servidor (no viaja en el cuerpo de la respuesta).
     */
    private static String rutaDe(Exception ex) {
        if (ex instanceof NoResourceFoundException noEncontrado) {
            return noEncontrado.getResourcePath();
        }
        if (ex instanceof NoHandlerFoundException noHandler) {
            return noHandler.getRequestURL();
        }
        return "-";
    }
}
