package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.CategoriaRequest;
import com.mitienda.ecommerce.dto.CategoriaResponse;
import com.mitienda.ecommerce.services.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de categorías
 * Público para consultas, ADMIN para modificaciones
 */
@RestController
@RequestMapping("/api/categorias")
@CrossOrigin(origins = "http://localhost:3000")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    /**
     * GET /api/categorias
     * Listar todas las categorías (público)
     */
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> getAllCategorias() {
        List<CategoriaResponse> categorias = categoriaService.getAllCategorias();
        return ResponseEntity.ok(categorias);
    }

    /**
     * GET /api/categorias/activas
     * Listar solo categorías activas (público)
     */
    @GetMapping("/activas")
    public ResponseEntity<List<CategoriaResponse>> getActiveCategorias() {
        List<CategoriaResponse> categorias = categoriaService.getActiveCategorias();
        return ResponseEntity.ok(categorias);
    }

    /**
     * GET /api/categorias/{id}
     * Obtener categoría por ID (público)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoriaById(@PathVariable Long id) {
        try {
            CategoriaResponse categoria = categoriaService.getCategoriaById(id);
            return ResponseEntity.ok(categoria);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/categorias
     * Crear nueva categoría (ADMIN)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCategoria(@Valid @RequestBody CategoriaRequest request) {
        try {
            CategoriaResponse createdCategoria = categoriaService.createCategoria(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCategoria);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/categorias/{id}
     * Actualizar categoría (ADMIN)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCategoria(@PathVariable Long id, 
                                            @Valid @RequestBody CategoriaRequest request) {
        try {
            CategoriaResponse updatedCategoria = categoriaService.updateCategoria(id, request);
            return ResponseEntity.ok(updatedCategoria);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/categorias/{id}
     * Eliminar categoría (ADMIN)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCategoria(@PathVariable Long id) {
        try {
            categoriaService.deleteCategoria(id);
            return ResponseEntity.ok(Map.of("message", "Categoría desactivada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/categorias/{id}/toggle-status
     * Activar/Desactivar categoría (ADMIN)
     */
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleCategoriaStatus(@PathVariable Long id) {
        try {
            CategoriaResponse categoria = categoriaService.toggleCategoriaStatus(id);
            return ResponseEntity.ok(categoria);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/categorias/estadisticas
     * Obtener estadísticas de categorías (ADMIN)
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCategoriaStatistics() {
        Long totalActivas = categoriaService.countActiveCategorias();
        return ResponseEntity.ok(Map.of("activas", totalActivas));
    }
}