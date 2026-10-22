package ar.com.atp.puestos.nucleo.web.mvc.producto;

/**
 * Filtro de anulados del panel de colección.
 *
 * <p>Se apoya en el parámetro {@code anulados} que ya expone el endpoint de lectura
 * ({@code null} = todos, {@code true} = sólo anulados, {@code false} = no anulados); no agrega ni
 * modifica endpoints (D-34).
 */
public enum FiltroAnulados {

    /** Sin filtro: se listan todos los productos. */
    TODOS(null),

    /** Sólo los productos no anulados. */
    NO(Boolean.FALSE),

    /** Sólo los productos anulados. */
    SI(Boolean.TRUE);

    private final Boolean parametro;

    FiltroAnulados(Boolean parametro) {
        this.parametro = parametro;
    }

    /** Valor del parámetro {@code anulados} que se envía al servicio. */
    public Boolean parametro() {
        return parametro;
    }

    /** Traduce el parámetro {@code anulados} de la URL al filtro de la vista. */
    public static FiltroAnulados deParametro(Boolean anulados) {
        if (anulados == null) {
            return TODOS;
        }
        return Boolean.TRUE.equals(anulados) ? SI : NO;
    }
}
