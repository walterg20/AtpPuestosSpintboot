package ar.gob.chaco.atp.puestoscamineros.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.gob.chaco.atp.puestoscamineros.nucleo.domain.Producto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.ProductoRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.repository.ProductoVigenciaRepository;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoEscrituraDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoVigenciaEscrituraDto;
import jakarta.persistence.Transient;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
 * <strong>T-110</strong>: verificaciÃ³n de quÃ© persiste el servicio y quÃ© NO.
 *
 * <ul>
 *   <li>{@code uMedNombre} no se persiste: {@code PRODUCTO} no tiene la columna â‡’ el atributo
 *       es {@code @Transient} y queda {@code null} aunque el cliente lo envÃ­e (R-PROD-19).</li>
 *   <li>{@code ProdIMax1..4} no se persisten: son fÃ³rmulas de la KB ({@code @Transient}); se
 *       calculan al vuelo (R-005, R-PROD-03).</li>
 *   <li>Los defaults {@code 0} de {@code ProdTipCal}, {@code TPPriID} y {@code ProdAnula} siguen
 *       vigentes cuando el cliente omite esos campos (R-PROD-29, plan Â§2.4).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ProductoEscrituraServicioTest {

    private static final Integer PROD_COD = 7;

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
    @DisplayName("T-110: los defaults 0 de ProdTipCal / TPPriID / ProdAnula se siguen aplicando")
    void defaultsDeColumnasNotNullSiguenVigentes() {
        ProductoEscrituraDto dto = new ProductoEscrituraDto(
                PROD_COD, "PRODUCTO TEST", null, null, "UN", null, "N", null, List.of());

        service.crearProducto(dto);

        Producto persistido = productoGuardado();
        assertThat(persistido.getProdTipCal()).as("ProdTipCal por defecto").isZero();
        assertThat(persistido.getTpPriID()).as("TPPriID por defecto").isZero();
        assertThat(persistido.getProdAnula()).as("ProdAnula por defecto").isZero();
        assertThat(persistido.getUMedNroId()).isEqualTo("UN");
        assertThat(persistido.getProdAcop()).isEqualTo("N");
    }

    @Test
    @DisplayName("T-110: los valores enviados se respetan cuando vienen informados")
    void valoresInformadosSeRespetan() {
        ProductoEscrituraDto dto = new ProductoEscrituraDto(
                PROD_COD, "PRODUCTO TEST", 1, 3, "KG", null, "S", 1, List.of());

        service.crearProducto(dto);

        Producto persistido = productoGuardado();
        assertThat(persistido.getProdTipCal()).isEqualTo(1);
        assertThat(persistido.getTpPriID()).isEqualTo(3);
        assertThat(persistido.getProdAnula()).isEqualTo(1);
    }

    @Test
    @DisplayName("T-110: uMedNombre no se persiste (la entidad lo ignora aunque el DTO lo traiga)")
    void uMedNombreNoSePersiste() throws Exception {
        ProductoEscrituraDto dto = new ProductoEscrituraDto(
                PROD_COD, "PRODUCTO TEST", 1, 1, "KG", "KILOGRAMO", "N", 0, List.of());

        service.crearProducto(dto);

        Producto persistido = productoGuardado();
        assertThat(persistido.getUMedNombre()).as("UMedNombre nunca se escribe en la entidad").isNull();
        assertThat(campoEsTransient("uMedNombre"))
                .as("uMedNombre debe estar marcado @Transient (PRODUCTO no tiene la columna)")
                .isTrue();
    }

    @Test
    @DisplayName("T-110: ProdIMax1..4 no se persisten (son fÃ³rmulas de la KB)")
    void derivadosNoSePersisten() throws Exception {
        ProductoEscrituraDto dto = new ProductoEscrituraDto(
                PROD_COD, "PRODUCTO TEST", 1, 1, "KG", null, "N", 0, List.of(vigencia()));

        service.crearProducto(dto);

        Producto persistido = productoGuardado();
        assertThat(persistido.getProdIMax1()).isNull();
        assertThat(persistido.getProdIMax2()).isNull();
        assertThat(persistido.getProdIMax3()).isNull();
        assertThat(persistido.getProdIMax4()).isNull();
        assertThat(campoEsTransient("prodIMax1")).isTrue();
        assertThat(campoEsTransient("prodIMax2")).isTrue();
        assertThat(campoEsTransient("prodIMax3")).isTrue();
        assertThat(campoEsTransient("prodIMax4")).isTrue();
    }

    @Test
    @DisplayName("T-110: la vigencia escrita conserva sus 4 importes y su fecha")
    void laVigenciaSePersisteCompleta() {
        ProductoEscrituraDto dto = new ProductoEscrituraDto(
                PROD_COD, "PRODUCTO TEST", 1, 1, "KG", null, "N", 0, List.of(vigencia()));

        service.crearProducto(dto);

        Producto persistido = productoGuardado();
        assertThat(persistido.getVigencias()).hasSize(1);
        var vigencia = persistido.getVigencias().get(0);
        assertThat(vigencia.getProdFchVig()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        assertThat(vigencia.getProdImpu1()).isEqualByComparingTo("100.00");
        assertThat(vigencia.getProdImpu2()).isEqualByComparingTo("200.00");
        assertThat(vigencia.getProdImpu3()).isEqualByComparingTo("300.00");
        assertThat(vigencia.getProdImpu4()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("T-110: el servicio no escribe columnas legacy (ProdReppa, ProdImpu5, ProdFeLog, ProdUsu)")
    void noSeEscribenColumnasLegacy() {
        ProductoEscrituraDto dto = new ProductoEscrituraDto(
                PROD_COD, "PRODUCTO TEST", 1, 1, "KG", null, "N", 0, List.of(vigencia()));

        service.crearProducto(dto);

        Producto persistido = productoGuardado();
        assertThat(persistido.getProdReppa()).isNull();
        var vigencia = persistido.getVigencias().get(0);
        assertThat(vigencia.getProdImpu5()).isNull();
        assertThat(vigencia.getProdFeLog()).isNull();
        assertThat(vigencia.getProdUsu()).isNull();
    }

    // ------------------------------------------------------------ helpers

    /** Captura la entidad que el servicio intentÃ³ guardar. */
    private Producto productoGuardado() {
        verify(productoRepository).save(productoCaptor.capture());
        return productoCaptor.getValue();
    }

    private static ProductoVigenciaEscrituraDto vigencia() {
        return new ProductoVigenciaEscrituraDto(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                new BigDecimal("100.00"),
                new BigDecimal("200.00"),
                new BigDecimal("300.00"),
                new BigDecimal("400.00"));
    }

    private static boolean campoEsTransient(String nombre) throws Exception {
        Field campo = Producto.class.getDeclaredField(nombre);
        return campo.isAnnotationPresent(Transient.class);
    }
}
