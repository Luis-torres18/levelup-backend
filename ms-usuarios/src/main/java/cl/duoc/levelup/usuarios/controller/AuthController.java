package cl.duoc.levelup.usuarios.controller;

import cl.duoc.levelup.usuarios.dto.AuthResponse;
import cl.duoc.levelup.usuarios.dto.LoginRequest;
import cl.duoc.levelup.usuarios.dto.RegistroRequest;
import cl.duoc.levelup.usuarios.dto.UsuarioResponse;
import cl.duoc.levelup.usuarios.security.UsuarioAutenticado;
import cl.duoc.levelup.usuarios.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de creación de usuarios y autenticación.
 * POST /registro y POST /login son públicos; GET /perfil requiere token.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(datos));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest datos) {
        return authService.login(datos);
    }

    @GetMapping("/perfil")
    public UsuarioResponse perfil(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return authService.perfil(usuario.id());
    }
}
