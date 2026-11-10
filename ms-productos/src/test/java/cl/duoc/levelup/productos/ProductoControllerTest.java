package cl.duoc.levelup.productos;

import cl.duoc.levelup.productos.model.Producto;
import cl.duoc.levelup.productos.repository.ProductoRepository;
import cl.duoc.levelup.productos.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de los endpoints REST con seguridad real (JWT) y base de datos H2 en memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductoControllerTest {

    private static final String PRODUCTO_JSON = """
            {
              "categoria": "Accesorios",
              "nombre": "Teclado mecánico",
              "descripcion": "Switches rojos y RGB",
              "imagen": "https://ejemplo.com/teclado.jpg",
              "precio": 29990
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private JwtService jwtService;

    private Producto existente;
    private String tokenAdmin;
    private String tokenCliente;

    @BeforeEach
    void setUp() {
        productoRepository.deleteAll();
        existente = productoRepository.save(new Producto(null, "Consolas", "PlayStation 5",
                "Consola de última generación", "https://ejemplo.com/ps5.jpg", 549990));
        tokenAdmin = "Bearer " + jwtService.generarToken(1L, "admin@duocuc.cl", "Admin", "ADMIN");
        tokenCliente = "Bearer " + jwtService.generarToken(2L, "cliente@gmail.com", "Cliente", "CLIENTE");
    }

    @Test
    void listarEsPublico() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("PlayStation 5"));
    }

    @Test
    void obtenerPorIdEsPublico() throws Exception {
        mockMvc.perform(get("/api/productos/" + existente.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(549990));
    }

    @Test
    void obtenerProductoInexistenteDevuelve404ConMensaje() throws Exception {
        mockMvc.perform(get("/api/productos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El producto no existe en el catálogo."));
    }

    @Test
    void crearSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void crearConTokenInvalidoDevuelve401() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-falso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearComoClienteDevuelve403() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tienes permisos para realizar esta acción."));
    }

    @Test
    void crearComoAdminDevuelve201() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Teclado mecánico"));
    }

    @Test
    void crearConDatosInvalidosDevuelve400() throws Exception {
        String invalido = PRODUCTO_JSON.replace("29990", "0");

        mockMvc.perform(post("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El precio debe ser un número mayor a 0."));
    }

    @Test
    void crearConJsonMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El formato de la solicitud no es válido."));
    }

    @Test
    void actualizarComoAdmin() throws Exception {
        mockMvc.perform(put("/api/productos/" + existente.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existente.getId().intValue()))
                .andExpect(jsonPath("$.nombre").value("Teclado mecánico"));
    }

    @Test
    void actualizarProductoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(put("/api/productos/999999")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarComoAdminDevuelve204() throws Exception {
        mockMvc.perform(delete("/api/productos/" + existente.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isNoContent());

        assertFalse(productoRepository.existsById(existente.getId()));
    }

    @Test
    void eliminarComoClienteDevuelve403() throws Exception {
        mockMvc.perform(delete("/api/productos/" + existente.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void idConFormatoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/productos/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void corsPermiteLaPeticionPreviaDelFrontend() throws Exception {
        mockMvc.perform(options("/api/productos")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }
}
