package ar.gob.chaco.atp.puestoscamineros.nucleo.web.mvc;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ServicioAutorizacion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.SmoothNavMenuData;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * Provee al view model los datos de navegación (barra horizontal) que consume el fragmento
 * {@code layout/base :: navegacion}: el contexto de sesión y el menú de programas autorizados.
 *
 * <p>Es un <strong>adaptador de presentación</strong>: no decide nada de negocio. Toma el
 * contexto ya persistido en la sesión (por {@link GestorContextoSesion}) y le pide al
 * {@link ServicioAutorizacion} el menú del tipo de acceso; las reglas viven en el servicio.
 *
 * <p>Se usa en toda pantalla que renderice la barra de navegación (no sólo {@code /home}): si el
 * modelo no trae {@code contexto} y {@code programas}, el fragmento no se dibuja.
 */
@Component
public class ProveedorNavegacion {

    private static final Logger log = LoggerFactory.getLogger(ProveedorNavegacion.class);

    /** Tipo de acceso por defecto cuando el contexto no lo define (paridad con el legacy). */
    private static final String TIPO_POR_DEFECTO = "PUE";

    private final GestorContextoSesion gestorContextoSesion;
    private final ServicioAutorizacion servicioAutorizacion;

    public ProveedorNavegacion(GestorContextoSesion gestorContextoSesion,
            ServicioAutorizacion servicioAutorizacion) {
        this.gestorContextoSesion = gestorContextoSesion;
        this.servicioAutorizacion = servicioAutorizacion;
    }

    /**
     * Carga en el modelo el contexto de sesión y el menú de programas.
     *
     * @param model modelo de la vista
     * @param request petición actual (de donde se recupera la sesión)
     */
    public void poblar(Model model, HttpServletRequest request) {
        ContextoSesion contexto = gestorContextoSesion.recuperar(request);
        model.addAttribute("contexto", contexto);
        model.addAttribute("programas", programas(contexto));
        // Ruta actual: la usa el fragmento del menú para marcar la tab activa (spec 05-ui-moderno).
        model.addAttribute("rutaActual", request.getRequestURI());
    }

    /**
     * Menú de programas autorizados para el tipo de acceso del contexto. Nunca devuelve
     * {@code null}: si el servicio falla, la lista vacía (el fragmento muestra el estado vacío).
     */
    private List<SmoothNavMenuData> programas(ContextoSesion contexto) {
        String tipo = (contexto != null && contexto.tipo() != null && !contexto.tipo().isBlank())
                ? contexto.tipo()
                : TIPO_POR_DEFECTO;
        try {
            List<SmoothNavMenuData> programas = servicioAutorizacion.programasPorTipo(tipo);
            return programas == null ? List.of() : programas;
        } catch (RuntimeException ex) {
            log.warn("No se pudo cargar la lista de programas del usuario", ex);
            return List.of();
        }
    }
}
