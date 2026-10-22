package ar.com.atp.puestos.nucleo.service;

import ar.com.atp.puestos.nucleo.domain.Producto;
import ar.com.atp.puestos.nucleo.domain.ProductoVigencia;
import ar.com.atp.puestos.nucleo.repository.ProductoRepository;
import ar.com.atp.puestos.nucleo.repository.ProductoVigenciaRepository;
import ar.com.atp.puestos.nucleo.web.dto.ImportesVigentesDto;
import ar.com.atp.puestos.nucleo.web.dto.ProductoDto;
import ar.com.atp.puestos.nucleo.web.dto.ProductoEscrituraDto;
import ar.com.atp.puestos.nucleo.web.dto.ProductoVigenciaEscrituraDto;
import ar.com.atp.puestos.shared.error.ConflictException;
import ar.com.atp.puestos.shared.error.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Productos (transacción de dos niveles, R-004, R-005) y su ABM atómico (R-034, R-035).
 * Las fórmulas {@code ProdIMax1..4} se calculan al vuelo y no se persisten (R-005).
 */
@Service
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoVigenciaRepository vigenciaRepository;

    public ProductoService(ProductoRepository productoRepository,
            ProductoVigenciaRepository vigenciaRepository) {
        this.productoRepository = productoRepository;
        this.vigenciaRepository = vigenciaRepository;
    }

    /**
     * Listado de productos con sus vigencias anidadas.
     *
     * <p><strong>T-103</strong>: ambas rutas cargan las vigencias con un {@code left join fetch}
     * (una sola consulta). Antes, la asociación {@code LAZY} se inicializaba con una consulta por
     * producto (244 productos ⇒ ~245 consultas). El contenido y el orden del resultado no cambian.
     */
    public List<ProductoDto> listarProductos(Boolean anulados) {
        List<Producto> productos = (anulados == null)
                ? productoRepository.findAllConVigencias()
                : productoRepository.findByProdAnulaOrderByProdDescAsc(anulados ? 1 : 0);
        return productos.stream().map(NucleoMapper::toDto).toList();
    }

    public ProductoDto obtenerProducto(Integer prodCod) {
        return NucleoMapper.toDto(buscarProducto(prodCod));
    }

    /**
     * Fórmulas {@code ProdIMax1..4} (R-005): importe de la última vigencia con fecha ≤ hoy; {@code 0}
     * si no hay ninguna. El corte se hace a fin del día para respetar el {@code CAST(GETDATE() AS date)}
     * legacy sobre columnas {@code datetime}.
     */
    public ImportesVigentesDto importesVigentes(Integer prodCod) {
        buscarProducto(prodCod); // valida existencia → 404 si no existe
        ProductoVigencia vigente = vigenciaRepository
                .findFirstById_ProdCodAndId_ProdFchVigLessThanEqualOrderById_ProdFchVigDesc(
                        prodCod, LocalDate.now().atTime(LocalTime.MAX))
                .orElse(null);
        return new ImportesVigentesDto(
                prodCod,
                importe(vigente, 1),
                importe(vigente, 2),
                importe(vigente, 3),
                importe(vigente, 4));
    }

    @Transactional
    public ProductoDto crearProducto(ProductoEscrituraDto dto) {
        if (productoRepository.existsById(dto.prodCod())) {
            throw new ConflictException("PK_DUPLICADA",
                    "Ya existe un producto con ProdCod " + dto.prodCod() + ".");
        }
        validarVigenciasUnicas(dto.vigencias());
        Producto producto = new Producto(dto.prodCod(), dto.prodDesc());
        aplicarCampos(producto, dto);
        reemplazarVigencias(producto, dto.vigencias());
        return NucleoMapper.toDto(productoRepository.save(producto));
    }

    @Transactional
    public ProductoDto actualizarProducto(Integer prodCod, ProductoEscrituraDto dto) {
        Producto producto = buscarProducto(prodCod);
        validarVigenciasUnicas(dto.vigencias());
        producto.setProdDesc(dto.prodDesc());
        aplicarCampos(producto, dto);
        reemplazarVigencias(producto, dto.vigencias());
        return NucleoMapper.toDto(productoRepository.save(producto));
    }

    @Transactional
    public void eliminarProducto(Integer prodCod) {
        Producto producto = buscarProducto(prodCod);
        productoRepository.delete(producto); // el cascade borra PRODUCT1
    }

    private Producto buscarProducto(Integer prodCod) {
        return productoRepository.findById(prodCod)
                .orElseThrow(() -> new NotFoundException("NO_ENCONTRADO",
                        "Producto " + prodCod + " no encontrado."));
    }

    /**
     * Aplica los campos de la escritura sobre la entidad raíz.
     *
     * <p><strong>T-110 (verificado, sin cambio de comportamiento)</strong>:
     * <ul>
     *   <li>{@code uMedNombre} <strong>no</strong> se persiste: {@code PRODUCTO} no tiene la
     *       columna (G-DATA) ⇒ en la entidad es {@code @Transient} y queda {@code null}
     *       (R-PROD-19, SA-P01).</li>
     *   <li>{@code ProdIMax1..4} <strong>no</strong> se persisten: son fórmulas de la KB
     *       ({@code @Transient}); se calculan al vuelo en
     *       {@link #importesVigentes(Integer)} (R-005, R-PROD-03).</li>
     *   <li>Los defaults {@code 0} de las columnas {@code NOT NULL} sin contraparte en el DTO
     *       ({@code ProdTipCal}, {@code TPPriID}, {@code ProdAnula}) se siguen aplicando cuando
     *       el cliente omite el campo (R-PROD-29, plan §2.4).</li>
     * </ul>
     */
    private void aplicarCampos(Producto producto, ProductoEscrituraDto dto) {
        producto.setProdTipCal(dto.prodTipCal() != null ? dto.prodTipCal() : 0);
        producto.setTpPriID(dto.tpPriID() != null ? dto.tpPriID() : 0);
        producto.setUMedNroId(dto.uMedNroId());
        producto.setProdAcop(dto.prodAcop());
        producto.setProdAnula(dto.prodAnula() != null ? dto.prodAnula() : 0);
    }

    /** Reemplaza el conjunto de vigencias manteniendo ambos lados de la asociación (R-004, R-034). */
    private void reemplazarVigencias(Producto producto, List<ProductoVigenciaEscrituraDto> vigencias) {
        producto.getVigencias().clear(); // orphanRemoval elimina las ausentes
        for (ProductoVigenciaEscrituraDto v : vigencias) {
            ProductoVigencia entidad = new ProductoVigencia(v.prodFchVig());
            entidad.setProdImpu1(nvl(v.prodImpu1()));
            entidad.setProdImpu2(nvl(v.prodImpu2()));
            entidad.setProdImpu3(nvl(v.prodImpu3()));
            entidad.setProdImpu4(nvl(v.prodImpu4()));
            producto.addVigencia(entidad);
        }
    }

    /** Unicidad de {@code ProdFchVig} dentro del producto (R-034); duplicado → 409. */
    private void validarVigenciasUnicas(List<ProductoVigenciaEscrituraDto> vigencias) {
        Set<Object> fechas = new HashSet<>();
        for (ProductoVigenciaEscrituraDto v : vigencias) {
            if (!fechas.add(v.prodFchVig())) {
                throw new ConflictException("VIGENCIA_DUPLICADA",
                        "La fecha de vigencia " + v.prodFchVig() + " está repetida.");
            }
        }
    }

    private static BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private static BigDecimal importe(ProductoVigencia v, int nro) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal valor = switch (nro) {
            case 1 -> v.getProdImpu1();
            case 2 -> v.getProdImpu2();
            case 3 -> v.getProdImpu3();
            case 4 -> v.getProdImpu4();
            default -> null;
        };
        // T-111 / D-38: la columna fisica es `money` (escala 4) y el tipo logico es
        // NUMERIC(12,2): el derivado se expone ya con escala 2.
        BigDecimal normalizado = ImporteNormalizador.aEscalaLogica(valor);
        // R-005: el derivado vale 0 cuando no hay importe aplicable (tampoco hay vigencia
        // vigente). El `null` de la columna se traduce a 0, como ya hacia el legacy.
        return normalizado != null ? normalizado : BigDecimal.ZERO;
    }
}
