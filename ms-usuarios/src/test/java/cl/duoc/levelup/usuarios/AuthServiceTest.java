package cl.duoc.levelup.usuarios;

import cl.duoc.levelup.usuarios.dto.AuthResponse;
import cl.duoc.levelup.usuarios.dto.LoginRequest;
import cl.duoc.levelup.usuarios.dto.RegistroRequest;
import cl.duoc.levelup.usuarios.dto.UsuarioResponse;
import cl.duoc.levelup.usuarios.model.Rol;
import cl.duoc.levelup.usuarios.model.Usuario;
import cl.duoc.levelup.usuarios.repository.UsuarioRepository;
import cl.duoc.levelup.usuarios.security.JwtService;
import cl.duoc.levelup.usuarios.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private static RegistroRequest registro(String correo, String telefono) {
        return new RegistroRequest(" Juan Pérez ", correo, "Clave123!", telefono,
                "Metropolitana de Santiago", "Santiago");
    }

    private static Usuario usuario(Long id, String correo, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Admin");
        usuario.setCorreo(correo);
        usuario.setContrasena("HASH");
        usuario.setRegion("Metropolitana de Santiago");
        usuario.setComuna("Santiago");
        usuario.setRol(rol);
        return usuario;
    }

    @Test
    void registrarCreaClienteConContrasenaCifrada() {
        when(usuarioRepository.existsByCorreo("juan@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("Clave123!")).thenReturn("HASH");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario guardado = inv.getArgument(0);
            guardado.setId(1L);
            return guardado;
        });

        UsuarioResponse respuesta = authService.registrar(registro("  Juan@Gmail.com ", " 912345678 "));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("HASH", captor.getValue().getContrasena());
        assertEquals("912345678", captor.getValue().getTelefono());
        assertEquals("Juan Pérez", respuesta.nombre());
        assertEquals("juan@gmail.com", respuesta.correo());
        assertEquals("CLIENTE", respuesta.rol());
        assertFalse(respuesta.descuentoDuoc());
    }

    @Test
    void registrarSinTelefonoLoGuardaVacio() {
        when(usuarioRepository.existsByCorreo("ana@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("Clave123!")).thenReturn("HASH");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse respuesta = authService.registrar(registro("ana@gmail.com", "  "));

        assertNull(respuesta.telefono());
    }

    @Test
    void registrarCorreoDuocActivaDescuentoPeroQuedaComoCliente() {
        when(usuarioRepository.existsByCorreo("profe@duocuc.cl")).thenReturn(false);
        when(passwordEncoder.encode("Clave123!")).thenReturn("HASH");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse respuesta = authService.registrar(registro("profe@duocuc.cl", null));

        assertEquals("CLIENTE", respuesta.rol());
        assertTrue(respuesta.descuentoDuoc());
    }

    @Test
    void registrarCorreoDuplicadoLanza409() {
        when(usuarioRepository.existsByCorreo("juan@gmail.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.registrar(registro("juan@gmail.com", null)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void loginCorrectoDevuelveTokenYDatosDelUsuario() {
        Usuario admin = usuario(3L, "admin@duocuc.cl", Rol.ADMIN);
        when(usuarioRepository.findByCorreo("admin@duocuc.cl")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Clave123!", "HASH")).thenReturn(true);
        when(jwtService.generarToken(3L, "admin@duocuc.cl", "Admin", "ADMIN")).thenReturn("TOKEN");
        when(jwtService.getExpiracionMs()).thenReturn(60_000L);

        AuthResponse respuesta = authService.login(new LoginRequest(" ADMIN@duocuc.cl ", "Clave123!"));

        assertEquals("TOKEN", respuesta.token());
        assertEquals("ADMIN", respuesta.usuario().rol());
        assertTrue(respuesta.expiraEn() > System.currentTimeMillis());
    }

    @Test
    void loginConContrasenaIncorrectaLanza401() {
        Usuario cliente = usuario(4L, "juan@gmail.com", Rol.CLIENTE);
        when(usuarioRepository.findByCorreo("juan@gmail.com")).thenReturn(Optional.of(cliente));
        when(passwordEncoder.matches("mala", "HASH")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("juan@gmail.com", "mala")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void loginConCorreoInexistenteLanza401() {
        when(usuarioRepository.findByCorreo("nadie@gmail.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("nadie@gmail.com", "Clave123!")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void perfilDevuelveLosDatosDelUsuario() {
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(usuario(4L, "juan@gmail.com", Rol.CLIENTE)));

        assertEquals("juan@gmail.com", authService.perfil(4L).correo());
    }

    @Test
    void perfilDeUsuarioInexistenteLanza404() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.perfil(9L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void esCorreoDuocReconoceAmbosDominios() {
        assertTrue(AuthService.esCorreoDuoc("alumno@duoc.cl"));
        assertTrue(AuthService.esCorreoDuoc("profe@duocuc.cl"));
        assertFalse(AuthService.esCorreoDuoc("persona@gmail.com"));
    }
}
