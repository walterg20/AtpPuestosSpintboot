package ar.com.atp.puestos.nucleo.web.mvc;

import ar.com.atp.puestos.nucleo.service.ProductoService;
import ar.com.atp.puestos.nucleo.web.dto.ImportesVigentesDto;
import ar.com.atp.puestos.nucleo.web.dto.ProductoDto;
import ar.com.atp.puestos.nucleo.web.dto.ProductoEscrituraDto;
import ar.com.atp.puestos.nucleo.web.mvc.producto.FiltroAnulados;
import ar.com.atp.puestos.nucleo.web.mvc.producto.ModoProducto;
import ar.com.atp.puestos.nucleo.web.mvc.producto.ProductoFilaView;
import ar.com.atp.puestos.nucleo.web.mvc.producto.ProductoForm;
import ar.com.atp.puestos.nucleo.web.mvc.producto.ProductoMapper;
import ar.com.atp.puestos.shared.error.ConflictException;
import ar.com.atp.puestos.shared.error.NotFoundException;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Pantalla del ABM de Productos (nivel 0 + nivel 1) en Thymeleaf.
 *
 * <p>Rutas (plan §4.1): {@code GET /productos}, {@code GET /productos?prodCod=…},
 * {@code GET /productos?anulados=…}, {@code POST /productos} (alta),
 * {@code POST /productos/{prodCod}} (modificación) y {@code POST /productos/{prodCod}/eliminar}
 * (baja). La vista es {@code nucleo/productos}.
 *
 * <p>La pantalla es 100 % servidor-rendered: el estado (selección, filtro, modo) viaja en la URL
 * (D-28). El controller <strong>no</strong> invoca la API REST por HTTP: usa el mismo
 * {@link ProductoService} que {@code web/rest} (D-29, G7).
 *
 * <p>Las reglas de negocio viven en el servicio; este adapter sólo mapea la ruta, invoca el
 * servicio, arma el view model (plan §4.2) y devuelve el nombre de la vista.
 */
@Controller
@RequestMapping("/productos")
public class ProductoMvcController {

    /** Nombre de la vista Thymeleaf de la pantalla. */
    private static final String VISTA = "nucleo/productos";

    private static final String RUTA_ALTA = "/productos";

    private static final Logger log = LoggerFactory.getLogger(ProductoMvcController.class);

    private final ProductoService productoService;

    public ProductoMvcController(ProductoService productoService) {
        this.productoService = productoService;
    }

    /**
     * Colección + formulario (ALTA o EDICIÓN según {@code prodCod}).
     */
    @GetMapping
    public String listar(@RequestParam(name = "prodCod", required = false) Integer prodCod,
            @RequestParam(name = "anulados", required = false) Boolean anulados,
            Model model) {
        FiltroAnulados filtro = FiltroAnulados.deParametro(anulados);
        List<ProductoDto> productos;
        try {
            productos = productoService.listarProductos(filtro.parametro());
        } catch (RuntimeException ex) {
            log.warn("No se pudo cargar la colección de productos", ex);
            poblar(model, List.of(), new ProductoForm(), ModoProducto.ALTA, null, filtro,
                    "No se pudo cargar la colección de productos.", false);
            return VISTA;
        }
        List<ProductoDto> ordenados = ordenar(productos);

        ProductoForm form = new ProductoForm();
        Integer productoId = null;
        String mensajeError = null;
        if (prodCod != null) {
            ProductoDto dto = buscar(ordenados, prodCod);
            if (dto == null) {
                try {
                    dto = productoService.obtenerProducto(prodCod);
                } catch (NotFoundException ex) {
                    mensajeError = ex.getMessage();
                }
            }
            if (dto != null) {
                productoId = dto.prodCod();
                form = ProductoMapper.aForm(dto, importesVigentes(productoId));
            }
        }
        poblar(model, ordenados, form,
                productoId == null ? ModoProducto.ALTA : ModoProducto.EDICION,
                productoId, filtro, mensajeError, false);
        return VISTA;
    }

