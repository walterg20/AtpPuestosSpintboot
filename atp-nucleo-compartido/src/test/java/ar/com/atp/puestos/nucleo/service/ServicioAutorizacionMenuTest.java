package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ServicioAutorizacionMenuTest {

    private final ServicioAutorizacion servicio = new ServicioAutorizacion(
            nombre -> true);

    @Test
    @DisplayName("Menú PUE: 5 ítems")
    void menuPueTieneCincoItems() {
        assertThat(servicio.programasPorTipo("PUE")).hasSize(5);
        assertThat(servicio.menuPorTipo("PUE")).hasSize(5);
    }

    @Test
    @DisplayName("Menú RECEP: 4 ítems")
    void menuRecepTieneCuatroItems() {
        assertThat(servicio.programasPorTipo("RECEP")).hasSize(4);
        assertThat(servicio.programasPorTipo("REC")).hasSize(4);
    }

    @Test
    @DisplayName("Menú COM: 1 ítem")
    void menuComTieneUnItem() {
        assertThat(servicio.programasPorTipo("COM")).hasSize(1);
    }

    @Test
    @DisplayName("Estructura recursiva SmoothNavMenuData")
    void estructuraRecursiva() {
        SmoothNavMenuData item = servicio.programasPorTipo("PUE").get(0);
        assertThat(item.items()).isNotNull();
    }

    @Test
    @DisplayName("Menú PUE: Formularios ACTAS apunta a /actas y Carga Manual a /actas/nueva")
    void menuPueActasLinks() {
        var items = servicio.programasPorTipo("PUE");
        SmoothNavMenuData formularios = items.stream()
                .filter(i -> "Formularios ACTAS".equals(i.title()))
                .findFirst().orElseThrow();
        SmoothNavMenuData cargaManual = items.stream()
                .filter(i -> "Carga Manual".equals(i.title()))
                .findFirst().orElseThrow();
        assertThat(formularios.link()).isEqualTo("/actas");
        assertThat(cargaManual.link()).isEqualTo("/actas/nueva");
    }
}
