package ar.com.atp.puestos.nucleo.web.mvc.producto;

/**
 * Modo del formulario de Productos.
 *
 * <p>{@code ALTA}: no hay producto seleccionado (formulario vacío). {@code EDICION}: hay producto
 * seleccionado; {@code prodCod} es de sólo lectura y el botón de baja está disponible.
 */
public enum ModoProducto {

    /** Alta: la clave primaria se informa en el formulario. */
    ALTA,

    /** Modificación: la clave primaria viene del producto seleccionado y no se edita. */
    EDICION
}
