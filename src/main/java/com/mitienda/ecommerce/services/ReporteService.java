package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ReporteClientesResponse;
import com.mitienda.ecommerce.dto.ReporteProductosResponse;
import com.mitienda.ecommerce.dto.ReporteVentasResponse;
import com.mitienda.ecommerce.models.Cliente;
import com.mitienda.ecommerce.models.DetalleVenta;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.repositories.DetalleVentaRepository;
import com.mitienda.ecommerce.repositories.InventarioRepository;
import com.mitienda.ecommerce.repositories.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para generación de reportes
 */
@Service
public class ReporteService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private InventarioRepository inventarioRepository;

    /**
     * Reporte de ventas por período
     */
    public ReporteVentasResponse getReporteVentas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Venta> ventas = ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(fechaInicio, fechaFin);

        Long totalVentas = (long) ventas.size();
        BigDecimal montoTotalVentas = ventas.stream()
                .map(Venta::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ticketPromedio = totalVentas > 0 
                ? montoTotalVentas.divide(BigDecimal.valueOf(totalVentas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<ReporteVentasResponse.VentaDetalleDTO> ventasDetalle = ventas.stream()
                .map(venta -> new ReporteVentasResponse.VentaDetalleDTO(
                        venta.getId(),
                        venta.getFechaVenta(),
                        venta.getNombreClienteCompleto(),  // ✅ Corregido: maneja cliente nulo
                        venta.getMontoTotal(),
                        venta.getMetodoPago().name(),
                        venta.getEstado().name()
                ))
                .collect(Collectors.toList());

        return new ReporteVentasResponse(
                fechaInicio, fechaFin,
                totalVentas, montoTotalVentas,
                ticketPromedio, ventasDetalle
        );
    }

    /**
     * Reporte de productos más vendidos
     */
    public ReporteProductosResponse getReporteProductosMasVendidos(Integer limite) {
        List<DetalleVenta> detalles = detalleVentaRepository.findAll();

        // Agrupar por producto
        Map<Producto, Long> productosCantidad = detalles.stream()
                .collect(Collectors.groupingBy(
                        DetalleVenta::getProducto,
                        Collectors.summingLong(DetalleVenta::getCantidad)
                ));

        Map<Producto, BigDecimal> productosMontos = detalles.stream()
                .collect(Collectors.groupingBy(
                        DetalleVenta::getProducto,
                        Collectors.reducing(BigDecimal.ZERO, DetalleVenta::getSubtotal, BigDecimal::add)
                ));

        List<ReporteProductosResponse.ProductoReporteDTO> productos = productosCantidad.entrySet().stream()
                .sorted(Map.Entry.<Producto, Long>comparingByValue().reversed())
                .limit(limite)
                .map(entry -> {
                    Producto producto = entry.getKey();
                    Integer stockActual = inventarioRepository.findByProductoId(producto.getId())
                            .map(inv -> inv.getCantidadDisponible())
                            .orElse(0);

                    return new ReporteProductosResponse.ProductoReporteDTO(
                            producto.getId(),
                            producto.getNombre(),
                            producto.getSku(),
                            producto.getCategoria().getNombre(),
                            entry.getValue(),
                            productosMontos.get(producto),
                            stockActual
                    );
                })
                .collect(Collectors.toList());

        return new ReporteProductosResponse(limite, productos);
    }

    /**
     * Reporte de clientes frecuentes
     */
    public ReporteClientesResponse getReporteClientesFrecuentes(Integer limite) {
        List<Venta> ventas = ventaRepository.findAll();

        // Filtrar ventas con cliente != null antes de agrupar
        List<Venta> ventasConCliente = ventas.stream()
                .filter(venta -> venta.getCliente() != null)
                .collect(Collectors.toList());

        // Agrupar por cliente (solo ventas con cliente registrado)
        Map<Cliente, Long> clientesCantidad = ventasConCliente.stream()
                .collect(Collectors.groupingBy(
                        Venta::getCliente,
                        Collectors.counting()
                ));

        Map<Cliente, BigDecimal> clientesMontos = ventasConCliente.stream()
                .collect(Collectors.groupingBy(
                        Venta::getCliente,
                        Collectors.reducing(BigDecimal.ZERO, Venta::getMontoTotal, BigDecimal::add)
                ));

        List<ReporteClientesResponse.ClienteReporteDTO> clientes = clientesCantidad.entrySet().stream()
                .sorted(Map.Entry.<Cliente, Long>comparingByValue().reversed())
                .limit(limite)
                .map(entry -> {
                    Cliente cliente = entry.getKey();
                    Long cantidadCompras = entry.getValue();
                    BigDecimal montoTotal = clientesMontos.get(cliente);
                    BigDecimal promedioCompra = cantidadCompras > 0
                            ? montoTotal.divide(BigDecimal.valueOf(cantidadCompras), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return new ReporteClientesResponse.ClienteReporteDTO(
                            cliente.getId(),
                            cliente.getNombreCompleto(),
                            cliente.getCelular(),
                            cliente.getCorreo(),
                            cantidadCompras,
                            montoTotal,
                            promedioCompra
                    );
                })
                .collect(Collectors.toList());

        return new ReporteClientesResponse(limite, clientes);
    }

    /**
     * Reporte de inventario valorizado
     */
    public Map<String, Object> getReporteInventarioValorizado() {
        List<Map<String, Object>> inventarios = inventarioRepository.findAll().stream()
                .map(inv -> {
                    Map<String, Object> item = Map.of(
                            "idProducto", inv.getProducto().getId(),
                            "nombreProducto", inv.getProducto().getNombre(),
                            "skuProducto", inv.getProducto().getSku(),
                            "cantidadDisponible", inv.getCantidadDisponible(),
                            "precioUnitario", inv.getProducto().getPrecioUnitario(),
                            "valorTotal", inv.getProducto().getPrecioUnitario()
                                    .multiply(BigDecimal.valueOf(inv.getCantidadDisponible())),
                            "ubicacion", inv.getUbicacion()
                    );
                    return item;
                })
                .collect(Collectors.toList());

        BigDecimal valorTotalInventario = inventarioRepository.findAll().stream()
                .map(inv -> inv.getProducto().getPrecioUnitario()
                        .multiply(BigDecimal.valueOf(inv.getCantidadDisponible())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "inventarios", inventarios,
                "valorTotal", valorTotalInventario,
                "totalProductos", inventarios.size()
        );
    }

    /**
     * Reporte de ventas por categoría
     */
    public List<Map<String, Object>> getReporteVentasPorCategoria(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Venta> ventas = ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(fechaInicio, fechaFin);

        Map<String, Long> ventasPorCategoria = new java.util.HashMap<>();
        Map<String, BigDecimal> montosPorCategoria = new java.util.HashMap<>();

        for (Venta venta : ventas) {
            for (DetalleVenta detalle : venta.getDetalles()) {
                String categoria = detalle.getProducto().getCategoria().getNombre();
                
                ventasPorCategoria.merge(categoria, (long) detalle.getCantidad(), Long::sum);
                montosPorCategoria.merge(categoria, detalle.getSubtotal(), BigDecimal::add);
            }
        }

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (String categoria : ventasPorCategoria.keySet()) {
            resultado.add(Map.of(
                    "categoria", categoria,
                    "cantidadVendida", ventasPorCategoria.get(categoria),
                    "montoTotal", montosPorCategoria.get(categoria)
            ));
        }

        return resultado;
    }

    /**
     * Reporte de ventas por método de pago
     */
    public List<Map<String, Object>> getReporteVentasPorMetodoPago(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Venta> ventas = ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(fechaInicio, fechaFin);

        Map<String, Long> ventasPorMetodo = ventas.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getMetodoPago().name(),
                        Collectors.counting()
                ));

        Map<String, BigDecimal> montosPorMetodo = ventas.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getMetodoPago().name(),
                        Collectors.reducing(BigDecimal.ZERO, Venta::getMontoTotal, BigDecimal::add)
                ));

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (String metodo : ventasPorMetodo.keySet()) {
            resultado.add(Map.of(
                    "metodoPago", metodo,
                    "cantidadVentas", ventasPorMetodo.get(metodo),
                    "montoTotal", montosPorMetodo.get(metodo)
            ));
        }

        return resultado;
    }
}