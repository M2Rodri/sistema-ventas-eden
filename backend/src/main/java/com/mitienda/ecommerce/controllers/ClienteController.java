package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.ClienteRequest;
import com.mitienda.ecommerce.dto.ClienteResponse;
import com.mitienda.ecommerce.dto.ClienteEstadisticas;
import com.mitienda.ecommerce.dto.HistorialComprasResponse;
import com.mitienda.ecommerce.models.TipoCliente;
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
     * GET /api/clientes/activos
     * Listar solo clientes activos (ADMIN/EMPLEADO)
     */
    @GetMapping("/activos")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<List<ClienteResponse>> getActiveClientes() {
        List<ClienteResponse> clientes = clienteService.getActiveClientes();
        return ResponseEntity.ok(clientes);
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
     * Crear nuevo cliente (ADMIN/EMPLEADO o público)
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
     * DELETE /api/clientes/{id}
     * Eliminar cliente (ADMIN)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCliente(@PathVariable Long id) {
        try {
            clienteService.deleteCliente(id);
            return ResponseEntity.ok(Map.of("message", "Cliente desactivado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/clientes/{id}/toggle-status
     * Activar/Desactivar cliente (ADMIN)
     */
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleClienteStatus(@PathVariable Long id) {
        try {
            ClienteResponse cliente = clienteService.toggleClienteStatus(id);
            return ResponseEntity.ok(cliente);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
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
     * GET /api/clientes/tipo/{tipo}
     * Filtrar clientes por tipo (ADMIN/EMPLEADO)
     */
    @GetMapping("/tipo/{tipo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<?> getClientesByTipo(@PathVariable String tipo) {
        try {
            TipoCliente tipoEnum = TipoCliente.valueOf(tipo.toUpperCase());
            List<ClienteResponse> clientes = clienteService.getClientesByTipo(tipoEnum);
            return ResponseEntity.ok(clientes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Tipo de cliente inválido: " + tipo));
        }
    }

    /**
     * GET /api/clientes/estadisticas-generales
     * Obtener estadísticas generales de clientes (ADMIN/EMPLEADO)
     * Para Interfaz P6.1 - Indicadores superiores
     */
    @GetMapping("/estadisticas-generales")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<ClienteEstadisticas> getEstadisticasGenerales() {
        ClienteEstadisticas estadisticas = clienteService.getEstadisticasGenerales();
        return ResponseEntity.ok(estadisticas);
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

    /**
     * GET /api/clientes/estadisticas
     * Obtener estadísticas de clientes (ADMIN)
     * LEGACY - mantener por compatibilidad
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getClienteStatistics() {
        Long totalActivos = clienteService.countActiveClientes();
        Long totalRegistrados = clienteService.countClientesByTipo(TipoCliente.REGISTRADO);
        Long totalInvitados = clienteService.countClientesByTipo(TipoCliente.INVITADO);

        return ResponseEntity.ok(Map.of(
            "activos", totalActivos,
            "registrados", totalRegistrados,
            "invitados", totalInvitados
        ));
    }
}