package ar.com.atp.puestos.nucleo.web.mvc.turno;

import ar.com.atp.puestos.nucleo.service.ContextoSesion;

/**
 * Copia del contexto de sesión con otro número de turno.
 *
 * <p>El alta y el cierre de un turno cambian {@code Context.Turno} (R-TUR-009 y R-TUR-012) y el
 * contexto tiene que persistirse en la sesión. El contexto es un {@code record} inmutable que vive
 * en la capa de servicio: la copia con el turno nuevo se arma <strong>aquí</strong>, en el borde de
 * la presentación, para no agregar reglas al servicio de dominio.
 */
public final class TurnoContexto {

    private TurnoContexto() {
        // clase de utilería
    }

    /**
     * Devuelve el contexto con el número de turno indicado y el resto de los campos intactos.
     *
     * @param contexto contexto actual (puede ser {@code null})
     * @param turno nuevo número de turno ({@code 0} = sin turno abierto)
     * @return el contexto con el turno nuevo, o el contexto vacío si el original era {@code null}
     */
    public static ContextoSesion conTurno(ContextoSesion contexto, Integer turno) {
        if (contexto == null) {
            return ContextoSesion.vacio();
        }
        return new ContextoSesion(
                contexto.usuario(),
                contexto.usuarioNombre(),
                contexto.personaId(),
                contexto.comision(),
                turno,
                contexto.sucursal(),
                contexto.sucursalNombre(),
                contexto.tipo(),
                contexto.responsable(),
                contexto.responsableNombre(),
                contexto.form2276(),
                contexto.f276Id(),
                contexto.ultimoMenu());
    }
}
