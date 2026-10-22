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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    /**
     * <strong>Actualización diferencial</strong> del conjunto de vigencias (D-39).
     *
     * <p>El contrato observable no cambia: al terminar, el conjunto de vigencias del producto es
     * <strong>exactamente</strong> el enviado (R-PROD-21 / SA-P08). Lo que cambia es <strong>cómo</strong>
     * se llega a ese conjunto:
     *
     * <ul>
     *   <li>La vigencia <strong>existente</strong> con la misma {@code ProdFchVig} se <strong>actualiza
     *       in situ</strong>: se le escriben los 4 importes nuevos y <strong>se conservan</strong> las
     *       columnas legacy {@code ProdImpu5}, {@code ProdFeLog} y {@code ProdUsu} (R-036). Antes
     *       (D-39) la modificación hacía {@code clear()} + reinserción: <strong>borraba y recreaba</strong>
     *       las filas hijas, con lo que esas tres columnas quedaban en {@code NULL} y se perdían
     *       datos de auditoría que sí existen en {@code Rentas} (1116 filas con {@code ProdUsu},
     *       1116 con {@code ProdFeLog}, 764 con {@code ProdImpu5}).</li>
     *   <li>La vigencia existente cuya {@code ProdFchVig} <strong>no</strong> llega en el body se
     *       <strong>elimina</strong> (el {@code orphanRemoval} de la asociación la borra).</li>
     *   <li>La vigencia <strong>nueva</strong> se <strong>inserta</strong> con las columnas legacy en
     *       {@code NULL}: la escritura de auditoría de las filas nuevas pertenece al módulo
     *       {@code 03-login} (<strong>SA-P11</strong>).</li>
     * </ul>
     *
     * <p>Además, la actualización in situ se <strong>acerca al legacy</strong>: GeneXus actualiza la
     * fila hija cuyos valores cambian, en vez de borrarla y recrearla.
     *
     * <p>La colección administrada se reordena al <strong>orden enviado</strong> para que la
     * respuesta conserve la forma observable del contrato (el orden con que vuelve la lista de
     * vigencias del {@code PUT}).
     */
    private void reemplazarVigencias(Producto producto, List<ProductoVigenciaEscrituraDto> vigencias) {
        // 1) indice de las vigencias existentes por ProdFchVig (la PK de PRODUCT1 es la fecha).
        Map<LocalDateTime, ProductoVigencia> existentes = new HashMap<>();
        for (ProductoVigencia vigente : producto.getVigencias()) {
            existentes.put(vigente.getProdFchVig(), vigente);
        }

        // 2) actualizar in situ las que coinciden e insertar las nuevas, en el orden enviado.
        List<ProductoVigencia> ordenEnviado = new ArrayList<>(vigencias.size());
        Set<LocalDateTime> fechasEnviadas = new HashSet<>();
        for (ProductoVigenciaEscrituraDto v : vigencias) {
            fechasEnviadas.add(v.prodFchVig());
            ProductoVigencia entidad = existentes.get(v.prodFchVig());
            if (entidad == null) {
                // Vigencia nueva: la referencia al padre completa el id derivado (@MapsId prodCod).
                entidad = new ProductoVigencia(v.prodFchVig());
                producto.addVigencia(entidad);
            }
            entidad.setProdImpu1(nvl(v.prodImpu1()));
            entidad.setProdImpu2(nvl(v.prodImpu2()));
            entidad.setProdImpu3(nvl(v.prodImpu3()));
            entidad.setProdImpu4(nvl(v.prodImpu4()));
            // ProdImpu5 / ProdFeLog / ProdUsu NO se tocan: se conservan (D-39, R-036).
            ordenEnviado.add(entidad);
        }

        // 3) eliminar las que no vinieron (orphanRemoval de la asociacion).
        for (ProductoVigencia vigente : new ArrayList<>(producto.getVigencias())) {
            if (!fechasEnviadas.contains(vigente.getProdFchVig())) {
                producto.getVigencias().remove(vigente);
            }
        }

        // 4) dejar la coleccion en el orden enviado (misma forma observable del contrato).
        reordenar(producto.getVigencias(), ordenEnviado);
    }

    /**
     * Reordena la coleccion administrada para que quede en el orden enviado. Solo mueve instancias
     * ya presentes: no agrega ni quita elementos, de modo que el calculo de filas orphans de
     * Hibernate no cambia.
     */
    private void reordenar(List<ProductoVigencia> coleccion, List<ProductoVigencia> orden) {
        if (coleccion.size() != orden.size()) {
            return;
        }
        for (int i = 0; i < orden.size(); i++) {
            if (coleccion.get(i) == orden.get(i)) {
                continue;
            }
            int j = coleccion.indexOf(orden.get(i));
            if (j < 0) {
                continue;
            }
            ProductoVigencia elemento = coleccion.remove(j);
            coleccion.add(i, elemento);
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
