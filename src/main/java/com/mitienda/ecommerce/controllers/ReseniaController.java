package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.ReseniaRequest;
import com.mitienda.ecommerce.dto.ReseniaResponse;
import com.mitienda.ecommerce.services.ReseniaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de reseñas
 */
@RestController
@RequestMapping("/api/resenias")
@CrossOrigin(origins = "http://localhost:3000")
public class ReseniaController {

    @Autowired
    private ReseniaService reseniaService;

    /**
     * GET /api/resenias
     * Listar todas las reseñas (ADMIN)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReseniaResponse>> getAllResenias() {
        List<ReseniaResponse> resenias = reseniaService.getAllResenias();
        return ResponseEntity.ok(resenias);
    }

    /**
     * GET /api/resenias/{id}
     * Obtener reseña por ID (ADMIN)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getReseniaById(@PathVariable Long id) {
        try {
            ReseniaResponse resenia = reseniaService.getReseniaById(id);
            return ResponseEntity.ok(resenia);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/resenias/producto/{idProducto}
     * Obtener reseñas aprobadas de un producto (público)
     */
    @GetMapping("/producto/{idProducto}")
    public ResponseEntity<List<ReseniaResponse>> getReseniasByProducto(@PathVariable Long idProducto) {
        List<ReseniaResponse> resenias = reseniaService.getReseniasByProducto(idProducto);
        return ResponseEntity.ok(resenias);
    }

    /**
     * GET /api/resenias/producto/{idProducto}/todas
     * Obtener todas las reseñas de un producto (ADMIN)
     */
    @GetMapping("/producto/{idProducto}/todas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReseniaResponse>> getAllReseniasByProducto(@PathVariable Long idProducto) {
        List<ReseniaResponse> resenias = reseniaService.getAllReseniasByProducto(idProducto);
        return ResponseEntity.ok(resenias);
    }

    /**
     * GET /api/resenias/cliente/{idCliente}
     * Obtener reseñas de un cliente (CLIENTE o ADMIN)
     */
    @GetMapping("/cliente/{idCliente}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<List<ReseniaResponse>> getReseniasByCliente(@PathVariable Long idCliente) {
        List<ReseniaResponse> resenias = reseniaService.getReseniasByCliente(idCliente);
        return ResponseEntity.ok(resenias);
    }

    /**
     * GET /api/resenias/pendientes
     * Obtener reseñas pendientes de aprobación (ADMIN)
     */
    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReseniaResponse>> getReseniasPendientes() {
        List<ReseniaResponse> resenias = reseniaService.getReseniasPendientes();
        return ResponseEntity.ok(resenias);
    }

    /**
     * POST /api/resenias
     * Crear nueva reseña (CLIENTE o público)
     */
    @PostMapping
    public ResponseEntity<?> createResenia(@Valid @RequestBody ReseniaRequest request) {
        try {
            ReseniaResponse createdResenia = reseniaService.createResenia(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdResenia);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/resenias/{id}/aprobar
     * Aprobar reseña (ADMIN)
     */
    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> aprobarResenia(@PathVariable Long id) {
        try {
            ReseniaResponse resenia = reseniaService.aprobarResenia(id);
            return ResponseEntity.ok(resenia);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/resenias/{id}/rechazar
     * Rechazar reseña (ADMIN)
     */
    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rechazarResenia(@PathVariable Long id) {
        try {
            ReseniaResponse resenia = reseniaService.rechazarResenia(id);
            return ResponseEntity.ok(resenia);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/resenias/{id}
     * Eliminar reseña (ADMIN)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteResenia(@PathVariable Long id) {
        try {
            reseniaService.deleteResenia(id);
            return ResponseEntity.ok(Map.of("message", "Reseña eliminada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/resenias/producto/{idProducto}/promedio
     * Obtener calificación promedio de un producto (público)
     */
    @GetMapping("/producto/{idProducto}/promedio")
    public ResponseEntity<?> getCalificacionPromedio(@PathVariable Long idProducto) {
        Double promedio = reseniaService.getCalificacionPromedio(idProducto);
        Long totalResenias = reseniaService.countReseniasByProducto(idProducto);
        
        return ResponseEntity.ok(Map.of(
            "promedio", promedio,
            "totalResenias", totalResenias
        ));
    }
}