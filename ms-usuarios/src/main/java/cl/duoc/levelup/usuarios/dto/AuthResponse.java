package cl.duoc.levelup.usuarios.dto;

/**
 * Respuesta del login: el token JWT, cuándo expira (milisegundos epoch) y los datos del usuario.
 */
public record AuthResponse(String token, long expiraEn, UsuarioResponse usuario) {
}
