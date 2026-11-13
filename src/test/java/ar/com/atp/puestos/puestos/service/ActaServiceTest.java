package ar.gob.chaco.atp.puestoscamineros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Turno;
import ar.gob.chaco.atp.puestoscamineros.domain.Acta;
import ar.gob.chaco.atp.puestoscamineros.domain.RangoManualFormulario;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaAlta;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaAltaProducto;
import ar.gob.chaco.atp.puestoscamineros.service.model.ResultadoAltaActa;import ar.gob.chaco.atp.puestoscamineros.domain.VersionSistema;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaFila;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaFiltro;
import ar.gob.chaco.atp.puestoscamineros.service.model.ActaPreparada;
import ar.gob.chaco.atp.puestoscamineros.service.model.DatosParte;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Suite unitaria del <strong>listado y las derivaciones</strong> del servicio de Actas
 * (spec {@code 09-formularios-acta} §4, T-ACTA-173).
 *
 * <p>Cubre: contraparte (4 casos), total, "Gen. Manual?" (4 casos límite), precio del producto
 * (2 vigencias × 2 clasificaciones), impuesto IB ({@code Nuc_CalculoImpIB}, 4 casos), versión del
 * sistema, estado del turno, tipo de formulario (5 casos) y derivación por CUIT.
 *
 * <p>El alta y sus validaciones están en {@link ActaAltaTest}.
 */
class ActaServiceTest extends ActaServicioTestBase {

    @Test
    @DisplayName("R-ACTA-006: con vendedor informado la contraparte es el vendedor")
    void contraparteVendedor() {
        Acta acta = acta(1);
        acta.setF276CuitVe(new BigDecimal(CUIT));
        acta.setF276VenRS("VENDEDOR SA");
        acta.setF276CuitDe(new BigDecimal(CUIT_DEST));
        acta.setF276DestRS("DESTINATARIO SA");
        stubListado(acta);

        Page<ActaFila> resultado = service.listar(ActaFiltro.vacio(), PageRequest.of(0, 10),
                contexto());

        ActaFila fila = resultado.getContent().get(0);
        assertThat(fila.conCuit()).isEqualTo(CUIT);
        assertThat(fila.conRazSoc()).isEqualTo("VENDEDOR SA");
    }

    @Test
    @DisplayName("R-ACTA-006: sin vendedor la contraparte es el destinatario")
    void contraparteDestinatario() {
        Acta acta = acta(2);
        acta.setF276CuitDe(new BigDecimal(CUIT_DEST));
        acta.setF276DestRS("DESTINATARIO SA");
        acta.setF276CuitTr(new BigDecimal(CUIT_TRAN));
        acta.setF276TranRS("TRANSPORTISTA SA");
        stubListado(acta);

        Page<ActaFila> resultado = service.listar(ActaFiltro.vacio(), PageRequest.of(0, 10),
                contexto());

        ActaFila fila = resultado.getContent().get(0);
        assertThat(fila.conCuit()).isEqualTo(CUIT_DEST);
        assertThat(fila.conRazSoc()).isEqualTo("DESTINATARIO SA");
    }

    @Test
    @DisplayName("R-ACTA-006: sin vendedor ni destinatario la contraparte es el transportista")
    void contraparteTransportista() {
        Acta acta = acta(3);
        acta.setF276CuitTr(new BigDecimal(CUIT_TRAN));
        acta.setF276TranRS("TRANSPORTISTA SA");
        stubListado(acta);

        Page<ActaFila> resultado = service.listar(ActaFiltro.vacio(), PageRequest.of(0, 10),
                contexto());

        ActaFila fila = resultado.getContent().get(0);
        assertThat(fila.conCuit()).isEqualTo(CUIT_TRAN);
        assertThat(fila.conRazSoc()).isEqualTo("TRANSPORTISTA SA");
    }

