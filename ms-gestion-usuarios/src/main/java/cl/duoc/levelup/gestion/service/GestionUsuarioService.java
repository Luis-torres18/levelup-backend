package cl.duoc.levelup.gestion.service;

import cl.duoc.levelup.gestion.dto.UsuarioActualizarRequest;
import cl.duoc.levelup.gestion.dto.UsuarioCrearRequest;
import cl.duoc.levelup.gestion.dto.UsuarioResponse;
import cl.duoc.levelup.gestion.model.Rol;
import cl.duoc.levelup.gestion.model.Usuario;
import cl.duoc.levelup.gestion.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

/**
 * CRUD de usuarios para el ADMIN: listar, ver, crear, editar (incluido el rol) y eliminar.
 */
@Service
public class GestionUsuarioService {

    private static final String CORREO_DUPLICADO = "Ya existe una cuenta registrada con este correo electrónico.";
    private static final String NO_EXISTE = "El usuario no existe.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public GestionUsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll(Sort.by("id")).stream()
                .map(UsuarioResponse::desde)
                .toList();
    }

    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.desde(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioCrearRequest datos) {
        String correo = normalizarCorreo(datos.correo());
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, CORREO_DUPLICADO);
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(datos.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setContrasena(passwordEncoder.encode(datos.contrasena()));
        usuario.setTelefono(limpiar(datos.telefono()));
        usuario.setRegion(datos.region().trim());
        usuario.setComuna(datos.comuna().trim());
        usuario.setRol(datos.rol());
        usuario.setDescuentoDuoc(esCorreoDuoc(correo));

        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    /**
     * @param idAdmin id del ADMIN que hace la petición (viene del token).
     */
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioActualizarRequest datos, Long idAdmin) {
        Usuario usuario = buscar(id);
        String correo = normalizarCorreo(datos.correo());

        if (usuarioRepository.existsByCorreoAndIdNot(correo, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, CORREO_DUPLICADO);
        }
        // Evita que el ADMIN se quite su propio rol y quede sin acceso al panel.
        if (id.equals(idAdmin) && datos.rol() != Rol.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No puedes quitarte a ti mismo el rol de administrador.");
        }

        usuario.setNombre(datos.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setTelefono(limpiar(datos.telefono()));
        usuario.setRegion(datos.region().trim());
        usuario.setComuna(datos.comuna().trim());
        usuario.setRol(datos.rol());
        usuario.setDescuentoDuoc(esCorreoDuoc(correo));
        if (datos.contrasena() != null && !datos.contrasena().isBlank()) {
            usuario.setContrasena(passwordEncoder.encode(datos.contrasena()));
        }

        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    @Transactional
    public void eliminar(Long id, Long idAdmin) {
        if (id.equals(idAdmin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes eliminar tu propia cuenta.");
        }
        if (!usuarioRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, NO_EXISTE);
        }
        usuarioRepository.deleteById(id);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NO_EXISTE));
    }

    static boolean esCorreoDuoc(String correo) {
        return correo.endsWith("@duoc.cl") || correo.endsWith("@duocuc.cl");
    }

    private static String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
