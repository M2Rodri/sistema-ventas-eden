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