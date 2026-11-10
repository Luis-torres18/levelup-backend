package cl.duoc.levelup.usuarios.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Genera y valida tokens JWT. Los 3 microservicios comparten la misma clave (JWT_SECRET),
 * así un token emitido por ms-usuarios es válido en ms-productos y ms-gestion-usuarios.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.expiration-ms:28800000}") long expiracionMs) {
        // HS256 exige una clave de al menos 32 caracteres (256 bits).
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(Long idUsuario, String correo, String nombre, String rol) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(String.valueOf(idUsuario))
                .claim("correo", correo)
                .claim("nombre", nombre)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMs))
                .signWith(clave)
                .compact();
    }

    /**
     * Devuelve los datos del token. Lanza JwtException si la firma no es válida o expiró.
     */
    public Claims validarToken(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpiracionMs() {
        return expiracionMs;
    }
}
