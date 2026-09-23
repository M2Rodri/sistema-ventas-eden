package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.ConfiguracionRequest;
import com.mitienda.ecommerce.dto.ConfiguracionResponse;
import com.mitienda.ecommerce.services.ConfiguracionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de configuraciones del sistema
 */
@RestController
@RequestMapping("/api/configuracion")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasRole('ADMIN')")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public ConfiguracionController(ConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
    }


    /**
     * GET /api/configuracion/negocio
     *
     * Datos del negocio (nombre, dirección, teléfono, horario). Antes era
     * público para que la tienda virtual los mostrara sin login; la tienda
     * quedó fuera del alcance del proyecto, así que ahora hereda el
     * @PreAuthorize de la clase (solo ADMIN) como el resto de configuración.
     *
     * Devuelve únicamente las claves que empiezan con "negocio_": así, agregar
     * un parámetro interno nuevo nunca lo publica por accidente.
     */
    @GetMapping("/negocio")
    public ResponseEntity<Map<String, String>> getDatosNegocio() {
        Map<String, String> datos = configuracionService.getAllConfiguraciones().stream()
                .filter(c -> c.getClave() != null && c.getClave().startsWith("negocio_"))
                .collect(java.util.stream.Collectors.toMap(
                        ConfiguracionResponse::getClave,
                        c -> c.getValor() != null ? c.getValor() : ""));
        return ResponseEntity.ok(datos);
    }

    /**
     * GET /api/configuracion
     * Listar todas las configuraciones
     */
    @GetMapping
    public ResponseEntity<List<ConfiguracionResponse>> getAllConfiguraciones() {
        List<ConfiguracionResponse> configuraciones = configuracionService.getAllConfiguraciones();
        return ResponseEntity.ok(configuraciones);
    }

    /**
     * GET /api/configuracion/{id}
     * Obtener configuración por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getConfiguracionById(@PathVariable Long id) {
        try {
            ConfiguracionResponse config = configuracionService.getConfiguracionById(id);
            return ResponseEntity.ok(config);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/configuracion/clave/{clave}
     * Obtener configuración por clave
     */
    @GetMapping("/clave/{clave}")
    public ResponseEntity<?> getConfiguracionByClave(@PathVariable String clave) {
        try {
            ConfiguracionResponse config = configuracionService.getConfiguracionByClave(clave);
            return ResponseEntity.ok(config);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/configuracion/valor/{clave}
     * Obtener solo el valor de una configuración
     */
    @GetMapping("/valor/{clave}")
    public ResponseEntity<?> getValorConfiguracion(@PathVariable String clave) {
        try {
            String valor = configuracionService.getValorConfiguracion(clave);
            return ResponseEntity.ok(Map.of("clave", clave, "valor", valor));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/configuracion
     * Crear nueva configuración
     */
    @PostMapping
    public ResponseEntity<?> createConfiguracion(@Valid @RequestBody ConfiguracionRequest request) {
        try {
            ConfiguracionResponse createdConfig = configuracionService.createConfiguracion(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdConfig);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/configuracion/{id}
     * Actualizar configuración existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateConfiguracion(@PathVariable Long id, 
                                                 @Valid @RequestBody ConfiguracionRequest request) {
        try {
            ConfiguracionResponse updatedConfig = configuracionService.updateConfiguracion(id, request);
            return ResponseEntity.ok(updatedConfig);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/configuracion/clave/{clave}
     * Actualizar solo el valor de una configuración
     */
    @PatchMapping("/clave/{clave}")
    public ResponseEntity<?> updateValorByClave(@PathVariable String clave, 
                                               @RequestParam String nuevoValor) {
        try {
            ConfiguracionResponse config = configuracionService.updateValorByClave(clave, nuevoValor);
            return ResponseEntity.ok(config);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/configuracion/{id}
     * Eliminar configuración
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConfiguracion(@PathVariable Long id) {
        try {
            configuracionService.deleteConfiguracion(id);
            return ResponseEntity.ok(Map.of("message", "Configuración eliminada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/configuracion/inicializar
     * Inicializar configuraciones por defecto
     */
    @PostMapping("/inicializar")
    public ResponseEntity<?> inicializarConfiguraciones() {
        try {
            configuracionService.inicializarConfiguracionesDefecto();
            return ResponseEntity.ok(Map.of("message", "Configuraciones inicializadas correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}