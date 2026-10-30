package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.atp.puestos.nucleo.domain.Turno;
import ar.com.atp.puestos.nucleo.domain.TurnoId;
import ar.com.atp.puestos.nucleo.web.dto.TurnoDto;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Módulo {@code 06-turnos} — pruebas del mapper (spec T-TUR-A4, D-TUR-004).
 *
 * <p>Los {@code char} de longitud fija llegan con relleno de SQL Server; el mapper los recorta en
 * el borde del dominio para que la grilla muestre {@code PUE} / {@code REC} y {@code A} / {@code C}.
 */
class TurnoMapperTest {

    @Test
    @DisplayName("D-TUR-004: el DTO recorta los char de longitud fija")
    void elDtoRecortaLosCharDeLongitudFija() {
        Turno turno = new Turno(new TurnoId(72, "REC", 5));
        turno.setTurEstado("C");
        turno.setTurUsuario("WALTERG   ");

        TurnoDto dto = TurnoMapper.toDto(turno);

        assertThat(dto.tipo()).isEqualTo("REC");
        assertThat(dto.estado()).isEqualTo("C");
        assertThat(dto.usuario()).isEqualTo("WALTERG");
        assertThat(dto.turno()).isEqualTo(5);
        assertThat(dto.sucursal()).isEqualTo(72);
    }

    @Test
    @DisplayName("T-TUR-A4: el ida y vuelta entidad -> DTO -> entidad conserva los valores")
    void elIdaYVueltaConservaLosValores() {
        Turno origen = new Turno(new TurnoId(72, "PUE", 9));
        origen.setTurEstado("A");
        origen.setTurUsuario("WALTERG");
        origen.setTurCpcId(345);
        origen.setTurFchApe(LocalDateTime.of(2026, 3, 12, 0, 0));
        origen.setTurFchCie(LocalDateTime.of(1753, 1, 1, 0, 0));
        origen.setTurHraApe(LocalDateTime.of(2026, 3, 12, 10, 15));
        origen.setTurHraCie(LocalDateTime.of(1753, 1, 1, 0, 0));

        Turno vuelta = TurnoMapper.toEntidad(TurnoMapper.toDto(origen));

        assertThat(vuelta.getSucNroId()).isEqualTo(72);
        assertThat(vuelta.getSucTipo()).isEqualTo("PUE");
        assertThat(vuelta.getTurNroId()).isEqualTo(9);
        assertThat(vuelta.getTurEstado()).isEqualTo("A");
        assertThat(vuelta.getTurUsuario()).isEqualTo("WALTERG");
        assertThat(vuelta.getTurCpcId()).isEqualTo(345);
        assertThat(vuelta.getTurFchApe()).isEqualTo(LocalDateTime.of(2026, 3, 12, 0, 0));
        assertThat(vuelta.getTurHraApe()).isEqualTo(LocalDateTime.of(2026, 3, 12, 10, 15));
        assertThat(vuelta.getTurFchPago()).isNull();
    }
}
