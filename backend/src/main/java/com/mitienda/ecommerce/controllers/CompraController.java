package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.CompraRequest;
import com.mitienda.ecommerce.dto.CompraResponse;
import com.mitienda.ecommerce.services.CompraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Controlador REST para gestión de compras */
@RestController
@RequestMapping("/api/v1/compras")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasRole('ADMIN')")
public class CompraController {
    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    /** Listar todas las compras */
    @GetMapping
    public ResponseEntity<List<CompraResponse>> getAllCompras() {
        List<CompraResponse> compras = compraService.getAllCompras();
        return ResponseEntity.ok(compras);
    }

    /** Obtener compra por ID */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCompraById(@PathVariable Long id) {
        CompraResponse compra = compraService.getCompraById(id);
        return ResponseEntity.ok(compra);
    }

    /** Crear nueva compra */
    @PostMapping
    public ResponseEntity<?> createCompra(@Valid @RequestBody CompraRequest request) {
        CompraResponse createdCompra = compraService.createCompra(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCompra);
    }

    /** Anular una compra: devuelve lo comprado al estado anterior del inventario */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelarCompra(@PathVariable Long id) {
        CompraResponse compra = compraService.cancelarCompra(id);
        return ResponseEntity.ok(compra);
    }
}