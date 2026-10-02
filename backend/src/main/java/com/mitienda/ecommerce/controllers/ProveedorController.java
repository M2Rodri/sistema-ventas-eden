package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.ProveedorRequest;
import com.mitienda.ecommerce.dto.ProveedorResponse;
import com.mitienda.ecommerce.services.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de proveedores
 */
@RestController
@RequestMapping("/api/v1/proveedores")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasRole('ADMIN')")
public class ProveedorController {

    private final ProveedorService proveedorService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }


    /**
     * GET /api/v1/proveedores
     * Listar todos los proveedores
     */
    @GetMapping
    public ResponseEntity<List<ProveedorResponse>> getAllProveedores() {
        List<ProveedorResponse> proveedores = proveedorService.getAllProveedores();
        return ResponseEntity.ok(proveedores);
    }

    /**
     * GET /api/v1/proveedores/activos
     * Listar solo proveedores activos
     */
    @GetMapping("/activos")
    public ResponseEntity<List<ProveedorResponse>> getActiveProveedores() {
        List<ProveedorResponse> proveedores = proveedorService.getActiveProveedores();
        return ResponseEntity.ok(proveedores);
    }

    /**
     * GET /api/v1/proveedores/{id}
     * Obtener proveedor por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProveedorById(@PathVariable Long id) {
        ProveedorResponse proveedor = proveedorService.getProveedorById(id);
        return ResponseEntity.ok(proveedor);
    }

    /**
     * POST /api/v1/proveedores
     * Crear nuevo proveedor
     */
    @PostMapping
    public ResponseEntity<?> createProveedor(@Valid @RequestBody ProveedorRequest request) {
        ProveedorResponse createdProveedor = proveedorService.createProveedor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProveedor);
    }

    /**
     * PUT /api/v1/proveedores/{id}
     * Actualizar proveedor existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProveedor(@PathVariable Long id, 
                                            @Valid @RequestBody ProveedorRequest request) {
        ProveedorResponse updatedProveedor = proveedorService.updateProveedor(id, request);
        return ResponseEntity.ok(updatedProveedor);
    }

    /**
     * PATCH /api/v1/proveedores/{id}/toggle-status
     * Activar/Desactivar proveedor
     */
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleProveedorStatus(@PathVariable Long id) {
        ProveedorResponse proveedor = proveedorService.toggleProveedorStatus(id);
        return ResponseEntity.ok(proveedor);
    }

    /**
     * GET /api/v1/proveedores/buscar?nombre=...
     * Buscar proveedores por nombre
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<ProveedorResponse>> searchProveedores(@RequestParam String nombre) {
        List<ProveedorResponse> proveedores = proveedorService.searchProveedores(nombre);
        return ResponseEntity.ok(proveedores);
    }
}