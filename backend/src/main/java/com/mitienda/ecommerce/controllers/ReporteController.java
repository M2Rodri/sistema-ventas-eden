package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.ReporteClientesResponse;
import com.mitienda.ecommerce.dto.ReporteCuentasPorCobrarResponse;
import com.mitienda.ecommerce.dto.ReporteFinancieroResponse;
import com.mitienda.ecommerce.dto.ReporteProductosResponse;
import com.mitienda.ecommerce.dto.ReporteVentasResponse;
import com.mitienda.ecommerce.services.ReporteService;
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

    private final ReporteService reporteService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }


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
     * Reporte de inventario valorizado. Es costo de referencia por producto
     * y su valor total, de punta a punta: solo ADMIN, igual que
     * /financiero. Sobrescribe el @PreAuthorize de la clase.
     */
    @GetMapping("/inventario-valorizado")
    @PreAuthorize("hasRole('ADMIN')")
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

    /**
     * GET /api/reportes/cuentas-por-cobrar
     * Ventas con saldo pendiente de cobro, ordenadas por antigüedad
     */
    @GetMapping("/cuentas-por-cobrar")
    public ResponseEntity<ReporteCuentasPorCobrarResponse> getReporteCuentasPorCobrar() {
        ReporteCuentasPorCobrarResponse reporte = reporteService.getReporteCuentasPorCobrar();
        return ResponseEntity.ok(reporte);
    }

    /**
     * GET /api/reportes/financiero?inicio=...&fin=...
     * Reporte financiero: ganancia real de lo vendido, e ingresos/egresos
     * del periodo. Solo ADMIN: el costo por producto que arma la ganancia
     * no debe llegarle a un EMPLEADO. Sobrescribe el @PreAuthorize de la
     * clase (que permite ADMIN y EMPLEADO) con uno mas estricto.
     */
    @GetMapping("/financiero")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReporteFinancieroResponse> getReporteFinanciero(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        ReporteFinancieroResponse reporte = reporteService.getReporteFinanciero(inicio, fin);
        return ResponseEntity.ok(reporte);
    }
}