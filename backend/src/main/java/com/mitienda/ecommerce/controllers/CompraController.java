package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.CompraRequest;
import com.mitienda.ecommerce.dto.CompraResponse;
import com.mitienda.ecommerce.models.EstadoCompra;
import com.mitienda.ecommerce.services.CompraService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    /**
     * GET /api/compras/proveedor/{proveedorId}
     * Listar compras de un proveedor
     */
    @GetMapping("/proveedor/{proveedorId}")
    public ResponseEntity<List<CompraResponse>> getComprasByProveedor(@PathVariable Long proveedorId) {
        List<CompraResponse> compras = compraService.getComprasByProveedor(proveedorId);
        return ResponseEntity.ok(compras);
    }

    /**
     * GET /api/compras/estado/{estado}
     * Filtrar compras por estado
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<?> getComprasByEstado(@PathVariable String estado) {
        try {
            EstadoCompra estadoEnum = EstadoCompra.valueOf(estado.toUpperCase());
            List<CompraResponse> compras = compraService.getComprasByEstado(estadoEnum);
            return ResponseEntity.ok(compras);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Estado de compra inválido: " + estado));
        }
    }

    /**
     * GET /api/compras/ultimas
     * Últimas 10 compras
     */
    @GetMapping("/ultimas")
    public ResponseEntity<List<CompraResponse>> getUltimasCompras() {
        List<CompraResponse> compras = compraService.getUltimasCompras();
        return ResponseEntity.ok(compras);
    }

    /**
     * GET /api/compras/fechas?inicio=...&fin=...
     * Compras entre fechas
     */
    @GetMapping("/fechas")
    public ResponseEntity<List<CompraResponse>> getComprasByFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        List<CompraResponse> compras = compraService.getComprasByFechas(inicio, fin);
        return ResponseEntity.ok(compras);
    }

    /**
     * GET /api/compras/total-fechas?inicio=...&fin=...
     * Total de compras en un rango de fechas
     */
    @GetMapping("/total-fechas")
    public ResponseEntity<?> getTotalComprasByFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        BigDecimal total = compraService.getTotalComprasByFechas(inicio, fin);
        return ResponseEntity.ok(Map.of("total", total));
    }

    /**
     * GET /api/compras/estadisticas
     * Obtener estadísticas de compras
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<?> getCompraStatistics() {
        Long totalPendientes = compraService.countComprasByEstado(EstadoCompra.PENDIENTE);
        Long totalRecibidas = compraService.countComprasByEstado(EstadoCompra.RECIBIDA);
        Long totalCanceladas = compraService.countComprasByEstado(EstadoCompra.CANCELADA);

        return ResponseEntity.ok(Map.of(
            "pendientes", totalPendientes,
            "recibidas", totalRecibidas,
            "canceladas", totalCanceladas
        ));
    }
}