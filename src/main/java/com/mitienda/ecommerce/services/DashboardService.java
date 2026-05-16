package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.DashboardResponse;
import com.mitienda.ecommerce.models.*;
import com.mitienda.ecommerce.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AlertaInventarioRepository alertaInventarioRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    public DashboardResponse getDashboardStats() {
        DashboardResponse dashboard = new DashboardResponse();

        dashboard.setVentasStats(getVentasStats());
        dashboard.setProductosStats(getProductosStats());
        dashboard.setInventarioStats(getInventarioStats());
        dashboard.setClientesStats(getClientesStats());
        dashboard.setPagosStats(getPagosStats());
        dashboard.setAlertasStats(getAlertasStats());
        dashboard.setProductosMasVendidos(getProductosMasVendidos(10));
        dashboard.setVentasUltimosDias(getVentasUltimosDias(7));

        return dashboard;
    }

    private DashboardResponse.VentasStats getVentasStats() {
        LocalDateTime hoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(23, 59, 59);
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicioAño = LocalDate.now().withDayOfYear(1).atStartOfDay();

        List<Venta> ventasHoy = ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(hoy, finHoy);
        Long totalVentasHoy = (long) ventasHoy.size();
        BigDecimal montoVentasHoy = ventasHoy.stream()
                .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA)
                .map(Venta::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Venta> ventasMes = ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(inicioMes, LocalDateTime.now());
        Long totalVentasMes = (long) ventasMes.size();
        BigDecimal montoVentasMes = ventasMes.stream()
                .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA)
                .map(Venta::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Venta> ventasAño = ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(inicioAño, LocalDateTime.now());
        Long totalVentasAño = (long) ventasAño.size();
        BigDecimal montoVentasAño = ventasAño.stream()
                .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA)
                .map(Venta::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal promedioVentaDiaria = totalVentasMes > 0
                ? montoVentasMes.divide(BigDecimal.valueOf(LocalDate.now().getDayOfMonth()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new DashboardResponse.VentasStats(
                totalVentasHoy, montoVentasHoy,
                totalVentasMes, montoVentasMes,
                totalVentasAño, montoVentasAño,
                promedioVentaDiaria
        );
    }

    private DashboardResponse.ProductosStats getProductosStats() {
        Long totalProductos = productoRepository.count();
        Long productosActivos = productoRepository.countByActivo(true);
        Long productosSinStock = (long) inventarioRepository.findProductosSinStock().size();
        Long productosBajoStock = inventarioRepository.countProductosConStockBajo();

        return new DashboardResponse.ProductosStats(
                totalProductos, productosActivos,
                productosSinStock, productosBajoStock
        );
    }

    private DashboardResponse.InventarioStats getInventarioStats() {
        Long alertasInventario = alertaInventarioRepository.countByEstado(EstadoAlerta.PENDIENTE);

        List<Inventario> inventarios = inventarioRepository.findAll();
        BigDecimal valorTotal = inventarios.stream()
                .map(inv -> inv.getProducto().getPrecioUnitario()
                        .multiply(BigDecimal.valueOf(inv.getCantidadDisponible())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer ajustesDelMes = 0;

        return new DashboardResponse.InventarioStats(
                alertasInventario, valorTotal, ajustesDelMes
        );
    }

    private DashboardResponse.ClientesStats getClientesStats() {
        Long totalClientes = clienteRepository.count();
        Long clientesActivos = clienteRepository.countByActivo(true);

        LocalDateTime hoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(23, 59, 59);
        Long clientesNuevosHoy = clienteRepository.findAll().stream()
                .filter(c -> c.getFechaRegistro().isAfter(hoy) && c.getFechaRegistro().isBefore(finHoy))
                .count();

        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Long clientesNuevosMes = clienteRepository.findAll().stream()
                .filter(c -> c.getFechaRegistro().isAfter(inicioMes))
                .count();

        return new DashboardResponse.ClientesStats(
                totalClientes, clientesNuevosHoy,
                clientesNuevosMes, clientesActivos
        );
    }

    private DashboardResponse.PagosStats getPagosStats() {
        Long cuotasPendientes = 0L;
        Long cuotasVencidas = 0L;
        BigDecimal montoCuotasPendientes = BigDecimal.ZERO;
        BigDecimal montoCuotasVencidas = BigDecimal.ZERO;

        return new DashboardResponse.PagosStats(
                cuotasPendientes, cuotasVencidas,
                montoCuotasPendientes, montoCuotasVencidas
        );
    }

    private DashboardResponse.AlertasStats getAlertasStats() {
        Long inventarioBajo = inventarioRepository.countProductosConStockBajo();
        Long cuotasVencidas = 0L;
        Long reseniasPendientes = 0L;

        Long totalAlertas = inventarioBajo + cuotasVencidas + reseniasPendientes;

        return new DashboardResponse.AlertasStats(
                0L, inventarioBajo,
                cuotasVencidas, reseniasPendientes,
                totalAlertas
        );
    }

    private List<DashboardResponse.ProductoMasVendidoDTO> getProductosMasVendidos(int limite) {
        List<DetalleVenta> detalles = detalleVentaRepository.findAll();

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

        return productosCantidad.entrySet().stream()
                .sorted(Map.Entry.<Producto, Long>comparingByValue().reversed())
                .limit(limite)
                .map(entry -> new DashboardResponse.ProductoMasVendidoDTO(
                        entry.getKey().getId(),
                        entry.getKey().getNombre(),
                        entry.getKey().getSku(),
                        entry.getValue(),
                        productosMontos.get(entry.getKey())
                ))
                .collect(Collectors.toList());
    }

    private List<DashboardResponse.VentaPorDiaDTO> getVentasUltimosDias(int dias) {
        List<DashboardResponse.VentaPorDiaDTO> ventasPorDia = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = dias - 1; i >= 0; i--) {
            LocalDate fecha = LocalDate.now().minusDays(i);
            LocalDateTime inicioFecha = fecha.atStartOfDay();
            LocalDateTime finFecha = fecha.atTime(23, 59, 59);

            List<Venta> ventasDia = ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(inicioFecha, finFecha);
            Long cantidadVentas = (long) ventasDia.size();
            BigDecimal montoTotal = ventasDia.stream()
                    .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA)
                    .map(Venta::getMontoTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            ventasPorDia.add(new DashboardResponse.VentaPorDiaDTO(
                    fecha.format(formatter),
                    cantidadVentas,
                    montoTotal
            ));
        }

        return ventasPorDia;
    }
}