    /**
     * Alta: el producto se crea con la clave informada en el formulario (no hay {@code IDENTITY}).
     */
    @PostMapping
    public String crear(@RequestParam(name = "anulados", required = false) Boolean anulados,
            @Valid @ModelAttribute("producto") ProductoForm form,
            BindingResult bindingResult,
            Model model) {
        List<ProductoDto> ordenados = ordenar(cargar(FiltroAnulados.deParametro(anulados)));
        if (bindingResult.hasErrors()) {
            poblar(model, ordenados, form, ModoProducto.ALTA, null,
                    FiltroAnulados.deParametro(anulados), null, true);
            return VISTA;
        }
        ProductoEscrituraDto dto = ProductoMapper.aEscritura(form);
        try {
            productoService.crearProducto(dto);
        } catch (ConflictException | NotFoundException ex) {
            poblar(model, ordenados, form, ModoProducto.ALTA, null,
                    FiltroAnulados.deParametro(anulados), ex.getMessage(), false);
            return VISTA;
        }
        return "redirect:" + RUTA_ALTA + "?prodCod=" + form.getProdCod() + sufijoFiltro(anulados);
    }

    /**
     * Modificación: el backend reemplaza el conjunto completo de vigencias, por lo que la pantalla
     * envía siempre todas las filas (R-PROD-21).
     */
    @PostMapping("/{prodCod}")
    public String actualizar(@PathVariable("prodCod") Integer prodCod,
            @RequestParam(name = "anulados", required = false) Boolean anulados,
            @Valid @ModelAttribute("producto") ProductoForm form,
            BindingResult bindingResult,
            Model model) {
        List<ProductoDto> ordenados = ordenar(cargar(FiltroAnulados.deParametro(anulados)));
        if (bindingResult.hasErrors()) {
            poblar(model, ordenados, form, ModoProducto.EDICION, prodCod,
                    FiltroAnulados.deParametro(anulados), null, true);
            return VISTA;
        }
        ProductoEscrituraDto dto = ProductoMapper.aEscritura(form);
        try {
            productoService.actualizarProducto(prodCod, dto);
        } catch (ConflictException | NotFoundException ex) {
            poblar(model, ordenados, form, ModoProducto.EDICION, prodCod,
                    FiltroAnulados.deParametro(anulados), ex.getMessage(), false);
            return VISTA;
        }
        return "redirect:" + RUTA_ALTA + "?prodCod=" + prodCod + sufijoFiltro(anulados);
    }

    /**
     * Baja: borra la raíz y sus vigencias en cascada (JPA).
     */
    @PostMapping("/{prodCod}/eliminar")
    public String eliminar(@PathVariable("prodCod") Integer prodCod,
            @RequestParam(name = "anulados", required = false) Boolean anulados,
            Model model) {
        try {
            productoService.eliminarProducto(prodCod);
        } catch (ConflictException | NotFoundException ex) {
            poblar(model, ordenar(cargar(FiltroAnulados.deParametro(anulados))), new ProductoForm(),
                    ModoProducto.ALTA, null, FiltroAnulados.deParametro(anulados), ex.getMessage(),
                    false);
            return VISTA;
        }
        return "redirect:" + RUTA_ALTA + sufijoFiltro(anulados);
    }

    /**
     * Sufijo de la URL que mantiene el filtro de anulados al volver de una escritura
     * (R-PROD-26). Sin filtro activo no se agrega nada.
     */
    private static String sufijoFiltro(Boolean anulados) {
        return anulados == null ? "" : "&anulados=" + anulados;
    }

    /**
     * Carga la colección para el filtro dado; si la carga falla devuelve la lista vacía (la
     * pantalla muestra el estado de error sin renderizar la grilla de datos).
     */
    private List<ProductoDto> cargar(FiltroAnulados filtro) {
        try {
            return productoService.listarProductos(filtro.parametro());
        } catch (RuntimeException ex) {
            log.warn("No se pudo cargar la colección de productos", ex);
            return List.of();
        }
    }

