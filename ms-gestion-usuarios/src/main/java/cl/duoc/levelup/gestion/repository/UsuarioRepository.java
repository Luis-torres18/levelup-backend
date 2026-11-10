package cl.duoc.levelup.gestion.repository;

import cl.duoc.levelup.gestion.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByCorreo(String correo);

    /** Para validar que al editar no se use el correo de OTRO usuario. */
    boolean existsByCorreoAndIdNot(String correo, Long id);
}
