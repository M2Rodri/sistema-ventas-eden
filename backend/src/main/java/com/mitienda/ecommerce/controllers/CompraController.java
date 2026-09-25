package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.CompraRequest;
import com.mitienda.ecommerce.dto.CompraResponse;
import com.mitienda.ecommerce.models.EstadoCompra;
import com.mitienda.ecommerce.services.CompraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de compras
 */
@RestController
@RequestMapping("/api/compras")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasRole('ADMIN')")
public class CompraController {

    private final CompraService compraService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }


    /**
     * GET /api/compras
     * Listar todas las compras
     */
    @GetMapping
    public ResponseEntity<List<CompraResponse>> getAllCompras() {
        List<CompraResponse> compras = compraService.getAllCompras();
        return ResponseEntity.ok(compras);
    }

    /**
     * GET /api/compras/{id}
     * Obtener compra por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCompraById(@PathVariable Long id) {
        try {
            CompraResponse compra = compraService.getCompraById(id);
            return ResponseEntity.ok(compra);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/compras
     * Crear nueva compra
     */
    @PostMapping
    public ResponseEntity<?> createCompra(@Valid @RequestBody CompraRequest request) {
        try {
            CompraResponse createdCompra = compraService.createCompra(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCompra);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/compras/{id}
     * Editar una compra pendiente (proveedor, factura, notas y productos)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCompra(@PathVariable Long id, @Valid @RequestBody CompraRequest request) {
        try {
            CompraResponse updatedCompra = compraService.updateCompra(id, request);
            return ResponseEntity.ok(updatedCompra);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/compras/{id}/estado
     * Cambiar estado de la compra
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstadoCompra(@PathVariable Long id, 
                                                 @RequestParam EstadoCompra nuevoEstado) {
        try {
            CompraResponse compra = compraService.cambiarEstadoCompra(id, nuevoEstado);
            return ResponseEntity.ok(compra);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/compras/{id}/recibir
     * Marcar compra como recibida y actualizar inventario
     */
    @PatchMapping("/{id}/recibir")
    public ResponseEntity<?> recibirCompra(@PathVariable Long id) {
        try {
            CompraResponse compra = compraService.recibirCompra(id);
            return ResponseEntity.ok(compra);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/compras/{id}/cancelar
     * Cancelar compra
     */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelarCompra(@PathVariable Long id) {
        try {
            CompraResponse compra = compraService.cancelarCompra(id);
            return ResponseEntity.ok(compra);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}