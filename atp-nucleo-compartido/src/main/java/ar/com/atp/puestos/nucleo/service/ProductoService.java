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

    public List<ProductoDto> listarProductos(Boolean anulados) {
        List<Producto> productos = (anulados == null)
                ? productoRepository.findAll()
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
        return valor != null ? valor : BigDecimal.ZERO;
    }
}
