package com.mitienda.ecommerce.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Servicio genérico para almacenamiento de archivos
 * Reutilizable para imágenes, modelos 3D, PDFs, etc.
 *
 * TODO (después del E2, antes del E3): migrar a Supabase Storage.
 * Hoy los archivos se guardan en el disco del contenedor. En Render (y en
 * cualquier plataforma similar sin disco persistente en el plan gratis) ese
 * disco se recrea vacío tanto al redesplegar como cada vez que el servicio
 * se duerme por inactividad y vuelve a levantarse -- lo que se suba se
 * pierde. La base ya vive en Supabase, que también ofrece Storage: mover
 * saveFile/deleteFile/fileExists/getFileSize a su API evita este problema
 * sin depender de un disco propio.
 */
@Service
public class FileStorageService {

    /**
     * Guardar archivo en el sistema de archivos
     * 
     * @param file Archivo a guardar
     * @param subdirectory Subdirectorio dentro de uploads/ (ej: "images", "modelos-3d")
     * @param allowedExtensions Extensiones permitidas (ej: [".jpg", ".png"])
     * @param maxSizeBytes Tamaño máximo en bytes
     * @return Ruta relativa del archivo guardado (ej: "/uploads/modelos-3d/xyz.glb")
     */
    public String saveFile(
            MultipartFile file, 
            String subdirectory,
            List<String> allowedExtensions,
            long maxSizeBytes
    ) throws IOException {
        
        System.out.println("📁 === GUARDANDO ARCHIVO ===");
        System.out.println("Subdirectorio: " + subdirectory);
        System.out.println("Extensiones permitidas: " + allowedExtensions);
        System.out.println("Tamaño máximo: " + (maxSizeBytes / (1024 * 1024)) + " MB");
        
        // Validar que el archivo no esté vacío
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }

        // Validar tamaño
        if (file.getSize() > maxSizeBytes) {
            long maxSizeMB = maxSizeBytes / (1024 * 1024);
            throw new IllegalArgumentException(
                String.format("El archivo excede el tamaño máximo permitido de %d MB", maxSizeMB)
            );
        }

