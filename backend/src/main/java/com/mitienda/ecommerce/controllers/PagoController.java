package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.exception.PeticionInvalidaException;
import com.mitienda.ecommerce.dto.PagoDTO;
import com.mitienda.ecommerce.dto.PagoRequest;
import com.mitienda.ecommerce.models.EstadoPago;
import com.mitienda.ecommerce.services.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Controlador REST para gestión de pagos */
@RestController
@RequestMapping("/api/v1/pagos")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
public class PagoController {
    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    /** Listar todos los pagos */
    @GetMapping
    public ResponseEntity<List<PagoDTO>> getAllPagos() {
        List<PagoDTO> pagos = pagoService.getAllPagos();
        return ResponseEntity.ok(pagos);
    }

    /** Obtener pago por ID */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPagoById(@PathVariable Long id) {
        PagoDTO pago = pagoService.getPagoById(id);
        return ResponseEntity.ok(pago);
    }

    /** Registrar un nuevo pago */
    @PostMapping
    public ResponseEntity<?> registrarPago(@Valid @RequestBody PagoRequest request) {
        PagoDTO createdPago = pagoService.registrarPago(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPago);
    }

    /** Adjuntar (o reemplazar) la foto de comprobante de un pago ya registrado */
    @PostMapping("/{id}/comprobante")
    public ResponseEntity<?> adjuntarComprobante(@PathVariable Long id,
                                                  @RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new PeticionInvalidaException("ARCHIVO_VACIO", "El archivo del comprobante no puede estar vacío.");
        }
        PagoDTO pago = pagoService.adjuntarComprobante(id, file);
        return ResponseEntity.ok(pago);
    }

    /** Obtener pagos de una venta */
    @GetMapping("/venta/{ventaId}")
    public ResponseEntity<List<PagoDTO>> getPagosByVenta(@PathVariable Long ventaId) {
        List<PagoDTO> pagos = pagoService.getPagosByVenta(ventaId);
        return ResponseEntity.ok(pagos);
    }

    /** Filtrar pagos por estado */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<?> getPagosByEstado(@PathVariable String estado) {
        EstadoPago estadoEnum;
        try {
            estadoEnum = EstadoPago.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PeticionInvalidaException("ESTADO_INVALIDO", "Estado de pago inválido: " + estado);
        }
        List<PagoDTO> pagos = pagoService.getPagosByEstado(estadoEnum);
        return ResponseEntity.ok(pagos);
    }

    /** Obtener estadísticas de pagos */
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