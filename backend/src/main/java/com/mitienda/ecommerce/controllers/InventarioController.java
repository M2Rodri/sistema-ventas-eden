package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.MovimientoInventarioRequest;
import com.mitienda.ecommerce.dto.MovimientoInventarioResponse;
import com.mitienda.ecommerce.dto.AlertaInventarioResponse;
import com.mitienda.ecommerce.dto.CatalogoProductoResponse;
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

/** Controlador REST para gestión de inventario */
@RestController
@RequestMapping("/api/v1/inventario")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
public class InventarioController {
    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    /** Listar todo el inventario */
    @GetMapping
    public ResponseEntity<List<InventarioResponse>> getAllInventario() {
        List<InventarioResponse> inventario = inventarioService.getAllInventario();
        return ResponseEntity.ok(inventario);
    }

    /** Productos activos con precio y stock juntos, para la app (Catálogo, Alertas de stock y Nueva venta). */
    @GetMapping("/catalogo")
    public ResponseEntity<List<CatalogoProductoResponse>> getCatalogoApp(
            @RequestParam(required = false) String nombre,
            @RequestParam(defaultValue = "false") boolean soloBajoMinimo) {
        return ResponseEntity.ok(inventarioService.getCatalogoApp(nombre, soloBajoMinimo));
    }

    /** Obtener inventario por ID */
    @GetMapping("/{id}")
    public ResponseEntity<?> getInventarioById(@PathVariable Long id) {
        InventarioResponse inventario = inventarioService.getInventarioById(id);
        return ResponseEntity.ok(inventario);
    }

    /** Obtener inventario por producto */
    @GetMapping("/producto/{idProducto}")
    public ResponseEntity<?> getInventarioByProducto(@PathVariable Long idProducto) {
        InventarioResponse inventario = inventarioService.getInventarioByProducto(idProducto);
        return ResponseEntity.ok(inventario);
    }

    /** Crear inventario para un producto (ADMIN) */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createInventario(@Valid @RequestBody InventarioRequest request) {
        InventarioResponse createdInventario = inventarioService.createInventario(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdInventario);
    }

    /** Actualizar inventario existente (ADMIN) */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateInventario(@PathVariable Long id, 
                                             @Valid @RequestBody InventarioRequest request) {
        InventarioResponse updatedInventario = inventarioService.updateInventario(id, request);
        return ResponseEntity.ok(updatedInventario);
    }

    /** Ajuste manual de stock (ADMIN); queda registrado con el usuario del token. */
    @PostMapping("/ajustar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> ajustarInventario(@Valid @RequestBody MovimientoInventarioRequest request) {
        InventarioResponse inventario = inventarioService.ajustarInventario(request);
        return ResponseEntity.ok(inventario);
    }

    /** Historial de ajustes de un producto */
    @GetMapping("/producto/{idProducto}/historial")
    public ResponseEntity<List<MovimientoInventarioResponse>> getHistorialAjustes(@PathVariable Long idProducto) {
        List<MovimientoInventarioResponse> historial = inventarioService.getHistorialAjustes(idProducto);
        return ResponseEntity.ok(historial);
    }

    /** Últimos 50 ajustes de inventario */
    @GetMapping("/ajustes/ultimos")
    public ResponseEntity<List<MovimientoInventarioResponse>> getUltimosAjustes() {
        List<MovimientoInventarioResponse> ajustes = inventarioService.getUltimosAjustes();
        return ResponseEntity.ok(ajustes);
    }

    /** Productos con stock bajo */
    @GetMapping("/stock-bajo")
    public ResponseEntity<List<InventarioResponse>> getProductosConStockBajo() {
        List<InventarioResponse> productos = inventarioService.getProductosConStockBajo();
        return ResponseEntity.ok(productos);
    }

    /** Productos sin stock */
    @GetMapping("/sin-stock")
    public ResponseEntity<List<InventarioResponse>> getProductosSinStock() {
        List<InventarioResponse> productos = inventarioService.getProductosSinStock();
        return ResponseEntity.ok(productos);
    }

    /** Verificar disponibilidad de stock */
    @GetMapping("/verificar-disponibilidad")
    public ResponseEntity<?> verificarDisponibilidad(@RequestParam Long idProducto, 
                                                     @RequestParam Integer cantidad) {
        boolean disponible = inventarioService.verificarDisponibilidad(idProducto, cantidad);
        return ResponseEntity.ok(Map.of(
            "disponible", disponible,
            "idProducto", idProducto,
            "cantidadSolicitada", cantidad
        ));
    }

    /** Listar alertas de inventario pendientes */
    @GetMapping("/alertas/pendientes")
    public ResponseEntity<List<AlertaInventarioResponse>> getAlertasPendientes() {
        List<AlertaInventarioResponse> alertas = inventarioService.getAlertasPendientes();
        return ResponseEntity.ok(alertas);
    }

    /** Marca una alerta como atendida (ADMIN): no avisa de nuevo hasta que el producto se reponga y vuelva a bajar. */
    @PatchMapping("/alertas/{id}/atender")
    public ResponseEntity<?> atenderAlerta(@PathVariable Long id) {
        inventarioService.marcarAlertaAtendida(id);
        return ResponseEntity.ok(Map.of("message", "Alerta marcada como atendida"));
    }

    /** Volver a avisar de un producto con alerta atendida (ADMIN) */
    @PatchMapping("/producto/{idProducto}/reactivar-alerta")
    public ResponseEntity<?> reactivarAlerta(@PathVariable Long idProducto) {
        inventarioService.reactivarAlerta(idProducto);
        return ResponseEntity.ok(Map.of("message", "Alerta reactivada"));
    }

    /** Obtener estadísticas de inventario (ADMIN) */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getInventarioStatistics() {
        Long totalConStockBajo = inventarioService.countProductosConStockBajo();
        return ResponseEntity.ok(Map.of("productosBajoStock", totalConStockBajo));
    }
}