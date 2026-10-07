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

/**
 * Controlador REST para gestión de ventas
 */
@RestController
@RequestMapping("/api/v1/ventas")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
public class VentaController {

    private final VentaService ventaService;

    private final ComprobanteService comprobanteService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public VentaController(VentaService ventaService, ComprobanteService comprobanteService) {
        this.ventaService = ventaService;
        this.comprobanteService = comprobanteService;
    }


    /**
     * GET /api/v1/ventas
     * Listar todas las ventas
     */
    @GetMapping
    public ResponseEntity<List<VentaResponse>> getAllVentas() {
        List<VentaResponse> ventas = ventaService.getAllVentas();
        return ResponseEntity.ok(ventas);
    }

    /**
     * GET /api/v1/ventas/{id}
     * Obtener venta por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getVentaById(@PathVariable Long id) {
        VentaResponse venta = ventaService.getVentaById(id);
        return ResponseEntity.ok(venta);
    }

    /**
     * POST /api/v1/ventas
     * Crear venta directa
     */
    @PostMapping
    public ResponseEntity<?> createVentaDirecta(@Valid @RequestBody VentaRequest request) {
        VentaResponse createdVenta = ventaService.createVentaDirecta(request);

        // Aparte y después de confirmada la venta: si esto falla (por
        // ejemplo, un numeroComprobante repetido), la venta ya quedó
        // registrada igual. Antes se generaba dentro de la misma
        // transacción de la venta, y una falla acá revertía la venta
        // entera con un error que no explicaba nada.
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

    /**
     * PATCH /api/v1/ventas/{id}/cancelar
     * Cancelar venta
     */
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cancelarVenta(@PathVariable Long id) {
        VentaResponse venta = ventaService.cancelarVenta(id);
        return ResponseEntity.ok(venta);
    }

    /**
     * PATCH /api/v1/ventas/{id}/entregar
     * Marcar la entrega de una venta como completada (ADMIN y EMPLEADO).
     */
    @PatchMapping("/{id}/entregar")
    public ResponseEntity<?> marcarEntregado(@PathVariable Long id) {
        VentaResponse venta = ventaService.marcarEntregado(id);
        return ResponseEntity.ok(venta);
    }

    /**
     * PATCH /api/v1/ventas/{id}/deshacer-entrega
     * Corregir una entrega marcada por error: ENTREGADO -> PENDIENTE (solo ADMIN).
     */
    @PatchMapping("/{id}/deshacer-entrega")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deshacerEntrega(@PathVariable Long id) {
        VentaResponse venta = ventaService.deshacerEntrega(id);
        return ResponseEntity.ok(venta);
    }

    /**
     * PATCH /api/v1/ventas/{id}/datos-entrega
     * Completar o corregir la dirección, la transportadora y la guía (solo ADMIN).
     */
    @PatchMapping("/{id}/datos-entrega")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> actualizarDatosEntrega(@PathVariable Long id,
                                                    @Valid @RequestBody DatosEntregaRequest request) {
        VentaResponse venta = ventaService.actualizarDatosEntrega(id, request);
        return ResponseEntity.ok(venta);
    }

    /**
     * PATCH /api/v1/ventas/{id}/fecha-limite-pago
     * Poner o cambiar hasta cuándo se espera el pago pendiente (ADMIN y EMPLEADO).
     */
    @PatchMapping("/{id}/fecha-limite-pago")
    public ResponseEntity<?> actualizarFechaLimitePago(@PathVariable Long id,
                                                       @Valid @RequestBody FechaLimitePagoRequest request) {
        VentaResponse venta = ventaService.actualizarFechaLimitePago(id, request);
        return ResponseEntity.ok(venta);
    }

    /**
     * GET /api/v1/ventas/cliente/{clienteId}
     * Listar ventas de un cliente
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<VentaResponse>> getVentasByCliente(@PathVariable Long clienteId) {
        List<VentaResponse> ventas = ventaService.getVentasByCliente(clienteId);
        return ResponseEntity.ok(ventas);
    }

    /**
     * GET /api/v1/ventas/estado/{estado}
     * Filtrar ventas por estado
     */
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

    /**
     * GET /api/v1/ventas/del-dia
     * Ventas del día actual
     */
    @GetMapping("/del-dia")
    public ResponseEntity<List<VentaResponse>> getVentasDelDia() {
        List<VentaResponse> ventas = ventaService.getVentasDelDia();
        return ResponseEntity.ok(ventas);
    }

    /**
     * GET /api/v1/ventas/ultimas
     * Últimas 10 ventas
     */
    @GetMapping("/ultimas")
    public ResponseEntity<List<VentaResponse>> getUltimasVentas() {
        List<VentaResponse> ventas = ventaService.getUltimasVentas();
        return ResponseEntity.ok(ventas);
    }

    /**
     * GET /api/v1/ventas/fechas?inicio=...&fin=...
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
     * GET /api/v1/ventas/total-fechas?inicio=...&fin=...
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
     * GET /api/v1/ventas/estadisticas
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