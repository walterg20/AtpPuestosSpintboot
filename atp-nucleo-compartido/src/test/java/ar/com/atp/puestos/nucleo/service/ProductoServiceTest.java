package ar.com.atp.puestos.nucleo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.com.atp.puestos.nucleo.domain.Producto;
import ar.com.atp.puestos.nucleo.domain.ProductoVigencia;
import ar.com.atp.puestos.nucleo.repository.ProductoRepository;
import ar.com.atp.puestos.nucleo.repository.ProductoVigenciaRepository;
import ar.com.atp.puestos.nucleo.web.dto.ImportesVigentesDto;
import ar.com.atp.puestos.shared.error.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * T-401 (R-005): fórmulas {@code ProdIMax1..4} de {@code ProductoService.importesVigentes}.
 *
 * <p>Test unitario puro con Mockito: el repositorio de vigencias se sustituye por un {@code
 * thenAnswer} que reproduce la consulta derivada
 * ({@code ProdFchVig <= corte}, orden descendente, toma la primera), de modo que la lógica del
 * servicio (elección de la vigencia y conversión {@code null → 0}) queda aislada de SQL Server.
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    private static final Integer PROD_COD = 7;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProductoVigenciaRepository vigenciaRepository;

    @InjectMocks
    private ProductoService service;

    @Captor
    private ArgumentCaptor<LocalDateTime> corteCaptor;

    // ------------------------------------------------------------------ R-005

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("casosVigencia")
    @DisplayName("R-005: importe = última vigencia con fecha <= fin de hoy; 0 si no hay")
    void importesVigentes_aplica_ultima_vigencia_o_cero(
            String escenario, List<ProductoVigencia> vigencias, String e1, String e2, String e3, String e4) {
        when(productoRepository.findById(PROD_COD)).thenReturn(Optional.of(producto()));
        stubVigencia(vigencias);

        ImportesVigentesDto dto = service.importesVigentes(PROD_COD);

        assertThat(dto.prodCod()).isEqualTo(PROD_COD);
        assertThat(dto.prodIMax1()).isEqualByComparingTo(e1);
        assertThat(dto.prodIMax2()).isEqualByComparingTo(e2);
        assertThat(dto.prodIMax3()).isEqualByComparingTo(e3);
        assertThat(dto.prodIMax4()).isEqualByComparingTo(e4);
    }

    static Stream<Arguments> casosVigencia() {
        LocalDateTime hoy = LocalDate.now().atTime(12, 0);
        LocalDateTime hace10 = LocalDate.now().minusDays(10).atTime(12, 0);
        LocalDateTime ayer = LocalDate.now().minusDays(1).atTime(12, 0);
        LocalDateTime manana = LocalDate.now().plusDays(1).atTime(12, 0);
        return Stream.of(
                Arguments.of("sin vigencias → todo 0", List.of(), "0", "0", "0", "0"),
                Arguments.of("solo una vigencia pasada", List.of(vig(hace10, "10", "20", "30", "40")),
                        "10", "20", "30", "40"),
                Arguments.of("varias pasadas: gana la más reciente",
                        List.of(vig(hace10, "10", "20", "30", "40"), vig(ayer, "50", "60", "70", "80")),
                        "50", "60", "70", "80"),
                Arguments.of("vigencia de hoy entra",
                        List.of(vig(hace10, "1", "1", "1", "1"), vig(hoy, "99", "98", "97", "96")),
                        "99", "98", "97", "96"),
                Arguments.of("solo futuras → todo 0", List.of(vig(manana, "1", "2", "3", "4")),
                        "0", "0", "0", "0"),
                Arguments.of("futura excluida frente a hoy",
                        List.of(vig(hoy, "11", "12", "13", "14"), vig(manana, "21", "22", "23", "24")),
                        "11", "12", "13", "14"),
                Arguments.of("importes nulos → 0", List.of(vig(hoy, null, null, null, null)),
                        "0", "0", "0", "0"),
                Arguments.of("importes parcialmente nulos",
                        List.of(vig(hoy, "11", null, "13", null)), "11", "0", "13", "0"));
    }

    @Test
    @DisplayName("R-005: el corte es fin de hoy (LocalTime.MAX) sobre fecha de hoy")
    void importesVigentes_usa_fin_de_hoy_como_corte() {
        when(productoRepository.findById(PROD_COD)).thenReturn(Optional.of(producto()));
        stubVigencia(List.of());

        service.importesVigentes(PROD_COD);

        verify(vigenciaRepository)
                .findFirstById_ProdCodAndId_ProdFchVigLessThanEqualOrderById_ProdFchVigDesc(
                        eq(PROD_COD), corteCaptor.capture());
        LocalDateTime corte = corteCaptor.getValue();
        assertThat(corte.toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(corte.toLocalTime()).isEqualTo(LocalTime.MAX);
    }

    @Test
    @DisplayName("R-005: producto inexistente → 404 y no consulta vigencias")
    void importesVigentes_productoInexistente_lanza404() {
        when(productoRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.importesVigentes(99))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");

        verifyNoInteractions(vigenciaRepository);
    }

    // --------------------------------------------------------------- helpers

    /** Reproduce la consulta derivada de Spring Data sin tocar la base de datos. */
    private void stubVigencia(List<ProductoVigencia> vigencias) {
        when(vigenciaRepository
                .findFirstById_ProdCodAndId_ProdFchVigLessThanEqualOrderById_ProdFchVigDesc(
                        eq(PROD_COD), any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    LocalDateTime corte = invocation.getArgument(1);
                    return vigencias.stream()
                            .filter(v -> !v.getProdFchVig().isAfter(corte))
                            .max(Comparator.comparing(ProductoVigencia::getProdFchVig));
                });
    }

    private static Producto producto() {
        return new Producto(PROD_COD, "PRODUCTO TEST");
    }

    private static ProductoVigencia vig(LocalDateTime fecha, String i1, String i2, String i3, String i4) {
        ProductoVigencia v = new ProductoVigencia(fecha);
        v.setProdImpu1(dec(i1));
        v.setProdImpu2(dec(i2));
        v.setProdImpu3(dec(i3));
        v.setProdImpu4(dec(i4));
        return v;
    }

    private static BigDecimal dec(String valor) {
        return valor == null ? null : new BigDecimal(valor);
    }
}
