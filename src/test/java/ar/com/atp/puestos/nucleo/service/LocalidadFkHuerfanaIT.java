package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Localidad;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Provincia;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.LocalidadRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.ProvinciaRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.LocalidadDto;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * T-410 (R-207): el {@code LocalidadDto} expone el <strong>valor físico</strong> de la columna
 * {@code Localida.ProvCod} en {@code provCod}, incluso para las filas con FK lógica huérfana
 * ({@code ProvCod = 0}, valor inexistente en {@code PROVINCI}), y mantiene {@code provNombre} en
 * {@code null} porque la provincia no existe y no se inventa.
 *
 * <p>Es la prueba de integración de lectura contra la BD real {@code Rentas}: sólo se ejecuta con
 * {@code ATP_DB_IT=true}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "ATP_DB_IT", matches = "true")
@Tag("db")
@DisplayName("T-410 - R-207: código físico de la FK en LocalidadDto")
class LocalidadFkHuerfanaIT {

    @Autowired
    private LocalidadRepository localidadRepository;

    @Autowired
    private ProvinciaRepository provinciaRepository;

    @Test
    @DisplayName("R-207: las huérfanas responden provCod = 0 y provNombre = null")
    void lasHuerfanasRespondenCodigoFisicoCeroYNombreNull() {
        List<Localidad> entidades = localidadRepository.findAllConProvincia();
        assertThat(entidades).isNotEmpty();

        List<LocalidadDto> dtos = entidades.stream().map(NucleoMapper::toDto).toList();
        assertThat(dtos).hasSameSizeAs(entidades);

        List<LocalidadDto> huerfanas = dtos.stream().filter(d -> d.provNombre() == null).toList();
        List<LocalidadDto> conProvincia = dtos.stream().filter(d -> d.provNombre() != null).toList();

        // La BD real tiene 30 filas con ProvCod = 0 (FK lógica huérfana).
        assertThat(huerfanas)
                .as("filas con FK lógica huérfana en la BD real")
                .isNotEmpty();
        assertThat(conProvincia).isNotEmpty();

        // (a) Huérfanas: el código físico 0 se expone (no null) y el nombre queda en null.
        assertThat(huerfanas).allSatisfy(d -> {
            assertThat(d.provCod())
                    .as("el código físico de la FK huérfana debe exponerse")
                    .isNotNull()
                    .isEqualTo(0);
            assertThat(d.provNombre()).isNull();
        });

        // (b) Filas con provincia válida: código y nombre de la provincia.
        List<Localidad> conProvinciaEntidad = entidades.stream()
                .filter(l -> l.getProvincia() != null && l.getProvincia().getProvNombre() != null)
                .toList();
        assertThat(conProvincia).hasSameSizeAs(conProvinciaEntidad);

        for (int i = 0; i < conProvincia.size(); i++) {
            LocalidadDto dto = conProvincia.get(i);
            Provincia provincia = conProvinciaEntidad.get(i).getProvincia();
            assertThat(dto.provCod()).isEqualTo(provincia.getProvCod());
            assertThat(dto.provNombre()).isEqualTo(provincia.getProvNombre());
        }
    }

    @Test
    @DisplayName("R-207: toda fila con ProvCod = 0 queda con provCod = 0 (coherencia con la BD)")
    void todaFilaConProvCodCeroExponeCero() {
        List<Localidad> entidades = localidadRepository.findAllConProvincia();

        List<Localidad> conCodigoFisicoCero = entidades.stream()
                .filter(l -> Integer.valueOf(0).equals(l.getProvCod()))
                .toList();

        assertThat(conCodigoFisicoCero)
                .as("filas con el valor físico 0 en Localida.ProvCod")
                .isNotEmpty();
        assertThat(conCodigoFisicoCero).allSatisfy(l -> {
            // La provincia 0 no existe en PROVINCI: no hay nombre que inventar.
            assertThat(l.getProvincia()).isNull();
            LocalidadDto dto = NucleoMapper.toDto(l);
            assertThat(dto.provCod()).isEqualTo(0);
            assertThat(dto.provNombre()).isNull();
        });
    }
}
