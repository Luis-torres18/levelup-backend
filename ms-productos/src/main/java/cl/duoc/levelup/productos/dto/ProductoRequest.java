package cl.duoc.levelup.productos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos que envía el frontend para crear o actualizar un producto (POST / PUT).
 */
public record ProductoRequest(
        @NotBlank(message = "La categoría es obligatoria.")
        @Size(max = 60, message = "La categoría no puede superar los 60 caracteres.")
        String categoria,

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
        String nombre,

        @NotBlank(message = "La descripción es obligatoria.")
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.")
        String descripcion,

        @NotBlank(message = "La imagen es obligatoria.")
        @Size(max = 1000, message = "El link de la imagen es demasiado largo.")
        @Pattern(regexp = "^https?://\\S+$",
                message = "La imagen debe ser un link válido que comience con http:// o https://.")
        String imagen,

        @NotNull(message = "El precio es obligatorio.")
        @Positive(message = "El precio debe ser un número mayor a 0.")
        Integer precio
) {
}
