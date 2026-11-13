package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Localidad;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Provincia;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.AreaRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.CalleRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.CaminoRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.FeriadoRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.JurisdiccionRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.LocalidadRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.ProvinciaRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.LocalidadDto;
import ar.gob.chaco.atp.puestoscamineros.shared.error.NotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

/**
 * Reglas de lectura de {@link Localidad} (R-020, R-021, R-024, R-025).
 *
 * <p>Contrato clave: el listado debe usar siempre las consultas que resuelven la provincia en la
 * misma consulta ({@code findAllConProvincia} / {@code findByProvCodConProvincia} /
 * {@code findByIdConProvincia}); nunca un {@code findAll(Sort)} plano, que deja la provincia como
 * proxy LAZY y termina en {@code 500} cuando la FK logica es huerfana.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NucleoConsultaService - Localidad")
class NucleoConsultaServiceTest {

    @Mock
    private ProvinciaRepository provinciaRepository;
    @Mock
    private LocalidadRepository localidadRepository;
    @Mock
    private CalleRepository calleRepository;
    @Mock
    private CaminoRepository caminoRepository;
    @Mock
    private AreaRepository areaRepository;
    @Mock
    private JurisdiccionRepository jurisdiccionRepository;
    @Mock
    private FeriadoRepository feriadoRepository;

    @InjectMocks
    private NucleoConsultaService service;

    @Test
    @DisplayName("R-025: el listado sin filtro usa la consulta que resuelve la provincia")
    void listarTodasUsaConsultaConProvincia() {
        when(localidadRepository.findAllConProvincia()).thenReturn(List.of(new Localidad(7, "SIN PROVINCIA")));

        List<LocalidadDto> dtos = service.listarLocalidades(null);

        assertThat(dtos).hasSize(1);
        verify(localidadRepository).findAllConProvincia();
        verify(localidadRepository, never()).findAll(any(Sort.class));
    }

    @Test
    @DisplayName("R-024: una localidad huerfana se expone con la provincia en null, sin excepcion")
    void listarTodasExponeLaProvinciaHuerfanaEnNull() {
        when(localidadRepository.findAllConProvincia()).thenReturn(List.of(new Localidad(7, "SIN PROVINCIA")));

        LocalidadDto dto = service.listarLocalidades(null).get(0);

        assertThat(dto.locCod()).isEqualTo(7);
        assertThat(dto.provCod()).isNull();
        assertThat(dto.provNombre()).isNull();
    }

    @Test
    @DisplayName("R-021: el listado con filtro usa la consulta con provincia resuelta")
    void listarPorProvinciaUsaConsultaConProvincia() {
        Localidad localidad = new Localidad(10, "CHARATA");
        localidad.setProvincia(new Provincia(1, "CHACO"));
        when(localidadRepository.findByProvCodConProvincia(1)).thenReturn(List.of(localidad));

        List<LocalidadDto> dtos = service.listarLocalidades(1);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).provCod()).isEqualTo(1);
        assertThat(dtos.get(0).provNombre()).isEqualTo("CHACO");
        verify(localidadRepository).findByProvCodConProvincia(1);
        verify(localidadRepository, never()).findAll(any(Sort.class));
    }

    @Test
    @DisplayName("R-024: obtener una localidad huerfana no lanza excepcion")
    void obtenerLocalidadHuerfana() {
        when(localidadRepository.findByIdConProvincia(7)).thenReturn(Optional.of(new Localidad(7, "SIN PROVINCIA")));

        LocalidadDto dto = service.obtenerLocalidad(7);

        assertThat(dto.locCod()).isEqualTo(7);
        assertThat(dto.provNombre()).isNull();
        verify(localidadRepository).findByIdConProvincia(7);
    }

    @Test
    @DisplayName("R-020: obtener una localidad inexistente responde NO_ENCONTRADO")
    void obtenerLocalidadInexistente() {
        when(localidadRepository.findByIdConProvincia(999)).thenReturn(Optional.empty());

        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                NotFoundException.class,
                () -> service.obtenerLocalidad(999)))
                .extracting(NotFoundException::getCode)
                .isEqualTo("NO_ENCONTRADO");
    }
}
