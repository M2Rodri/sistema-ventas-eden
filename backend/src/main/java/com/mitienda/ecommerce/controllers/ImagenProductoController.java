package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.ImagenProductoDTO;
import com.mitienda.ecommerce.models.ImagenProducto;
import com.mitienda.ecommerce.services.ImagenProductoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/imagenes-producto")
public class ImagenProductoController {

    private final ImagenProductoService imagenProductoService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public ImagenProductoController(ImagenProductoService imagenProductoService) {
        this.imagenProductoService = imagenProductoService;
    }


    @GetMapping("/producto/{idProducto}")
    public ResponseEntity<List<ImagenProductoDTO>> getImagenesByProductoId(@PathVariable Long idProducto) {
        List<ImagenProducto> imagenes = imagenProductoService.getImagenesByProductoId(idProducto);
        List<ImagenProductoDTO> responses = imagenes.stream()
                .map(ImagenProductoDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/producto/{idProducto}")
    public ResponseEntity<?> saveImagenProducto(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long idProducto,
            @RequestParam(defaultValue = "false") Boolean esPrincipal) {
        
        System.out.println("===========================================");
        System.out.println("🔥 CONTROLADOR DE IMÁGENES - INICIO");
        System.out.println("ID Producto: " + idProducto);
        System.out.println("Archivo: " + (file != null ? file.getOriginalFilename() : "NULL"));
        System.out.println("Es Principal: " + esPrincipal);
        System.out.println("Tamaño: " + (file != null ? file.getSize() : 0) + " bytes");
        System.out.println("Vacío: " + (file != null ? file.isEmpty() : "NULL"));
        System.out.println("===========================================");
        
        try {
            if (file == null || file.isEmpty()) {
                System.err.println("❌ Archivo vacío o nulo");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El archivo de imagen no puede estar vacío."));
            }
            
            System.out.println("✅ Llamando al servicio...");
            ImagenProducto savedImagen = imagenProductoService.saveImagenProducto(file, idProducto, esPrincipal);
            System.out.println("✅ Imagen guardada correctamente: " + savedImagen.getUrlImagen());
            
            ImagenProductoDTO response = new ImagenProductoDTO(savedImagen);
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            System.err.println("❌ ERROR DE IO:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar la imagen: " + e.getMessage()));
                    
        } catch (RuntimeException e) {
            System.err.println("❌ ERROR DE RUNTIME:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
                    
        } catch (Exception e) {
            System.err.println("❌ ERROR INESPERADO:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error inesperado: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{idImagen}")
    public ResponseEntity<?> deleteImagenProducto(@PathVariable Long idImagen) {
        try {
            imagenProductoService.deleteImagenProducto(idImagen);
            return ResponseEntity.ok(Map.of("message", "Imagen eliminada correctamente."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{idImagen}/principal/{idProducto}")
    public ResponseEntity<?> setImagenPrincipal(@PathVariable Long idImagen, @PathVariable Long idProducto) {
        try {
            imagenProductoService.setImagenPrincipal(idImagen, idProducto);
            return ResponseEntity.ok(Map.of("message", "Imagen marcada como principal correctamente."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}