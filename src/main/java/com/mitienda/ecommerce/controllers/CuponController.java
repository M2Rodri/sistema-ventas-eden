package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.*;
import com.mitienda.ecommerce.services.CuponService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de cupones
 */
@RestController
@RequestMapping("/api/cupones")
@CrossOrigin(origins = "http://localhost:3000")
public class CuponController {

    @Autowired
    private CuponService cuponService;

    /**
     * GET /api/cupones
     * Listar todos los cupones (ADMIN)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CuponResponse>> getAllCupones() {
        List<CuponResponse> cupones = cuponService.getAllCupones();
        return ResponseEntity.ok(cupones);
    }

    /**
     * GET /api/cupones/activos
     * Listar cupones activos (ADMIN)
     */
    @GetMapping("/activos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CuponResponse>> getActiveCupones() {
        List<CuponResponse> cupones = cuponService.getActiveCupones();
        return ResponseEntity.ok(cupones);
    }

    /**
     * GET /api/cupones/vigentes
     * Listar cupones vigentes (público)
     */
    @GetMapping("/vigentes")
    public ResponseEntity<List<CuponResponse>> getCuponesVigentes() {
        List<CuponResponse> cupones = cuponService.getCuponesVigentes();
        return ResponseEntity.ok(cupones);
    }

    /**
     * GET /api/cupones/{id}
     * Obtener cupón por ID (ADMIN)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCuponById(@PathVariable Long id) {
        try {
            CuponResponse cupon = cuponService.getCuponById(id);
            return ResponseEntity.ok(cupon);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/cupones/codigo/{codigo}
     * Obtener cupón por código (ADMIN)
     */
    @GetMapping("/codigo/{codigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCuponByCodigo(@PathVariable String codigo) {
        try {
            CuponResponse cupon = cuponService.getCuponByCodigo(codigo);
            return ResponseEntity.ok(cupon);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/cupones
     * Crear nuevo cupón (ADMIN)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCupon(@Valid @RequestBody CuponRequest request) {
        try {
            CuponResponse createdCupon = cuponService.createCupon(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCupon);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/cupones/{id}
     * Actualizar cupón (ADMIN)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCupon(@PathVariable Long id, 
                                        @Valid @RequestBody CuponRequest request) {
        try {
            CuponResponse updatedCupon = cuponService.updateCupon(id, request);
            return ResponseEntity.ok(updatedCupon);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/cupones/{id}
     * Eliminar cupón (ADMIN)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCupon(@PathVariable Long id) {
        try {
            cuponService.deleteCupon(id);
            return ResponseEntity.ok(Map.of("message", "Cupón desactivado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/cupones/{id}/toggle-status
     * Activar/Desactivar cupón (ADMIN)
     */
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleCuponStatus(@PathVariable Long id) {
        try {
            CuponResponse cupon = cuponService.toggleCuponStatus(id);
            return ResponseEntity.ok(cupon);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/cupones/validar
     * Validar cupón (público)
     */
    @PostMapping("/validar")
    public ResponseEntity<ValidarCuponResponse> validarCupon(@Valid @RequestBody ValidarCuponRequest request) {
        ValidarCuponResponse validacion = cuponService.validarCupon(request);
        return ResponseEntity.ok(validacion);
    }

    /**
     * GET /api/cupones/estadisticas
     * Obtener estadísticas de cupones (ADMIN)
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCuponStatistics() {
        Long totalActivos = cuponService.countActiveCupones();
        return ResponseEntity.ok(Map.of("activos", totalActivos));
    }
}