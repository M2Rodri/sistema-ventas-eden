package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.AuthResponse;
import com.mitienda.ecommerce.dto.LoginRequest;
import com.mitienda.ecommerce.exception.CredencialesInvalidasException;
import com.mitienda.ecommerce.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para autenticación
 * Endpoints: /api/v1/auth/register y /api/v1/auth/login
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

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


    // El endpoint POST /api/v1/auth/register se eliminó a propósito.
    // La tienda web no tiene inicio de sesión, así que los únicos usuarios son
    // el dueño y los vendedores. Las altas las hace el ADMIN desde
    // /api/v1/users. Dejarlo abierto permitía que cualquiera se creara una cuenta
    // contra la API sin pasar por el sistema.

    /**
     * POST /api/v1/auth/login
     * Login de usuario existente
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            // Único caso que realmente son credenciales inválidas: usuario
            // inexistente, contraseña incorrecta o cuenta desactivada. Cualquier
            // otra excepción (base de datos caída, columna que no coincide con la
            // entidad, etc.) NO es un problema de credenciales: sigue de largo y el
            // manejador global la registra y responde 500.
            throw new CredencialesInvalidasException();
        }
    }
}
