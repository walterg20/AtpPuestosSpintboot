package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ar.com.atp.puestos.nucleo.domain.Camino;
import ar.com.atp.puestos.nucleo.domain.Localidad;
import ar.com.atp.puestos.nucleo.repository.CaminoRepository;
import ar.com.atp.puestos.nucleo.repository.LocalidadRepository;
import ar.com.atp.puestos.nucleo.web.dto.CaminoRutaDto;
import ar.com.atp.puestos.nucleo.web.dto.LocalidadNombreDto;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * T-402 (R-010, R-011, R-022): procedimientos de búsqueda legacy {@code Bus_DesLoc} y {@code
 * BuscarCamino}. Contrato clave: <strong>nunca</strong> responden 404 — devuelven cadena vacía
 * cuando el código no existe o el valor es {@code null}.
 */
@ExtendWith(MockitoExtension.class)
class BusquedaServiceTest {

    @Mock
    private LocalidadRepository localidadRepository;

    @Mock
    private CaminoRepository caminoRepository;

    @InjectMocks
    private BusquedaService service;

    // ------------------------------------------------------------- Bus_DesLoc

    @Test
    @DisplayName("R-010: localidad existente → devuelve LocNom")
    void nombreLocalidad_existente() {
        when(localidadRepository.findById(10)).thenReturn(Optional.of(new Localidad(10, "CHARATA")));

        assertThat(service.nombreLocalidad(10).locNom()).isEqualTo("CHARATA");
    }

    @Test
    @DisplayName("R-010: localidad inexistente → cadena vacía (sin 404)")
    void nombreLocalidad_inexistente() {
        when(localidadRepository.findById(999)).thenReturn(Optional.empty());

        assertThat(service.nombreLocalidad(999).locNom()).isEmpty();
    }

    @Test
    @DisplayName("R-010: LocNom nulo → cadena vacía")
    void nombreLocalidad_nulo() {
        when(localidadRepository.findById(11)).thenReturn(Optional.of(new Localidad(11, null)));

        assertThat(service.nombreLocalidad(11).locNom()).isEmpty();
    }

    // ---------------------------------------------------------- BuscarCamino

    @Test
    @DisplayName("R-011: camino existente → devuelve CanRuta")
    void rutaCamino_existente() {
        when(caminoRepository.findById("001")).thenReturn(Optional.of(new Camino("001", "RUTA NORTE")));

        assertThat(service.rutaCamino("001").canRuta()).isEqualTo("RUTA NORTE");
    }

    @Test
    @DisplayName("R-011: camino inexistente → cadena vacía (sin 404)")
    void rutaCamino_inexistente() {
        when(caminoRepository.findById("ZZZ")).thenReturn(Optional.empty());

        assertThat(service.rutaCamino("ZZZ").canRuta()).isEmpty();
    }

    @Test
    @DisplayName("R-011: ruta nula → cadena vacía")
    void rutaCamino_nulo() {
        when(caminoRepository.findById("002")).thenReturn(Optional.of(new Camino("002", null)));

        assertThat(service.rutaCamino("002").canRuta()).isEmpty();
    }

    @Test
    @DisplayName("R-011: se refuerza el trim del char(n) por defensa")
    void rutaCamino_recorta_espacios() {
        when(caminoRepository.findById("003")).thenReturn(Optional.of(new Camino("003", "  RUTA SUR  ")));

        assertThat(service.rutaCamino("003").canRuta()).isEqualTo("RUTA SUR");
    }

    @Test
    @DisplayName("R-010/R-011: los DTOs conservan el campo esperado")
    void devuelve_dtos_esperados() {
        when(localidadRepository.findById(1)).thenReturn(Optional.of(new Localidad(1, "A")));
        when(caminoRepository.findById("X")).thenReturn(Optional.of(new Camino("X", "Y")));

        LocalidadNombreDto nombre = service.nombreLocalidad(1);
        CaminoRutaDto ruta = service.rutaCamino("X");

        assertThat(nombre).isEqualTo(new LocalidadNombreDto("A"));
        assertThat(ruta).isEqualTo(new CaminoRutaDto("Y"));
    }
}
