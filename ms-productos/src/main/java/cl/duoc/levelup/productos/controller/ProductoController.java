package cl.duoc.levelup.productos.controller;

import cl.duoc.levelup.productos.dto.ProductoRequest;
import cl.duoc.levelup.productos.model.Producto;
import cl.duoc.levelup.productos.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints REST de productos.
 * GET es público (catálogo). POST, PUT y DELETE requieren rol ADMIN (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<Producto> listarProductos() {
        return productoService.obtenerTodos();
    }

    @GetMapping("/{id}")
    public Producto obtenerProducto(@PathVariable("id") Long id) {
        return productoService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<Producto> crearProducto(@Valid @RequestBody ProductoRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(datos));
    }

    @PutMapping("/{id}")
    public Producto actualizarProducto(@PathVariable("id") Long id, @Valid @RequestBody ProductoRequest datos) {
        return productoService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable("id") Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
