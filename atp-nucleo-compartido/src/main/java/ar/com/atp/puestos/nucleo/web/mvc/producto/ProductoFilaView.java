package ar.com.atp.puestos.nucleo.web.mvc.producto;

/**
 * Fila del panel de colección: sólo los 3 datos que muestra la grilla izquierda
 * ({@code Código}, {@code Producto}, {@code Prod Anulado}).
 *
 * @param prodCod   clave del producto
 * @param prodDesc  descripción del producto
 * @param prodAnula indicador de anulación
 */
public record ProductoFilaView(Integer prodCod, String prodDesc, Integer prodAnula) {
}
