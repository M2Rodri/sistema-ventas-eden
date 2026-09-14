package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.UsuarioRequest;
import com.mitienda.ecommerce.dto.UsuarioResponse;
import com.mitienda.ecommerce.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de usuarios
 * Solo accesible por usuarios con rol ADMIN
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }


    /**
     * GET /api/usuarios
     * Listar todos los usuarios
     */
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> getAllUsers() {
        List<UsuarioResponse> users = usuarioService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/usuarios/{id}
     * Obtener usuario por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            UsuarioResponse user = usuarioService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/usuarios
     * Crear nuevo usuario
     */
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UsuarioRequest request) {
        try {
            UsuarioResponse createdUser = usuarioService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/usuarios/{id}
     * Actualizar usuario existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, 
                                       @Valid @RequestBody UsuarioRequest request) {
        try {
            UsuarioResponse updatedUser = usuarioService.updateUser(id, request);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/usuarios/{id}
     * Eliminar usuario (desactivar)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            usuarioService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "Usuario desactivado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/usuarios/{id}/toggle-status
     * Activar/Desactivar usuario
     */
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        try {
            UsuarioResponse user = usuarioService.toggleUserStatus(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/usuarios/activos
     * Listar solo usuarios activos
     */
    @GetMapping("/activos")
    public ResponseEntity<List<UsuarioResponse>> getActiveUsers() {
        List<UsuarioResponse> users = usuarioService.getActiveUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/usuarios/rol/{role}
     * Filtrar usuarios por rol
     */
    @GetMapping("/rol/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
        // El rol ya no es un enum: se compara contra el nombre guardado en la
        // tabla 'roles', así que un rol agregado por el ADMIN funciona acá sin
        // tocar código.
        List<UsuarioResponse> users = usuarioService.getUsersByRole(role);
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/usuarios/estadisticas
     * Obtener estadísticas de usuarios
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<?> getUserStatistics() {
        Long totalActivos = usuarioService.countActiveUsers();
        Long totalAdmins = usuarioService.countUsersByRole("ADMIN");
        Long totalEmpleados = usuarioService.countUsersByRole("EMPLEADO");

        // Ya no se cuentan CLIENTE: ese rol se eliminó. Los clientes no son
        // usuarios del sistema, viven en la tabla 'clientes' y no tienen acceso.
        return ResponseEntity.ok(Map.of(
            "activos", totalActivos,
            "admins", totalAdmins,
            "empleados", totalEmpleados
        ));
    }
}