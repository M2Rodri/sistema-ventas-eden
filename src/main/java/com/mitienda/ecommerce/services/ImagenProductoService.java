package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ImagenProductoDTO;
import com.mitienda.ecommerce.models.ImagenProducto;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.repositories.ImagenProductoRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class ImagenProductoService {

    @Autowired
    private ImagenProductoRepository imagenProductoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Value("${file.upload.dir:uploads/images/}")
    private String uploadDir;

    public List<ImagenProducto> getImagenesByProductoId(Long idProducto) {
        return imagenProductoRepository.findByProductoIdOrderByOrdenAsc(idProducto);
    }

    @Transactional
    public ImagenProducto saveImagenProducto(MultipartFile file, Long idProducto, Boolean esPrincipal) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo de imagen no puede estar vacío.");
        }

        System.out.println("🔥 SERVICE - INICIO");
        System.out.println("uploadDir configurado: " + uploadDir);

        // Buscar la entidad Producto JPA
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + idProducto));

        // ✅ CORREGIDO: Crear ruta ABSOLUTA
        // Obtener el directorio actual del proyecto
        String currentDir = System.getProperty("user.dir");
        Path uploadPath = Paths.get(currentDir, uploadDir).toAbsolutePath().normalize();
        
        System.out.println("Directorio actual (user.dir): " + currentDir);
        System.out.println("Ruta absoluta de uploads: " + uploadPath.toString());

        // Crear directorio si no existe
        if (!Files.exists(uploadPath)) {
            System.out.println("⚠️  Directorio no existe, creándolo...");
            Files.createDirectories(uploadPath);
            System.out.println("✅ Directorio creado: " + uploadPath.toString());
        } else {
            System.out.println("✅ Directorio ya existe");
        }

        // Generar nombre único para el archivo
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        
        System.out.println("Ruta completa del archivo: " + filePath.toString());

        // Guardar archivo en disco
        try {
            file.transferTo(filePath.toFile());
            System.out.println("✅ Archivo guardado exitosamente");
        } catch (IOException e) {
            System.err.println("❌ Error al guardar archivo: " + e.getMessage());
            throw e;
        }

        // Crear entidad ImagenProducto
        ImagenProducto newImagen = new ImagenProducto();
        newImagen.setUrlImagen("/uploads/images/" + fileName); // Ruta relativa para acceso web
        newImagen.setEsPrincipal(esPrincipal != null && esPrincipal);
        newImagen.setProducto(producto);

        // Si se marca como principal, desmarcar la anterior del mismo producto
        if (newImagen.getEsPrincipal()) {
            desmarcarImagenPrincipalAnterior(idProducto);
        }

        // Guardar la nueva imagen en la base de datos
        ImagenProducto savedImagen = imagenProductoRepository.save(newImagen);
        System.out.println("✅ Imagen guardada en BD con ID: " + savedImagen.getId());
        
        return savedImagen;
    }

    private void desmarcarImagenPrincipalAnterior(Long idProducto) {
        List<ImagenProducto> imagenesExistentes = imagenProductoRepository.findByProductoIdOrderByOrdenAsc(idProducto);
        for (ImagenProducto img : imagenesExistentes) {
            if (img.getEsPrincipal()) {
                img.setEsPrincipal(false);
                imagenProductoRepository.save(img);
            }
        }
    }

    @Transactional
    public void deleteImagenProducto(Long idImagen) {
        ImagenProducto imagen = imagenProductoRepository.findById(idImagen)
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada con ID: " + idImagen));

        // Eliminar archivo físico si existe
        try {
            String currentDir = System.getProperty("user.dir");
            Path imagePath = Paths.get(currentDir, imagen.getUrlImagen()).toAbsolutePath().normalize();
            File fileToDelete = imagePath.toFile();
            
            if (fileToDelete.exists()) {
                fileToDelete.delete();
                System.out.println("✅ Archivo físico eliminado: " + imagePath);
            }
        } catch (Exception e) {
            System.err.println("⚠️  Error al eliminar archivo físico: " + e.getMessage());
        }

        imagenProductoRepository.deleteById(idImagen);
    }

    @Transactional
    public void setImagenPrincipal(Long idImagen, Long idProducto) {
        ImagenProducto imagen = imagenProductoRepository.findById(idImagen)
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada con ID: " + idImagen));

        if (!imagen.getProducto().getId().equals(idProducto)) {
            throw new RuntimeException("La imagen no pertenece al producto especificado");
        }

        desmarcarImagenPrincipalAnterior(idProducto);

        imagen.setEsPrincipal(true);
        imagenProductoRepository.save(imagen);
    }
}