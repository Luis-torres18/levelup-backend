package cl.duoc.levelup.productos;

import cl.duoc.levelup.productos.dto.ProductoRequest;
import cl.duoc.levelup.productos.model.Producto;
import cl.duoc.levelup.productos.repository.ProductoRepository;
import cl.duoc.levelup.productos.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del servicio (el repositorio se simula con Mockito).
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    private Producto catan;

    @BeforeEach
    void setUp() {
        catan = new Producto(1L, "Juegos de Mesa", "Catan", "Juego de estrategia",
                "https://ejemplo.com/catan.jpg", 29990);
    }

    @Test
    void obtenerTodosDevuelveLaListaOrdenadaPorId() {
        when(productoRepository.findAll(Sort.by("id"))).thenReturn(List.of(catan));

        List<Producto> resultado = productoService.obtenerTodos();

        assertEquals(1, resultado.size());
        assertEquals("Catan", resultado.get(0).getNombre());
    }

    @Test
    void obtenerPorIdDevuelveElProducto() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(catan));

        assertSame(catan, productoService.obtenerPorId(1L));
    }

    @Test
    void obtenerPorIdLanza404SiNoExiste() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productoService.obtenerPorId(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void crearLimpiaEspaciosYGuarda() {
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        ProductoRequest datos = new ProductoRequest("  Consolas ", " PS5 ", " Consola ",
                " https://ejemplo.com/ps5.jpg ", 549990);

        Producto creado = productoService.crear(datos);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captor.capture());
        assertEquals("Consolas", captor.getValue().getCategoria());
        assertEquals("PS5", creado.getNombre());
        assertEquals("Consola", creado.getDescripcion());
        assertEquals("https://ejemplo.com/ps5.jpg", creado.getImagen());
        assertEquals(549990, creado.getPrecio());
    }

    @Test
    void actualizarModificaElProductoExistente() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(catan));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        ProductoRequest datos = new ProductoRequest("Juegos de Mesa", "Catan Deluxe", "Edición especial",
                "https://ejemplo.com/catan2.jpg", 39990);

        Producto actualizado = productoService.actualizar(1L, datos);

        assertEquals(1L, actualizado.getId());
        assertEquals("Catan Deluxe", actualizado.getNombre());
        assertEquals(39990, actualizado.getPrecio());
    }

    @Test
    void actualizarLanza404SiNoExiste() {
        when(productoRepository.findById(5L)).thenReturn(Optional.empty());
        ProductoRequest datos = new ProductoRequest("A", "B", "C", "https://x.cl/a.jpg", 10);

        assertThrows(ResponseStatusException.class, () -> productoService.actualizar(5L, datos));
        verify(productoRepository, never()).save(any(Producto.class));
    }

    @Test
    void eliminarBorraSiExiste() {
        when(productoRepository.existsById(1L)).thenReturn(true);

        productoService.eliminar(1L);

        verify(productoRepository).deleteById(1L);
    }

    @Test
    void eliminarLanza404SiNoExiste() {
        when(productoRepository.existsById(7L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productoService.eliminar(7L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(productoRepository, never()).deleteById(7L);
    }
}
