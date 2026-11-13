package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import java.io.Serializable;

/**
 * Contexto de sesión: réplica del SDT {@code Context} del legacy (spec 03-login §3.5, D-LOG-08).
 *
 * <p>Tiene <strong>13 campos</strong>, igual que el SDT, y el login carga <strong>10</strong>; los
 * tres restantes ({@code form2276}, {@code f276Id}, {@code ultimoMenu}) quedan en su valor por
 * defecto porque los consumen los módulos de formularios 2276 y de navegación de menús
 * (R-LOG-016).
 *
 * <p>El contexto se persiste en la sesión HTTP bajo la clave {@code "context"} (paridad con
 * {@code SetContext} / {@code LoadContext}) — R-LOG-017.
 *
 * @param usuario identificador del usuario autenticado, en mayúsculas
 * @param usuarioNombre nombre y apellido ({@code SCRTY.UsuNomYApe})
 * @param personaId persona asociada ({@code PERSONAS.PerId})
 * @param comision número de comisión ({@code COMISION.CPCId}); {@code 0} en la rama de receptoría
 * @param turno número de turno ({@code PPRIM_TU.TurNroId}); {@code 0} si no hay
 * @param sucursal código de sucursal ({@code PUEPPRIM.PueCod} o {@code RECEPTOR.ReceId})
 * @param sucursalNombre descripción de la sucursal ({@code PueDesc} / {@code ReceDsc})
 * @param tipo tipo de acceso ({@code PUE} / {@code REC})
 * @param responsable persona responsable de la comisión ({@code COMISION.CPCResId}); {@code 0} si no
 *        hay responsable
 * @param responsableNombre nombre y apellido del responsable ({@code PERSONAS.PerNomyApe})
 * @param form2276 <strong>no lo carga el login</strong> (módulo de formularios 2276)
 * @param f276Id <strong>no lo carga el login</strong> (módulo de formularios 2276)
 * @param ultimoMenu <strong>no lo carga el login</strong> (navegación de menús)
 */
public record ContextoSesion(
        String usuario,
        String usuarioNombre,
        Integer personaId,
        Integer comision,
        Integer turno,
        Integer sucursal,
        String sucursalNombre,
        String tipo,
        Integer responsable,
        String responsableNombre,
        Object form2276,
        Integer f276Id,
        String ultimoMenu) implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Contexto vacío: los 13 campos en su valor por defecto. */
    public static ContextoSesion vacio() {
        return new ContextoSesion("", "", 0, 0, 0, 0, "", "", 0, "", null, null, null);
    }

    /**
     * Contexto <strong>parcial</strong> de la rama "fue responsable de una comisión" (4 campos:
     * {@code Usuario}, {@code UsuarioNombre}, {@code PersonaId}, {@code Tipo}) — R-LOG-015.
     */
    public static ContextoSesion parcial(String usuario, String usuarioNombre, Integer personaId,
            String tipo) {
        return new ContextoSesion(usuario, usuarioNombre, personaId, 0, 0, 0, "", tipo, 0, "",
                null, null, null);
    }

    /**
     * Contexto <strong>completo</strong>: los 10 campos que carga el login (R-LOG-016).
     */
    public static ContextoSesion completo(String usuario, String usuarioNombre, Integer personaId,
            Integer comision, Integer turno, Integer sucursal, String sucursalNombre, String tipo,
            Integer responsable, String responsableNombre) {
        return new ContextoSesion(usuario, usuarioNombre, personaId, comision, turno, sucursal,
                sucursalNombre, tipo, responsable, responsableNombre, null, null, null);
    }
}
