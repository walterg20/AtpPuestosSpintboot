package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.com.atp.puestos.nucleo.domain.Producto;
import ar.com.atp.puestos.nucleo.domain.ProductoVigencia;
import ar.com.atp.puestos.nucleo.repository.ProductoRepository;
import ar.com.atp.puestos.nucleo.repository.ProductoVigenciaRepository;
import ar.com.atp.puestos.nucleo.web.dto.ProductoEscrituraDto;
import ar.com.atp.puestos.nucleo.web.dto.ProductoVigenciaEscrituraDto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <strong>T-112</strong> (unitario): <strong>actualización diferencial</strong> de las vigencias
 * (<strong>D-39</strong>).
 *
 * <p>Reproduce el defecto que se cierra sin tocar la base: la modificación anterior hacía
 * {@code clear()} + reinserción, con lo que las columnas legacy {@code ProdUsu}, {@code ProdFeLog} y
 * {@code ProdImpu5} de las filas existentes quedaban en {@code NULL} (la fila se borraba y se
 * recreaba). Con D-39:
 *
 * <ul>
 *   <li>la vigencia existente con la misma {@code ProdFchVig} se <strong>actualiza in situ</strong> y
 *       <strong>conserva</strong> {@code ProdUsu}, {@code ProdFeLog} y {@code ProdImpu5};</li>
 *   <li>la vigencia existente que no viene en el body se <strong>elimina</strong> de la colección
 *       (el {@code orphanRemoval} la borra en la base);</li>
 *   <li>la vigencia <strong>nueva</strong> se <strong>inserta</strong> con las columnas legacy en
 *       {@code NULL} (SA-P11: la auditoría de las filas nuevas es de {@code 03-login});</li>
 *   <li>el conjunto final es <strong>exactamente</strong> el enviado, en el orden enviado
 *       (R-PROD-21 / SA-P08).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ProductoVigenciaDiferencialTest {

    private static final Integer PROD_COD = 7;

    private static final LocalDateTime F1 = LocalDateTime.of(2011, 3, 12, 0, 0);
    private static final LocalDateTime F2 = LocalDateTime.of(2017, 12, 15, 0, 0);
    private static final LocalDateTime F3 = LocalDateTime.of(2018, 5, 17, 0, 0);
    private static final LocalDateTime F_NUEVA = LocalDateTime.of(2018, 4, 24, 0, 0);

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProductoVigenciaRepository vigenciaRepository;

    @InjectMocks
    private ProductoService service;

    @Captor
    private ArgumentCaptor<Producto> productoCaptor;

    @BeforeEach
    void elMockDeSaveDevuelveLaEntidadRecibida() {
        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("T-112/D-39: la vigencia existente conserva ProdUsu, ProdFeLog y ProdImpu5")
    void laVigenciaExistenteConservaLasColumnasLegacy() {
        Producto producto = productoConTresVigenciasDeAuditoria();
        when(productoRepository.findById(PROD_COD)).thenReturn(Optional.of(producto));

        service.actualizarProducto(PROD_COD, cuerpoConF1Modificada());

        Producto persistido = productoGuardado();
        ProductoVigencia vigencia = vigenciaDe(persistido, F1);
        assertThat(vigencia.getProdUsu()).as("ProdUsu se conserva").isEqualTo("SEBAM");
        assertThat(vigencia.getProdFeLog()).as("ProdFeLog se conserva")
                .isEqualTo(LocalDateTime.of(2011, 3, 11, 11, 15, 57));
        assertThat(vigencia.getProdImpu5()).as("ProdImpu5 se conserva")
                .isEqualByComparingTo(new BigDecimal("0.0000"));
        // Los 4 importes del body se escriben.
        assertThat(vigencia.getProdImpu1()).as("ProdImpu1 nuevo").isEqualByComparingTo("19.00");
        assertThat(vigencia.getProdImpu2()).as("ProdImpu2 nuevo").isEqualByComparingTo("2.00");
        assertThat(vigencia.getProdImpu3()).as("ProdImpu3 nuevo").isEqualByComparingTo("3.00");
        assertThat(vigencia.getProdImpu4()).as("ProdImpu4 nuevo").isEqualByComparingTo("4.00");
    }

    @Test
    @DisplayName("T-112/D-39: la vigencia que no viene en el body se elimina de la coleccion")
    void laVigenciaQueNoVieneSeElimina() {
        Producto producto = productoConTresVigenciasDeAuditoria();
        when(productoRepository.findById(PROD_COD)).thenReturn(Optional.of(producto));

        service.actualizarProducto(PROD_COD, cuerpoConF1Modificada());

        Producto persistido = productoGuardado();
        assertThat(persistido.getVigencias())
                .as("la vigencia quitada no debe quedar en la coleccion (orphanRemoval la borra)")
                .extracting(ProductoVigencia::getProdFchVig)
                .doesNotContain(F2);
    }

    @Test
    @DisplayName("T-112/D-39: la vigencia nueva se inserta con las columnas legacy en NULL (SA-P11)")
    void laVigenciaNuevaSeInsertaSinColumnasLegacy() {
        Producto producto = productoConTresVigenciasDeAuditoria();
        when(productoRepository.findById(PROD_COD)).thenReturn(Optional.of(producto));

        service.actualizarProducto(PROD_COD, cuerpoConF1Modificada());

        Producto persistido = productoGuardado();
        ProductoVigencia nueva = vigenciaDe(persistido, F_NUEVA);
        assertThat(nueva).as("la vigencia nueva debe estar en la coleccion").isNotNull();
        assertThat(nueva.getProdUsu()).as("ProdUsu de la nueva").isNull();
        assertThat(nueva.getProdFeLog()).as("ProdFeLog de la nueva").isNull();
        assertThat(nueva.getProdImpu5()).as("ProdImpu5 de la nueva").isNull();
        assertThat(nueva.getProdImpu1()).as("ProdImpu1 de la nueva").isEqualByComparingTo("11.11");
        assertThat(nueva.getProducto()).as("la vigencia nueva referencia al padre").isSameAs(persistido);
        assertThat(nueva.getProdCod()).as("el id derivado se completa con el prodCod del padre")
                .isEqualTo(PROD_COD);
    }

    @Test
    @DisplayName("T-112/D-39: el conjunto final es exactamente el enviado, en el orden enviado")
    void elConjuntoFinalEsExactamenteElEnviado() {
        Producto producto = productoConTresVigenciasDeAuditoria();
        when(productoRepository.findById(PROD_COD)).thenReturn(Optional.of(producto));

        service.actualizarProducto(PROD_COD, cuerpoConF1Modificada());

        Producto persistido = productoGuardado();
        assertThat(persistido.getVigencias())
                .as("el conjunto final de vigencias (R-PROD-21 / SA-P08)")
                .extracting(ProductoVigencia::getProdFchVig)
                .containsExactly(F1, F_NUEVA, F3);
    }

    @Test
    @DisplayName("T-112/D-39: las vigencias no tocadas conservan sus columnas legacy")
    void lasVigenciasNoTocadasConservanSusColumnasLegacy() {
        Producto producto = productoConTresVigenciasDeAuditoria();
        when(productoRepository.findById(PROD_COD)).thenReturn(Optional.of(producto));

        service.actualizarProducto(PROD_COD, cuerpoConF1Modificada());

        Producto persistido = productoGuardado();
        ProductoVigencia intacta = vigenciaDe(persistido, F3);
        assertThat(intacta.getProdUsu()).as("ProdUsu intacta").isEqualTo("SEBAM");
        assertThat(intacta.getProdFeLog()).as("ProdFeLog intacta")
                .isEqualTo(LocalDateTime.of(2018, 5, 16, 9, 54, 14));
        assertThat(intacta.getProdImpu1()).as("ProdImpu1 intacto").isEqualByComparingTo("60.00");
    }

    // ------------------------------------------------------------ helpers

    /** Captura la entidad que el servicio intent\u00f3 guardar. */
    private Producto productoGuardado() {
        verify(productoRepository).save(productoCaptor.capture());
        return productoCaptor.getValue();
    }

    /** Producto con 3 vigencias reales (con las columnas legacy cargadas, como en la base). */
    private static Producto productoConTresVigenciasDeAuditoria() {
        Producto producto = new Producto(PROD_COD, "PRODUCTO TEST");
        producto.addVigencia(vigenciaExistente(F1, "18.0000", "SEBAM",
                LocalDateTime.of(2011, 3, 11, 11, 15, 57), "0.0000"));
        producto.addVigencia(vigenciaExistente(F2, "150.0000", "SEBAM",
                LocalDateTime.of(2017, 12, 13, 11, 22, 41), "0.0000"));
        producto.addVigencia(vigenciaExistente(F3, "60.0000", "SEBAM",
                LocalDateTime.of(2018, 5, 16, 9, 54, 14), "0.0000"));
        return producto;
    }

    /** Vigencia ya persistida, con las columnas legacy cargadas. */
    private static ProductoVigencia vigenciaExistente(LocalDateTime fecha, String impu1, String usu,
            LocalDateTime felog, String impu5) {
        ProductoVigencia v = new ProductoVigencia(fecha);
        v.setProdImpu1(new BigDecimal(impu1));
        v.setProdImpu2(BigDecimal.ZERO);
        v.setProdImpu3(BigDecimal.ZERO);
        v.setProdImpu4(BigDecimal.ZERO);
        v.setProdUsu(usu);
        v.setProdFeLog(felog);
        if (impu5 != null) {
            v.setProdImpu5(new BigDecimal(impu5));
        }
        return v;
    }

    /**
     * El body: {@code F1} con importes nuevos, {@code F2} <strong>no</strong> viene (se elimina),
     * {@code F3} intacta y {@code F_NUEVA} nueva (entre {@code F1} y {@code F3}).
     */
    private static ProductoEscrituraDto cuerpoConF1Modificada() {
        List<ProductoVigenciaEscrituraDto> vigencias = List.of(
                new ProductoVigenciaEscrituraDto(F1,
                        new BigDecimal("19.00"), new BigDecimal("2.00"),
                        new BigDecimal("3.00"), new BigDecimal("4.00")),
                new ProductoVigenciaEscrituraDto(F_NUEVA,
                        new BigDecimal("11.11"), new BigDecimal("22.22"),
                        new BigDecimal("33.33"), new BigDecimal("44.44")),
                new ProductoVigenciaEscrituraDto(F3,
                        new BigDecimal("60.00"), BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO));
        return new ProductoEscrituraDto(PROD_COD, "PRODUCTO TEST", 1, 3, "TON", null, "N", 0, vigencias);
    }

    /** La vigencia de la fecha pedida dentro de la coleccion persistida. */
    private static ProductoVigencia vigenciaDe(Producto producto, LocalDateTime fecha) {
        return producto.getVigencias().stream()
                .filter(v -> fecha.equals(v.getProdFchVig()))
                .findFirst()
                .orElse(null);
    }
}
