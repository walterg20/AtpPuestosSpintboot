package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Módulo {@code 06-turnos} — los mensajes verbatim del legacy (spec §3.2, §3.3, R-TUR-005,
 * R-TUR-010, R-TUR-012; T-TUR-B6, T-TUR-E1).
 *
 * <p>Se comparan <strong>carácter a carácter</strong> con los {@code msg()} del legacy
 * ({@code TurnosListado}) y con el {@code JSEvent} del botón de cierre. Las erratas del legacy
 * ("esta" sin tilde, la coma separada) forman parte del contrato observable y no se corrigen.
 */
class MensajesTurnoTest {

    @Test
    @DisplayName("R-TUR-010: el mensaje de comision cerrada es verbatim")
    void elMensajeDeComisionCerradaEsVerbatim() {
        assertThat(MensajesTurno.COMISION_CERRADA)
                .isEqualTo("La comisión actual esta cerrada , no se puede abrir nuevos turnos");
    }

    @Test
    @DisplayName("R-TUR-012: los mensajes del cierre son verbatim")
    void losMensajesDelCierreSonVerbatim() {
        assertThat(MensajesTurno.TURNO_YA_CERRADO).isEqualTo("El turno se encuentra cerrado");
        assertThat(MensajesTurno.FORMULARIOS_ABIERTOS).isEqualTo("Existen Formularios Abiertos");
        assertThat(MensajesTurno.ERROR_CIERRE).isEqualTo("No se puede cerrar el turno");
        assertThat(MensajesTurno.TURNO_CERRADO).isEqualTo("El turno seleccionado ha sido cerrado");
    }

    @Test
    @DisplayName("R-TUR-005: la confirmacion del cierre es verbatim")
    void laConfirmacionDelCierreEsVerbatim() {
        assertThat(MensajesTurno.CONFIRMAR_CIERRE).isEqualTo("¿Confirma que desea cerrar el Turno?");
    }
}