    @Test
    @DisplayName("R-ACTA-006: sin ninguna parte las celdas de contraparte quedan vacias")
    void contraparteVacia() {
        stubListado(acta(4));

        Page<ActaFila> resultado = service.listar(ActaFiltro.vacio(), PageRequest.of(0, 10),
                contexto());

        ActaFila fila = resultado.getContent().get(0);
        assertThat(fila.conCuit()).isNull();
        assertThat(fila.conRazSoc()).isNull();
    }

    @Test
    @DisplayName("R-ACTA-007: el total es F276AdiTot + F276ImpTotal")
    void totalDelActa() {
        Acta acta = acta(5);
        acta.setF276GenWeb("N");
        acta.setF276AdicFl(new BigDecimal("10"));
        acta.setF276AdIBTo(new BigDecimal("1"));
        acta.setF276AdCMTo(new BigDecimal("2"));
        acta.setF276ImpuFl(new BigDecimal("5"));

        BigDecimal total = service.calcularTotal(acta, new BigDecimal("3"), new BigDecimal("4"));

        // (10 + 1 + 2) + (5 + 3 + 4) = 25
        assertThat(total).isEqualByComparingTo("25");
    }

    @Test
    @DisplayName("R-ACTA-007: con F276GenWeb='A' los impuestos del detalle no suman")
    void totalConGeneracionWeb() {
        Acta acta = acta(6);
        acta.setF276GenWeb("A");
        acta.setF276AdicFl(new BigDecimal("10"));
        acta.setF276ImpuFl(new BigDecimal("5"));

        BigDecimal total = service.calcularTotal(acta, new BigDecimal("300"), new BigDecimal("400"));

        // (10) + (5 + 0 + 0) = 15
        assertThat(total).isEqualByComparingTo("15");
    }

    @Test
    @DisplayName("R-ACTA-008: 'Gen. Manual?' es Si dentro del rango y No fuera")
    void genManualSegunElRango() {
        RangoManualFormulario rango = new RangoManualFormulario();
        rango.setSucNroId(SUCURSAL);
        rango.setSucTipo(TIPO);
        rango.setSucNuForm(2276);
        rango.setForRecIni(100);
        rango.setForRecCant(50);
        when(rangoManualRepositorio.findBySucursalYFormulario(SUCURSAL, TIPO, 2276))
                .thenReturn(List.of(rango));
        stubListado(acta(99), acta(100), acta(149), acta(150));

        Page<ActaFila> resultado = service.listar(ActaFiltro.vacio(), PageRequest.of(0, 10),
                contexto());

        assertThat(resultado.getContent()).extracting(ActaFila::genManual)
                .containsExactly("No", "Sí", "Sí", "No");
        assertThat(resultado.getContent()).extracting(ActaFila::imprimible)
                .containsExactly(true, false, false, true);
    }

    @Test
    @DisplayName("R-ACTA-022: el precio sale de la vigencia vigente y de la columna de la clasif")
    void precioDelProducto() {
        when(vigenciaRepository.findFirstById_ProdCodAndId_ProdFchVigLessThanEqualOrderById_ProdFchVigDesc(
                eq(10), any())).thenReturn(Optional.of(vigencia(new BigDecimal("10"),
                        new BigDecimal("20"), new BigDecimal("30"), new BigDecimal("40"))));

        // Salida: ProdImpu1 (precio fijo) y ProdImpu2 (cosecha mecanica).
        assertThat(service.precioProducto(10, "S", LocalDate.now(), false))
                .isEqualByComparingTo("10");
        assertThat(service.precioProducto(10, "S", LocalDate.now(), true))
                .isEqualByComparingTo("20");
        // Entrada: ProdImpu3 (precio fijo) y ProdImpu4 (cosecha mecanica).
        assertThat(service.precioProducto(10, "E", LocalDate.now(), false))
                .isEqualByComparingTo("30");
        assertThat(service.precioProducto(10, "E", LocalDate.now(), true))
                .isEqualByComparingTo("40");
    }

