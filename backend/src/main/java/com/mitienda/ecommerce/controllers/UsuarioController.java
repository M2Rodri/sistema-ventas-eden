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

/** Gestión de usuarios; solo ADMIN. */
@RestController
@RequestMapping("/api/v1/usuarios")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Listar todos los usuarios */
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> getAllUsers() {
        List<UsuarioResponse> users = usuarioService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /** Obtener usuario por ID */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        UsuarioResponse user = usuarioService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /** Crear nuevo usuario */
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse createdUser = usuarioService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /** Actualizar usuario existente */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, 
                                       @Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse updatedUser = usuarioService.updateUser(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    /** Eliminar usuario (desactivar) */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        usuarioService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "Usuario desactivado correctamente"));
    }

    /** Activar/Desactivar usuario */
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        UsuarioResponse user = usuarioService.toggleUserStatus(id);
        return ResponseEntity.ok(user);
    }

    /** Cierra las sesiones abiertas de un usuario en todos sus dispositivos (ADMIN), p. ej. por un celular perdido. */
    @PostMapping("/{id}/cerrar-sesiones")
    public ResponseEntity<Void> cerrarSesiones(@PathVariable Long id) {
        usuarioService.cerrarSesiones(id);
        return ResponseEntity.noContent().build();
    }

    /** Listar solo usuarios activos */
    @GetMapping("/activos")
    public ResponseEntity<List<UsuarioResponse>> getActiveUsers() {
        List<UsuarioResponse> users = usuarioService.getActiveUsers();
        return ResponseEntity.ok(users);
    }

    /** Filtrar usuarios por rol */
    @GetMapping("/rol/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
        // El rol es el nombre guardado en la tabla 'roles', no un enum.
        List<UsuarioResponse> users = usuarioService.getUsersByRole(role);
        return ResponseEntity.ok(users);
    }

    /** Obtener estadísticas de usuarios */
    @GetMapping("/estadisticas")
    public ResponseEntity<?> getUserStatistics() {
        Long totalActivos = usuarioService.countActiveUsers();
        Long totalAdmins = usuarioService.countUsersByRole("ADMIN");
        Long totalEmpleados = usuarioService.countUsersByRole("EMPLEADO");

        // Los clientes no son usuarios del sistema: viven en la tabla 'clientes'.
        return ResponseEntity.ok(Map.of(
            "activos", totalActivos,
            "admins", totalAdmins,
            "empleados", totalEmpleados
        ));
    }
}