package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.ImagenProductoDTO;
import com.mitienda.ecommerce.exception.PeticionInvalidaException;
import com.mitienda.ecommerce.models.ImagenProducto;
import com.mitienda.ecommerce.services.ImagenProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/imagenes-producto")
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
            @RequestParam(defaultValue = "false") Boolean esPrincipal) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new PeticionInvalidaException("ARCHIVO_VACIO", "El archivo de imagen no puede estar vacío.");
        }
        ImagenProducto savedImagen = imagenProductoService.saveImagenProducto(file, idProducto, esPrincipal);
        return ResponseEntity.ok(new ImagenProductoDTO(savedImagen));
    }

    @DeleteMapping("/{idImagen}")
    public ResponseEntity<?> deleteImagenProducto(@PathVariable Long idImagen) {
        imagenProductoService.deleteImagenProducto(idImagen);
        return ResponseEntity.ok(Map.of("message", "Imagen eliminada correctamente."));
    }

    @PutMapping("/{idImagen}/principal/{idProducto}")
    public ResponseEntity<?> setImagenPrincipal(@PathVariable Long idImagen, @PathVariable Long idProducto) {
        imagenProductoService.setImagenPrincipal(idImagen, idProducto);
        return ResponseEntity.ok(Map.of("message", "Imagen marcada como principal correctamente."));
    }
}