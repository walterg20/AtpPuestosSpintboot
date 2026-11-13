package ar.gob.chaco.atp.puestoscamineros.service;

/**
 * Mensajes <strong>verbatim</strong> del legacy para el módulo de Actas
 * (spec {@code 09-formularios-acta} §3.9, plan §3.4, D-ACTA-08, T-ACTA-132).
 *
 * <p>Los textos se conservan <strong>carácter a carácter</strong>, incluidas las erratas
 * ({@code maxima} sin tilde, {@code guia} sin tilde, {@code prod.} abreviado). La paridad de
 * identidad manda ({@code openspec/constitution/conventions.md} §6): <strong>no</strong> se
 * traducen, no se capitalizan y no se "corrigen".
 *
 * <p>Los dos únicos mensajes que <strong>no</strong> provienen del legacy están marcados como
 * tales ({@link #MSG_FORMULARIO_ABIERTO}): el legacy no expone un texto para el bloqueo por
 * lote/formulario abierto (R-ACTA-041 pide "informar el estado", no un texto literal).
 */
public final class MensajesActa {

    /** "Debe iniciar un turno..." ({@code ActaListadoWC.Start}). */
    public static final String MSG_TURNO_INACTIVO = "Debe iniciar un turno...";

    /** Tooltip "Imprimir" (Acta no manual). */
    public static final String MSG_IMPRIMIR = "Imprimir";

    /** Tooltip "No Imprime" (Acta manual). */
    public static final String MSG_NO_IMPRIME = "No Imprime";

    /** "Seleccionar el motivo de Acta" ({@code PPActaDetalle.Validar}). */
    public static final String MSG_MOTIVO_ACTA = "Seleccionar el motivo de Acta";

    /** "Debe seleccionar Tipo de Movimiento" ({@code PPActaDetalle.Validar}). */
    public static final String MSG_TIPO_MOVIMIENTO = "Debe seleccionar Tipo de Movimiento";

    /** "Debe ingresar CUIT del Vendedor" ({@code PPActaDetalle.Validar}). */
    public static final String MSG_CUIT_VENDEDOR = "Debe ingresar CUIT del Vendedor";

    /** "Debe ingresar CUIT del Destinatario" ({@code PPActaDetalle.Validar}). */
    public static final String MSG_CUIT_DESTINATARIO = "Debe ingresar CUIT del Destinatario";

    /** "Debe ingresar CUIT del Transportista" ({@code PPActaDetalle.Validar}). */
    public static final String MSG_CUIT_TRANSPORTISTA = "Debe ingresar CUIT del Transportista";

    /** "Debe ingresar Razón Social del Vendedor" ({@code PPActaDetalle.Validar}). */
    public static final String MSG_RAZON_SOCIAL_VENDEDOR = "Debe ingresar Razón Social del Vendedor";

    /** "Debe ingresar Razón Social del Destinatario" ({@code PPActaDetalle.Validar}). */
    public static final String MSG_RAZON_SOCIAL_DESTINATARIO =
            "Debe ingresar Razón Social del Destinatario";

    /** "Debe ingresar Razón Social del Transportista" ({@code PPActaDetalle.Validar}). */
    public static final String MSG_RAZON_SOCIAL_TRANSPORTISTA =
            "Debe ingresar Razón Social del Transportista";

    /** "Ingrese todos los datos del Transporte" ({@code PPActaDetalle.Validar}). */
    public static final String MSG_DATOS_TRANSPORTE = "Ingrese todos los datos del Transporte";

    /** "Debe ingresar el detalle de los productos" ({@code PPActaDetalle.Validar}). */
    public static final String MSG_DETALLE_PRODUCTOS = "Debe ingresar el detalle de los productos";

    /** "La cantidad maxima de productos que se pueden agregar es de seis". */
    public static final String MSG_MAX_PRODUCTOS =
            "La cantidad maxima de productos que se pueden agregar es de seis";

    /** "El movimiento de Entrada solo puede generar un Acta por falta de Documentación de Prod. Primaria". */
    public static final String MSG_ENTRADA_SOLO_PROD_PRIM = "El movimiento de Entrada solo puede "
            + "generar un Acta por falta de Documentación de Prod. Primaria";

    /** "Debe ingresar un Nro. de Formulario Generado mediante el Sistema" ({@code F2276} rules). */
    public static final String MSG_NRO_FORMULARIO_GENERADO =
            "Debe ingresar un Nro. de Formulario Generado mediante el Sistema";

    /** "El número de Acta no corresponde a un número generado manualmente". */
    public static final String MSG_NRO_ACTA_NO_MANUAL =
            "El número de Acta no corresponde a un número generado manualmente";

    /** "Debe seleccionar algun producto" ({@code ActaCargarDetalle.Validaciones}). */
    public static final String MSG_SIN_PRODUCTO = "Debe seleccionar algun producto";

    /** "El producto ya ha sido ingresado" ({@code PPActaDetalle.Cargar}). */
    public static final String MSG_PRODUCTO_DUPLICADO = "El producto ya ha sido ingresado";

    /** "Debe seleccionar un tipo de comprobante" ({@code F2276} rules). */
    public static final String MSG_TIPO_COMPROBANTE = "Debe seleccionar un tipo de comprobante";

    /** "Debe ingresar un nro. de comprobante" ({@code F2276} rules). */
    public static final String MSG_NRO_COMPROBANTE = "Debe ingresar un nro. de comprobante";

    /** "La fecha no puede ser superior a la fecha actual" ({@code F2276} rules). */
    public static final String MSG_FECHA_FUTURA = "La fecha no puede ser superior a la fecha actual";

    /** "No se puede generar la guia porque el turno asociado esta cerrado" ({@code F2276} rules). */
    public static final String MSG_TURNO_CERRADO =
            "No se puede generar la guia porque el turno asociado esta cerrado";

    /** "El precio ingresado no puede ser inferior al Precio registrado del Producto". */
    public static final String MSG_PRECIO_INFERIOR =
            "El precio ingresado no puede ser inferior al Precio registrado del Producto";

    /** "¿Confirma que desea enviar el Formulario de Acta?" (JS {@code confirm}). */
    public static final String MSG_CONFIRMACION =
            "¿Confirma que desea enviar el Formulario de Acta?";

    /** "El acta se ha creado correctamente" (éxito del alta). */
    public static final String MSG_EXITO = "El acta se ha creado correctamente";

    /** Prefijo del mensaje de éxito con problemas en la generación de la multa (SA-ACTA-02). */
    public static final String MSG_EXITO_CON_MULTA =
            "El acta se ha creado correctamente , pero hubo problemas con la generacion de la multa.(";

    /** Valor de la columna "Gen. Manual?" cuando el número cae en un rango manual. */
    public static final String GEN_MANUAL_SI = "Sí";

    /** Valor de la columna "Gen. Manual?" cuando el número no cae en un rango manual. */
    public static final String GEN_MANUAL_NO = "No";

    /**
     * Motivo del bloqueo por formulario abierto — <strong>no</strong> es un mensaje del legacy
     * (R-ACTA-041 pide informar el estado; el legacy no expone un texto).
     */
    public static final String MSG_FORMULARIO_ABIERTO =
            "El turno tiene formularios abiertos, no se puede generar el Acta";

    private MensajesActa() {
        // clase de constantes
    }
}
