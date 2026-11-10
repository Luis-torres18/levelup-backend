package cl.duoc.levelup.usuarios;

import cl.duoc.levelup.usuarios.dto.Patrones;
import cl.duoc.levelup.usuarios.model.Rol;
import cl.duoc.levelup.usuarios.model.Usuario;
import cl.duoc.levelup.usuarios.repository.UsuarioRepository;
import cl.duoc.levelup.usuarios.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de los endpoints de registro, login y perfil con seguridad real y H2 en memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String REGISTRO_JSON = """
            {
              "nombre": "Juan Pérez",
              "correo": "juan@gmail.com",
              "contrasena": "Clave123!",
              "telefono": "+56 9 1234 5678",
              "region": "Metropolitana de Santiago",
              "comuna": "Santiago"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void limpiarBase() {
        usuarioRepository.deleteAll();
    }

    private Usuario crearUsuario(String correo, String contrasena, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario Prueba");
        usuario.setCorreo(correo);
        usuario.setContrasena(passwordEncoder.encode(contrasena));
        usuario.setRegion("Metropolitana de Santiago");
        usuario.setComuna("Santiago");
        usuario.setRol(rol);
        return usuarioRepository.save(usuario);
    }

    @Test
    void registroDevuelve201YNoExponeLaContrasena() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.correo").value("juan@gmail.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());

        Usuario guardado = usuarioRepository.findByCorreo("juan@gmail.com").orElseThrow();
        assertNotEquals("Clave123!", guardado.getContrasena());
        assertTrue(passwordEncoder.matches("Clave123!", guardado.getContrasena()));
    }

    @Test
    void registroConCorreoDuocucQuedaComoClienteConDescuento() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON.replace("juan@gmail.com", "nuevo@duocuc.cl")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.descuentoDuoc").value(true));
    }

    @Test
    void registroDuplicadoDevuelve409() throws Exception {
        crearUsuario("juan@gmail.com", "Clave123!", Rol.CLIENTE);

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una cuenta registrada con este correo electrónico."));
    }

    @Test
    void registroConContrasenaDebilDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON.replace("Clave123!", "clavedebil")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(Patrones.MENSAJE_CONTRASENA));
    }

    @Test
    void loginCorrectoDevuelveTokenYRol() throws Exception {
        crearUsuario("admin@duocuc.cl", "Admin123!", Rol.ADMIN);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"admin@duocuc.cl\",\"contrasena\":\"Admin123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiraEn").isNumber())
                .andExpect(jsonPath("$.usuario.rol").value("ADMIN"))
                .andExpect(jsonPath("$.usuario.contrasena").doesNotExist());
    }

    @Test
    void loginIncorrectoDevuelve401() throws Exception {
        crearUsuario("juan@gmail.com", "Clave123!", Rol.CLIENTE);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"juan@gmail.com\",\"contrasena\":\"Otra123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos."));
    }

    @Test
    void loginSinDatosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void perfilConTokenDevuelveElUsuario() throws Exception {
        Usuario cliente = crearUsuario("juan@gmail.com", "Clave123!", Rol.CLIENTE);
        String token = jwtService.generarToken(cliente.getId(), cliente.getCorreo(), cliente.getNombre(), "CLIENTE");

        mockMvc.perform(get("/api/auth/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("juan@gmail.com"));
    }

    @Test
    void perfilSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/auth/perfil"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void perfilDeUsuarioEliminadoDevuelve404() throws Exception {
        String token = jwtService.generarToken(999999L, "borrado@gmail.com", "Borrado", "CLIENTE");

        mockMvc.perform(get("/api/auth/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
