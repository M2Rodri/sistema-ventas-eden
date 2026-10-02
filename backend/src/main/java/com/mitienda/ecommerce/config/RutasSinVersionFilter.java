package com.mitienda.ecommerce.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Toda ruta de la API va con el prefijo /api/v1. Una ruta que empiece con /api/
 * pero sin esa versión responde 404, con o sin sesión.
 *
 * Sin este filtro, una ruta vieja sin sesión respondería 401 (la seguridad
 * actúa antes de saber que la ruta no existe) y parecería que sigue activa.
 *
 * Además protege hacia adelante: un controlador nuevo escrito con /api/algo,
 * sin versión, queda inalcanzable en vez de quedar publicado por descuido. Con
 * esto también quedan apagados los módulos fuera de alcance que siguen sin
 * versión (envíos, transportadoras, promociones, configuración, auditorías,
 * mensajes de contacto y multimedia 3D), sin borrar todavía su código.
 *
 * Corre antes que Spring Security, que es lo que lo hace valer también sin token.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RutasSinVersionFilter extends OncePerRequestFilter {

    static final String PREFIJO_API = "/api/";
    static final String PREFIJO_VERSIONADO = "/api/v1/";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ruta = request.getRequestURI();
        if (ruta.startsWith(PREFIJO_API) && !ruta.startsWith(PREFIJO_VERSIONADO)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Ruta no encontrada. Las rutas de la API van con el prefijo /api/v1\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
