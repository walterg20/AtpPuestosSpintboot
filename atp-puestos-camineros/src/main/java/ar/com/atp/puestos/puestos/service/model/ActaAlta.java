package ar.com.atp.puestos.puestos.service.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Comando de alta de un Acta (spec {@code 09-formularios-acta} §3.3, plan §3.3, D-ACTA-22).
 *
 * <p><strong>No</strong> contiene sucursal, tipo ni turno: esos valores salen siempre del
 * contexto de sesión (R-ACTA-044). Tampoco contiene el número de Acta: el módulo es
 * "carga manual" y el número lo genera el servidor desde el contador {@code GES_SUCN}
 * (SA-ACTA-08, R-ACTA-036).
 *
 * @param f276ActaTi motivo del Acta ({@code F276ActaTi}): {@code N}/{@code F}/{@code P}/{@code T}
 * @param f276Clasif tipo de movimiento ({@code F276Clasif}): {@code N}/{@code S}/{@code E}
 * @param f276FchMov fecha de movimiento ({@code F276FchMov})
 * @param f276CuitVe CUIT del vendedor
 * @param f276VenRS razón social del vendedor
 * @param f276VenDom domicilio del vendedor
 * @param f276VenCLo código de localidad del vendedor
 * @param f276CuitDe CUIT del destinatario
 * @param f276DestRS razón social del destinatario
 * @param f276DestDo domicilio del destinatario
 * @param f276DestCL código de localidad del destinatario
 * @param f276CuitTr CUIT del transportista
 * @param f276TranRS razón social del transportista
 * @param f276TranDo domicilio del transportista
 * @param f276TranCL código de localidad del transportista
 * @param f276TCpbte tipo de comprobante ({@code F276TCpbte})
 * @param f276NCpbte número de comprobante ({@code F276NCpbte})
 * @param f276Chasis patente del chasis
 * @param f276Acopla patente del acoplado
 * @param f276KmFle kilómetros de flete ({@code F276KmFle})
 * @param productos filas de la grilla de productos
 * @param f276Id número de Acta informado (sólo en carga manual; {@code null} en alta normal)
 * @param turNroId turno seleccionado (sólo en carga manual; {@code null} ⇒ se usa el del contexto)
 */
public record ActaAlta(
        String f276ActaTi,
        String f276Clasif,
        LocalDate f276FchMov,
        Long f276CuitVe,
        String f276VenRS,
        String f276VenDom,
        Integer f276VenCLo,
        Long f276CuitDe,
        String f276DestRS,
        String f276DestDo,
        Integer f276DestCL,
        Long f276CuitTr,
        String f276TranRS,
        String f276TranDo,
        Integer f276TranCL,
        Integer f276TCpbte,
        String f276NCpbte,
        String f276Chasis,
        String f276Acopla,
        BigDecimal f276KmFle,
        List<ActaAltaProducto> productos,
        Integer f276Id,
        Integer turNroId) {

    /** Constructor de compatibilidad (alta normal, sin número manual ni turno seleccionado). */
    public ActaAlta(String f276ActaTi, String f276Clasif, LocalDate f276FchMov,
            Long f276CuitVe, String f276VenRS, String f276VenDom, Integer f276VenCLo,
            Long f276CuitDe, String f276DestRS, String f276DestDo, Integer f276DestCL,
            Long f276CuitTr, String f276TranRS, String f276TranDo, Integer f276TranCL,
            Integer f276TCpbte, String f276NCpbte, String f276Chasis, String f276Acopla,
            BigDecimal f276KmFle, List<ActaAltaProducto> productos) {
        this(f276ActaTi, f276Clasif, f276FchMov, f276CuitVe, f276VenRS, f276VenDom, f276VenCLo,
                f276CuitDe, f276DestRS, f276DestDo, f276DestCL, f276CuitTr, f276TranRS, f276TranDo,
                f276TranCL, f276TCpbte, f276NCpbte, f276Chasis, f276Acopla, f276KmFle, productos,
                null, null);
    }
}
