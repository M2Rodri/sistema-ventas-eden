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

/** Controlador REST para gestión de promociones */
@RestController
@RequestMapping("/api/promociones")
@CrossOrigin(origins = "http://localhost:3000")
public class PromocionController {
    private final PromocionService promocionService;

    public PromocionController(PromocionService promocionService) {
        this.promocionService = promocionService;
    }

    /** Listar todas las promociones (ADMIN) */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PromocionResponse>> getAllPromociones() {
        List<PromocionResponse> promociones = promocionService.getAllPromociones();
        return ResponseEntity.ok(promociones);
    }

    /** Listar solo promociones activas (público) */
    @GetMapping("/activas")
    public ResponseEntity<List<PromocionResponse>> getActivePromociones() {
        List<PromocionResponse> promociones = promocionService.getActivePromociones();
        return ResponseEntity.ok(promociones);
    }

    /** Listar promociones vigentes (público - para tienda virtual) */
    @GetMapping("/vigentes")
    public ResponseEntity<List<PromocionResponse>> getPromocionesVigentes() {
        List<PromocionResponse> promociones = promocionService.getPromocionesVigentes();
        return ResponseEntity.ok(promociones);
    }

    /** Obtener promocion por ID (público) */
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

    /** Crear nueva promocion (ADMIN) */
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

    /** Actualizar promocion existente (ADMIN) */
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

    /** Eliminar promocion (desactivar) (ADMIN) */
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

    /** Activar/Desactivar promocion (ADMIN) */
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

    /** Obtener estadísticas de promociones (ADMIN) */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPromocionStatistics() {
        Long totalActivas = promocionService.countActivePromociones();
        return ResponseEntity.ok(Map.of("activas", totalActivas));
    }
}