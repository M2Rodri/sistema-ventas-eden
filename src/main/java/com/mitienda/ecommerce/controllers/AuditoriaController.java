package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.AuditoriaResponse;
import com.mitienda.ecommerce.services.AuditoriaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para consulta de auditorías
 */
@RestController
@RequestMapping("/api/auditorias")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }


    /**
     * GET /api/auditorias
     * Listar todas las auditorías
     */
    @GetMapping
    public ResponseEntity<List<AuditoriaResponse>> getAllAuditorias() {
        List<AuditoriaResponse> auditorias = auditoriaService.getAllAuditorias();
        return ResponseEntity.ok(auditorias);
    }

    /**
     * GET /api/auditorias/{id}
     * Obtener auditoría por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAuditoriaById(@PathVariable Long id) {
        try {
            AuditoriaResponse auditoria = auditoriaService.getAuditoriaById(id);
            return ResponseEntity.ok(auditoria);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/auditorias/usuario/{idUsuario}
     * Auditorías de un usuario
     */
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<AuditoriaResponse>> getAuditoriasByUsuario(@PathVariable Long idUsuario) {
        List<AuditoriaResponse> auditorias = auditoriaService.getAuditoriasByUsuario(idUsuario);
        return ResponseEntity.ok(auditorias);
    }

    /**
     * GET /api/auditorias/tabla/{tablaAfectada}
     * Auditorías de una tabla específica
     */
    @GetMapping("/tabla/{tablaAfectada}")
    public ResponseEntity<List<AuditoriaResponse>> getAuditoriasByTabla(@PathVariable String tablaAfectada) {
        List<AuditoriaResponse> auditorias = auditoriaService.getAuditoriasByTabla(tablaAfectada);
        return ResponseEntity.ok(auditorias);
    }

    /**
     * GET /api/auditorias/accion/{accion}
     * Auditorías de una acción específica
     */
    @GetMapping("/accion/{accion}")
    public ResponseEntity<List<AuditoriaResponse>> getAuditoriasByAccion(@PathVariable String accion) {
        List<AuditoriaResponse> auditorias = auditoriaService.getAuditoriasByAccion(accion);
        return ResponseEntity.ok(auditorias);
    }

    /**
     * GET /api/auditorias/fechas?inicio=...&fin=...
     * Auditorías entre fechas
     */
    @GetMapping("/fechas")
    public ResponseEntity<List<AuditoriaResponse>> getAuditoriasByFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        List<AuditoriaResponse> auditorias = auditoriaService.getAuditoriasByFechas(inicio, fin);
        return ResponseEntity.ok(auditorias);
    }

    /**
     * GET /api/auditorias/ultimas
     * Últimas 100 auditorías
     */
    @GetMapping("/ultimas")
    public ResponseEntity<List<AuditoriaResponse>> getUltimasAuditorias() {
        List<AuditoriaResponse> auditorias = auditoriaService.getUltimasAuditorias();
        return ResponseEntity.ok(auditorias);
    }

    /**
     * GET /api/auditorias/del-dia
     * Auditorías del día actual
     */
    @GetMapping("/del-dia")
    public ResponseEntity<List<AuditoriaResponse>> getAuditoriasDelDia() {
        List<AuditoriaResponse> auditorias = auditoriaService.getAuditoriasDelDia();
        return ResponseEntity.ok(auditorias);
    }
}