    /** Ordena la colección por {@code prodCod} ascendente (R-PROD-25, D-33). */
    private static List<ProductoDto> ordenar(List<ProductoDto> productos) {
        List<ProductoDto> ordenados = new ArrayList<>(productos);
        ordenados.sort(Comparator.comparing(ProductoDto::prodCod,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return ordenados;
    }

    /** Busca el producto en la colección ya descargada (D-23: no hace falta una segunda consulta). */
    private static ProductoDto buscar(List<ProductoDto> productos, Integer prodCod) {
        return productos.stream()
                .filter(p -> Objects.equals(p.prodCod(), prodCod))
                .findFirst()
                .orElse(null);
    }

    /**
     * Segunda llamada al servicio para los 4 derivados {@code ProdIMax1..4} (D-22). Si el producto
     * no existe, los derivados quedan en {@code 0}.
     */
    private ImportesVigentesDto importesVigentes(Integer prodCod) {
        try {
            return productoService.importesVigentes(prodCod);
        } catch (NotFoundException ex) {
            log.warn("No se pudieron obtener los importes vigentes del producto {}", prodCod, ex);
            return null;
        }
    }

    /**
     * Arma el view model (plan §4.2). Los 13 atributos del modelo son:
     * {@code producto}, {@code coleccion}, {@code modo}, {@code accionFormulario},
     * {@code accionEliminar}, {@code filtroAnulados}, {@code productoId}, {@code mensajeError},
     * {@code tieneErrores}, {@code indiceAnterior}, {@code indiceSiguiente}, {@code primerCodigo} y
     * {@code ultimoCodigo}.
     */
    private void poblar(Model model,
            List<ProductoDto> productos,
            ProductoForm form,
            ModoProducto modo,
            Integer productoId,
            FiltroAnulados filtro,
            String mensajeError,
            boolean tieneErrores) {
        List<ProductoFilaView> coleccion = productos.stream()
                .map(p -> new ProductoFilaView(p.prodCod(), p.prodDesc(), p.prodAnula()))
                .toList();

        model.addAttribute("producto", form);
        model.addAttribute("coleccion", coleccion);
        model.addAttribute("modo", modo);
        model.addAttribute("accionFormulario",
                productoId == null ? RUTA_ALTA : RUTA_ALTA + "/" + productoId);
        model.addAttribute("accionEliminar",
                productoId == null ? null : RUTA_ALTA + "/" + productoId + "/eliminar");
        model.addAttribute("filtroAnulados", filtro);
        model.addAttribute("productoId", productoId);
        model.addAttribute("mensajeError", mensajeError);
        model.addAttribute("tieneErrores", tieneErrores);

        Integer indiceAnterior = null;
        Integer indiceSiguiente = null;
        Integer primerCodigo = null;
        Integer ultimoCodigo = null;
        if (!coleccion.isEmpty()) {
            primerCodigo = coleccion.get(0).prodCod();
            ultimoCodigo = coleccion.get(coleccion.size() - 1).prodCod();
            if (productoId != null) {
                for (int i = 0; i < coleccion.size(); i++) {
                    if (Objects.equals(coleccion.get(i).prodCod(), productoId)) {
                        indiceAnterior = i > 0 ? coleccion.get(i - 1).prodCod() : null;
                        indiceSiguiente = i < coleccion.size() - 1 ? coleccion.get(i + 1).prodCod() : null;
                        break;
                    }
                }
            }
        }
        model.addAttribute("indiceAnterior", indiceAnterior);
        model.addAttribute("indiceSiguiente", indiceSiguiente);
        model.addAttribute("primerCodigo", primerCodigo);
        model.addAttribute("ultimoCodigo", ultimoCodigo);
    }
}
