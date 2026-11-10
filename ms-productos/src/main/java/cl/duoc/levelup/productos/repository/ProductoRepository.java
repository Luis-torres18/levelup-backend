package cl.duoc.levelup.productos.repository;

import cl.duoc.levelup.productos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA: entrega las operaciones CRUD básicas sobre la tabla "productos".
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
