package ar.com.atp.puestos.puestos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.com.atp.puestos.nucleo.domain.Turno;
import ar.com.atp.puestos.puestos.domain.Acta;
import ar.com.atp.puestos.puestos.domain.ActaProducto;
import ar.com.atp.puestos.puestos.service.model.ActaAlta;
import ar.com.atp.puestos.puestos.service.model.ActaAltaProducto;
import ar.com.atp.puestos.puestos.service.model.ResultadoAltaActa;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Suite unitaria del <strong>alta</strong> de Actas y de sus validaciones
 * (spec {@code 09-formularios-acta} §4.2, §4.3, T-ACTA-173).
 *
 * <p>Verifica los <strong>mensajes verbatim</strong> de la spec §3.9 y el orden de evaluación
 * (R-ACTA-040): ante varias infracciones sólo se devuelve el primer mensaje.
 */
class ActaAltaTest extends ActaServicioTestBase {

    @Test
    @DisplayName("R-ACTA-036: el alta genera el numero desde el contador y persiste cabecera, detalle y pase")
    void altaCompleta() {
        when(servicioNumeracionActa.siguienteNumeroActa(SUCURSAL, TIPO)).thenReturn(1234);
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto(10, 1)));
        when(vigenciaRepository.findFirstById_ProdCodAndId_ProdFchVigLessThanEqualOrderById_ProdFchVigDesc(
                eq(10), any())).thenReturn(Optional.of(vigencia(BigDecimal.TEN, BigDecimal.TEN,
                        BigDecimal.TEN, BigDecimal.TEN)));

        ResultadoAltaActa resultado = service.guardar(altaValida(), contexto());

        assertThat(resultado.ok()).isTrue();
        assertThat(resultado.mensaje()).isEqualTo("El acta se ha creado correctamente");
        assertThat(resultado.f276Id()).isEqualTo(1234);

        ArgumentCaptor<Acta> cabecera = ArgumentCaptor.forClass(Acta.class);
        verify(actaRepository).save(cabecera.capture());
        Acta guardada = cabecera.getValue();
        assertThat(guardada.getF276Id()).isEqualTo(1234);
        assertThat(guardada.getSucTipo()).isEqualTo("PUE");
        assertThat(guardada.getSucNroId()).isEqualTo(SUCURSAL);
        assertThat(guardada.getTurNroId()).isEqualTo(TURNO);
        assertThat(guardada.getCpcPerId()).isEqualTo(42);
        assertThat(guardada.getF276GenWeb()).isEqualTo("A");
        assertThat(guardada.getF276Operad()).isEqualTo("OPERADOR");
        assertThat(guardada.getF276Estad()).isEqualTo("C");
        assertThat(guardada.getF276Multa()).isEqualByComparingTo("0");
        assertThat(guardada.getF276ForPag()).isEqualTo("EFE");
        assertThat(guardada.getF276FeGra()).isNotNull();
        assertThat(guardada.getF276ImpuFl()).isEqualByComparingTo("0");
        // Columnas huerfanas: default null (R-ACTA-051).
        assertThat(guardada.getPueCod()).isNull();
        assertThat(guardada.getCpcId()).isNull();

        ArgumentCaptor<ActaProducto> detalle = ArgumentCaptor.forClass(ActaProducto.class);
        verify(actaProductoRepository).save(detalle.capture());
        assertThat(detalle.getValue().getF276Id()).isEqualTo(1234);
        assertThat(detalle.getValue().getF276RMarCM()).isEqualTo("N");
        assertThat(detalle.getValue().getF276RAlicu()).isEqualByComparingTo("1");
        assertThat(detalle.getValue().getF276RFijo()).isEqualByComparingTo("10");

        verify(actaPaseRepository).save(any());
    }

    @Test
    @DisplayName("R-ACTA-040: con cuatro infracciones simultaneas solo se devuelve el primer mensaje")
    void soloElPrimerMensaje() {
        ActaAlta alta = new ActaAlta("N", "N", LocalDate.now(), null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, List.of());

        ResultadoAltaActa resultado = service.guardar(alta, contexto());

        assertThat(resultado.ok()).isFalse();
        assertThat(resultado.mensaje()).isEqualTo("Seleccionar el motivo de Acta");
        verify(servicioNumeracionActa, never()).siguienteNumeroActa(anyInt(), anyString());
        verify(actaRepository, never()).save(any());
    }

    @Test
    @DisplayName("R-ACTA-026: motivo de acta sin informar")
    void motivoDeActa() {
        assertThat(service.guardar(altaCon("N", "S", CUIT, "VENDEDOR SA"), contexto()).mensaje())
                .isEqualTo("Seleccionar el motivo de Acta");
    }

    @Test
    @DisplayName("R-ACTA-027: tipo de movimiento sin informar")
    void tipoDeMovimiento() {
        assertThat(service.guardar(altaCon("F", "N", CUIT, "VENDEDOR SA"), contexto()).mensaje())
                .isEqualTo("Debe seleccionar Tipo de Movimiento");
    }

    @Test
    @DisplayName("R-ACTA-028: CUIT del vendedor invalido")
    void cuitDelVendedor() {
        assertThat(service.guardar(altaCon("F", "S", null, "VENDEDOR SA"), contexto()).mensaje())
                .isEqualTo("Debe ingresar CUIT del Vendedor");
    }

    @Test
    @DisplayName("R-ACTA-028: CUIT del destinatario invalido")
    void cuitDelDestinatario() {
        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now(), CUIT, "VENDEDOR SA", "D", 1,
                null, "DESTINATARIO SA", "D", 2, CUIT_TRAN, "TRANSPORTISTA SA", "D", 3, 1, "1",
                "AB123CD", "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(10, BigDecimal.ONE, BigDecimal.ONE)));
        assertThat(service.guardar(alta, contexto()).mensaje())
                .isEqualTo("Debe ingresar CUIT del Destinatario");
    }

    @Test
    @DisplayName("R-ACTA-028: CUIT del transportista invalido")
    void cuitDelTransportista() {
        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now(), CUIT, "VENDEDOR SA", "D", 1,
                CUIT_DEST, "DESTINATARIO SA", "D", 2, null, "TRANSPORTISTA SA", "D", 3, 1, "1",
                "AB123CD", "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(10, BigDecimal.ONE, BigDecimal.ONE)));
        assertThat(service.guardar(alta, contexto()).mensaje())
                .isEqualTo("Debe ingresar CUIT del Transportista");
    }

    @Test
    @DisplayName("R-CUIT-002: CUIT del vendedor con digito verificador incorrecto se rechaza")
    void cuitDelVendedorDvIncorrecto() {
        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now(), 20123456789L, "VENDEDOR SA", "D", 1,
                CUIT_DEST, "DESTINATARIO SA", "D", 2, CUIT_TRAN, "TRANSPORTISTA SA", "D", 3, 1, "1",
                "AB123CD", "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(10, BigDecimal.ONE, BigDecimal.ONE)));
        assertThat(service.guardar(alta, contexto()).mensaje())
                .isEqualTo("Debe ingresar CUIT del Vendedor");
    }

    @Test
    @DisplayName("R-CUIT-002: CUIT del destinatario con digito verificador incorrecto se rechaza")
    void cuitDelDestinatarioDvIncorrecto() {
        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now(), CUIT, "VENDEDOR SA", "D", 1,
                30123456784L, "DESTINATARIO SA", "D", 2, CUIT_TRAN, "TRANSPORTISTA SA", "D", 3, 1,
                "1", "AB123CD", "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(10, BigDecimal.ONE, BigDecimal.ONE)));
        assertThat(service.guardar(alta, contexto()).mensaje())
                .isEqualTo("Debe ingresar CUIT del Destinatario");
    }

    @Test
    @DisplayName("R-CUIT-002: CUIT del transportista con digito verificador incorrecto se rechaza")
    void cuitDelTransportistaDvIncorrecto() {
        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now(), CUIT, "VENDEDOR SA", "D", 1,
                CUIT_DEST, "DESTINATARIO SA", "D", 2, 27123456789L, "TRANSPORTISTA SA", "D", 3, 1,
                "1", "AB123CD", "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(10, BigDecimal.ONE, BigDecimal.ONE)));
        assertThat(service.guardar(alta, contexto()).mensaje())
                .isEqualTo("Debe ingresar CUIT del Transportista");
    }

    @Test
    @DisplayName("R-CUIT-003: orden Vendedor -> Destinatario -> Transportista con dos CUITs invalidos")
    void ordenCuitInvalidos() {
        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now(), 20123456789L, "VENDEDOR SA", "D", 1,
                30123456784L, "DESTINATARIO SA", "D", 2, CUIT_TRAN, "TRANSPORTISTA SA", "D", 3, 1,
                "1", "AB123CD", "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(10, BigDecimal.ONE, BigDecimal.ONE)));
        assertThat(service.guardar(alta, contexto()).mensaje())
                .isEqualTo("Debe ingresar CUIT del Vendedor");
    }

    @Test
    @DisplayName("R-ACTA-029: razon social del vendedor vacia")
    void razonSocialDelVendedor() {
        assertThat(service.guardar(altaCon("F", "S", CUIT, null), contexto()).mensaje())
                .isEqualTo("Debe ingresar Razón Social del Vendedor");
    }

    @Test
    @DisplayName("R-ACTA-030: faltan los datos del transporte")
    void datosDelTransporte() {
        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now(), CUIT, "VENDEDOR SA", "D", 1,
                CUIT_DEST, "DESTINATARIO SA", "D", 2, CUIT_TRAN, "TRANSPORTISTA SA", "D", 3, 1,
                "1", null, "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(10, BigDecimal.ONE, BigDecimal.ONE)));
        assertThat(service.guardar(alta, contexto()).mensaje())
                .isEqualTo("Ingrese todos los datos del Transporte");
    }

    @Test
    @DisplayName("R-ACTA-031: la grilla de productos esta vacia")
    void detalleDeProductosVacio() {
        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now(), CUIT, "VENDEDOR SA", "D", 1,
                CUIT_DEST, "DESTINATARIO SA", "D", 2, CUIT_TRAN, "TRANSPORTISTA SA", "D", 3, 1,
                "1", "AB123CD", "EF456GH", new BigDecimal("1"), List.of());
        assertThat(service.guardar(alta, contexto()).mensaje())
                .isEqualTo("Debe ingresar el detalle de los productos");
    }

    @Test
    @DisplayName("R-ACTA-023: mas de seis productos se rechaza")
    void maximoDeProductos() {
        List<ActaAltaProducto> productos = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            productos.add(new ActaAltaProducto(i, BigDecimal.ONE, BigDecimal.ONE));
        }
        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now(), CUIT, "VENDEDOR SA", "D", 1,
                CUIT_DEST, "DESTINATARIO SA", "D", 2, CUIT_TRAN, "TRANSPORTISTA SA", "D", 3, 1,
                "1", "AB123CD", "EF456GH", new BigDecimal("1"), productos);
        assertThat(service.guardar(alta, contexto()).mensaje())
                .isEqualTo("La cantidad maxima de productos que se pueden agregar es de seis");
    }

    @Test
    @DisplayName("R-ACTA-038: el mismo producto dos veces se rechaza")
    void productoDuplicado() {
        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now(), CUIT, "VENDEDOR SA", "D", 1,
                CUIT_DEST, "DESTINATARIO SA", "D", 2, CUIT_TRAN, "TRANSPORTISTA SA", "D", 3, 1,
                "1", "AB123CD", "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(10, BigDecimal.ONE, BigDecimal.ONE),
                        new ActaAltaProducto(10, BigDecimal.ONE, BigDecimal.ONE)));
        assertThat(service.guardar(alta, contexto()).mensaje())
                .isEqualTo("El producto ya ha sido ingresado");
    }

    @Test
    @DisplayName("R-ACTA-039: una fila de producto sin codigo se rechaza")
    void productoSinCodigo() {
        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now(), CUIT, "VENDEDOR SA", "D", 1,
                CUIT_DEST, "DESTINATARIO SA", "D", 2, CUIT_TRAN, "TRANSPORTISTA SA", "D", 3, 1,
                "1", "AB123CD", "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(null, BigDecimal.ONE, BigDecimal.ONE)));
        assertThat(service.guardar(alta, contexto()).mensaje())
                .isEqualTo("Debe seleccionar algun producto");
    }

    @Test
    @DisplayName("R-ACTA-032: la Entrada solo admite el motivo de falta de documentacion primaria")
    void entradaSoloConProdPrimaria() {
        assertThat(service.guardar(altaCon("F", "E", CUIT, "VENDEDOR SA"), contexto()).mensaje())
                .isEqualTo("El movimiento de Entrada solo puede generar un Acta por falta de "
                        + "Documentación de Prod. Primaria");
    }

    @Test
    @DisplayName("R-ACTA-033: sin tipo de comprobante")
    void sinTipoDeComprobante() {
        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now(), CUIT, "VENDEDOR SA", "D", 1,
                CUIT_DEST, "DESTINATARIO SA", "D", 2, CUIT_TRAN, "TRANSPORTISTA SA", "D", 3, null,
                "1", "AB123CD", "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(10, BigDecimal.ONE, BigDecimal.ONE)));
        assertThat(service.guardar(alta, contexto()).mensaje())
                .isEqualTo("Ingrese todos los datos del Transporte");
    }

    @Test
    @DisplayName("R-ACTA-033: sin numero de comprobante")
    void sinNumeroDeComprobante() {
        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now(), CUIT, "VENDEDOR SA", "D", 1,
                CUIT_DEST, "DESTINATARIO SA", "D", 2, CUIT_TRAN, "TRANSPORTISTA SA", "D", 3, 1,
                null, "AB123CD", "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(10, BigDecimal.ONE, BigDecimal.ONE)));
        assertThat(service.guardar(alta, contexto()).mensaje())
                .isEqualTo("Ingrese todos los datos del Transporte");
    }

    @Test
    @DisplayName("R-ACTA-034: la fecha no puede ser futura")
    void fechaFutura() {
        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now().plusDays(1), CUIT, "VENDEDOR SA",
                "D", 1, CUIT_DEST, "DESTINATARIO SA", "D", 2, CUIT_TRAN, "TRANSPORTISTA SA", "D",
                3, 1, "1", "AB123CD", "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(10, BigDecimal.ONE, BigDecimal.ONE)));
        assertThat(service.guardar(alta, contexto()).mensaje())
                .isEqualTo("La fecha no puede ser superior a la fecha actual");
    }

    @Test
    @DisplayName("R-ACTA-035: con el turno cerrado el alta se rechaza")
    void turnoCerrado() {
        Turno turno = turno("C");
        when(turnoRepositorio.findById(turno.getId())).thenReturn(Optional.of(turno));

        assertThat(service.guardar(altaValida(), contexto()).mensaje())
                .isEqualTo("No se puede generar la guia porque el turno asociado esta cerrado");
        verify(servicioNumeracionActa, never()).siguienteNumeroActa(anyInt(), anyString());
    }

    @Test
    @DisplayName("R-ACTA-037: el precio informado no puede ser inferior al precio registrado")
    void precioInferiorAlRegistrado() {
        stubPrecioRegistrado();
        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now(), CUIT, "VENDEDOR SA", "D", 1,
                CUIT_DEST, "DESTINATARIO SA", "D", 2, CUIT_TRAN, "TRANSPORTISTA SA", "D", 3, 1,
                "1", "AB123CD", "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(10, BigDecimal.ONE, new BigDecimal("1"))));

        assertThat(service.guardar(alta, contexto()).mensaje())
                .isEqualTo("El precio ingresado no puede ser inferior al Precio registrado del Producto");
    }

    @Test
    @DisplayName("R-ACTA-037: el precio informado en cero toma el precio registrado")
    void precioEnCeroTomaElRegistrado() {
        stubPrecioRegistrado();
        when(servicioNumeracionActa.siguienteNumeroActa(SUCURSAL, TIPO)).thenReturn(9);
        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now(), CUIT, "VENDEDOR SA", "D", 1,
                CUIT_DEST, "DESTINATARIO SA", "D", 2, CUIT_TRAN, "TRANSPORTISTA SA", "D", 3, 1,
                "1", "AB123CD", "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(10, BigDecimal.ONE, BigDecimal.ZERO)));

        ResultadoAltaActa resultado = service.guardar(alta, contexto());

        assertThat(resultado.ok()).isTrue();
        ArgumentCaptor<ActaProducto> detalle = ArgumentCaptor.forClass(ActaProducto.class);
        verify(actaProductoRepository).save(detalle.capture());
        assertThat(detalle.getValue().getF276RSPre()).isEqualByComparingTo("10");
    }

    /** Producto con precio fijo registrado de 10. */
    private void stubPrecioRegistrado() {
        when(productoRepository.findById(10)).thenReturn(Optional.empty());
        when(vigenciaRepository.findFirstById_ProdCodAndId_ProdFchVigLessThanEqualOrderById_ProdFchVigDesc(
                eq(10), any())).thenReturn(Optional.of(vigencia(BigDecimal.TEN, BigDecimal.TEN,
                        BigDecimal.TEN, BigDecimal.TEN)));
    }
}
