package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.VentaRequest;
import com.mitienda.ecommerce.dto.VentaResponse;
import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.MetodoPago;
import com.mitienda.ecommerce.services.VentaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Controlador REST para gestión de ventas
 */
@RestController
@RequestMapping("/api/ventas")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
public class VentaController {

    @Autowired
    private VentaService ventaService;

    /**
     * GET /api/ventas
     * Listar todas las ventas
     */
    @GetMapping
    public ResponseEntity<List<VentaResponse>> getAllVentas() {
        List<VentaResponse> ventas = ventaService.getAllVentas();
        return ResponseEntity.ok(ventas);
    }

    /**
     * GET /api/ventas/{id}
     * Obtener venta por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getVentaById(@PathVariable Long id) {
        try {
            VentaResponse venta = ventaService.getVentaById(id);
            return ResponseEntity.ok(venta);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/ventas
     * Crear venta directa
     */
    @PostMapping
    public ResponseEntity<?> createVentaDirecta(@Valid @RequestBody VentaRequest request,
                                                @RequestParam Long idUsuario) {
        try {
            VentaResponse createdVenta = ventaService.createVentaDirecta(request, idUsuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdVenta);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/ventas/{id}/cancelar
     * Cancelar venta
     */
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cancelarVenta(@PathVariable Long id) {
        try {
            VentaResponse venta = ventaService.cancelarVenta(id);
            return ResponseEntity.ok(venta);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/ventas/cliente/{clienteId}
     * Listar ventas de un cliente
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<VentaResponse>> getVentasByCliente(@PathVariable Long clienteId) {
        List<VentaResponse> ventas = ventaService.getVentasByCliente(clienteId);
        return ResponseEntity.ok(ventas);
    }

    /**
     * GET /api/ventas/estado/{estado}
     * Filtrar ventas por estado
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<?> getVentasByEstado(@PathVariable String estado) {
        try {
            EstadoVenta estadoEnum = EstadoVenta.valueOf(estado.toUpperCase());
            List<VentaResponse> ventas = ventaService.getVentasByEstado(estadoEnum);
            return ResponseEntity.ok(ventas);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Estado de venta inválido: " + estado));
        }
    }

    /**
     * GET /api/ventas/del-dia
     * Ventas del día actual
     */
    @GetMapping("/del-dia")
    public ResponseEntity<List<VentaResponse>> getVentasDelDia() {
        List<VentaResponse> ventas = ventaService.getVentasDelDia();
        return ResponseEntity.ok(ventas);
    }

    /**
     * GET /api/ventas/ultimas
     * Últimas 10 ventas
     */
    @GetMapping("/ultimas")
    public ResponseEntity<List<VentaResponse>> getUltimasVentas() {
        List<VentaResponse> ventas = ventaService.getUltimasVentas();
        return ResponseEntity.ok(ventas);
    }

    /**
     * GET /api/ventas/fechas?inicio=...&fin=...
     * Ventas entre fechas
     */
    @GetMapping("/fechas")
    public ResponseEntity<List<VentaResponse>> getVentasByFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        List<VentaResponse> ventas = ventaService.getVentasByFechas(inicio, fin);
        return ResponseEntity.ok(ventas);
    }

    /**
     * GET /api/ventas/total-fechas?inicio=...&fin=...
     * Total de ventas en un rango de fechas
     */
    @GetMapping("/total-fechas")
    public ResponseEntity<?> getTotalVentasByFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        BigDecimal total = ventaService.getTotalVentasByFechas(inicio, fin);
        return ResponseEntity.ok(Map.of("total", total));
    }

    /**
     * GET /api/ventas/estadisticas
     * Obtener estadísticas de ventas (ADMIN)
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getVentaStatistics() {
        Long totalCompletadas = ventaService.countVentasByEstado(EstadoVenta.COMPLETADA);
        Long totalPendientes  = ventaService.countVentasByEstado(EstadoVenta.PENDIENTE_PAGO);
        Long totalCanceladas  = ventaService.countVentasByEstado(EstadoVenta.CANCELADA);

        return ResponseEntity.ok(Map.of(
                "completadas", totalCompletadas,
                "pendientes",  totalPendientes,
                "canceladas",  totalCanceladas
        ));
    }
}