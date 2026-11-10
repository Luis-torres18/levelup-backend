package cl.duoc.levelup.gestion.controller;

import cl.duoc.levelup.gestion.dto.UsuarioActualizarRequest;
import cl.duoc.levelup.gestion.dto.UsuarioCrearRequest;
import cl.duoc.levelup.gestion.dto.UsuarioResponse;
import cl.duoc.levelup.gestion.security.UsuarioAutenticado;
import cl.duoc.levelup.gestion.service.GestionUsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de gestión de usuarios. Todos requieren rol ADMIN (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/usuarios")
public class GestionUsuarioController {

    private final GestionUsuarioService gestionUsuarioService;

    public GestionUsuarioController(GestionUsuarioService gestionUsuarioService) {
        this.gestionUsuarioService = gestionUsuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listarUsuarios() {
        return gestionUsuarioService.listar();
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtenerUsuario(@PathVariable("id") Long id) {
        return gestionUsuarioService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crearUsuario(@Valid @RequestBody UsuarioCrearRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gestionUsuarioService.crear(datos));
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizarUsuario(@PathVariable("id") Long id,
                                             @Valid @RequestBody UsuarioActualizarRequest datos,
                                             @AuthenticationPrincipal UsuarioAutenticado admin) {
        return gestionUsuarioService.actualizar(id, datos, admin.id());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable("id") Long id,
                                                @AuthenticationPrincipal UsuarioAutenticado admin) {
        gestionUsuarioService.eliminar(id, admin.id());
        return ResponseEntity.noContent().build();
    }
}
