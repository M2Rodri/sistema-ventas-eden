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
import java.util.Map;

/**
 * Controlador REST para gestión de clientes
 * Cumple con CU6 - Gestionar Clientes
 */
@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "http://localhost:3000")
public class ClienteController {

    private final ClienteService clienteService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }


    /**
     * GET /api/clientes
     * Listar todos los clientes (ADMIN/EMPLEADO)
     * Interfaz P6.1
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<List<ClienteResponse>> getAllClientes() {
        List<ClienteResponse> clientes = clienteService.getAllClientes();
        return ResponseEntity.ok(clientes);
    }

    /**
     * GET /api/clientes/con-estadisticas
     * Listar todos los clientes con número de compras, monto total y última
     * compra, para la tabla de Clientes (ADMIN/EMPLEADO).
     */
    @GetMapping("/con-estadisticas")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<List<ClienteConEstadisticasResponse>> getAllClientesConEstadisticas() {
        return ResponseEntity.ok(clienteService.getAllClientesConEstadisticas());
    }

    /**
     * GET /api/clientes/{id}
     * Obtener cliente por ID (ADMIN/EMPLEADO)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<?> getClienteById(@PathVariable Long id) {
        try {
            ClienteResponse cliente = clienteService.getClienteById(id);
            return ResponseEntity.ok(cliente);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/clientes
     * Crear nuevo cliente (ADMIN/EMPLEADO).
     *
     * Sin @PreAuthorize propio, pero no es público: la tienda virtual quedó
     * fuera de alcance (ver SecurityConfig), así que la regla general
     * `anyRequest().authenticated()` ya exige sesión acá. En la práctica hoy
     * nadie llama esto directo: un cliente nuevo se crea solo al registrar
     * su primera venta (ver VentaService).
     */
    @PostMapping
    public ResponseEntity<?> createCliente(@Valid @RequestBody ClienteRequest request) {
        try {
            ClienteResponse createdCliente = clienteService.createCliente(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCliente);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/clientes/{id}
     * Actualizar cliente (ADMIN/EMPLEADO)
     * CU: Modificar Cliente - Interfaz P6.2
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<?> updateCliente(@PathVariable Long id, 
                                          @Valid @RequestBody ClienteRequest request) {
        try {
            ClienteResponse updatedCliente = clienteService.updateCliente(id, request);
            return ResponseEntity.ok(updatedCliente);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/clientes/buscar?q=...
     * Buscar clientes por nombre, teléfono o NIT/CI (ADMIN/EMPLEADO)
     * CU: Buscar/Consultar Cliente - Interfaz P6.1
     */
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<List<ClienteResponse>> searchClientes(@RequestParam String q) {
        List<ClienteResponse> clientes = clienteService.searchClientes(q);
        return ResponseEntity.ok(clientes);
    }

    /**
     * GET /api/clientes/{id}/historial-compras
     * Obtener historial completo de compras de un cliente
     * CU: Ver Historial de Compras del Cliente - Interfaz P6.3
     */
    @GetMapping("/{id}/historial-compras")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<?> getHistorialCompras(@PathVariable Long id) {
        try {
            HistorialComprasResponse historial = clienteService.getHistorialCompras(id);
            return ResponseEntity.ok(historial);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/clientes/{id}/historial-compras/filtrado?inicio=...&fin=...
     * Obtener historial de compras filtrado por fechas
     * CU: Ver Historial de Compras del Cliente - Filtro de fechas
     */
    @GetMapping("/{id}/historial-compras/filtrado")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<?> getHistorialComprasFiltrado(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        try {
            HistorialComprasResponse historial = clienteService.getHistorialComprasFiltrado(id, inicio, fin);
            return ResponseEntity.ok(historial);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}