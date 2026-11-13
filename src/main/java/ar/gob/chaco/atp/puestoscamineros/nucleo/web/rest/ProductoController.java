package ar.gob.chaco.atp.puestoscamineros.nucleo.web.rest;

import ar.gob.chaco.atp.puestoscamineros.nucleo.service.ProductoService;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ImportesVigentesDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoDto;
import ar.gob.chaco.atp.puestoscamineros.nucleo.web.dto.ProductoEscrituraDto;
import ar.gob.chaco.atp.puestoscamineros.shared.web.RecursoUri;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Productos: lectura con vigencias anidadas (P-03), fórmulas {@code ProdIMax1..4} (R-005) y ABM
 * atómico raíz+vigencias (R-034/R-035).
 */
@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<ProductoDto> listar(@RequestParam(required = false) Boolean anulados) {
        return productoService.listarProductos(anulados);
    }

    @GetMapping("/{prodCod}")
    public ProductoDto obtener(@PathVariable Integer prodCod) {
        return productoService.obtenerProducto(prodCod);
    }

    @GetMapping("/{prodCod}/importes-vigentes")
    public ImportesVigentesDto importesVigentes(@PathVariable Integer prodCod) {
        return productoService.importesVigentes(prodCod);
    }

    @PostMapping
    public ResponseEntity<ProductoDto> crear(@Valid @RequestBody ProductoEscrituraDto dto) {
        ProductoDto creado = productoService.crearProducto(dto);
        return ResponseEntity.created(RecursoUri.crear(creado.prodCod())).body(creado);
    }

    @PutMapping("/{prodCod}")
    public ProductoDto actualizar(@PathVariable Integer prodCod,
            @Valid @RequestBody ProductoEscrituraDto dto) {
        return productoService.actualizarProducto(prodCod, dto);
    }

    @DeleteMapping("/{prodCod}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer prodCod) {
        productoService.eliminarProducto(prodCod);
        return ResponseEntity.noContent().build();
    }
}
