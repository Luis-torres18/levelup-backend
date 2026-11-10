package cl.duoc.levelup.usuarios.service;

import cl.duoc.levelup.usuarios.dto.AuthResponse;
import cl.duoc.levelup.usuarios.dto.LoginRequest;
import cl.duoc.levelup.usuarios.dto.RegistroRequest;
import cl.duoc.levelup.usuarios.dto.UsuarioResponse;
import cl.duoc.levelup.usuarios.model.Rol;
import cl.duoc.levelup.usuarios.model.Usuario;
import cl.duoc.levelup.usuarios.repository.UsuarioRepository;
import cl.duoc.levelup.usuarios.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * Registro de usuarios nuevos e inicio de sesión.
 */
@Service
public class AuthService {

    static final String CREDENCIALES_INVALIDAS = "Correo o contraseña incorrectos.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Crea un usuario nuevo. Siempre queda con rol CLIENTE: solo un ADMIN puede cambiar roles
     * (desde ms-gestion-usuarios).
     */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest datos) {
        String correo = normalizarCorreo(datos.correo());

        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una cuenta registrada con este correo electrónico.");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(datos.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setContrasena(passwordEncoder.encode(datos.contrasena()));
        usuario.setTelefono(limpiar(datos.telefono()));
        usuario.setRegion(datos.region().trim());
        usuario.setComuna(datos.comuna().trim());
        usuario.setRol(Rol.CLIENTE);
        usuario.setDescuentoDuoc(esCorreoDuoc(correo));

        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    /**
     * Valida correo y contraseña. Si son correctos devuelve un token JWT con el rol del usuario.
     */
    public AuthResponse login(LoginRequest datos) {
        Usuario usuario = usuarioRepository.findByCorreo(normalizarCorreo(datos.correo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS));

        if (!passwordEncoder.matches(datos.contrasena(), usuario.getContrasena())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS);
        }

        String token = jwtService.generarToken(usuario.getId(), usuario.getCorreo(),
                usuario.getNombre(), usuario.getRol().name());
        long expiraEn = System.currentTimeMillis() + jwtService.getExpiracionMs();

        return new AuthResponse(token, expiraEn, UsuarioResponse.desde(usuario));
    }

    public UsuarioResponse perfil(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .map(UsuarioResponse::desde)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario ya no existe."));
    }

    /** Los correos de Duoc (alumnos y docentes) tienen 20% de descuento en la tienda. */
    public static boolean esCorreoDuoc(String correo) {
        return correo.endsWith("@duoc.cl") || correo.endsWith("@duocuc.cl");
    }

    static String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
