package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.ComprobanteRequest;
import com.mitienda.ecommerce.dto.ComprobanteResponse;
import com.mitienda.ecommerce.services.ComprobanteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Comprobante interno de la venta: consultarlo y crearlo desde el detalle de la venta. */
@RestController
@RequestMapping("/api/v1/comprobantes")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
public class ComprobanteController {
    private final ComprobanteService comprobanteService;

    public ComprobanteController(ComprobanteService comprobanteService) {
        this.comprobanteService = comprobanteService;
    }

    /** Obtener comprobante por venta */
    @GetMapping("/venta/{idVenta}")
    public ResponseEntity<?> getComprobanteByVenta(@PathVariable Long idVenta) {
        ComprobanteResponse comprobante = comprobanteService.getComprobanteByVenta(idVenta);
        return ResponseEntity.ok(comprobante);
    }

    /** Crear comprobante */
    @PostMapping
    public ResponseEntity<?> createComprobante(@Valid @RequestBody ComprobanteRequest request) {
        ComprobanteResponse createdComprobante = comprobanteService.createComprobante(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComprobante);
    }
}
