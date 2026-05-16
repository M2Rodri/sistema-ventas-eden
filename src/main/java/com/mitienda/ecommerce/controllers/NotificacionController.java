package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.NotificacionDTO;
import com.mitienda.ecommerce.dto.NotificacionRequest;
import com.mitienda.ecommerce.models.Notificacion;
import com.mitienda.ecommerce.services.NotificacionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para Notificaciones
 */
@RestController
@RequestMapping("/api/notificaciones")
@CrossOrigin(origins = "http://localhost:3000")
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    /**
     * Obtener todas las notificaciones del usuario autenticado
     * GET /api/notificaciones
     */
    @GetMapping
    public ResponseEntity<List<NotificacionDTO>> obtenerNotificaciones(Authentication authentication) {
        Long usuarioId = obtenerUsuarioId(authentication);
        List<Notificacion> notificaciones = notificacionService.obtenerNotificacionesUsuario(usuarioId);
        
        List<NotificacionDTO> dtos = notificaciones.stream()
                .map(n -> notificacionService.convertirADTO(n))
                .toList();
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * Obtener notificaciones no leídas
     * GET /api/notificaciones/no-leidas
     */
    @GetMapping("/no-leidas")
    public ResponseEntity<List<NotificacionDTO>> obtenerNoLeidas(Authentication authentication) {
        Long usuarioId = obtenerUsuarioId(authentication);
        List<Notificacion> notificaciones = notificacionService.obtenerNotificacionesNoLeidas(usuarioId);
        
        List<NotificacionDTO> dtos = notificaciones.stream()
                .map(n -> notificacionService.convertirADTO(n))
                .toList();
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * Contar notificaciones no leídas
     * GET /api/notificaciones/contador
     */
    @GetMapping("/contador")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(Authentication authentication) {
        Long usuarioId = obtenerUsuarioId(authentication);
        Long contador = notificacionService.contarNoLeidas(usuarioId);
        
        Map<String, Long> response = new HashMap<>();
        response.put("noLeidas", contador);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Crear notificación manualmente (solo ADMIN)
     * POST /api/notificaciones
     */
    @PostMapping
    public ResponseEntity<?> crearNotificacion(@Valid @RequestBody NotificacionRequest request) {
        try {
            Notificacion notificacion = notificacionService.crearDesdeDTO(request);
            return ResponseEntity.ok(notificacionService.convertirADTO(notificacion));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Marcar notificación como leída
     * PUT /api/notificaciones/{id}/leida
     */
    @PutMapping("/{id}/leida")
    public ResponseEntity<String> marcarComoLeida(@PathVariable Long id) {
        try {
            notificacionService.marcarComoLeida(id);
            return ResponseEntity.ok("Notificación marcada como leída");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Marcar todas como leídas
     * PUT /api/notificaciones/marcar-todas-leidas
     */
    @PutMapping("/marcar-todas-leidas")
    public ResponseEntity<String> marcarTodasComoLeidas(Authentication authentication) {
        Long usuarioId = obtenerUsuarioId(authentication);
        notificacionService.marcarTodasComoLeidas(usuarioId);
        return ResponseEntity.ok("Todas las notificaciones marcadas como leídas");
    }

    /**
     * Eliminar notificación
     * DELETE /api/notificaciones/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminar(@PathVariable Long id) {
        try {
            notificacionService.eliminarNotificacion(id);
            return ResponseEntity.ok("Notificación eliminada");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Limpiar notificaciones antiguas (solo ADMIN)
     * DELETE /api/notificaciones/limpiar-antiguas
     */
    @DeleteMapping("/limpiar-antiguas")
    public ResponseEntity<String> limpiarAntiguas() {
        notificacionService.limpiarNotificacionesAntiguas();
        return ResponseEntity.ok("Notificaciones antiguas eliminadas");
    }

    // Método helper para obtener el ID del usuario autenticado
    private Long obtenerUsuarioId(Authentication authentication) {
        // Aquí deberías obtener el ID real del usuario desde el token JWT
        // Por ahora retorna un ID de ejemplo
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        // Implementar lógica para obtener ID desde email
        return 1L; // Temporal
    }
}