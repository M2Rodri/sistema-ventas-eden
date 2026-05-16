package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.OfertaRequest;
import com.mitienda.ecommerce.dto.OfertaResponse;
import com.mitienda.ecommerce.services.OfertaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de ofertas
 */
@RestController
@RequestMapping("/api/ofertas")
@CrossOrigin(origins = "http://localhost:3000")
public class OfertaController {

    @Autowired
    private OfertaService ofertaService;

    /**
     * GET /api/ofertas
     * Listar todas las ofertas (ADMIN)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OfertaResponse>> getAllOfertas() {
        List<OfertaResponse> ofertas = ofertaService.getAllOfertas();
        return ResponseEntity.ok(ofertas);
    }

    /**
     * GET /api/ofertas/activas
     * Listar solo ofertas activas (público)
     */
    @GetMapping("/activas")
    public ResponseEntity<List<OfertaResponse>> getActiveOfertas() {
        List<OfertaResponse> ofertas = ofertaService.getActiveOfertas();
        return ResponseEntity.ok(ofertas);
    }

    /**
     * GET /api/ofertas/vigentes
     * Listar ofertas vigentes (público - para tienda virtual)
     */
    @GetMapping("/vigentes")
    public ResponseEntity<List<OfertaResponse>> getOfertasVigentes() {
        List<OfertaResponse> ofertas = ofertaService.getOfertasVigentes();
        return ResponseEntity.ok(ofertas);
    }

    /**
     * GET /api/ofertas/{id}
     * Obtener oferta por ID (público)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOfertaById(@PathVariable Long id) {
        try {
            OfertaResponse oferta = ofertaService.getOfertaById(id);
            return ResponseEntity.ok(oferta);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/ofertas
     * Crear nueva oferta (ADMIN)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createOferta(@Valid @RequestBody OfertaRequest request) {
        try {
            OfertaResponse createdOferta = ofertaService.createOferta(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOferta);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/ofertas/{id}
     * Actualizar oferta existente (ADMIN)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateOferta(@PathVariable Long id, 
                                         @Valid @RequestBody OfertaRequest request) {
        try {
            OfertaResponse updatedOferta = ofertaService.updateOferta(id, request);
            return ResponseEntity.ok(updatedOferta);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/ofertas/{id}
     * Eliminar oferta (desactivar) (ADMIN)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteOferta(@PathVariable Long id) {
        try {
            ofertaService.deleteOferta(id);
            return ResponseEntity.ok(Map.of("message", "Oferta desactivada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/ofertas/{id}/toggle-status
     * Activar/Desactivar oferta (ADMIN)
     */
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleOfertaStatus(@PathVariable Long id) {
        try {
            OfertaResponse oferta = ofertaService.toggleOfertaStatus(id);
            return ResponseEntity.ok(oferta);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/ofertas/estadisticas
     * Obtener estadísticas de ofertas (ADMIN)
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOfertaStatistics() {
        Long totalActivas = ofertaService.countActiveOfertas();
        return ResponseEntity.ok(Map.of("activas", totalActivas));
    }
}