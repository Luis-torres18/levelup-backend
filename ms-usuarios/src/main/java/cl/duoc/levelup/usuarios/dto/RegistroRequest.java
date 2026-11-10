package cl.duoc.levelup.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos del formulario de registro. Todo usuario nuevo queda con rol CLIENTE.
 */
public record RegistroRequest(
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

        @NotBlank(message = "Debes seleccionar tu región.")
        String region,

        @NotBlank(message = "Debes seleccionar tu comuna.")
        String comuna
) {
}
