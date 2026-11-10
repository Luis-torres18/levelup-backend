package cl.duoc.levelup.gestion.security;

/**
 * Datos del usuario que viene dentro del token JWT.
 */
public record UsuarioAutenticado(Long id, String correo, String rol) {
}
