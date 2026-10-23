package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.TransportadoraRequest;
import com.mitienda.ecommerce.dto.TransportadoraResponse;
import com.mitienda.ecommerce.services.TransportadoraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Controlador REST para gestión de transportadoras */
@RestController
@RequestMapping("/api/transportadoras")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
public class TransportadoraController {
    private final TransportadoraService transportadoraService;

    public TransportadoraController(TransportadoraService transportadoraService) {
        this.transportadoraService = transportadoraService;
    }

    /** Listar todas las transportadoras */
    @GetMapping
    public ResponseEntity<List<TransportadoraResponse>> getAllTransportadoras() {
        List<TransportadoraResponse> transportadoras = transportadoraService.getAllTransportadoras();
        return ResponseEntity.ok(transportadoras);
    }

    /** Listar solo transportadoras activas */
    @GetMapping("/activas")
    public ResponseEntity<List<TransportadoraResponse>> getActiveTransportadoras() {
        List<TransportadoraResponse> transportadoras = transportadoraService.getActiveTransportadoras();
        return ResponseEntity.ok(transportadoras);
    }

    /** Obtener transportadora por ID */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTransportadoraById(@PathVariable Long id) {
        try {
            TransportadoraResponse transportadora = transportadoraService.getTransportadoraById(id);
            return ResponseEntity.ok(transportadora);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** Crear nueva transportadora (ADMIN) */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createTransportadora(@Valid @RequestBody TransportadoraRequest request) {
        try {
            TransportadoraResponse createdTransportadora = transportadoraService.createTransportadora(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTransportadora);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** Actualizar transportadora (ADMIN) */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateTransportadora(@PathVariable Long id, 
                                                 @Valid @RequestBody TransportadoraRequest request) {
        try {
            TransportadoraResponse updatedTransportadora = transportadoraService.updateTransportadora(id, request);
            return ResponseEntity.ok(updatedTransportadora);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** Eliminar transportadora (ADMIN) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTransportadora(@PathVariable Long id) {
        try {
            transportadoraService.deleteTransportadora(id);
            return ResponseEntity.ok(Map.of("message", "Transportadora desactivada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** Activar/Desactivar transportadora (ADMIN) */
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleTransportadoraStatus(@PathVariable Long id) {
        try {
            TransportadoraResponse transportadora = transportadoraService.toggleTransportadoraStatus(id);
            return ResponseEntity.ok(transportadora);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** Obtener estadísticas de transportadoras (ADMIN) */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getTransportadoraStatistics() {
        Long totalActivas = transportadoraService.countActiveTransportadoras();
        return ResponseEntity.ok(Map.of("activas", totalActivas));
    }
}