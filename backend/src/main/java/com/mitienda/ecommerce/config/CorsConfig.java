package com.mitienda.ecommerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    // Dominios extra separados por coma (por ejemplo, la URL publica del
    // frontend una vez desplegado), sin tener que tocar el codigo para
    // agregarlos. Los de abajo quedan siempre permitidos como base.
    @Value("${CORS_ALLOWED_ORIGINS:}")
    private String origenesExtra;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origenes = new ArrayList<>(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:5001",
                "https://frontend-sistema-ventas-eight.vercel.app",
                "https://frontend-sistema-ventas-8u2xm2x4o-rodrimrx-s-projects.vercel.app"
            ));
        if (origenesExtra != null && !origenesExtra.isBlank()) {
            for (String origen : origenesExtra.split(",")) {
                if (!origen.isBlank()) {
                    origenes.add(origen.trim());
                }
            }
        }
        configuration.setAllowedOrigins(origenes);

        // Métodos HTTP permitidos (AGREGADO PATCH)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Headers permitidos
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Permitir credenciales (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Exponer headers
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        // Aplicar a todas las rutas
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}