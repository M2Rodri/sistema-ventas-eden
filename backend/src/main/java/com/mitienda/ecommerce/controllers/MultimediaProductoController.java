package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.MultimediaProductoDTO;
import com.mitienda.ecommerce.dto.MultimediaProductoRequest;
import com.mitienda.ecommerce.services.MultimediaProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/multimedia-productos")
@CrossOrigin(origins = "http://localhost:3000")
public class MultimediaProductoController {
    private final MultimediaProductoService multimediaService;

    public MultimediaProductoController(MultimediaProductoService multimediaService) {
        this.multimediaService = multimediaService;
    }

    /** Sube el modelo 3D (.glb, .gltf, .usdz) del producto, con vista previa opcional y realidad aumentada opcional. */
    @PostMapping("/subir-modelo/{productoId}")
    public ResponseEntity<?> subirModelo3D(
            @RequestParam("archivoModelo") MultipartFile archivoModelo,
            @RequestParam(value = "archivoPreview", required = false) MultipartFile archivoPreview,
            @PathVariable Long productoId,
            @RequestParam(defaultValue = "false") Boolean habilitadoRa
    ) {
        System.out.println("===========================================");
        System.out.println("🎨 CONTROLADOR MULTIMEDIA 3D - INICIO");
        System.out.println("===========================================");
        System.out.println("ID Producto: " + productoId);
        System.out.println("Archivo Modelo: " + (archivoModelo != null ? archivoModelo.getOriginalFilename() : "NULL"));
        System.out.println("Tamaño Modelo: " + (archivoModelo != null ? archivoModelo.getSize() / 1024 : 0) + " KB");
        System.out.println("Archivo Preview: " + (archivoPreview != null ? archivoPreview.getOriginalFilename() : "NULL"));
        System.out.println("Tamaño Preview: " + (archivoPreview != null ? archivoPreview.getSize() / 1024 : 0) + " KB");
        System.out.println("Habilitado RA: " + habilitadoRa);
        System.out.println("===========================================");
        
        try {
            if (archivoModelo == null || archivoModelo.isEmpty()) {
                System.err.println("❌ Archivo del modelo 3D vacío o nulo");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "error", "El archivo del modelo 3D es obligatorio",
                            "codigo", "ARCHIVO_VACIO"
                        ));
            }
            
            // Llamar al servicio para guardar el modelo
            System.out.println("✅ Llamando al servicio para guardar modelo 3D...");
            MultimediaProductoDTO multimedia = multimediaService.subirModelo3D(
                archivoModelo, 
                archivoPreview, 
                productoId, 
                habilitadoRa
            );
            
            System.out.println("✅ Modelo 3D guardado correctamente");
            System.out.println("ID Multimedia: " + multimedia.getId());
            System.out.println("URL Modelo: " + multimedia.getUrlModelo3d());
            System.out.println("URL Preview: " + multimedia.getUrlVistaPrevia());
            System.out.println("===========================================");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(multimedia);
            
        } catch (IllegalArgumentException e) {
            // Errores de validación (extensión no permitida, tamaño excedido, etc.)
            System.err.println("❌ ERROR DE VALIDACIÓN: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "error", e.getMessage(),
                        "codigo", "VALIDACION_FALLIDA"
                    ));
                    
        } catch (RuntimeException e) {
            // Errores de negocio (producto no encontrado, etc.)
            System.err.println("❌ ERROR DE NEGOCIO: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "error", e.getMessage(),
                        "codigo", "ERROR_NEGOCIO"
                    ));
                    
        } catch (Exception e) {
            // Errores inesperados
            System.err.println("❌ ERROR INESPERADO:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "Error al guardar el modelo 3D: " + e.getMessage(),
                        "codigo", "ERROR_INTERNO"
                    ));
        }
    }

    /** Crea la multimedia de un producto con URLs manuales. */
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody MultimediaProductoRequest request) {
        try {
            MultimediaProductoDTO multimedia = multimediaService.crear(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(multimedia);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Multimedia de un producto; 404 si no existe. */
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<?> obtenerPorProducto(@PathVariable Long productoId) {
        System.out.println("🔍 Buscando multimedia para producto ID: " + productoId);
        
        Optional<MultimediaProductoDTO> multimedia = multimediaService.obtenerPorProducto(productoId);
        
        if (multimedia.isPresent()) {
            System.out.println("✅ Multimedia encontrada: " + multimedia.get().getId());
            return ResponseEntity.ok(multimedia.get());
        } else {
            System.out.println("⚠️  No se encontró multimedia para el producto ID: " + productoId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "mensaje", "No se encontró multimedia para este producto",
                        "productoId", productoId
                    ));
        }
    }

    /** Actualiza la multimedia (URLs manuales). */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable Long id, 
            @Valid @RequestBody MultimediaProductoRequest request
    ) {
        try {
            MultimediaProductoDTO multimedia = multimediaService.actualizar(id, request);
            return ResponseEntity.ok(multimedia);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Elimina el registro y los archivos físicos. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            multimediaService.eliminar(id);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Multimedia eliminada correctamente",
                "id", id
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar multimedia: " + e.getMessage()));
        }
    }

    /** Productos con realidad aumentada habilitada. */
    @GetMapping("/ra-habilitados")
    public ResponseEntity<List<MultimediaProductoDTO>> obtenerConRA() {
        System.out.println("🔍 Obteniendo productos con RA habilitado...");
        List<MultimediaProductoDTO> multimedia = multimediaService.obtenerProductosConRA();
        System.out.println("✅ Encontrados " + multimedia.size() + " productos con RA");
        return ResponseEntity.ok(multimedia);
    }

    /** GET /api/multimedia-productos/health */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "servicio", "MultimediaProducto API",
            "version", "1.0",
            "timestamp", System.currentTimeMillis()
        ));
    }
}