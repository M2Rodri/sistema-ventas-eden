package com.mitienda.ecommerce.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Herramienta SOLO de desarrollo para saber por qué una pantalla tarda.
 *
 * Por cada petición a /api/ anota en target/medicion-rendimiento.log cuánto
 * tardó el servidor en responder, cuántas sentencias SQL mandó a la base y
 * cuánto pesó la respuesta. Muchas consultas para una sola pantalla suelen
 * indicar consultas repetidas (el problema "N+1").
 *
 * Solo existe con el perfil "dev": en producción no se crea.
 */
@Component
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MedicionRendimiento extends OncePerRequestFilter {

    private static final ThreadLocal<int[]> CONSULTAS = ThreadLocal.withInitial(() -> new int[1]);
    private static final Path ARCHIVO = Path.of("target", "medicion-rendimiento.log");

    /** Cuenta cada sentencia SQL que Hibernate manda a la base en el hilo actual. */
    public static class ContadorSql implements StatementInspector {
        @Override
        public String inspect(String sql) {
            CONSULTAS.get()[0]++;
            return sql;
        }
    }

    @Configuration
    @Profile("dev")
    public static class Registro {
        @Bean
        HibernatePropertiesCustomizer contadorDeConsultas() {
            return propiedades -> propiedades.put(AvailableSettings.STATEMENT_INSPECTOR, new ContadorSql());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/") || "OPTIONS".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingResponseWrapper envuelta = new ContentCachingResponseWrapper(response);
        CONSULTAS.get()[0] = 0;
        long inicio = System.nanoTime();
        try {
            chain.doFilter(request, envuelta);
        } finally {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            int consultas = CONSULTAS.get()[0];
            int bytes = envuelta.getContentSize();
            envuelta.copyBodyToResponse();
            anotar(request, envuelta.getStatus(), ms, consultas, bytes);
        }
    }

    private void anotar(HttpServletRequest request, int estado, long ms, int consultas, int bytes) {
        String consulta = request.getQueryString() != null ? "?" + request.getQueryString() : "";
        String linea = String.format("%s %-6s %s%s -> %d | %d ms | %d consultas | %d KB%n",
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), request.getMethod(),
                request.getRequestURI(), consulta, estado, ms, consultas, bytes / 1024);
        try {
            synchronized (ARCHIVO) {
                Files.writeString(ARCHIVO, linea, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            // Es solo una ayuda de desarrollo: si no puede escribir, no debe molestar.
        }
    }
}