        // Validar nombre del archivo
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del archivo no puede ser nulo o vacío");
        }

        // Validar extensión
        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        if (!allowedExtensions.contains(fileExtension)) {
            throw new IllegalArgumentException(
                String.format("Extensión de archivo no permitida. Permitidas: %s, Recibida: %s", 
                    String.join(", ", allowedExtensions),
                    fileExtension)
            );
        }

        System.out.println("✅ Validaciones pasadas");
        System.out.println("Nombre original: " + originalFilename);
        System.out.println("Extensión: " + fileExtension);
        System.out.println("Tamaño: " + (file.getSize() / 1024) + " KB");

        // Crear directorio si no existe
        String currentDir = System.getProperty("user.dir");
        Path uploadPath = Paths.get(currentDir, "uploads", subdirectory)
                               .toAbsolutePath()
                               .normalize();

        System.out.println("Directorio de destino: " + uploadPath);

        if (!Files.exists(uploadPath)) {
            System.out.println("📁 Directorio no existe, creándolo...");
            Files.createDirectories(uploadPath);
            System.out.println("✅ Directorio creado exitosamente");
        } else {
            System.out.println("✅ Directorio ya existe");
        }

        // Generar nombre unico.
        //
        // El nombre lo elige quien sube el archivo, asi que antes de usarlo hay
        // que limpiarlo: se descarta cualquier componente de ruta. Sin esto, un
        // nombre como "../../algo.glb" hacia que resolve() escribiera fuera de
        // la carpeta de subidas.
        String uniqueFileName = UUID.randomUUID() + "_" + limpiarNombreArchivo(originalFilename);
        Path filePath = uploadPath.resolve(uniqueFileName).normalize();

        // Cinturon y tirantes: aunque el nombre ya viene saneado, se comprueba
        // que la ruta final siga estando dentro de la carpeta de destino. Es la
        // verificacion que de verdad cierra el paso, porque no depende de haber
        // previsto todas las formas de escribir una ruta.
        if (!filePath.startsWith(uploadPath)) {
            throw new IllegalArgumentException("Nombre de archivo no permitido: " + originalFilename);
        }

        // Guardar archivo
        try {
            file.transferTo(filePath.toFile());
            System.out.println("✅ Archivo guardado exitosamente en disco");
        } catch (IOException e) {
            System.err.println("❌ Error al guardar archivo en disco: " + e.getMessage());
            throw new IOException("Error al guardar el archivo: " + e.getMessage(), e);
        }

        // Retornar ruta relativa
        String relativeUrl = "/uploads/" + subdirectory + "/" + uniqueFileName;
        System.out.println("URL relativa para BD: " + relativeUrl);
        System.out.println("=========================");
        
        return relativeUrl;
    }

    /**
     * Eliminar archivo del sistema de archivos
     */
    public void deleteFile(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.trim().isEmpty()) {
            System.out.println("⚠️  URL relativa nula o vacía, no se eliminará nada");
            return;
        }

        try {
            String currentDir = System.getProperty("user.dir");
            Path filePath = Paths.get(currentDir, relativeUrl)
                                 .toAbsolutePath()
                                 .normalize();
            
            File file = filePath.toFile();
            if (file.exists()) {
                if (file.delete()) {
                    System.out.println("🗑️  Archivo eliminado exitosamente: " + filePath);
                } else {
                    System.err.println("⚠️  No se pudo eliminar el archivo: " + filePath);
                }
            } else {
                System.out.println("⚠️  Archivo no existe (ya fue eliminado): " + filePath);
            }
        } catch (Exception e) {
            System.err.println("⚠️  Error al eliminar archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Verificar si un archivo existe
     */
    public boolean fileExists(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.trim().isEmpty()) {
            return false;
        }

        try {
            String currentDir = System.getProperty("user.dir");
            Path filePath = Paths.get(currentDir, relativeUrl)
                                 .toAbsolutePath()
                                 .normalize();
            return Files.exists(filePath);
        } catch (Exception e) {
            System.err.println("⚠️  Error al verificar existencia del archivo: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtener extensión de un archivo
     */
    /**
     * Deja solo el nombre del archivo, sin ninguna parte de ruta.
     *
     * Toma el ultimo segmento despues de / o de \\ (el cliente puede mandar
     * separadores de Windows o de Unix) y reemplaza por guion bajo todo lo que
     * no sea una letra, un numero, un punto, un guion o un guion bajo. Asi
     * quedan fuera los ".." y cualquier caracter raro para el sistema de
     * archivos, pero se conserva la extension, que se valida aparte.
     */
    private String limpiarNombreArchivo(String filename) {
        String soloNombre = filename;

        int ultimaBarra = Math.max(soloNombre.lastIndexOf('/'), soloNombre.lastIndexOf('\\'));
        if (ultimaBarra >= 0) {
            soloNombre = soloNombre.substring(ultimaBarra + 1);
        }

        soloNombre = soloNombre.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Un nombre que era solo puntos o separadores puede quedar vacio.
        if (soloNombre.isBlank() || soloNombre.replace(".", "").isBlank()) {
            throw new IllegalArgumentException("Nombre de archivo no permitido: " + filename);
        }

        // Evita nombres desmesurados que rompan el limite del sistema de archivos.
        if (soloNombre.length() > 150) {
            String extension = getFileExtension(soloNombre);
            soloNombre = soloNombre.substring(0, 150 - extension.length()) + extension;
        }

        return soloNombre;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        
        return filename.substring(lastDotIndex);
    }

    /**
     * Obtener tamaño de un archivo en bytes
     */
    public long getFileSize(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.trim().isEmpty()) {
            return -1;
        }

        try {
            String currentDir = System.getProperty("user.dir");
            Path filePath = Paths.get(currentDir, relativeUrl)
                                 .toAbsolutePath()
                                 .normalize();
            
            File file = filePath.toFile();
            if (file.exists()) {
                return file.length();
            }
        } catch (Exception e) {
            System.err.println("⚠️  Error al obtener tamaño del archivo: " + e.getMessage());
        }
        
        return -1;
    }
}