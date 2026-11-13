package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import java.io.Serializable;
import java.security.Principal;
import java.util.Locale;

/**
 * Principal de la autenticación: el usuario autenticado y su contexto de sesión (plan §3.7).
 *
 * <p>Se registra en la sesión HTTP a través del {@code SecurityContextRepository} (R-LOG-017). El
 * nombre del principal es el identificador del usuario <strong>en mayúsculas</strong>.
 *
 * @param usuario identificador del usuario autenticado
 * @param contexto contexto de sesión (los 10 campos cargados por el login)
 */
public record UsuarioAutenticado(String usuario, ContextoSesion contexto)
        implements Principal, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return usuario == null ? "" : usuario.toUpperCase(Locale.ROOT);
    }
}
