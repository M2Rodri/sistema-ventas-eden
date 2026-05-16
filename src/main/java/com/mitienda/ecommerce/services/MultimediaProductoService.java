package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.MultimediaProductoDTO;
import com.mitienda.ecommerce.dto.MultimediaProductoRequest;
import com.mitienda.ecommerce.models.MultimediaProducto;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.repositories.MultimediaProductoRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MultimediaProductoService {

    @Autowired
    private MultimediaProductoRepository multimediaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private FileStorageService fileStorageService;

    // Configuración desde application.properties
    @Value("${file.upload.models3d.max-size:52428800}") // 50MB por defecto
    private long maxFileSize;

    // Extensiones permitidas para modelos 3D
    private static final List<String> ALLOWED_MODEL_EXTENSIONS = Arrays.asList(".glb", ".gltf", ".usdz");
    
    // Extensiones permitidas para imágenes de preview
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".webp");

    /**
     * ✅ NUEVO: Subir modelo 3D para un producto
     * 
     * @param archivoModelo Archivo del modelo 3D (.glb, .gltf, .usdz)
     * @param archivoPreview Imagen de vista previa (opcional)
     * @param productoId ID del producto
     * @param habilitadoRa Si se habilita la realidad aumentada
     * @return DTO con la información de la multimedia guardada
     * @throws IOException Si hay error al guardar los archivos
     */
    @Transactional
    public MultimediaProductoDTO subirModelo3D(
            MultipartFile archivoModelo,
            MultipartFile archivoPreview,
            Long productoId,
            Boolean habilitadoRa
    ) throws IOException {
        
        System.out.println("🎨 ==========================================");
        System.out.println("🎨 === SERVICIO: SUBIR MODELO 3D ===");
        System.out.println("🎨 ==========================================");
        System.out.println("Producto ID: " + productoId);
        System.out.println("Archivo modelo: " + (archivoModelo != null ? archivoModelo.getOriginalFilename() : "NULL"));
        System.out.println("Tamaño modelo: " + (archivoModelo != null ? archivoModelo.getSize() / 1024 : 0) + " KB");
        System.out.println("Archivo preview: " + (archivoPreview != null ? archivoPreview.getOriginalFilename() : "NULL"));
        System.out.println("Habilitado RA: " + habilitadoRa);
        
        // ========== VALIDAR PRODUCTO ==========
        
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + productoId));
        
        System.out.println("✅ Producto encontrado: " + producto.getNombre());

        // ========== VALIDAR ARCHIVO MODELO ==========
        
        if (archivoModelo == null || archivoModelo.isEmpty()) {
            throw new IllegalArgumentException("El archivo del modelo 3D es obligatorio");
        }

        // ========== GUARDAR MODELO 3D ==========
        
        System.out.println("📦 Guardando modelo 3D...");
        String urlModelo3d = fileStorageService.saveFile(
            archivoModelo,
            "modelos-3d",
            ALLOWED_MODEL_EXTENSIONS,
            maxFileSize
        );
        System.out.println("✅ Modelo 3D guardado: " + urlModelo3d);

        // ========== GUARDAR PREVIEW (OPCIONAL) ==========
        
        String urlVistaPrevia = null;
        if (archivoPreview != null && !archivoPreview.isEmpty()) {
            System.out.println("🖼️  Guardando imagen de vista previa...");
            urlVistaPrevia = fileStorageService.saveFile(
                archivoPreview,
                "modelos-3d/previews",
                ALLOWED_IMAGE_EXTENSIONS,
                10 * 1024 * 1024 // 10MB max para preview
            );
            System.out.println("✅ Preview guardado: " + urlVistaPrevia);
        } else {
            System.out.println("ℹ️  No se proporcionó imagen de preview (opcional)");
        }

        // ========== BUSCAR SI YA EXISTE MULTIMEDIA PARA ESTE PRODUCTO ==========
        
        Optional<MultimediaProducto> existente = multimediaRepository.findByProductoId(productoId);
        
        MultimediaProducto multimedia;
        
        if (existente.isPresent()) {
            // ===== ACTUALIZAR REGISTRO EXISTENTE =====
            System.out.println("⚠️  Ya existe multimedia para este producto, actualizando...");
            multimedia = existente.get();
            
            // Eliminar archivos anteriores del disco
            if (multimedia.getUrlModelo3d() != null) {
                System.out.println("🗑️  Eliminando modelo 3D anterior: " + multimedia.getUrlModelo3d());
                fileStorageService.deleteFile(multimedia.getUrlModelo3d());
            }
            if (multimedia.getUrlVistaPrevia() != null) {
                System.out.println("🗑️  Eliminando preview anterior: " + multimedia.getUrlVistaPrevia());
                fileStorageService.deleteFile(multimedia.getUrlVistaPrevia());
            }
            
            // Actualizar con nuevos valores
            multimedia.setUrlModelo3d(urlModelo3d);
            multimedia.setUrlVistaPrevia(urlVistaPrevia);
            multimedia.setHabilitadoRa(habilitadoRa != null ? habilitadoRa : false);
            multimedia.setActivo(true);
            
            System.out.println("✅ Multimedia actualizada");
            
        } else {
            // ===== CREAR NUEVO REGISTRO =====
            System.out.println("➕ Creando nueva multimedia para el producto...");
            multimedia = new MultimediaProducto(
                producto,
                urlModelo3d,
                urlVistaPrevia,
                habilitadoRa != null ? habilitadoRa : false
            );
            
            System.out.println("✅ Nueva multimedia creada");
        }

        // ========== GUARDAR EN BASE DE DATOS ==========
        
        MultimediaProducto saved = multimediaRepository.save(multimedia);
        System.out.println("💾 Multimedia guardada en BD con ID: " + saved.getId());
        System.out.println("🎨 ==========================================");
        
        return convertirADTO(saved);
    }

    /**
     * Crear multimedia para producto (método original - URLs manuales)
     */
    @Transactional
    public MultimediaProductoDTO crear(MultimediaProductoRequest request) {
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        MultimediaProducto multimedia = new MultimediaProducto(
            producto,
            request.getUrlModelo3d(),
            request.getUrlVistaPrevia(),
            request.getHabilitadoRa()
        );

        MultimediaProducto saved = multimediaRepository.save(multimedia);
        return convertirADTO(saved);
    }

    /**
     * Obtener multimedia por producto
     */
    public Optional<MultimediaProductoDTO> obtenerPorProducto(Long productoId) {
        return multimediaRepository.findByProductoId(productoId)
                .map(this::convertirADTO);
    }

    /**
     * Actualizar multimedia (URLs manuales)
     */
    @Transactional
    public MultimediaProductoDTO actualizar(Long id, MultimediaProductoRequest request) {
        MultimediaProducto multimedia = multimediaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Multimedia no encontrada"));

        multimedia.setUrlModelo3d(request.getUrlModelo3d());
        multimedia.setUrlVistaPrevia(request.getUrlVistaPrevia());
        multimedia.setHabilitadoRa(request.getHabilitadoRa());

        MultimediaProducto updated = multimediaRepository.save(multimedia);
        return convertirADTO(updated);
    }

    /**
     * Eliminar multimedia (incluye archivos físicos)
     */
    @Transactional
    public void eliminar(Long id) {
        MultimediaProducto multimedia = multimediaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Multimedia no encontrada con ID: " + id));
        
        System.out.println("🗑️  Eliminando multimedia ID: " + id);
        
        // Eliminar archivos físicos del disco
        if (multimedia.getUrlModelo3d() != null) {
            System.out.println("🗑️  Eliminando modelo 3D: " + multimedia.getUrlModelo3d());
            fileStorageService.deleteFile(multimedia.getUrlModelo3d());
        }
        if (multimedia.getUrlVistaPrevia() != null) {
            System.out.println("🗑️  Eliminando preview: " + multimedia.getUrlVistaPrevia());
            fileStorageService.deleteFile(multimedia.getUrlVistaPrevia());
        }
        
        // Eliminar registro de la base de datos
        multimediaRepository.deleteById(id);
        System.out.println("✅ Multimedia eliminada correctamente");
    }

    /**
     * Listar productos con RA habilitado
     */
    public List<MultimediaProductoDTO> obtenerProductosConRA() {
        return multimediaRepository.findByHabilitadoRaTrueAndActivoTrue()
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Convertir entidad a DTO
     */
    private MultimediaProductoDTO convertirADTO(MultimediaProducto multimedia) {
        MultimediaProductoDTO dto = new MultimediaProductoDTO();
        dto.setId(multimedia.getId());
        dto.setProductoId(multimedia.getProducto().getId());
        dto.setUrlModelo3d(multimedia.getUrlModelo3d());
        dto.setUrlVistaPrevia(multimedia.getUrlVistaPrevia());
        dto.setHabilitadoRa(multimedia.getHabilitadoRa());
        dto.setActivo(multimedia.getActivo());
        return dto;
    }
}