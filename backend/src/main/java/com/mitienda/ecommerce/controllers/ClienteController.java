package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.ClienteRequest;
import com.mitienda.ecommerce.dto.ClienteResponse;
import com.mitienda.ecommerce.dto.ClienteConEstadisticasResponse;
import com.mitienda.ecommerce.dto.HistorialComprasResponse;
import com.mitienda.ecommerce.services.ClienteService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** Gestión de clientes (CU6). */
@RestController
@RequestMapping("/api/v1/clientes")
@CrossOrigin(origins = "http://localhost:3000")
public class ClienteController {
    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    /** Lista todos los clientes (ADMIN/EMPLEADO). */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<List<ClienteResponse>> getAllClientes() {
        List<ClienteResponse> clientes = clienteService.getAllClientes();
        return ResponseEntity.ok(clientes);
    }

    /** Clientes con número de compras, monto total y última compra (ADMIN/EMPLEADO). */
    @GetMapping("/con-estadisticas")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<List<ClienteConEstadisticasResponse>> getAllClientesConEstadisticas() {
        return ResponseEntity.ok(clienteService.getAllClientesConEstadisticas());
    }

    /** Obtener cliente por ID (ADMIN/EMPLEADO) */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<?> getClienteById(@PathVariable Long id) {
        ClienteResponse cliente = clienteService.getClienteById(id);
        return ResponseEntity.ok(cliente);
    }

    /** Crea un cliente; en la práctica se crea solo al registrar su primera venta (ver VentaService). */
    @PostMapping
    public ResponseEntity<?> createCliente(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse createdCliente = clienteService.createCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCliente);
    }

    /** Actualiza un cliente (ADMIN/EMPLEADO). */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<?> updateCliente(@PathVariable Long id, 
                                          @Valid @RequestBody ClienteRequest request) {
        ClienteResponse updatedCliente = clienteService.updateCliente(id, request);
        return ResponseEntity.ok(updatedCliente);
    }

    /** Busca por nombre, teléfono o NIT/CI (ADMIN/EMPLEADO). */
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<List<ClienteResponse>> searchClientes(@RequestParam String q) {
        List<ClienteResponse> clientes = clienteService.searchClientes(q);
        return ResponseEntity.ok(clientes);
    }

    /** Historial completo de compras de un cliente. */
    @GetMapping("/{id}/historial-compras")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<?> getHistorialCompras(@PathVariable Long id) {
        HistorialComprasResponse historial = clienteService.getHistorialCompras(id);
        return ResponseEntity.ok(historial);
    }

    /** Historial de compras de un cliente entre dos fechas. */
    @GetMapping("/{id}/historial-compras/filtrado")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<?> getHistorialComprasFiltrado(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        HistorialComprasResponse historial = clienteService.getHistorialComprasFiltrado(id, inicio, fin);
        return ResponseEntity.ok(historial);
    }
}