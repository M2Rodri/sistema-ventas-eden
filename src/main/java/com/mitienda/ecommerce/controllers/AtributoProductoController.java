package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.AtributoProductoDTO;
import com.mitienda.ecommerce.services.AtributoProductoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para atributos de productos
 */
@RestController
@RequestMapping("/api/productos/{idProducto}/atributos")
@CrossOrigin(origins = "http://localhost:3000")
public class AtributoProductoController {

    @Autowired
    private AtributoProductoService atributoService;

    /**
     * GET /api/productos/{idProducto}/atributos
     * Obtener atributos de un producto (público)
     */
    @GetMapping
    public ResponseEntity<List<AtributoProductoDTO>> getAtributosByProducto(@PathVariable Long idProducto) {
        List<AtributoProductoDTO> atributos = atributoService.getAtributosByProducto(idProducto);
        return ResponseEntity.ok(atributos);
    }

    /**
     * POST /api/productos/{idProducto}/atributos
     * Agregar atributo a un producto (ADMIN)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addAtributo(@PathVariable Long idProducto,
                                        @Valid @RequestBody AtributoProductoDTO dto) {
        try {
            AtributoProductoDTO createdAtributo = atributoService.addAtributo(idProducto, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAtributo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/productos/{idProducto}/atributos/{id}
     * Actualizar atributo (ADMIN)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateAtributo(@PathVariable Long idProducto,
                                           @PathVariable Long id,
                                           @Valid @RequestBody AtributoProductoDTO dto) {
        try {
            AtributoProductoDTO updatedAtributo = atributoService.updateAtributo(id, dto);
            return ResponseEntity.ok(updatedAtributo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/productos/{idProducto}/atributos/{id}
     * Eliminar atributo (ADMIN)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAtributo(@PathVariable Long idProducto,
                                           @PathVariable Long id) {
        try {
            atributoService.deleteAtributo(id);
            return ResponseEntity.ok(Map.of("message", "Atributo eliminado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/productos/{idProducto}/atributos
     * Actualizar todos los atributos de un producto (reemplazar) (ADMIN)
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateAllAtributos(@PathVariable Long idProducto,
                                               @Valid @RequestBody List<AtributoProductoDTO> atributos) {
        try {
            List<AtributoProductoDTO> updatedAtributos = atributoService.updateAllAtributos(idProducto, atributos);
            return ResponseEntity.ok(updatedAtributos);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}