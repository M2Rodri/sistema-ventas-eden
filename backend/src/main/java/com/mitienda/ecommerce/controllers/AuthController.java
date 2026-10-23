package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.AuthResponse;
import com.mitienda.ecommerce.dto.LoginRequest;
import com.mitienda.ecommerce.exception.CredencialesInvalidasException;
import com.mitienda.ecommerce.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

/** Inicio de sesión, renovación del token y cierre de sesiones. */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // No hay registro público: las altas las hace el ADMIN desde /api/v1/usuarios.

    /** Login de usuario existente */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            // Solo las credenciales malas dan 401; cualquier otro error sigue al manejador global (500).
            throw new CredencialesInvalidasException();
        }
    }

    /** Renueva el token mientras la sesión viva y dentro de su tope; sin token válido responde 401. */
    @PostMapping("/renovar")
    public ResponseEntity<AuthResponse> renovar(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(authService.renovar(authorization));
    }

    /** Cierra todas las sesiones del usuario del token en todos sus dispositivos; cualquiera puede cortar las suyas. */
    @PostMapping("/cerrar-sesiones")
    public ResponseEntity<Void> cerrarSesiones(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.cerrarSesiones(authorization);
        return ResponseEntity.noContent().build();
    }
}
