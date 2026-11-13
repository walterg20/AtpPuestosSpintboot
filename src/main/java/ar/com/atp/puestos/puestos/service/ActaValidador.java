package ar.gob.chaco.atp.puestoscamineros.service;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.TurnoMapper;
import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ValidadorCuit;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaAlta;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaAltaProducto;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Validaciones del alta de un Acta (spec {@code 09-formularios-acta} §3.9, §4.3, R-ACTA-026…
 * R-ACTA-040).
 *
 * <p>Reproduce <strong>en el orden exacto</strong> el {@code Sub 'Validar'} del legacy
 * ({@code PPActaDetalle}) y las {@code rules} de la transacción {@code F2276}, y devuelve
 * <strong>siempre el primer</strong> mensaje verbatim (nunca todos).
 *
 * <p>Se separa del servicio orquestador para respetar el límite de 500 líneas por archivo (G4):
 * la <strong>API pública</strong> sigue estando en {@link ActaService}, que delega aquí.
 */
@Component
public class ActaValidador {

    /** Máximo de filas de la grilla de productos ({@code &Productos.Count > 6}). */
    public static final int MAX_PRODUCTOS = 6;

    private final ActaDerivaciones derivaciones;

    public ActaValidador(ActaDerivaciones derivaciones) {
        this.derivaciones = derivaciones;
    }

    /**
     * Validaciones del panel ({@code Sub 'Validar'} del legacy), en el orden exacto del legacy
     * (R-ACTA-040).
     *
     * <p>Orden: motivo → nº de Acta (modo manual; no aplica en carga manual, SA-ACTA-08) → tipo
     * de movimiento → CUIT de las tres partes → razón social de las tres partes → datos del
     * transporte → detalle de productos → máximo de 6 productos → producto sin seleccionar →
     * producto duplicado → restricción de Entrada.
     *
     * @param alta comando de alta
     * @return el primer mensaje verbatim, o {@code null} si todo está bien
     */
    public String validar(ActaAlta alta) {
        if (esVacio(alta.f276ActaTi()) || "N".equals(TurnoMapper.trim(alta.f276ActaTi()))) {
            return MensajesActa.MSG_MOTIVO_ACTA;
        }
        if (esVacio(alta.f276Clasif()) || "N".equals(TurnoMapper.trim(alta.f276Clasif()))) {
            return MensajesActa.MSG_TIPO_MOVIMIENTO;
        }
        if (!cuitValido(alta.f276CuitVe())) {
            return MensajesActa.MSG_CUIT_VENDEDOR;
        }
        if (!cuitValido(alta.f276CuitDe())) {
            return MensajesActa.MSG_CUIT_DESTINATARIO;
        }
        if (!cuitValido(alta.f276CuitTr())) {
            return MensajesActa.MSG_CUIT_TRANSPORTISTA;
        }
        if (esVacio(alta.f276VenRS())) {
            return MensajesActa.MSG_RAZON_SOCIAL_VENDEDOR;
        }
        if (esVacio(alta.f276DestRS())) {
            return MensajesActa.MSG_RAZON_SOCIAL_DESTINATARIO;
        }
        if (esVacio(alta.f276TranRS())) {
            return MensajesActa.MSG_RAZON_SOCIAL_TRANSPORTISTA;
        }
        if (esVacio(alta.f276Chasis()) || esVacio(alta.f276Acopla())
                || alta.f276TCpbte() == null || esVacio(alta.f276NCpbte())
                || alta.f276KmFle() == null) {
            return MensajesActa.MSG_DATOS_TRANSPORTE;
        }
        List<ActaAltaProducto> productos = alta.productos();
        if (productos == null || productos.isEmpty()) {
            return MensajesActa.MSG_DETALLE_PRODUCTOS;
        }
        if (productos.size() > MAX_PRODUCTOS) {
            return MensajesActa.MSG_MAX_PRODUCTOS;
        }
        Set<Integer> codigos = new HashSet<>();
        for (ActaAltaProducto linea : productos) {
            if (linea.prodCod() == null) {
                return MensajesActa.MSG_SIN_PRODUCTO;
            }
            if (!codigos.add(linea.prodCod())) {
                return MensajesActa.MSG_PRODUCTO_DUPLICADO;
            }
        }
        if ("E".equals(TurnoMapper.trim(alta.f276Clasif()))
                && !"P".equals(TurnoMapper.trim(alta.f276ActaTi()))) {
            return MensajesActa.MSG_ENTRADA_SOLO_PROD_PRIM;
        }
        return null;
    }

    /**
     * Reglas de la transacción {@code F2276} que dependen de la sesión y de la fecha.
     *
     * <p>Orden: fecha futura → turno cerrado. El tipo y número de comprobante se validan junto
     * con los demás datos del transporte en {@link #validar(ActaAlta)} (R-ACTA-040). El
     * precio inferior al registrado se valida por renglón de detalle (ver
     * {@code ActaService#guardar}), porque depende del precio fijo del producto.
     *
     * @param alta comando de alta
     * @param ctx contexto de sesión
     * @return el primer mensaje verbatim, o {@code null} si todo está bien
     */
    public String validarReglas(ActaAlta alta, ContextoSesion ctx) {
        if (fechaMovimiento(alta).isAfter(LocalDate.now())) {
            return MensajesActa.MSG_FECHA_FUTURA;
        }
        if ("C".equals(derivaciones.estadoTurno(ctx.sucursal(), ctx.tipo(), ctx.turno()))) {
            return MensajesActa.MSG_TURNO_CERRADO;
        }
        return null;
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    /**
     * ¿El CUIT es válido? (paridad de {@code Nuc_ValidaCuit}, módulo 11).
     *
     * <p>Delega en {@link ValidadorCuit} del núcleo: valida existencia, 11 dígitos y el dígito
     * verificador con el algoritmo completo del legacy (R-CUIT-002, R-CUIT-007).
     *
     * @param valor CUIT informado
     * @return {@code true} si el CUIT es válido
     */
    private static boolean cuitValido(Long valor) {
        return ValidadorCuit.esValido(valor);
    }

    /** Fecha de movimiento del alta (por defecto: hoy). */
    private static LocalDate fechaMovimiento(ActaAlta alta) {
        return alta.f276FchMov() == null ? LocalDate.now() : alta.f276FchMov();
    }
}
