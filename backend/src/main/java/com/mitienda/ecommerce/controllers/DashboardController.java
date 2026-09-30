package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.DashboardResponse;
import com.mitienda.ecommerce.dto.VentasSemanalResponse;
import com.mitienda.ecommerce.services.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controlador REST para el dashboard con estadísticas
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }


    /**
     * GET /api/dashboard/estadisticas
     * Obtener todas las estadísticas del dashboard
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<DashboardResponse> getDashboardStats() {
        DashboardResponse dashboard = dashboardService.getDashboardStats();
        return ResponseEntity.ok(dashboard);
    }

    /**
     * GET /api/dashboard/ventas-semanal?fecha=2026-09-28
     * Ventas de la semana calendario (lunes a domingo) que contiene a la fecha;
     * sin parámetro, la semana en curso.
     */
    @GetMapping("/ventas-semanal")
    public ResponseEntity<VentasSemanalResponse> getVentasSemanal(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(dashboardService.getVentasSemanal(fecha));
    }
}