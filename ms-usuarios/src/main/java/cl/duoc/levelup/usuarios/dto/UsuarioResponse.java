package cl.duoc.levelup.usuarios.dto;

import cl.duoc.levelup.usuarios.model.Usuario;

import java.time.LocalDateTime;

/**
 * Datos públicos de un usuario: NUNCA incluye la contraseña.
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        String telefono,
        String region,
        String comuna,
        String rol,
        boolean descuentoDuoc,
        LocalDateTime creadoEl
) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getRegion(),
                usuario.getComuna(),
                usuario.getRol().name(),
                usuario.isDescuentoDuoc(),
                usuario.getCreadoEl());
    }
}
