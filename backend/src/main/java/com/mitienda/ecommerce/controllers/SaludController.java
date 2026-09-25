package com.mitienda.ecommerce.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Ruta de salud publica (sin login), requerida por la plataforma de
 * despliegue para confirmar que el backend sigue arriba. El CORS ya lo
 * resuelve el bean global en CorsConfig, no hace falta anotarlo acá.
 */
@RestController
@RequestMapping("/api/v1")
public class SaludController {

    @GetMapping("/salud")
    public ResponseEntity<?> salud() {
        return ResponseEntity.ok(Map.of(
                "estado", "OK",
                "servicio", "Mueblería Edén API",
                "fecha", LocalDateTime.now().toString()
        ));
    }
}
