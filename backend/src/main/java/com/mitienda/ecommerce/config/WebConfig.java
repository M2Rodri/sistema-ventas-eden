package com.mitienda.ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Obtener la ruta absoluta del directorio uploads
        String currentDir = System.getProperty("user.dir");
        
        // ========== IMÁGENES (Ya existente) ==========
        Path uploadPathImages = Paths.get(currentDir, "uploads", "images").toAbsolutePath().normalize();
        String uploadDirImages = "file:" + uploadPathImages.toString() + "/";
        
        System.out.println("🔥 CONFIGURANDO SERVIR ARCHIVOS ESTÁTICOS");
        System.out.println("Directorio IMÁGENES: " + uploadDirImages);
        
        // Mapear /uploads/images/** a la carpeta física uploads/images/
        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(uploadDirImages)
                .setCachePeriod(0); // Sin caché para desarrollo
        
        // ========== MODELOS 3D (Nuevo) ==========
        Path uploadPathModels = Paths.get(currentDir, "uploads", "modelos-3d").toAbsolutePath().normalize();
        String uploadDirModels = "file:" + uploadPathModels.toString() + "/";
        
        System.out.println("Directorio MODELOS 3D: " + uploadDirModels);
        
        // Mapear /uploads/modelos-3d/** a la carpeta física uploads/modelos-3d/
        registry.addResourceHandler("/uploads/modelos-3d/**")
                .addResourceLocations(uploadDirModels)
                .setCachePeriod(0); // Sin caché para desarrollo
    }
}