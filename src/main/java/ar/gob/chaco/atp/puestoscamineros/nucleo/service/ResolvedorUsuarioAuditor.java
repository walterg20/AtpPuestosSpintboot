package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resuelve el usuario auditor de una escritura (R-LOG-032).
 *
 * <p>Con sesión autenticada, el usuario auditor es el {@code Context.Usuario} de la sesión; sin
 * sesión, es el valor por defecto de {@code app.auditoria.usuario} ({@code SISTEMA}).
 *
 * <p>Se apoya en el {@code SecurityContextHolder} (y no en la {@code HttpSession}): el servicio
 * queda libre de dependencias del contenedor web (D-LOG-15).
 */
@Component
public class ResolvedorUsuarioAuditor {

    private final String usuarioPorDefecto;

    public ResolvedorUsuarioAuditor(@Value("${app.auditoria.usuario:SISTEMA}") String usuarioPorDefecto) {
        this.usuarioPorDefecto = usuarioPorDefecto;
    }

    /**
     * Usuario auditor del contexto actual.
     *
     * @return el usuario autenticado si hay sesión; si no, el valor por defecto
     */
    public String usuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null
                && autenticacion.getPrincipal() instanceof UsuarioAutenticado principal) {
            String nombre = principal.getName();
            if (nombre != null && !nombre.isBlank()) {
                return nombre;
            }
        }
        return usuarioPorDefecto;
    }

    /** Valor por defecto configurado (usado cuando no hay sesión autenticada). */
    public String usuarioPorDefecto() {
        return usuarioPorDefecto;
    }
}
