package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.MovimientoInventarioRequest;
import com.mitienda.ecommerce.dto.MovimientoInventarioResponse;
import com.mitienda.ecommerce.dto.AlertaInventarioResponse;
import com.mitienda.ecommerce.dto.InventarioRequest;
import com.mitienda.ecommerce.dto.InventarioResponse;
import com.mitienda.ecommerce.services.InventarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de inventario
 */
@RestController
@RequestMapping("/api/inventario")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
public class InventarioController {

    private final InventarioService inventarioService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }


    /**
     * GET /api/inventario
     * Listar todo el inventario
     */
    @GetMapping
    public ResponseEntity<List<InventarioResponse>> getAllInventario() {
        List<InventarioResponse> inventario = inventarioService.getAllInventario();
        return ResponseEntity.ok(inventario);
    }

    /**
     * GET /api/inventario/{id}
     * Obtener inventario por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getInventarioById(@PathVariable Long id) {
        try {
            InventarioResponse inventario = inventarioService.getInventarioById(id);
            return ResponseEntity.ok(inventario);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/inventario/producto/{idProducto}
     * Obtener inventario por producto
     */
    @GetMapping("/producto/{idProducto}")
    public ResponseEntity<?> getInventarioByProducto(@PathVariable Long idProducto) {
        try {
            InventarioResponse inventario = inventarioService.getInventarioByProducto(idProducto);
            return ResponseEntity.ok(inventario);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/inventario
     * Crear inventario para un producto (ADMIN)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createInventario(@Valid @RequestBody InventarioRequest request) {
        try {
            InventarioResponse createdInventario = inventarioService.createInventario(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdInventario);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/inventario/{id}
     * Actualizar inventario existente (ADMIN)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateInventario(@PathVariable Long id, 
                                             @Valid @RequestBody InventarioRequest request) {
        try {
            InventarioResponse updatedInventario = inventarioService.updateInventario(id, request);
            return ResponseEntity.ok(updatedInventario);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/inventario/ajustar
     * Ajustar inventario manualmente (entrada/salida). Siempre queda
     * registrado en movimientos_inventario, con el usuario autenticado que
     * hizo el ajuste: no existe un camino de ajuste manual sin rastro.
     */
    @PostMapping("/ajustar")
    public ResponseEntity<?> ajustarInventario(@Valid @RequestBody MovimientoInventarioRequest request) {
        try {
            InventarioResponse inventario = inventarioService.ajustarInventario(request);
            return ResponseEntity.ok(inventario);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/inventario/producto/{idProducto}/historial
     * Historial de ajustes de un producto
     */
    @GetMapping("/producto/{idProducto}/historial")
    public ResponseEntity<List<MovimientoInventarioResponse>> getHistorialAjustes(@PathVariable Long idProducto) {
        List<MovimientoInventarioResponse> historial = inventarioService.getHistorialAjustes(idProducto);
        return ResponseEntity.ok(historial);
    }

    /**
     * GET /api/inventario/ajustes/ultimos
     * Últimos 50 ajustes de inventario
     */
    @GetMapping("/ajustes/ultimos")
    public ResponseEntity<List<MovimientoInventarioResponse>> getUltimosAjustes() {
        List<MovimientoInventarioResponse> ajustes = inventarioService.getUltimosAjustes();
        return ResponseEntity.ok(ajustes);
    }

    /**
     * GET /api/inventario/stock-bajo
     * Productos con stock bajo
     */
    @GetMapping("/stock-bajo")
    public ResponseEntity<List<InventarioResponse>> getProductosConStockBajo() {
        List<InventarioResponse> productos = inventarioService.getProductosConStockBajo();
        return ResponseEntity.ok(productos);
    }

    /**
     * GET /api/inventario/sin-stock
     * Productos sin stock
     */
    @GetMapping("/sin-stock")
    public ResponseEntity<List<InventarioResponse>> getProductosSinStock() {
        List<InventarioResponse> productos = inventarioService.getProductosSinStock();
        return ResponseEntity.ok(productos);
    }

    /**
     * GET /api/inventario/verificar-disponibilidad?idProducto=...&cantidad=...
     * Verificar disponibilidad de stock
     */
    @GetMapping("/verificar-disponibilidad")
    public ResponseEntity<?> verificarDisponibilidad(@RequestParam Long idProducto, 
                                                     @RequestParam Integer cantidad) {
        try {
            boolean disponible = inventarioService.verificarDisponibilidad(idProducto, cantidad);
            return ResponseEntity.ok(Map.of(
                "disponible", disponible,
                "idProducto", idProducto,
                "cantidadSolicitada", cantidad
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/inventario/alertas/pendientes
     * Listar alertas de inventario pendientes
     */
    @GetMapping("/alertas/pendientes")
    public ResponseEntity<List<AlertaInventarioResponse>> getAlertasPendientes() {
        List<AlertaInventarioResponse> alertas = inventarioService.getAlertasPendientes();
        return ResponseEntity.ok(alertas);
    }

    /**
     * PATCH /api/inventario/alertas/{id}/atender
     * Marcar alerta como atendida
     */
    @PatchMapping("/alertas/{id}/atender")
    public ResponseEntity<?> marcarAlertaAtendida(@PathVariable Long id) {
        try {
            AlertaInventarioResponse alerta = inventarioService.marcarAlertaAtendida(id);
            return ResponseEntity.ok(alerta);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/inventario/estadisticas
     * Obtener estadísticas de inventario (ADMIN)
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getInventarioStatistics() {
        Long totalConStockBajo = inventarioService.countProductosConStockBajo();
        return ResponseEntity.ok(Map.of("productosBajoStock", totalConStockBajo));
    }
}