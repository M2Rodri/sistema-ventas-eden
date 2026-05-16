package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.ProductoRequest;
import com.mitienda.ecommerce.dto.ProductoResponse;
import com.mitienda.ecommerce.models.TipoProducto;
import com.mitienda.ecommerce.services.ProductoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "http://localhost:3000")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    // ========================================
    // ENDPOINTS PÚBLICOS (sin autenticación)
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
        try {
            ProductoResponse producto = productoService.getProductoById(id);
            return ResponseEntity.ok(producto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<?> getProductoBySku(@PathVariable String sku) {
        try {
            ProductoResponse producto = productoService.getProductoBySku(sku);
            return ResponseEntity.ok(producto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<?> getProductosByCategoria(@PathVariable Long categoriaId) {
        try {
            List<ProductoResponse> productos = productoService.getProductosByCategoria(categoriaId);
            return ResponseEntity.ok(productos);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<?> getProductosByTipo(@PathVariable String tipo) {
        try {
            TipoProducto tipoEnum = TipoProducto.valueOf(tipo.toUpperCase());
            List<ProductoResponse> productos = productoService.getProductosByTipo(tipoEnum);
            return ResponseEntity.ok(productos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Tipo de producto inválido: " + tipo));
        }
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
        try {
            ProductoResponse createdProducto = productoService.createProducto(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProducto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProducto(@PathVariable Long id, 
                                           @Valid @RequestBody ProductoRequest request) {
        try {
            ProductoResponse updatedProducto = productoService.updateProducto(id, request);
            return ResponseEntity.ok(updatedProducto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProducto(@PathVariable Long id) {
        try {
            productoService.deleteProducto(id);
            return ResponseEntity.ok(Map.of("message", "Producto desactivado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleProductoStatus(@PathVariable Long id) {
        try {
            ProductoResponse producto = productoService.toggleProductoStatus(id);
            return ResponseEntity.ok(producto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getProductoStatistics() {
        Long totalActivos = productoService.countActiveProductos();
        return ResponseEntity.ok(Map.of("activos", totalActivos));
    }
}