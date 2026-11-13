package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Turno;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.TurnoId;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.TurnoDto;

/**
 * Conversión entre {@link Turno} y {@link TurnoDto} (spec {@code 06-turnos}, T-TUR-A4, D-TUR-004).
 *
 * <p><strong>Normalización de los {@code char} de longitud fija</strong>: {@code SucTipo},
 * {@code TurEstado} y {@code TurUsuario} son {@code char} con relleno en SQL Server. El recorte
 * ocurre <strong>aquí</strong>, en el borde del dominio, y no en las vistas: la grilla del legacy
 * muestra {@code PUE} / {@code REC} y {@code A} / {@code C} sin espacios.
 *
 * <p>El recorte no se aplica en la entidad (ni en la clave primaria): hacerlo ahí alteraría la
 * clave y la comparación con la base.
 */
public final class TurnoMapper {

    private TurnoMapper() {
        // clase de utilería
    }

    /**
     * Entidad → DTO de lectura.
     *
     * @param turno entidad leída de {@code PPRIM_TU}
     * @return el DTO con los {@code char} normalizados
     */
    public static TurnoDto toDto(Turno turno) {
        if (turno == null) {
            return null;
        }
        return new TurnoDto(
                turno.getSucNroId(),
                trim(turno.getSucTipo()),
                turno.getTurNroId(),
                trim(turno.getTurUsuario()),
                turno.getTurCpcId(),
                turno.getTurFchApe(),
                turno.getTurFchCie(),
                turno.getTurHraApe(),
                turno.getTurHraCie(),
                trim(turno.getTurEstado()),
                turno.getSnSinNum());
    }

    /**
     * DTO → entidad.
     *
     * @param dto datos del turno
     * @return la entidad con los {@code char} normalizados
     */
    public static Turno toEntidad(TurnoDto dto) {
        if (dto == null) {
            return null;
        }
        Turno turno = new Turno(new TurnoId(dto.sucursal(), trim(dto.tipo()), dto.turno()));
        turno.setTurUsuario(trim(dto.usuario()));
        turno.setTurCpcId(dto.comision());
        turno.setTurFchApe(dto.fechaApertura());
        turno.setTurFchCie(dto.fechaCierre());
        turno.setTurHraApe(dto.horaApertura());
        turno.setTurHraCie(dto.horaCierre());
        turno.setTurEstado(trim(dto.estado()));
        turno.setTurFchPago(null);
        turno.setSnSinNum(dto.snNum());
        return turno;
    }

    /**
     * Recorta un {@code char} de longitud fija.
     *
     * @param valor valor leído de la base (puede venir con relleno o ser {@code null})
     * @return el valor sin espacios;a {@code null} si el valor era {@code null}
     */
    public static String trim(String valor) {
        return valor == null ? null : valor.trim();
    }
}
