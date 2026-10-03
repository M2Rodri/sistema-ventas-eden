package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.exception.RespuestaError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Ruta de salud publica (sin login), requerida por la plataforma de
 * despliegue para confirmar que el backend sigue arriba. El CORS ya lo
 * resuelve el bean global en CorsConfig, no hace falta anotarlo acá.
 *
 * Además de que el servicio responda, comprueba que la base de datos
 * conteste (SELECT 1). Si no contesta, responde 503 con el formato único de error.
 */
@RestController
@RequestMapping("/api/v1")
public class SaludController {

    private static final Logger log = LoggerFactory.getLogger(SaludController.class);

    private final JdbcTemplate jdbc;

    public SaludController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/salud")
    public ResponseEntity<?> salud() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
        } catch (DataAccessException e) {
            log.error("La base de datos no responde: {}", e.getMostSpecificCause().getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(RespuestaError.de("BASE_NO_DISPONIBLE", "La base de datos no está disponible"));
        }
        return ResponseEntity.ok(Map.of(
                "estado", "OK",
                "servicio", "Mueblería Edén API",
                "baseDeDatos", "DISPONIBLE",
                "fecha", LocalDateTime.now().toString()
        ));
    }
}
