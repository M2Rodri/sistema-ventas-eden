package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.PagoDTO;
import com.mitienda.ecommerce.dto.PagoRequest;
import com.mitienda.ecommerce.models.EstadoPago;
import com.mitienda.ecommerce.services.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de pagos
 */
@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
public class PagoController {

    private final PagoService pagoService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }


    /**
     * GET /api/pagos
     * Listar todos los pagos
     */
    @GetMapping
    public ResponseEntity<List<PagoDTO>> getAllPagos() {
        List<PagoDTO> pagos = pagoService.getAllPagos();
        return ResponseEntity.ok(pagos);
    }

    /**
     * GET /api/pagos/{id}
     * Obtener pago por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPagoById(@PathVariable Long id) {
        try {
            PagoDTO pago = pagoService.getPagoById(id);
            return ResponseEntity.ok(pago);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/pagos
     * Registrar un nuevo pago
     */
    @PostMapping
    public ResponseEntity<?> registrarPago(@Valid @RequestBody PagoRequest request) {
        try {
            PagoDTO createdPago = pagoService.registrarPago(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPago);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/pagos/venta/{ventaId}
     * Obtener pagos de una venta
     */
    @GetMapping("/venta/{ventaId}")
    public ResponseEntity<List<PagoDTO>> getPagosByVenta(@PathVariable Long ventaId) {
        List<PagoDTO> pagos = pagoService.getPagosByVenta(ventaId);
        return ResponseEntity.ok(pagos);
    }

    /**
     * GET /api/pagos/estado/{estado}
     * Filtrar pagos por estado
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<?> getPagosByEstado(@PathVariable String estado) {
        try {
            EstadoPago estadoEnum = EstadoPago.valueOf(estado.toUpperCase());
            List<PagoDTO> pagos = pagoService.getPagosByEstado(estadoEnum);
            return ResponseEntity.ok(pagos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Estado de pago inválido: " + estado));
        }
    }

    /**
     * GET /api/pagos/estadisticas
     * Obtener estadísticas de pagos
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPagoStatistics() {
        Long totalCompletados = pagoService.countPagosByEstado(EstadoPago.COMPLETADO);
        Long totalPendientes = pagoService.countPagosByEstado(EstadoPago.PENDIENTE);
        Long totalRechazados = pagoService.countPagosByEstado(EstadoPago.RECHAZADO);

        return ResponseEntity.ok(Map.of(
            "completados", totalCompletados,
            "pendientes", totalPendientes,
            "rechazados", totalRechazados
        ));
    }
}