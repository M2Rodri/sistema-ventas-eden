package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.DashboardResponse;
import com.mitienda.ecommerce.models.*;
import com.mitienda.ecommerce.repositories.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final VentaRepository ventaRepository;

    private final ProductoRepository productoRepository;

    private final InventarioRepository inventarioRepository;

    private final ClienteRepository clienteRepository;

    private final AlertaInventarioRepository alertaInventarioRepository;

    private final DetalleVentaRepository detalleVentaRepository;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public DashboardService(VentaRepository ventaRepository,
                            ProductoRepository productoRepository,
                            InventarioRepository inventarioRepository,
                            ClienteRepository clienteRepository,
                            AlertaInventarioRepository alertaInventarioRepository,
                            DetalleVentaRepository detalleVentaRepository) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository;
        this.clienteRepository = clienteRepository;
        this.alertaInventarioRepository = alertaInventarioRepository;
        this.detalleVentaRepository = detalleVentaRepository;
    }


    /**
     * Estadísticas del panel de inicio.
     *
     * Requiere transacción: getProductosMasVendidos() agrupa por el Producto de
     * cada DetalleVenta, que es una relación perezosa. Con
     * spring.jpa.open-in-view=false no hay sesión abierta fuera de la
     * transacción, y sin esta anotación el endpoint fallaba entero con
     * LazyInitializationException.
     */
    @Transactional(readOnly = true)
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
                .map(inv -> inv.getProducto().getCostoReferencial()
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

    /**
     * Productos mas vendidos de toda la historia del negocio.
     *
     * Antes este metodo traia la tabla detalle_venta completa con findAll() y
     * agrupaba en memoria, sin filtrar por estado: las ventas canceladas
     * seguian sumando al ranking. Ahora agrupa la base de datos y solo cuenta
     * las ventas COMPLETADAS.
     */
    private List<DashboardResponse.ProductoMasVendidoDTO> getProductosMasVendidos(int limite) {
        // Desde una fecha bien anterior a cualquier venta posible: equivale a
        // "sin limite inferior", sin necesitar una segunda consulta.
        return getMasVendidosEntre(LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.now().plusDays(1), limite);
    }

    /**
     * Productos mas vendidos dentro de un rango.
     *
     * Se expone aparte porque el aviso de cierre del dia necesita responder
     * "que se vendio hoy", y el metodo anterior solo sabia de toda la historia.
     *
     * @param desde  inclusive
     * @param hasta  exclusive
     */
    public List<DashboardResponse.ProductoMasVendidoDTO> getMasVendidosEntre(
            LocalDateTime desde, LocalDateTime hasta, int limite) {

        Pageable tope = PageRequest.of(0, limite);

        return detalleVentaRepository.findMasVendidosEntre(desde, hasta, tope)
                .stream()
                .map(fila -> new DashboardResponse.ProductoMasVendidoDTO(
                        (Long) fila[0],
                        (String) fila[1],
                        (String) fila[2],
                        // SUM sobre un Integer devuelve Long en JPQL; SUM sobre
                        // un BigDecimal devuelve BigDecimal.
                        ((Number) fila[3]).longValue(),
                        (BigDecimal) fila[4]
                ))
                .collect(Collectors.toList());
    }

    /**
     * Lo que se vendio hoy, para el aviso de cierre del dia.
     *
     * El dia va de las 00:00 de hoy a las 00:00 de manana, sin incluir ese
     * limite: asi una venta registrada a las 23:59:59 entra en el dia correcto.
     */
    public List<DashboardResponse.ProductoMasVendidoDTO> getMasVendidosHoy(int limite) {
        LocalDateTime inicioDelDia = LocalDate.now().atStartOfDay();
        return getMasVendidosEntre(inicioDelDia, inicioDelDia.plusDays(1), limite);
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