package com.mitienda.ecommerce.config;

import com.mitienda.ecommerce.exception.RespuestaError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Toda ruta de la API lleva el prefijo /api/v1; una /api/ sin versión responde 404 con o sin sesión (sin este filtro
 * respondería 401 y parecería activa). Corre antes de Spring Security, por eso aplica acá las reglas CORS.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RutasSinVersionFilter extends OncePerRequestFilter {
    static final String PREFIJO_API = "/api/";
    static final String PREFIJO_VERSIONADO = "/api/v1/";

    private final CorsConfigurationSource corsConfigurationSource;
    private final DefaultCorsProcessor corsProcessor = new DefaultCorsProcessor();

    public RutasSinVersionFilter(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ruta = request.getRequestURI();
        if (ruta.startsWith(PREFIJO_API) && !ruta.startsWith(PREFIJO_VERSIONADO)) {
            CorsConfiguration cors = corsConfigurationSource.getCorsConfiguration(request);
            if (!corsProcessor.processRequest(cors, request, response) || CorsUtils.isPreFlightRequest(request)) {
                return;
            }
            RespuestaError.escribir(response, HttpStatus.NOT_FOUND, "RUTA_NO_ENCONTRADA",
                    "Ruta no encontrada. Las rutas de la API van con el prefijo /api/v1");
            return;
        }
        chain.doFilter(request, response);
    }
}
