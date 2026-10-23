package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.exception.PeticionInvalidaException;
import com.mitienda.ecommerce.dto.ComprobanteRequest;
import com.mitienda.ecommerce.dto.DatosEntregaRequest;
import com.mitienda.ecommerce.dto.FechaLimitePagoRequest;
import com.mitienda.ecommerce.dto.VentaRequest;
import com.mitienda.ecommerce.dto.VentaResponse;
import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.MetodoPago;
import com.mitienda.ecommerce.models.TipoComprobante;
import com.mitienda.ecommerce.services.ComprobanteService;
import com.mitienda.ecommerce.services.VentaService;
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

/** Controlador REST para gestión de ventas */
@RestController
@RequestMapping("/api/v1/ventas")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
public class VentaController {
    private final VentaService ventaService;

    private final ComprobanteService comprobanteService;

    public VentaController(VentaService ventaService, ComprobanteService comprobanteService) {
        this.ventaService = ventaService;
        this.comprobanteService = comprobanteService;
    }

    /** Listar todas las ventas */
    @GetMapping
    public ResponseEntity<List<VentaResponse>> getAllVentas() {
        List<VentaResponse> ventas = ventaService.getAllVentas();
        return ResponseEntity.ok(ventas);
    }

    /** Obtener venta por ID */
    @GetMapping("/{id}")
    public ResponseEntity<?> getVentaById(@PathVariable Long id) {
        VentaResponse venta = ventaService.getVentaById(id);
        return ResponseEntity.ok(venta);
    }

    /** Crear venta directa */
    @PostMapping
    public ResponseEntity<?> createVentaDirecta(@Valid @RequestBody VentaRequest request) {
        VentaResponse createdVenta = ventaService.createVentaDirecta(request);

        // El comprobante se genera aparte, ya confirmada la venta: si falla, la venta no se revierte.
        try {
            ComprobanteRequest comprobanteRequest = new ComprobanteRequest();
            comprobanteRequest.setIdVenta(createdVenta.getId());
            comprobanteRequest.setTipoComprobante(TipoComprobante.RECIBO);
            comprobanteRequest.setNombreCliente(createdVenta.getNombreCliente());
            comprobanteService.createComprobante(comprobanteRequest);
        } catch (Exception e) {
            System.err.println("Error al generar comprobante de la venta "
                    + createdVenta.getId() + ": " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(createdVenta);
    }

    /** Cancelar venta */
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cancelarVenta(@PathVariable Long id) {
        VentaResponse venta = ventaService.cancelarVenta(id);
        return ResponseEntity.ok(venta);
    }

    /** Marcar la entrega de una venta como completada (ADMIN y EMPLEADO). */
    @PatchMapping("/{id}/entregar")
    public ResponseEntity<?> marcarEntregado(@PathVariable Long id) {
        VentaResponse venta = ventaService.marcarEntregado(id);
        return ResponseEntity.ok(venta);
    }

    /** Corregir una entrega marcada por error: ENTREGADO -> PENDIENTE (solo ADMIN). */
    @PatchMapping("/{id}/deshacer-entrega")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deshacerEntrega(@PathVariable Long id) {
        VentaResponse venta = ventaService.deshacerEntrega(id);
        return ResponseEntity.ok(venta);
    }

    /** Completar o corregir la dirección, la transportadora y la guía (solo ADMIN). */
    @PatchMapping("/{id}/datos-entrega")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> actualizarDatosEntrega(@PathVariable Long id,
                                                    @Valid @RequestBody DatosEntregaRequest request) {
        VentaResponse venta = ventaService.actualizarDatosEntrega(id, request);
        return ResponseEntity.ok(venta);
    }

    /** Poner o cambiar hasta cuándo se espera el pago pendiente (ADMIN y EMPLEADO). */
    @PatchMapping("/{id}/fecha-limite-pago")
    public ResponseEntity<?> actualizarFechaLimitePago(@PathVariable Long id,
                                                       @Valid @RequestBody FechaLimitePagoRequest request) {
        VentaResponse venta = ventaService.actualizarFechaLimitePago(id, request);
        return ResponseEntity.ok(venta);
    }

    /** Listar ventas de un cliente */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<VentaResponse>> getVentasByCliente(@PathVariable Long clienteId) {
        List<VentaResponse> ventas = ventaService.getVentasByCliente(clienteId);
        return ResponseEntity.ok(ventas);
    }

    /** Filtrar ventas por estado */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<?> getVentasByEstado(@PathVariable String estado) {
        EstadoVenta estadoEnum;
        try {
            estadoEnum = EstadoVenta.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PeticionInvalidaException("ESTADO_INVALIDO", "Estado de venta inválido: " + estado);
        }
        List<VentaResponse> ventas = ventaService.getVentasByEstado(estadoEnum);
        return ResponseEntity.ok(ventas);
    }

    /** Ventas del día actual */
    @GetMapping("/del-dia")
    public ResponseEntity<List<VentaResponse>> getVentasDelDia() {
        List<VentaResponse> ventas = ventaService.getVentasDelDia();
        return ResponseEntity.ok(ventas);
    }

    /** Últimas 10 ventas */
    @GetMapping("/ultimas")
    public ResponseEntity<List<VentaResponse>> getUltimasVentas() {
        List<VentaResponse> ventas = ventaService.getUltimasVentas();
        return ResponseEntity.ok(ventas);
    }

    /** Ventas entre fechas */
    @GetMapping("/fechas")
    public ResponseEntity<List<VentaResponse>> getVentasByFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        List<VentaResponse> ventas = ventaService.getVentasByFechas(inicio, fin);
        return ResponseEntity.ok(ventas);
    }

    /** Total de ventas en un rango de fechas */
    @GetMapping("/total-fechas")
    public ResponseEntity<?> getTotalVentasByFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        BigDecimal total = ventaService.getTotalVentasByFechas(inicio, fin);
        return ResponseEntity.ok(Map.of("total", total));
    }

    /** Obtener estadísticas de ventas (ADMIN) */
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