package ar.com.atp.puestos.nucleo.service;

import org.springframework.stereotype.Component;

/**
 * Implementación vigente de la autorización por programa: <strong>devuelve siempre
 * {@code true}</strong> (D-LOG-13).
 *
 * <p>En la KB actual el procedimiento {@code IsAuthorized} tiene su lógica <strong>comentada</strong>
 * ({@code &Authorized = True} seguido del bloque que denegaría {@code ViewInvoices} a los usuarios
 * de nivel 0) ⇒ la función devuelve siempre verdadero y, en consecuencia, la lista de programas
 * es siempre la completa (SA-LOG-02).
 *
 * <p>La estructura queda filtrable: cuando exista una fuente de autorización real, se cambia este
 * bean y ni {@link ServicioAutorizacion} ni los adaptadores se tocan.
 */
@Component
public class VerificadorAutorizacionLegacy implements VerificadorAutorizacion {

    @Override
    public boolean isAuthorized(String nombrePrograma) {
        return true;
    }
}
