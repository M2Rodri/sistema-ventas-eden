package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.ProductoRequest;
import com.mitienda.ecommerce.dto.ProductoResponse;
import com.mitienda.ecommerce.services.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/productos")
@CrossOrigin(origins = "http://localhost:3000")
public class ProductoController {

    private final ProductoService productoService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }


    // ========================================
    // LECTURA (ADMIN y EMPLEADO) — ver SecurityConfig
    // ========================================

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> getAllProductos() {
        List<ProductoResponse> productos = productoService.getAllProductos();
        return ResponseEntity.ok(productos);
    }

    @GetMapping("/activos")
    public ResponseEntity<List<ProductoResponse>> getActiveProductos() {
        List<ProductoResponse> productos = productoService.getActiveProductos();
        return ResponseEntity.ok(productos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductoById(@PathVariable Long id) {
        ProductoResponse producto = productoService.getProductoById(id);
        return ResponseEntity.ok(producto);
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<?> getProductoBySku(@PathVariable String sku) {
        ProductoResponse producto = productoService.getProductoBySku(sku);
        return ResponseEntity.ok(producto);
    }

    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<?> getProductosByCategoria(@PathVariable Long categoriaId) {
        List<ProductoResponse> productos = productoService.getProductosByCategoria(categoriaId);
        return ResponseEntity.ok(productos);
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<ProductoResponse>> searchProductos(@RequestParam String nombre) {
        List<ProductoResponse> productos = productoService.searchProductos(nombre);
        return ResponseEntity.ok(productos);
    }

    // ========================================
    // ENDPOINTS PROTEGIDOS (Solo ADMIN)
    // ========================================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProducto(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse createdProducto = productoService.createProducto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProducto);
    }

    /**
     * PATCH /api/v1/productos/{id}/stock-minimo
     * Cambia el umbral que dispara las alertas de stock bajo.
     */
    @PatchMapping("/{id}/stock-minimo")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<?> actualizarStockMinimo(@PathVariable Long id,
                                                   @RequestParam Integer stockMinimo) {
        return ResponseEntity.ok(productoService.actualizarStockMinimo(id, stockMinimo));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProducto(@PathVariable Long id, 
                                           @Valid @RequestBody ProductoRequest request) {
        ProductoResponse updatedProducto = productoService.updateProducto(id, request);
        return ResponseEntity.ok(updatedProducto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProducto(@PathVariable Long id) {
        productoService.deleteProducto(id);
        return ResponseEntity.ok(Map.of("message", "Producto desactivado correctamente"));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleProductoStatus(@PathVariable Long id) {
        ProductoResponse producto = productoService.toggleProductoStatus(id);
        return ResponseEntity.ok(producto);
    }

    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getProductoStatistics() {
        Long totalActivos = productoService.countActiveProductos();
        return ResponseEntity.ok(Map.of("activos", totalActivos));
    }
}