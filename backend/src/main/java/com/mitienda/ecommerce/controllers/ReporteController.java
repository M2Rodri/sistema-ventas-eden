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

/** Controlador REST para generación de reportes */
@RestController
@RequestMapping("/api/v1/reportes")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasRole('ADMIN')")
public class ReporteController {
    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /** Reporte de ventas por período */
    @GetMapping("/ventas")
    public ResponseEntity<ReporteVentasResponse> getReporteVentas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        ReporteVentasResponse reporte = reporteService.getReporteVentas(inicio, fin);
        return ResponseEntity.ok(reporte);
    }

    /** Reporte de productos más vendidos */
    @GetMapping("/productos-mas-vendidos")
    public ResponseEntity<ReporteProductosResponse> getReporteProductosMasVendidos(
            @RequestParam(defaultValue = "10") Integer limite) {
        ReporteProductosResponse reporte = reporteService.getReporteProductosMasVendidos(limite);
        return ResponseEntity.ok(reporte);
    }

    /** Reporte de clientes frecuentes */
    @GetMapping("/clientes-frecuentes")
    public ResponseEntity<ReporteClientesResponse> getReporteClientesFrecuentes(
            @RequestParam(defaultValue = "10") Integer limite) {
        ReporteClientesResponse reporte = reporteService.getReporteClientesFrecuentes(limite);
        return ResponseEntity.ok(reporte);
    }

    /** Reporte de inventario valorizado. */
    @GetMapping("/inventario-valorizado")
    public ResponseEntity<Map<String, Object>> getReporteInventarioValorizado() {
        Map<String, Object> reporte = reporteService.getReporteInventarioValorizado();
        return ResponseEntity.ok(reporte);
    }

    /** Reporte de ventas por categoría */
    @GetMapping("/ventas-por-categoria")
    public ResponseEntity<List<Map<String, Object>>> getReporteVentasPorCategoria(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        List<Map<String, Object>> reporte = reporteService.getReporteVentasPorCategoria(inicio, fin);
        return ResponseEntity.ok(reporte);
    }

    /** Cada producto vendido en cada venta del período (sin las canceladas) */
    @GetMapping("/ventas-por-producto")
    public ResponseEntity<List<Map<String, Object>>> getReporteVentasPorProducto(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(reporteService.getReporteVentasPorProducto(inicio, fin));
    }

    /** Reporte de ventas por método de pago */
    @GetMapping("/ventas-por-metodo-pago")
    public ResponseEntity<List<Map<String, Object>>> getReporteVentasPorMetodoPago(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        List<Map<String, Object>> reporte = reporteService.getReporteVentasPorMetodoPago(inicio, fin);
        return ResponseEntity.ok(reporte);
    }

    /** Ventas con saldo pendiente de cobro, ordenadas por antigüedad */
    @GetMapping("/cuentas-por-cobrar")
    public ResponseEntity<ReporteCuentasPorCobrarResponse> getReporteCuentasPorCobrar() {
        ReporteCuentasPorCobrarResponse reporte = reporteService.getReporteCuentasPorCobrar();
        return ResponseEntity.ok(reporte);
    }

    /** Reporte financiero: ganancia real de lo vendido e ingresos y egresos del período. */
    @GetMapping("/financiero")
    public ResponseEntity<ReporteFinancieroResponse> getReporteFinanciero(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        ReporteFinancieroResponse reporte = reporteService.getReporteFinanciero(inicio, fin);
        return ResponseEntity.ok(reporte);
    }

    /** Detalle de ganancia por línea de venta para el módulo móvil de ganancias. */
    @GetMapping("/ganancias-por-producto")
    public ResponseEntity<List<Map<String, Object>>> getGananciasPorProducto(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(reporteService.getGananciasPorProducto(inicio, fin));
    }

    @GetMapping("/fecha-inicio")
    public ResponseEntity<Map<String, Object>> getFechaInicioRegistros() {
        var fecha = reporteService.getFechaInicioRegistros();
        return ResponseEntity.ok(fecha == null
                ? Map.of()
                : Map.of("fecha", fecha));
    }
}
