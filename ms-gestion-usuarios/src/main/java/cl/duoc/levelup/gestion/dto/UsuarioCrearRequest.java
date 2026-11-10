package cl.duoc.levelup.gestion.dto;

import cl.duoc.levelup.gestion.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para que un ADMIN cree un usuario (POST). Aquí sí se elige el rol.
 */
public record UsuarioCrearRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres.")
        String nombre,

        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "Ingresa un correo electrónico válido.")
        @Size(max = 100, message = "El correo no puede superar los 100 caracteres.")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(max = 72, message = "La contraseña no puede superar los 72 caracteres.")
        @Pattern(regexp = Patrones.CONTRASENA, message = Patrones.MENSAJE_CONTRASENA)
        String contrasena,

        @Pattern(regexp = Patrones.TELEFONO, message = Patrones.MENSAJE_TELEFONO)
        String telefono,

        @NotBlank(message = "La región es obligatoria.")
        String region,

        @NotBlank(message = "La comuna es obligatoria.")
        String comuna,

        @NotNull(message = "El rol es obligatorio (ADMIN o CLIENTE).")
        Rol rol
) {
}
