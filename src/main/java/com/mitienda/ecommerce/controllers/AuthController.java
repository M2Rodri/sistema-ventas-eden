package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.AuthResponse;
import com.mitienda.ecommerce.dto.LoginRequest;
import com.mitienda.ecommerce.services.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para autenticación
 * Endpoints: /api/auth/register y /api/auth/login
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    // El endpoint POST /api/auth/register se eliminó a propósito.
    // La tienda web no tiene inicio de sesión, así que los únicos usuarios son
    // el dueño y los vendedores. Las altas las hace el ADMIN desde
    // /api/users. Dejarlo abierto permitía que cualquiera se creara una cuenta
    // contra la API sin pasar por el sistema.

    /**
     * POST /api/auth/login
     * Login de usuario existente
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            // Único caso que realmente son credenciales inválidas: usuario
            // inexistente, contraseña incorrecta o cuenta desactivada.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
        } catch (RuntimeException e) {
            // Cualquier otra excepción (base de datos caída, columna que no
            // coincide con la entidad, etc.) NO es un problema de credenciales.
            // Antes se devolvía "Credenciales inválidas" para todo, lo que
            // ocultaba la causa real y volvía imposible diagnosticar.
            log.error("Error no relacionado con credenciales durante el login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno al iniciar sesión: " + e.getMessage());
        }
    }

    /**
     * GET /api/auth/test
     * Endpoint de prueba (público)
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("✅ API funcionando correctamente");
    }
}
