package com.mitienda.ecommerce;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class CamasColchonesApplication {
    public static void main(String[] args) {
        // Render corre en UTC: se fija la zona de Bolivia para que "hoy" y las fechas coincidan en local y en producción.
        TimeZone.setDefault(TimeZone.getTimeZone("America/La_Paz"));

        SpringApplication.run(CamasColchonesApplication.class, args);

        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                            ║");
        System.out.println("║   🛏️  API SISTEMA DE VENTA DE CAMAS Y COLCHONES 🛏️        ║");
        System.out.println("║                                                            ║");
        System.out.println("║   🚀 Servidor iniciado: http://localhost:8080             ║");
        System.out.println("║   📊 Base de datos PostgreSQL: CONECTADA                  ║");
        System.out.println("║   📖 Documentación API: http://localhost:8080/swagger-ui  ║");
        System.out.println("║                                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
    }

    /** CORS: permite las peticiones del frontend (localhost:3000 en desarrollo). */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:3000")  // Frontend Next.js
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}