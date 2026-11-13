package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Localidad;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Provincia;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.LocalidadDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Conversion entidad → DTO de {@link Localidad}.
 *
 * <p>Contrato clave (R-024): una localidad cuya FK logica {@code ProvCod} no existe en
 * {@code PROVINCI} debe exponer {@code provNombre} en {@code null} en lugar de propagar una
 * excepcion.
 *
 * <p>Contrato clave (R-207): el campo {@code provCod} expone el <em>valor fisico</em> de la
 * columna {@code Localida.ProvCod}, no el de la asociacion resuelta. Para las 30 filas huerfanas
 * reales ({@code ProvCod = 0}) el codigo vale {@code 0} y {@code provNombre} vale {@code null}:
 * la provincia no existe y no se inventa, pero el codigo si se expone, para que el cliente
 * pueda representar el estado huerfano. La fila huerfana puede llegar de dos formas segun el
 * camino de carga: con la asociacion en {@code null}, o con la asociacion presente pero sin
 * nombre resoluble (proxy pre-inicializado por el {@code left join fetch}, R-025).
 */
@DisplayName("NucleoMapper - Localidad")
class NucleoMapperTest {

    @Test
    @DisplayName("R-207: FK logica huerfana → provCod = 0 (codigo fisico) y provNombre = null")
    void toDtoConFkHuerfanaExponeElCodigoFisico() {
        // Estado real de la fila huerfana de Localida: la columna ProvCod vale 0 (inexistente
        // en PROVINCI) y la asociacion no se puede resolver.
        Localidad localidad = new Localidad(16342, "ACHIRAS");
        localidad.setLocCodPost(5833);
        localidad.setProvCod(0);

        LocalidadDto dto = NucleoMapper.toDto(localidad);

        assertThat(dto.locCod()).isEqualTo(16342);
        assertThat(dto.locNom()).isEqualTo("ACHIRAS");
        assertThat(dto.locCodPost()).isEqualTo(5833);
        assertThat(dto.provCod()).isNotNull();
        assertThat(dto.provCod()).isEqualTo(0);
        assertThat(dto.provNombre()).isNull();
    }

    @Test
    @DisplayName("R-024: sin valor fisico ni asociacion → provCod y provNombre en null")
    void toDtoSinValorFisicoNiAsociacion() {
        Localidad localidad = new Localidad(4242, "SIN PROVINCIA");
        localidad.setLocCodPost(3730);

        LocalidadDto dto = NucleoMapper.toDto(localidad);

        assertThat(dto.locCod()).isEqualTo(4242);
        assertThat(dto.locNom()).isEqualTo("SIN PROVINCIA");
        assertThat(dto.locCodPost()).isEqualTo(3730);
        assertThat(dto.provCod()).isNull();
        assertThat(dto.provNombre()).isNull();
    }

    @Test
    @DisplayName("R-003: localidad con provincia resuelta → expone provCod y provNombre")
    void toDtoConProvinciaResuelta() {
        Localidad localidad = new Localidad(10, "CHARATA");
        localidad.setProvincia(new Provincia(1, "CHACO"));
        // Valor fisico de la columna (lo que devuelve la BD).
        localidad.setProvCod(1);

        LocalidadDto dto = NucleoMapper.toDto(localidad);

        assertThat(dto.provCod()).isEqualTo(1);
        assertThat(dto.provNombre()).isEqualTo("CHACO");
    }

    @Test
    @DisplayName("R-207/R-003: entidad no refrescada (sin valor fisico) → el codigo sale de la asociacion")
    void toDtoSinValorFisicoUsaLaAsociacionResuelta() {
        // Alta/modificacion: Hibernate no refresca la columna de solo lectura y la asociacion
        // es la que escribe la FK.
        Localidad localidad = new Localidad(10, "CHARATA");
        localidad.setProvincia(new Provincia(12, "CORRIENTES"));

        LocalidadDto dto = NucleoMapper.toDto(localidad);

        assertThat(dto.provCod()).isEqualTo(12);
        assertThat(dto.provNombre()).isEqualTo("CORRIENTES");
    }

    @Test
    @DisplayName("R-207: FK huerfana con proxy sin nombre → provCod = 0 y provNombre = null")
    void toDtoConProvinciaHuerfanaSinNombre() {
        // Estado real de la fila huerfana de Localida (ProvCod = 0): la asociacion llega
        // presente pero sin nombre resoluble, porque PROVINCI no tiene ese ProvCod.
        Localidad localidad = new Localidad(3730, "LOCALIDAD SIN PROVINCIA");
        localidad.setProvincia(new Provincia(0, null));
        localidad.setProvCod(0);

        LocalidadDto dto = NucleoMapper.toDto(localidad);

        assertThat(dto.locCod()).isEqualTo(3730);
        assertThat(dto.provCod()).isEqualTo(0);
        assertThat(dto.provNombre()).isNull();
    }
}
