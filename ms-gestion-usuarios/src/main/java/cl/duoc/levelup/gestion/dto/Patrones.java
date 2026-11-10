package cl.duoc.levelup.gestion.dto;

/**
 * Expresiones regulares compartidas por las validaciones (mismas reglas que el frontend).
 */
public final class Patrones {

    /** Mínimo 8 caracteres, con mayúscula, minúscula, número y símbolo. */
    public static final String CONTRASENA = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";

    /** Teléfono chileno opcional: +56 9 XXXX XXXX o 9XXXXXXXX (vacío también es válido). */
    public static final String TELEFONO = "^$|^(\\+?56)?\\s?9\\s?\\d{4}\\s?\\d{4}$";

    public static final String MENSAJE_CONTRASENA =
            "La contraseña debe tener al menos 8 caracteres e incluir mayúscula, minúscula, número y símbolo.";

    public static final String MENSAJE_TELEFONO = "Formato de teléfono inválido. Usa +56 9 XXXX XXXX o 9XXXXXXXX.";

    private Patrones() {
    }
}