    @Test
    @DisplayName("R-ACTA-022: sin vigencia el precio es 0")
    void precioSinVigencia() {
        when(vigenciaRepository.findFirstById_ProdCodAndId_ProdFchVigLessThanEqualOrderById_ProdFchVigDesc(
                eq(10), any())).thenReturn(Optional.empty());

        assertThat(service.precioProducto(10, "S", LocalDate.now(), false))
                .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("R-ACTA-047: Nuc_CalculoImpIB con TipCal=1 es MAX(Round(cant*fijo,2), Round(monto*alic/100,2))")
    void impuestoIbPorTonelada() {
        // Round(10 * 5, 2) = 50.00 ; Round(1000 * 1 / 100, 2) = 10.00 => 50.00
        assertThat(service.impuestoIb(1, new BigDecimal("10"), new BigDecimal("1000"),
                new BigDecimal("5"), BigDecimal.ONE)).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("R-ACTA-047: Nuc_CalculoImpIB con TipCal=2 usa el mismo calculo que TipCal=1")
    void impuestoIbPorUnidad() {
        assertThat(service.impuestoIb(2, new BigDecimal("10"), new BigDecimal("1000"),
                new BigDecimal("5"), BigDecimal.ONE)).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("R-ACTA-047: Nuc_CalculoImpIB con TipCal=3 usa Calculo1 = fijo")
    void impuestoIbPorMontoFijo() {
        // Calculo1 = fijo = 5 ; Calculo2 = Round(1000 * 1 / 100, 2) = 10.00 => 10.00
        assertThat(service.impuestoIb(3, new BigDecimal("10"), new BigDecimal("1000"),
                new BigDecimal("5"), BigDecimal.ONE)).isEqualByComparingTo("10.00");
        // Con el fijo mayor que el calculo por alicuota gana el fijo.
        assertThat(service.impuestoIb(3, new BigDecimal("1"), new BigDecimal("1000"),
                new BigDecimal("500"), BigDecimal.ONE)).isEqualByComparingTo("500");
    }

    @Test
    @DisplayName("R-ACTA-047: Nuc_CalculoImpIB con TipCal fuera de {1,2,3} devuelve 0")
    void impuestoIbTipCalFueraDeRango() {
        assertThat(service.impuestoIb(9, new BigDecimal("10"), new BigDecimal("1000"),
                new BigDecimal("5"), BigDecimal.ONE)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("R-ACTA-047: el maximo gana cuando el calculo por alicuota es mayor")
    void impuestoIbTomaElMaximo() {
        // Round(1 * 5, 2) = 5.00 ; Round(100000 * 1 / 100, 2) = 1000.00 => 1000.00
        assertThat(service.impuestoIb(1, new BigDecimal("1"), new BigDecimal("100000"),
                new BigDecimal("5"), BigDecimal.ONE)).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("R-ACTA-045: la version del sistema es VerVersion.VerRevisio.VerBuild")
    void versionDelSistema() {
        VersionSistema version = new VersionSistema();
        version.setVerApliId("SWP");
        version.setVerDescr("WEB PUESTOS");
        version.setVerVersion(3);
        version.setVerRevisio(1);
        version.setVerBuild(new BigDecimal("1"));
        when(versionSistemaRepositorio.buscarPorAplicacion("SWP")).thenReturn(List.of(version));

        assertThat(service.versionSistema()).isEqualTo("3.1.1");
    }

    @Test
    @DisplayName("R-ACTA-045: sin fila en VERSIONE la version es la cadena vacia")
    void versionDelSistemaSinFila() {
        when(versionSistemaRepositorio.buscarPorAplicacion("SWP")).thenReturn(List.of());

        assertThat(service.versionSistema()).isEmpty();
    }

    @Test
    @DisplayName("R-ACTA-035: el estado del turno se lee de PPRIM_TU")
    void estadoDelTurno() {
        Turno turno = turno("C");
        when(turnoRepositorio.findById(turno.getId())).thenReturn(Optional.of(turno));

        assertThat(service.estadoTurno(SUCURSAL, TIPO, TURNO)).isEqualTo("C");
    }

    @Test
    @DisplayName("R-ACTA-035: un turno inexistente no tiene estado")
    void estadoDeTurnoInexistente() {
        Turno turno = turno("A");
        when(turnoRepositorio.findById(turno.getId())).thenReturn(Optional.empty());

        assertThat(service.estadoTurno(SUCURSAL, TIPO, TURNO)).isNull();
    }

    @Test
    @DisplayName("R-ACTA-046: Entrada sin infraccion da tipo de formulario 1")
    void tipoFormularioEntradaSinInfraccion() {
        when(contribuyenteRepository.findByConCuit(new BigDecimal(CUIT)))
                .thenReturn(contribuyente((short) 12));

        assertThat(service.tipoFormulario(CUIT, "E")).isEqualTo(1);
    }

    @Test
    @DisplayName("R-ACTA-046: Entrada con infraccion da tipo de formulario 2")
    void tipoFormularioEntradaConInfraccion() {
        when(contribuyenteRepository.findByConCuit(new BigDecimal(CUIT)))
                .thenReturn(contribuyente((short) 1));

        assertThat(service.tipoFormulario(CUIT, "E")).isEqualTo(2);
    }

    @Test
    @DisplayName("R-ACTA-046: estado 14 (transitorio especial) no es infraccion")
    void tipoFormularioEstadoCatorce() {
        when(contribuyenteRepository.findByConCuit(new BigDecimal(CUIT)))
                .thenReturn(contribuyente((short) 14));

        assertThat(service.tipoFormulario(CUIT, "E")).isEqualTo(1);
    }

    @Test
    @DisplayName("R-ACTA-046: un contribuyente inexistente no esta en infraccion")
    void tipoFormularioContribuyenteInexistente() {
        when(contribuyenteRepository.findByConCuit(new BigDecimal(CUIT))).thenReturn(null);

        assertThat(service.tipoFormulario(CUIT, "E")).isEqualTo(1);
    }

    @Test
    @DisplayName("R-ACTA-046: Salida da tipo de formulario 3")
    void tipoFormularioSalida() {
        assertThat(service.tipoFormulario(CUIT, "S")).isEqualTo(3);
    }

    @Test
    @DisplayName("R-ACTA-018: un contribuyente inexistente devuelve los datos vacios")
    void derivarPorCuitInexistente() {
        when(contribuyenteRepository.findByConCuit(new BigDecimal(CUIT))).thenReturn(null);

        DatosParte datos = service.derivarPorCuit(CUIT);

        assertThat(datos.razonSocial()).isEmpty();
        assertThat(datos.domicilio()).isEmpty();
        assertThat(datos.codigoLocalidad()).isNull();
    }

    @Test
    @DisplayName("R-ACTA-018: un contribuyente existente devuelve razon social y localidad")
    void derivarPorCuitExistente() {
        ar.gob.chaco.atp.puestoscamineros.domain.Contribuyente contribuyente =
                contribuyente((short) 1);
        contribuyente.setLocCodR(1234);
        contribuyente.setConNMzanaR(56);
        when(contribuyenteRepository.findByConCuit(new BigDecimal(CUIT))).thenReturn(contribuyente);

        DatosParte datos = service.derivarPorCuit(CUIT);

        assertThat(datos.razonSocial()).isEqualTo("VENDEDOR SA");
        assertThat(datos.codigoLocalidad()).isEqualTo(1234);
        assertThat(datos.domicilio()).contains("56");
    }

    @Test
    @DisplayName("R-ACTA-041: sin turno el formulario queda bloqueado con el mensaje verbatim")
    void prepararFormularioSinTurno() {
        ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion sinTurno =
                ar.gob.chaco.atp.puestoscamineros.nucleo.service.ContextoSesion.completo("operador", "Operador", 42,
                        7, 0, SUCURSAL, "Sucursal 5", TIPO, 1, "Responsable");

        ActaPreparada preparada = service.prepararFormulario(sinTurno);

        assertThat(preparada.bloqueado()).isTrue();
        assertThat(preparada.mensajeBloqueo()).isEqualTo("Debe iniciar un turno...");
    }

    @Test
    @DisplayName("R-ACTA-042: con el turno cerrado el formulario queda bloqueado")
    void prepararFormularioConTurnoCerrado() {
        Turno turno = turno("C");
        when(turnoRepositorio.findById(turno.getId())).thenReturn(Optional.of(turno));

        ActaPreparada preparada = service.prepararFormulario(contexto());

        assertThat(preparada.bloqueado()).isTrue();
        assertThat(preparada.mensajeBloqueo())
                .isEqualTo("No se puede generar la guia porque el turno asociado esta cerrado");
        assertThat(preparada.turnoEstado()).isEqualTo("C");
    }

    @Test
    @DisplayName("R-ACTA-041: con formularios abiertos el formulario queda bloqueado")
    void prepararFormularioConFormulariosAbiertos() {
        when(servicioEstadoDeForm.hayFormularioAbierto(SUCURSAL, TIPO, TURNO)).thenReturn(true);

        ActaPreparada preparada = service.prepararFormulario(contexto());

        assertThat(preparada.bloqueado()).isTrue();
        assertThat(preparada.mensajeBloqueo()).isNotBlank();
    }

    @Test
    @DisplayName("R-ACTA-041: sin bloqueos el formulario queda disponible con el numero sugerido")
    void prepararFormularioDisponible() {
        when(servicioNumeracionActa.proximoNumeroActa(SUCURSAL, TIPO)).thenReturn(4321);

        ActaPreparada preparada = service.prepararFormulario(contexto());

        assertThat(preparada.bloqueado()).isFalse();
        assertThat(preparada.mensajeBloqueo()).isNull();
        assertThat(preparada.f276IdSugerido()).isEqualTo(4321);
        assertThat(preparada.fechaMovimiento()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("R-TURNO-004: esTurnoAbierto devuelve true para un turno abierto")
    void esTurnoAbiertoAbierto() {
        Turno turno = turno("A");
        when(turnoRepositorio.findById(turno.getId())).thenReturn(Optional.of(turno));

        assertThat(service.esTurnoAbierto(SUCURSAL, TIPO, TURNO)).isTrue();
    }

    @Test
    @DisplayName("R-TURNO-004: esTurnoAbierto devuelve false para un turno cerrado")
    void esTurnoAbiertoCerrado() {
        Turno turno = turno("C");
        when(turnoRepositorio.findById(turno.getId())).thenReturn(Optional.of(turno));

        assertThat(service.esTurnoAbierto(SUCURSAL, TIPO, TURNO)).isFalse();
    }

    private RangoManualFormulario rangoManual(int ini, int cant, String tipo) {
        RangoManualFormulario r = new RangoManualFormulario();
        r.setSucNroId(SUCURSAL);
        r.setSucTipo(TIPO);
        r.setSucNuForm(2276);
        r.setForRecIni(ini);
        r.setForRecCant(cant);
        r.setForRecTipo(tipo);
        return r;
    }

    @Test
    @DisplayName("R-TURNO-010: validarNumeroManual dentro del rango (limites incluidos)")
    void validarNumeroManualDentro() {
        when(rangoManualRepositorio.findBySucursalYFormulario(SUCURSAL, TIPO, 2276))
                .thenReturn(List.of(rangoManual(100, 50, "ACTI")));

        assertThat(service.validarNumeroManual(SUCURSAL, TIPO, 100, "E")).isTrue();
        assertThat(service.validarNumeroManual(SUCURSAL, TIPO, 149, "E")).isTrue();
    }

    @Test
    @DisplayName("R-TURNO-010: validarNumeroManual fuera del rango (limites excluidos)")
    void validarNumeroManualFuera() {
        when(rangoManualRepositorio.findBySucursalYFormulario(SUCURSAL, TIPO, 2276))
                .thenReturn(List.of(rangoManual(100, 50, "ACTI")));

        assertThat(service.validarNumeroManual(SUCURSAL, TIPO, 99, "E")).isFalse();
        assertThat(service.validarNumeroManual(SUCURSAL, TIPO, 150, "E")).isFalse();
        assertThat(service.validarNumeroManual(SUCURSAL, TIPO, 0, "E")).isFalse();
    }

    @Test
    @DisplayName("R-TURNO-011: obtenerTipoActaManual deriva E para ACTI")
    void obtenerTipoActaManualActi() {
        when(rangoManualRepositorio.findBySucursalYFormulario(SUCURSAL, TIPO, 2276))
                .thenReturn(List.of(rangoManual(100, 50, "ACTI")));

        assertThat(service.obtenerTipoActaManual(SUCURSAL, TIPO, 100)).isEqualTo("E");
    }

    @Test
    @DisplayName("R-TURNO-011: obtenerTipoActaManual deriva S para ACTS")
    void obtenerTipoActaManualActs() {
        when(rangoManualRepositorio.findBySucursalYFormulario(SUCURSAL, TIPO, 2276))
                .thenReturn(List.of(rangoManual(100, 50, "ACTS")));

        assertThat(service.obtenerTipoActaManual(SUCURSAL, TIPO, 100)).isEqualTo("S");
    }

    @Test
    @DisplayName("R-TURNO-012/013: guardar en modo manual no incrementa GES_SUCN y persiste F276Id y TurNroId seleccionado")
    void guardarManualNoIncrementaNumeracion() {
        Turno turnoSel = new Turno(new ar.gob.chaco.atp.puestoscamineros.nucleo.domain.TurnoId(SUCURSAL, TIPO, 42));
        turnoSel.setTurEstado("A");
        when(turnoRepositorio.findById(turnoSel.getId())).thenReturn(Optional.of(turnoSel));
        when(rangoManualRepositorio.findBySucursalYFormulario(SUCURSAL, TIPO, 2276))
                .thenReturn(List.of(rangoManual(100, 50, "ACTS")));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto(10, 1)));
        when(vigenciaRepository.findFirstById_ProdCodAndId_ProdFchVigLessThanEqualOrderById_ProdFchVigDesc(
                eq(10), any())).thenReturn(Optional.of(vigencia(BigDecimal.TEN, BigDecimal.TEN,
                        BigDecimal.TEN, BigDecimal.TEN)));

        ActaAlta alta = new ActaAlta("F", "S", LocalDate.now(), CUIT, "VENDEDOR SA", "D", 1,
                CUIT_DEST, "DESTINATARIO SA", "D", 2, CUIT_TRAN, "TRANSPORTISTA SA", "D", 3, 1,
                "1", "AB123CD", "EF456GH", new BigDecimal("1"),
                List.of(new ActaAltaProducto(10, BigDecimal.ONE, BigDecimal.TEN)), 100, 42);

        ResultadoAltaActa resultado = service.guardar(alta, contexto(), true);

        assertThat(resultado.ok()).isTrue();
        assertThat(resultado.f276Id()).isEqualTo(100);

        ArgumentCaptor<Acta> cabecera = ArgumentCaptor.forClass(Acta.class);
        verify(actaRepository).save(cabecera.capture());
        Acta guardada = cabecera.getValue();
        assertThat(guardada.getF276Id()).isEqualTo(100);
        assertThat(guardada.getTurNroId()).isEqualTo(42);

        verify(servicioNumeracionActa, never()).siguienteNumeroActa(anyInt(), anyString());
    }

    /** Simula la respuesta del repositorio para el listado del turno. */
    private void stubListado(Acta... actas) {
        when(actaRepository.buscar(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any())).thenReturn(pagina(List.of(actas)));
    }
}
