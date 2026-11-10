package cl.duoc.levelup.gestion;

import cl.duoc.levelup.gestion.dto.UsuarioActualizarRequest;
import cl.duoc.levelup.gestion.dto.UsuarioCrearRequest;
import cl.duoc.levelup.gestion.dto.UsuarioResponse;
import cl.duoc.levelup.gestion.model.Rol;
import cl.duoc.levelup.gestion.model.Usuario;
import cl.duoc.levelup.gestion.repository.UsuarioRepository;
import cl.duoc.levelup.gestion.service.GestionUsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionUsuarioServiceTest {

    private static final Long ID_ADMIN = 1L;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private GestionUsuarioService gestionUsuarioService;

    private static Usuario usuario(Long id, String correo, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Juan Pérez");
        usuario.setCorreo(correo);
        usuario.setContrasena("HASH-ANTIGUO");
        usuario.setRegion("Metropolitana de Santiago");
        usuario.setComuna("Santiago");
        usuario.setRol(rol);
        return usuario;
    }

    private static UsuarioActualizarRequest edicion(String correo, String contrasena, Rol rol) {
        return new UsuarioActualizarRequest("Juan Editado", correo, contrasena, "912345678",
                "Valparaíso", "Viña del Mar", rol);
    }

    @Test
    void listarDevuelveTodosSinContrasena() {
        when(usuarioRepository.findAll(Sort.by("id")))
                .thenReturn(List.of(usuario(1L, "admin@duocuc.cl", Rol.ADMIN), usuario(2L, "juan@gmail.com", Rol.CLIENTE)));

        List<UsuarioResponse> lista = gestionUsuarioService.listar();

        assertEquals(2, lista.size());
        assertEquals("CLIENTE", lista.get(1).rol());
    }

    @Test
    void obtenerLanza404SiNoExiste() {
        when(usuarioRepository.findById(5L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> gestionUsuarioService.obtener(5L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void crearGuardaConElRolElegidoYContrasenaCifrada() {
        when(usuarioRepository.existsByCorreo("nuevo@duoc.cl")).thenReturn(false);
        when(passwordEncoder.encode("Clave123!")).thenReturn("HASH");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse creado = gestionUsuarioService.crear(new UsuarioCrearRequest("Nuevo Admin", " Nuevo@Duoc.cl ",
                "Clave123!", "", "Metropolitana de Santiago", "Santiago", Rol.ADMIN));

        assertEquals("nuevo@duoc.cl", creado.correo());
        assertEquals("ADMIN", creado.rol());
        assertTrue(creado.descuentoDuoc());
    }

    @Test
    void crearConCorreoDuplicadoLanza409() {
        when(usuarioRepository.existsByCorreo("juan@gmail.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gestionUsuarioService.crear(new UsuarioCrearRequest("Juan", "juan@gmail.com", "Clave123!",
                        null, "Metropolitana de Santiago", "Santiago", Rol.CLIENTE)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void actualizarCambiaDatosYRolSinTocarLaContrasenaSiVieneVacia() {
        Usuario juan = usuario(2L, "juan@gmail.com", Rol.CLIENTE);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(juan));
        when(usuarioRepository.existsByCorreoAndIdNot("juan@gmail.com", 2L)).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse editado = gestionUsuarioService.actualizar(2L, edicion("juan@gmail.com", "", Rol.ADMIN), ID_ADMIN);

        assertEquals("Juan Editado", editado.nombre());
        assertEquals("ADMIN", editado.rol());
        assertEquals("Viña del Mar", editado.comuna());
        assertEquals("HASH-ANTIGUO", juan.getContrasena());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void actualizarConNuevaContrasenaLaCifra() {
        Usuario juan = usuario(2L, "juan@gmail.com", Rol.CLIENTE);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(juan));
        when(usuarioRepository.existsByCorreoAndIdNot("juan@gmail.com", 2L)).thenReturn(false);
        when(passwordEncoder.encode("Nueva123!")).thenReturn("HASH-NUEVO");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        gestionUsuarioService.actualizar(2L, edicion("juan@gmail.com", "Nueva123!", Rol.CLIENTE), ID_ADMIN);

        assertEquals("HASH-NUEVO", juan.getContrasena());
    }

    @Test
    void actualizarConCorreoDeOtroUsuarioLanza409() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, "juan@gmail.com", Rol.CLIENTE)));
        when(usuarioRepository.existsByCorreoAndIdNot("admin@duocuc.cl", 2L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gestionUsuarioService.actualizar(2L, edicion("admin@duocuc.cl", null, Rol.CLIENTE), ID_ADMIN));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void adminNoPuedeQuitarseSuPropioRol() {
        when(usuarioRepository.findById(ID_ADMIN)).thenReturn(Optional.of(usuario(ID_ADMIN, "admin@duocuc.cl", Rol.ADMIN)));
        when(usuarioRepository.existsByCorreoAndIdNot("admin@duocuc.cl", ID_ADMIN)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gestionUsuarioService.actualizar(ID_ADMIN, edicion("admin@duocuc.cl", null, Rol.CLIENTE), ID_ADMIN));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void eliminarBorraAOtroUsuario() {
        when(usuarioRepository.existsById(2L)).thenReturn(true);

        gestionUsuarioService.eliminar(2L, ID_ADMIN);

        verify(usuarioRepository).deleteById(2L);
    }

    @Test
    void adminNoPuedeEliminarSuPropiaCuenta() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gestionUsuarioService.eliminar(ID_ADMIN, ID_ADMIN));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    void eliminarUsuarioInexistenteLanza404() {
        when(usuarioRepository.existsById(9L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gestionUsuarioService.eliminar(9L, ID_ADMIN));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
