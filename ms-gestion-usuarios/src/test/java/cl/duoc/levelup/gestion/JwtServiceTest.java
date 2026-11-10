package cl.duoc.levelup.gestion;

import cl.duoc.levelup.gestion.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRETO = "clave-de-pruebas-unitarias-con-mas-de-32-caracteres-123456";

    @Test
    void generaYValidaUnToken() {
        JwtService jwtService = new JwtService(SECRETO, 60_000);

        String token = jwtService.generarToken(5L, "admin@duocuc.cl", "Admin", "ADMIN");
        Claims claims = jwtService.validarToken(token);

        assertEquals("5", claims.getSubject());
        assertEquals("admin@duocuc.cl", claims.get("correo", String.class));
        assertEquals("ADMIN", claims.get("rol", String.class));
        assertEquals(60_000, jwtService.getExpiracionMs());
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        JwtService emisor = new JwtService("otra-clave-distinta-con-mas-de-32-caracteres-abcdef", 60_000);
        JwtService receptor = new JwtService(SECRETO, 60_000);

        String token = emisor.generarToken(1L, "a@b.cl", "A", "ADMIN");

        assertThrows(JwtException.class, () -> receptor.validarToken(token));
    }

    @Test
    void rechazaTokenExpirado() {
        JwtService jwtService = new JwtService(SECRETO, -1_000);

        String token = jwtService.generarToken(1L, "a@b.cl", "A", "CLIENTE");

        assertThrows(JwtException.class, () -> jwtService.validarToken(token));
    }
}
