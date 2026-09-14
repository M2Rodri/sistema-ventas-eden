package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.DashboardResponse;
import com.mitienda.ecommerce.services.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}