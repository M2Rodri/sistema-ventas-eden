package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.ComprobanteRequest;
import com.mitienda.ecommerce.dto.ComprobanteResponse;
import com.mitienda.ecommerce.services.ComprobanteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Comprobante interno de la venta (recibo para el cliente).
 *
 * Solo quedan las dos rutas que usa la web en el detalle de la venta: consultar el comprobante
 * de una venta y crearlo. La creación automática al registrar la venta no pasa por acá.
 */
@RestController
@RequestMapping("/api/v1/comprobantes")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
public class ComprobanteController {

    private final ComprobanteService comprobanteService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public ComprobanteController(ComprobanteService comprobanteService) {
        this.comprobanteService = comprobanteService;
    }


    /**
     * GET /api/v1/comprobantes/venta/{idVenta}
     * Obtener comprobante por venta
     */
    @GetMapping("/venta/{idVenta}")
    public ResponseEntity<?> getComprobanteByVenta(@PathVariable Long idVenta) {
        try {
            ComprobanteResponse comprobante = comprobanteService.getComprobanteByVenta(idVenta);
            return ResponseEntity.ok(comprobante);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/v1/comprobantes
     * Crear comprobante
     */
    @PostMapping
    public ResponseEntity<?> createComprobante(@Valid @RequestBody ComprobanteRequest request) {
        try {
            ComprobanteResponse createdComprobante = comprobanteService.createComprobante(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdComprobante);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
