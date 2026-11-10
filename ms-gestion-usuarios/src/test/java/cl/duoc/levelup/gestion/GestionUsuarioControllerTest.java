package cl.duoc.levelup.gestion;

import cl.duoc.levelup.gestion.model.Rol;
import cl.duoc.levelup.gestion.model.Usuario;
import cl.duoc.levelup.gestion.repository.UsuarioRepository;
import cl.duoc.levelup.gestion.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de los endpoints /api/usuarios con seguridad real (solo ADMIN) y H2 en memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GestionUsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Usuario admin;
    private Usuario cliente;
    private String tokenAdmin;
    private String tokenCliente;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
        admin = guardar("Administrador", "admin@duocuc.cl", Rol.ADMIN);
        cliente = guardar("Juan Pérez", "juan@gmail.com", Rol.CLIENTE);
        tokenAdmin = "Bearer " + jwtService.generarToken(admin.getId(), admin.getCorreo(), admin.getNombre(), "ADMIN");
        tokenCliente = "Bearer " + jwtService.generarToken(cliente.getId(), cliente.getCorreo(), cliente.getNombre(), "CLIENTE");
    }

    private Usuario guardar(String nombre, String correo, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        usuario.setContrasena(passwordEncoder.encode("Clave123!"));
        usuario.setRegion("Metropolitana de Santiago");
        usuario.setComuna("Santiago");
        usuario.setRol(rol);
        return usuarioRepository.save(usuario);
    }

    private static String json(String nombre, String correo, String contrasena, String rol) {
        return """
                {
                  "nombre": "%s",
                  "correo": "%s",
                  "contrasena": "%s",
                  "telefono": "",
                  "region": "Valparaíso",
                  "comuna": "Viña del Mar",
                  "rol": "%s"
                }
                """.formatted(nombre, correo, contrasena, rol);
    }

    @Test
    void adminListaUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].correo").value("admin@duocuc.cl"))
                .andExpect(jsonPath("$[0].contrasena").doesNotExist());
    }

    @Test
    void clienteNoPuedeListarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminObtieneUnUsuario() throws Exception {
        mockMvc.perform(get("/api/usuarios/" + cliente.getId()).header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Juan Pérez"));
    }

    @Test
    void obtenerUsuarioInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/usuarios/999999").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El usuario no existe."));
    }

    @Test
    void adminCreaUsuarioConRol() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Ana Torres", "ana@gmail.com", "Clave123!", "ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("ADMIN"));

        assertTrue(usuarioRepository.existsByCorreo("ana@gmail.com"));
    }

    @Test
    void crearConRolInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Ana Torres", "ana@gmail.com", "Clave123!", "SUPERUSUARIO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminEditaUsuarioYLeCambiaElRol() throws Exception {
        mockMvc.perform(put("/api/usuarios/" + cliente.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Juan Editado", "juan@gmail.com", "", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Juan Editado"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));

        Usuario editado = usuarioRepository.findById(cliente.getId()).orElseThrow();
        assertEquals(Rol.ADMIN, editado.getRol());
        assertTrue(passwordEncoder.matches("Clave123!", editado.getContrasena()));
    }

    @Test
    void editarConCorreoDeOtroUsuarioDevuelve409() throws Exception {
        mockMvc.perform(put("/api/usuarios/" + cliente.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Juan Pérez", "admin@duocuc.cl", "", "CLIENTE")))
                .andExpect(status().isConflict());
    }

    @Test
    void adminNoPuedeQuitarseSuRol() throws Exception {
        mockMvc.perform(put("/api/usuarios/" + admin.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Administrador", "admin@duocuc.cl", "", "CLIENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No puedes quitarte a ti mismo el rol de administrador."));
    }

    @Test
    void adminEliminaUsuario() throws Exception {
        mockMvc.perform(delete("/api/usuarios/" + cliente.getId()).header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isNoContent());

        assertFalse(usuarioRepository.existsById(cliente.getId()));
    }

    @Test
    void adminNoPuedeEliminarseASiMismo() throws Exception {
        mockMvc.perform(delete("/api/usuarios/" + admin.getId()).header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No puedes eliminar tu propia cuenta."));
    }

    @Test
    void clienteNoPuedeEliminar() throws Exception {
        mockMvc.perform(delete("/api/usuarios/" + admin.getId()).header(HttpHeaders.AUTHORIZATION, tokenCliente))
                .andExpect(status().isForbidden());
    }
}
