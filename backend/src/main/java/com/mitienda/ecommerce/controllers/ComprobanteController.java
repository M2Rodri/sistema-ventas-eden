package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.ComprobanteRequest;
import com.mitienda.ecommerce.dto.ComprobanteResponse;
import com.mitienda.ecommerce.models.TipoComprobante;
import com.mitienda.ecommerce.services.ComprobanteService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de comprobantes
 */
@RestController
@RequestMapping("/api/comprobantes")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
public class ComprobanteController {

    private final ComprobanteService comprobanteService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public ComprobanteController(ComprobanteService comprobanteService) {
        this.comprobanteService = comprobanteService;
    }


    /**
     * GET /api/comprobantes
     * Listar todos los comprobantes
     */
    @GetMapping
    public ResponseEntity<List<ComprobanteResponse>> getAllComprobantes() {
        List<ComprobanteResponse> comprobantes = comprobanteService.getAllComprobantes();
        return ResponseEntity.ok(comprobantes);
    }

    /**
     * GET /api/comprobantes/{id}
     * Obtener comprobante por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getComprobanteById(@PathVariable Long id) {
        try {
            ComprobanteResponse comprobante = comprobanteService.getComprobanteById(id);
            return ResponseEntity.ok(comprobante);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/comprobantes/numero/{numeroComprobante}
     * Obtener comprobante por número
     */
    @GetMapping("/numero/{numeroComprobante}")
    public ResponseEntity<?> getComprobanteByNumero(@PathVariable String numeroComprobante) {
        try {
            ComprobanteResponse comprobante = comprobanteService.getComprobanteByNumero(numeroComprobante);
            return ResponseEntity.ok(comprobante);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/comprobantes/venta/{idVenta}
     * Obtener comprobante por venta
     */
    @GetMapping("/venta/{idVenta}")
    public ResponseEntity<?> getComprobanteByVenta(@PathVariable Long idVenta) {
        try {
            ComprobanteResponse comprobante = comprobanteService.getComprobanteByVenta(idVenta);
            return ResponseEntity.ok(comprobante);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/comprobantes
     * Crear comprobante
     */
    @PostMapping
    public ResponseEntity<?> createComprobante(@Valid @RequestBody ComprobanteRequest request) {
        try {
            ComprobanteResponse createdComprobante = comprobanteService.createComprobante(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdComprobante);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/comprobantes/{id}/anular
     * Anular comprobante
     */
    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> anularComprobante(@PathVariable Long id, 
                                               @RequestParam String motivo) {
        try {
            ComprobanteResponse comprobante = comprobanteService.anularComprobante(id, motivo);
            return ResponseEntity.ok(comprobante);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/comprobantes/activos
     * Comprobantes no anulados
     */
    @GetMapping("/activos")
    public ResponseEntity<List<ComprobanteResponse>> getComprobantesActivos() {
        List<ComprobanteResponse> comprobantes = comprobanteService.getComprobantesActivos();
        return ResponseEntity.ok(comprobantes);
    }

    /**
     * GET /api/comprobantes/anulados
     * Comprobantes anulados
     */
    @GetMapping("/anulados")
    public ResponseEntity<List<ComprobanteResponse>> getComprobantesAnulados() {
        List<ComprobanteResponse> comprobantes = comprobanteService.getComprobantesAnulados();
        return ResponseEntity.ok(comprobantes);
    }

    /**
     * GET /api/comprobantes/tipo/{tipo}
     * Comprobantes por tipo
     */
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<?> getComprobantesByTipo(@PathVariable String tipo) {
        try {
            TipoComprobante tipoEnum = TipoComprobante.valueOf(tipo.toUpperCase());
            List<ComprobanteResponse> comprobantes = comprobanteService.getComprobantesByTipo(tipoEnum);
            return ResponseEntity.ok(comprobantes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Tipo de comprobante inválido: " + tipo));
        }
    }

    /**
     * GET /api/comprobantes/fechas?inicio=...&fin=...
     * Comprobantes entre fechas
     */
    @GetMapping("/fechas")
    public ResponseEntity<List<ComprobanteResponse>> getComprobantesByFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        List<ComprobanteResponse> comprobantes = comprobanteService.getComprobantesByFechas(inicio, fin);
        return ResponseEntity.ok(comprobantes);
    }

    /**
     * GET /api/comprobantes/ultimos
     * Últimos 20 comprobantes
     */
    @GetMapping("/ultimos")
    public ResponseEntity<List<ComprobanteResponse>> getUltimosComprobantes() {
        List<ComprobanteResponse> comprobantes = comprobanteService.getUltimosComprobantes();
        return ResponseEntity.ok(comprobantes);
    }

    /**
     * GET /api/comprobantes/estadisticas
     * Obtener estadísticas de comprobantes (ADMIN)
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getComprobanteStatistics() {
        Long totalDelMes = comprobanteService.countComprobantesDelMes();
        return ResponseEntity.ok(Map.of("comprobantesMesActual", totalDelMes));
    }
}