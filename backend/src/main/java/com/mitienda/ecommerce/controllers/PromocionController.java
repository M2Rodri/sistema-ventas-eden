package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.PromocionRequest;
import com.mitienda.ecommerce.dto.PromocionResponse;
import com.mitienda.ecommerce.services.PromocionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de promociones
 */
@RestController
@RequestMapping("/api/promociones")
@CrossOrigin(origins = "http://localhost:3000")
public class PromocionController {

    private final PromocionService promocionService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public PromocionController(PromocionService promocionService) {
        this.promocionService = promocionService;
    }


    /**
     * GET /api/promociones
     * Listar todas las promociones (ADMIN)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PromocionResponse>> getAllPromociones() {
        List<PromocionResponse> promociones = promocionService.getAllPromociones();
        return ResponseEntity.ok(promociones);
    }

    /**
     * GET /api/promociones/activas
     * Listar solo promociones activas (público)
     */
    @GetMapping("/activas")
    public ResponseEntity<List<PromocionResponse>> getActivePromociones() {
        List<PromocionResponse> promociones = promocionService.getActivePromociones();
        return ResponseEntity.ok(promociones);
    }

    /**
     * GET /api/promociones/vigentes
     * Listar promociones vigentes (público - para tienda virtual)
     */
    @GetMapping("/vigentes")
    public ResponseEntity<List<PromocionResponse>> getPromocionesVigentes() {
        List<PromocionResponse> promociones = promocionService.getPromocionesVigentes();
        return ResponseEntity.ok(promociones);
    }

    /**
     * GET /api/promociones/{id}
     * Obtener promocion por ID (público)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPromocionById(@PathVariable Long id) {
        try {
            PromocionResponse promocion = promocionService.getPromocionById(id);
            return ResponseEntity.ok(promocion);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/promociones
     * Crear nueva promocion (ADMIN)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPromocion(@Valid @RequestBody PromocionRequest request) {
        try {
            PromocionResponse createdPromocion = promocionService.createPromocion(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPromocion);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/promociones/{id}
     * Actualizar promocion existente (ADMIN)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePromocion(@PathVariable Long id, 
                                         @Valid @RequestBody PromocionRequest request) {
        try {
            PromocionResponse updatedPromocion = promocionService.updatePromocion(id, request);
            return ResponseEntity.ok(updatedPromocion);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/promociones/{id}
     * Eliminar promocion (desactivar) (ADMIN)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletePromocion(@PathVariable Long id) {
        try {
            promocionService.deletePromocion(id);
            return ResponseEntity.ok(Map.of("message", "Promocion desactivada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/promociones/{id}/toggle-status
     * Activar/Desactivar promocion (ADMIN)
     */
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> togglePromocionStatus(@PathVariable Long id) {
        try {
            PromocionResponse promocion = promocionService.togglePromocionStatus(id);
            return ResponseEntity.ok(promocion);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/promociones/estadisticas
     * Obtener estadísticas de promociones (ADMIN)
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPromocionStatistics() {
        Long totalActivas = promocionService.countActivePromociones();
        return ResponseEntity.ok(Map.of("activas", totalActivas));
    }
}