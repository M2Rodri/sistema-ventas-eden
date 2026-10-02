package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.CategoriaRequest;
import com.mitienda.ecommerce.dto.CategoriaResponse;
import com.mitienda.ecommerce.services.CategoriaService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/categorias")
@CrossOrigin(origins = "http://localhost:3000")
public class CategoriaController {

    private final CategoriaService categoriaService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }


    /**
     * GET /api/v1/categorias
     * Listar todas las categorías (público)
     */
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> getAllCategorias() {
        List<CategoriaResponse> categorias = categoriaService.getAllCategorias();
        return ResponseEntity.ok(categorias);
    }

    /**
     * GET /api/v1/categorias/activas
     * Listar solo categorías activas (público)
     */
    @GetMapping("/activas")
    public ResponseEntity<List<CategoriaResponse>> getActiveCategorias() {
        List<CategoriaResponse> categorias = categoriaService.getActiveCategorias();
        return ResponseEntity.ok(categorias);
    }

    /**
     * GET /api/v1/categorias/{id}
     * Obtener categoría por ID (público)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoriaById(@PathVariable Long id) {
        CategoriaResponse categoria = categoriaService.getCategoriaById(id);
        return ResponseEntity.ok(categoria);
    }

    /**
     * POST /api/v1/categorias
     * Crear nueva categoría (ADMIN)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCategoria(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse createdCategoria = categoriaService.createCategoria(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategoria);
    }

    /**
     * PUT /api/v1/categorias/{id}
     * Actualizar categoría (ADMIN)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCategoria(@PathVariable Long id, 
                                            @Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse updatedCategoria = categoriaService.updateCategoria(id, request);
        return ResponseEntity.ok(updatedCategoria);
    }

    /**
     * DELETE /api/v1/categorias/{id}
     * Eliminar categoría (ADMIN)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCategoria(@PathVariable Long id) {
        categoriaService.deleteCategoria(id);
        return ResponseEntity.ok(Map.of("message", "Categoría desactivada correctamente"));
    }

    /**
     * PATCH /api/v1/categorias/{id}/toggle-status
     * Activar/Desactivar categoría (ADMIN)
     */
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleCategoriaStatus(@PathVariable Long id) {
        CategoriaResponse categoria = categoriaService.toggleCategoriaStatus(id);
        return ResponseEntity.ok(categoria);
    }

    /**
     * GET /api/v1/categorias/estadisticas
     * Obtener estadísticas de categorías (ADMIN)
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCategoriaStatistics() {
        Long totalActivas = categoriaService.countActiveCategorias();
        return ResponseEntity.ok(Map.of("activas", totalActivas));
    }
}