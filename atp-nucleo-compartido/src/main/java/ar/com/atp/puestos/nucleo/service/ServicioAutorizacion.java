package ar.com.atp.puestos.nucleo.service;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ServicioAutorizacion {

    /** Los 2 programas del legacy, con su nombre y su descripción verbatim. */
    private static final List<ProgramaAutorizado> PROGRAMAS = List.of(
            new ProgramaAutorizado("WWPPrim_Turnos", "Turnos de Comisiones", null),
            new ProgramaAutorizado("WWTDetProdPri", "Buscar Guias", null));

    /** Ruta de la pantalla de turnos (módulo `06-turnos`). */
    private static final String RUTA_TURNOS = "/turnos";

    /** Ruta de la pantalla de inicio. */
    private static final String RUTA_INICIO = "/home";

    /** Ruta de la pantalla de Control de Cargas (módulo cargas). */
    private static final String RUTA_CARGAS = "/cargas";

    // ---- Menús recursivos por Tipo (spec 04-ui-legacy, D-UI-002) ----
    private static final List<SmoothNavMenuData> MENU_PUE = List.of(
            new SmoothNavMenuData("2", "Control de Cargas", "Control de Cargas", RUTA_CARGAS, null, List.of()),
            new SmoothNavMenuData("5", "Formularios ACTAS", "Listado de Actas de Infración", null, null, List.of()),            new SmoothNavMenuData("5", "Carga Manual", "Carga Manual de Actas de Infración", null, null, List.of()),
            new SmoothNavMenuData("6", "Turnos", "Turnos de Comisión", RUTA_TURNOS, null, List.of()),
            new SmoothNavMenuData("7", "Comisiones", "Comisiones en Puestos Camineros", null, null, List.of()));

    private static final List<SmoothNavMenuData> MENU_RECEP = List.of(
            new SmoothNavMenuData("1", "Inicio", "Inicio", RUTA_INICIO, null, List.of()),
            new SmoothNavMenuData("2", "Formularios 2276", "Listado de Formularios 2276", null, null, List.of()),
            new SmoothNavMenuData("3", "Carga Manual", "Carga Manual de Formularios 2276", null, null, List.of()),
            new SmoothNavMenuData("4", "Turnos", "Turnos de Comisión", RUTA_TURNOS, null, List.of()));

    private static final List<SmoothNavMenuData> MENU_COM = List.of(
            new SmoothNavMenuData("1", "Comisiones", "Comisiones en Puestos Camineros", null, null, List.of()));

    private final VerificadorAutorizacion verificador;

    public ServicioAutorizacion(VerificadorAutorizacion verificador) {
        this.verificador = verificador;
    }

    public List<ProgramaAutorizado> programasAutorizados() {
        return PROGRAMAS.stream()
                .filter(programa -> verificador.isAuthorized(programa.nombre()))
                .toList();
    }

    // ---- Menú por Tipo ----

    public List<SmoothNavMenuData> programasPorTipo(String tipo) {
        return menuPorTipo(tipo);
    }

    public List<SmoothNavMenuData> menuPorTipo(String tipo) {
        String normalizado = normalizarTipo(tipo);
        return switch (normalizado) {
            case "RECEP" -> MENU_RECEP;
            case "COM" -> MENU_COM;
            default -> MENU_PUE;
        };
    }

    public List<SmoothNavMenuData> menu(String tipo) {
        return menuPorTipo(tipo);
    }

    public List<SmoothNavMenuData> obtenerMenu(String tipo) {
        return menuPorTipo(tipo);
    }

    public List<SmoothNavMenuData> itemsPorTipo(String tipo) {
        return menuPorTipo(tipo);
    }

    public List<SmoothNavMenuData> smoothNavMenu(String tipo) {
        return menuPorTipo(tipo);
    }

    public List<SmoothNavMenuData> programasAutorizados(String tipo) {
        return menuPorTipo(tipo);
    }

    private static String normalizarTipo(String tipo) {
        if (tipo == null) {
            return "PUE";
        }
        String t = tipo.trim().toUpperCase();
        if (t.equals("REC") || t.equals("RECEP") || t.equals("RECEPTORIA") || t.equals("RECEPCION")) {
            return "RECEP";
        }
        if (t.equals("COM") || t.equals("COMISION") || t.equals("COMISIONES")) {
            return "COM";
        }
        if (t.equals("PUE") || t.equals("PUESTO")) {
            return "PUE";
        }
        return "PUE";
    }
}
