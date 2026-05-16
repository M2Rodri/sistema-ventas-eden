package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.ReporteClientesResponse;
import com.mitienda.ecommerce.dto.ReporteProductosResponse;
import com.mitienda.ecommerce.dto.ReporteVentasResponse;
import com.mitienda.ecommerce.services.ReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para generación de reportes
 */
@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    /**
     * GET /api/reportes/ventas?inicio=...&fin=...
     * Reporte de ventas por período
     */
    @GetMapping("/ventas")
    public ResponseEntity<ReporteVentasResponse> getReporteVentas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        ReporteVentasResponse reporte = reporteService.getReporteVentas(inicio, fin);
        return ResponseEntity.ok(reporte);
    }

    /**
     * GET /api/reportes/productos-mas-vendidos?limite=10
     * Reporte de productos más vendidos
     */
    @GetMapping("/productos-mas-vendidos")
    public ResponseEntity<ReporteProductosResponse> getReporteProductosMasVendidos(
            @RequestParam(defaultValue = "10") Integer limite) {
        ReporteProductosResponse reporte = reporteService.getReporteProductosMasVendidos(limite);
        return ResponseEntity.ok(reporte);
    }

    /**
     * GET /api/reportes/clientes-frecuentes?limite=10
     * Reporte de clientes frecuentes
     */
    @GetMapping("/clientes-frecuentes")
    public ResponseEntity<ReporteClientesResponse> getReporteClientesFrecuentes(
            @RequestParam(defaultValue = "10") Integer limite) {
        ReporteClientesResponse reporte = reporteService.getReporteClientesFrecuentes(limite);
        return ResponseEntity.ok(reporte);
    }

    /**
     * GET /api/reportes/inventario-valorizado
     * Reporte de inventario valorizado
     */
    @GetMapping("/inventario-valorizado")
    public ResponseEntity<Map<String, Object>> getReporteInventarioValorizado() {
        Map<String, Object> reporte = reporteService.getReporteInventarioValorizado();
        return ResponseEntity.ok(reporte);
    }

    /**
     * GET /api/reportes/ventas-por-categoria?inicio=...&fin=...
     * Reporte de ventas por categoría
     */
    @GetMapping("/ventas-por-categoria")
    public ResponseEntity<List<Map<String, Object>>> getReporteVentasPorCategoria(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        List<Map<String, Object>> reporte = reporteService.getReporteVentasPorCategoria(inicio, fin);
        return ResponseEntity.ok(reporte);
    }

    /**
     * GET /api/reportes/ventas-por-metodo-pago?inicio=...&fin=...
     * Reporte de ventas por método de pago
     */
    @GetMapping("/ventas-por-metodo-pago")
    public ResponseEntity<List<Map<String, Object>>> getReporteVentasPorMetodoPago(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        List<Map<String, Object>> reporte = reporteService.getReporteVentasPorMetodoPago(inicio, fin);
        return ResponseEntity.ok(reporte);
    }
}