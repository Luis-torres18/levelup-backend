package cl.duoc.levelup.productos.service;

import cl.duoc.levelup.productos.dto.ProductoRequest;
import cl.duoc.levelup.productos.model.Producto;
import cl.duoc.levelup.productos.repository.ProductoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Lógica de negocio de productos.
 */
@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> obtenerTodos() {
        return productoRepository.findAll(Sort.by("id"));
    }

    public Producto obtenerPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El producto no existe en el catálogo."));
    }

    public Producto crear(ProductoRequest datos) {
        Producto producto = new Producto();
        copiarDatos(datos, producto);
        return productoRepository.save(producto);
    }

    public Producto actualizar(Long id, ProductoRequest datos) {
        Producto producto = obtenerPorId(id);
        copiarDatos(datos, producto);
        return productoRepository.save(producto);
    }

    public void eliminar(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto no existe en el catálogo.");
        }
        productoRepository.deleteById(id);
    }

    private void copiarDatos(ProductoRequest datos, Producto producto) {
        producto.setCategoria(datos.categoria().trim());
        producto.setNombre(datos.nombre().trim());
        producto.setDescripcion(datos.descripcion().trim());
        producto.setImagen(datos.imagen().trim());
        producto.setPrecio(datos.precio());
    }
}
