package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.EnvioRequest;
import com.mitienda.ecommerce.dto.EnvioResponse;
import com.mitienda.ecommerce.models.EstadoEnvio;
import com.mitienda.ecommerce.services.EnvioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Controlador REST para gestión de envíos */
@RestController
@RequestMapping("/api/envios")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
public class EnvioController {
    private final EnvioService envioService;

    public EnvioController(EnvioService envioService) {
        this.envioService = envioService;
    }

    /** Listar todos los envíos */
    @GetMapping
    public ResponseEntity<List<EnvioResponse>> getAllEnvios() {
        List<EnvioResponse> envios = envioService.getAllEnvios();
        return ResponseEntity.ok(envios);
    }

    /** Obtener envío por ID */
    @GetMapping("/{id}")
    public ResponseEntity<?> getEnvioById(@PathVariable Long id) {
        try {
            EnvioResponse envio = envioService.getEnvioById(id);
            return ResponseEntity.ok(envio);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** Obtener envío por venta */
    @GetMapping("/venta/{idVenta}")
    public ResponseEntity<?> getEnvioByVenta(@PathVariable Long idVenta) {
        try {
            EnvioResponse envio = envioService.getEnvioByVenta(idVenta);
            return ResponseEntity.ok(envio);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** Obtener envío por guía de remisión (público para tracking) */
    @GetMapping("/guia/{guiaRemision}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getEnvioByGuia(@PathVariable String guiaRemision) {
        try {
            EnvioResponse envio = envioService.getEnvioByGuia(guiaRemision);
            return ResponseEntity.ok(envio);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** Crear nuevo envío */
    @PostMapping
    public ResponseEntity<?> createEnvio(@Valid @RequestBody EnvioRequest request) {
        try {
            EnvioResponse createdEnvio = envioService.createEnvio(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdEnvio);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** Actualizar envío existente */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEnvio(@PathVariable Long id,
                                         @Valid @RequestBody EnvioRequest request) {
        try {
            EnvioResponse updatedEnvio = envioService.updateEnvio(id, request);
            return ResponseEntity.ok(updatedEnvio);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** Cambiar estado del envío */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstadoEnvio(@PathVariable Long id,
                                                @RequestParam EstadoEnvio nuevoEstado) {
        try {
            EnvioResponse envio = envioService.cambiarEstadoEnvio(id, nuevoEstado);
            return ResponseEntity.ok(envio);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** Marcar envío como entregado */
    @PatchMapping("/{id}/entregar")
    public ResponseEntity<?> marcarComoEntregado(@PathVariable Long id) {
        try {
            EnvioResponse envio = envioService.marcarComoEntregado(id);
            return ResponseEntity.ok(envio);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** Filtrar envíos por estado */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<?> getEnviosByEstado(@PathVariable String estado) {
        try {
            EstadoEnvio estadoEnum = EstadoEnvio.valueOf(estado.toUpperCase());
            List<EnvioResponse> envios = envioService.getEnviosByEstado(estadoEnum);
            return ResponseEntity.ok(envios);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Estado de envío inválido: " + estado));
        }
    }

    /** Envíos por transportadora */
    @GetMapping("/transportadora/{idTransportadora}")
    public ResponseEntity<List<EnvioResponse>> getEnviosByTransportadora(@PathVariable Long idTransportadora) {
        List<EnvioResponse> envios = envioService.getEnviosByTransportadora(idTransportadora);
        return ResponseEntity.ok(envios);
    }

    /** Envíos pendientes */
    @GetMapping("/pendientes")
    public ResponseEntity<List<EnvioResponse>> getEnviosPendientes() {
        List<EnvioResponse> envios = envioService.getEnviosPendientes();
        return ResponseEntity.ok(envios);
    }

    /** Envíos en camino */
    @GetMapping("/en-camino")
    public ResponseEntity<List<EnvioResponse>> getEnviosEnCamino() {
        List<EnvioResponse> envios = envioService.getEnviosEnCamino();
        return ResponseEntity.ok(envios);
    }

    /** Envíos por entregar hoy o atrasados */
    @GetMapping("/por-entregar")
    public ResponseEntity<List<EnvioResponse>> getEnviosPorEntregar() {
        List<EnvioResponse> envios = envioService.getEnviosPorEntregar();
        return ResponseEntity.ok(envios);
    }

    /** Obtener estadísticas de envíos (ADMIN) */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getEnvioStatistics() {
        Long totalPendientes      = envioService.countEnviosByEstado(EstadoEnvio.PENDIENTE);
        Long totalEnPreparacion   = envioService.countEnviosByEstado(EstadoEnvio.EN_PREPARACION);
        Long totalEnCamino        = envioService.countEnviosByEstado(EstadoEnvio.EN_CAMINO);
        Long totalEntregados      = envioService.countEnviosByEstado(EstadoEnvio.ENTREGADO);
        Long totalDevueltos       = envioService.countEnviosByEstado(EstadoEnvio.DEVUELTO);
        Long totalCancelados      = envioService.countEnviosByEstado(EstadoEnvio.CANCELADO);

        return ResponseEntity.ok(Map.of(
                "pendientes",     totalPendientes,
                "enPreparacion",  totalEnPreparacion,
                "enCamino",       totalEnCamino,
                "entregados",     totalEntregados,
                "devueltos",      totalDevueltos,
                "cancelados",     totalCancelados
        ));
    }
}