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

/** Gestión de categorías: consulta pública, modificación solo ADMIN. */
@RestController
@RequestMapping("/api/v1/categorias")
@CrossOrigin(origins = "http://localhost:3000")
public class CategoriaController {
    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    /** Listar todas las categorías (público) */
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> getAllCategorias() {
        List<CategoriaResponse> categorias = categoriaService.getAllCategorias();
        return ResponseEntity.ok(categorias);
    }

    /** Listar solo categorías activas (público) */
    @GetMapping("/activas")
    public ResponseEntity<List<CategoriaResponse>> getActiveCategorias() {
        List<CategoriaResponse> categorias = categoriaService.getActiveCategorias();
        return ResponseEntity.ok(categorias);
    }

    /** Obtener categoría por ID (público) */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoriaById(@PathVariable Long id) {
        CategoriaResponse categoria = categoriaService.getCategoriaById(id);
        return ResponseEntity.ok(categoria);
    }

    /** Crear nueva categoría (ADMIN) */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCategoria(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse createdCategoria = categoriaService.createCategoria(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategoria);
    }

    /** Actualizar categoría (ADMIN) */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCategoria(@PathVariable Long id, 
                                            @Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse updatedCategoria = categoriaService.updateCategoria(id, request);
        return ResponseEntity.ok(updatedCategoria);
    }

    /** Eliminar categoría (ADMIN) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCategoria(@PathVariable Long id) {
        categoriaService.deleteCategoria(id);
        return ResponseEntity.ok(Map.of("message", "Categoría desactivada correctamente"));
    }

    /** Activar/Desactivar categoría (ADMIN) */
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleCategoriaStatus(@PathVariable Long id) {
        CategoriaResponse categoria = categoriaService.toggleCategoriaStatus(id);
        return ResponseEntity.ok(categoria);
    }

    /** Obtener estadísticas de categorías (ADMIN) */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCategoriaStatistics() {
        Long totalActivas = categoriaService.countActiveCategorias();
        return ResponseEntity.ok(Map.of("activas", totalActivas));
    }
}