package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.DashboardResponse;
import com.mitienda.ecommerce.dto.VentasSemanalResponse;
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

    private final UsuarioActualService usuarioActualService;

    public DashboardService(VentaRepository ventaRepository,
                            ProductoRepository productoRepository,
                            InventarioRepository inventarioRepository,
                            ClienteRepository clienteRepository,
                            AlertaInventarioRepository alertaInventarioRepository,
                            DetalleVentaRepository detalleVentaRepository,
                            UsuarioActualService usuarioActualService) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository;
        this.clienteRepository = clienteRepository;
        this.alertaInventarioRepository = alertaInventarioRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.usuarioActualService = usuarioActualService;
    }

    /** Estadísticas del panel de inicio; requiere transacción porque agrupa por relaciones perezosas. */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardStats() {
        DashboardResponse dashboard = new DashboardResponse();

        // Una consulta por tema: cada ida y vuelta a la base pesa en producción.
        Object[] inventario = inventarioRepository.resumenInventario().get(0);
        dashboard.setVentasStats(getVentasStats());
        dashboard.setProductosStats(getProductosStats(inventario));
        dashboard.setInventarioStats(getInventarioStats(inventario));
        dashboard.setClientesStats(getClientesStats());
        dashboard.setPagosStats(getPagosStats());
        dashboard.setProductosMasVendidos(getProductosMasVendidos(10));
        dashboard.setVentasUltimosDias(getVentasUltimosDias(7));
        dashboard.setVentasPorEntregar(ventaRepository.countVentasPorEntregar());

        return dashboard;
    }

    private DashboardResponse.VentasStats getVentasStats() {
        LocalDateTime hoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(23, 59, 59);
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicioAño = LocalDate.now().withDayOfYear(1).atStartOfDay();

        // El conteo usa los mismos estados que el monto (COMPLETADA).
        Object[] fila = ventaRepository.resumenVentasCompletadas(hoy, finHoy, inicioMes, inicioAño).get(0);
        Long totalVentasHoy = aLong(fila[0]);
        BigDecimal montoVentasHoy = aDecimal(fila[1]);
        Long totalVentasMes = aLong(fila[2]);
        BigDecimal montoVentasMes = aDecimal(fila[3]);
        Long totalVentasAño = aLong(fila[4]);
        BigDecimal montoVentasAño = aDecimal(fila[5]);

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

    private DashboardResponse.ProductosStats getProductosStats(Object[] inventario) {
        Object[] conteo = productoRepository.resumenConteo().get(0);
        return new DashboardResponse.ProductosStats(
                aLong(conteo[0]), aLong(conteo[1]),
                aLong(inventario[0]), aLong(inventario[1])
        );
    }

    private DashboardResponse.InventarioStats getInventarioStats(Object[] inventario) {
        Long alertasInventario = alertaInventarioRepository.countByEstado(EstadoAlerta.PENDIENTE);

        // Usa precioCompra: para EMPLEADO (o sin sesión) queda en null.
        BigDecimal valorTotal = usuarioActualService.esAdmin() ? aDecimal(inventario[2]) : null;

        Integer ajustesDelMes = 0;

        return new DashboardResponse.InventarioStats(
                alertasInventario, valorTotal, ajustesDelMes
        );
    }

    private DashboardResponse.ClientesStats getClientesStats() {
        LocalDateTime hoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(23, 59, 59);
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        Object[] fila = clienteRepository.resumenClientes(hoy, finHoy, inicioMes).get(0);
        return new DashboardResponse.ClientesStats(
                aLong(fila[0]), aLong(fila[2]),
                aLong(fila[3]), aLong(fila[1])
        );
    }

    private static Long aLong(Object valor) {
        return valor == null ? 0L : ((Number) valor).longValue();
    }

    private static BigDecimal aDecimal(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }

    /** Por cobrar: ventas con saldo pendiente. 'Cuotas' es un nombre histórico y no hay fecha de vencimiento. */
    private DashboardResponse.PagosStats getPagosStats() {
        Object[] fila = ventaRepository.resumenPorCobrar().get(0);
        return new DashboardResponse.PagosStats(aLong(fila[0]), aDecimal(fila[1]));
    }

    /** Productos más vendidos de toda la historia, solo ventas COMPLETADA. */
    private List<DashboardResponse.ProductoMasVendidoDTO> getProductosMasVendidos(int limite) {
        // Fecha anterior a cualquier venta: equivale a no tener límite inferior.
        return getMasVendidosEntre(LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.now().plusDays(1), limite);
    }

    /** Productos más vendidos en un rango (desde inclusive, hasta exclusive); lo usa el aviso de cierre del día. */
    public List<DashboardResponse.ProductoMasVendidoDTO> getMasVendidosEntre(
            LocalDateTime desde, LocalDateTime hasta, int limite) {
        Pageable tope = PageRequest.of(0, limite);

        return detalleVentaRepository.findMasVendidosEntre(desde, hasta, tope)
                .stream()
                .map(fila -> new DashboardResponse.ProductoMasVendidoDTO(
                        (Long) fila[0],
                        (String) fila[1],
                        (String) fila[2],
                        // SUM de Integer da Long en JPQL; SUM de BigDecimal da BigDecimal.
                        ((Number) fila[3]).longValue(),
                        (BigDecimal) fila[4]
                ))
                .collect(Collectors.toList());
    }

    /** Lo vendido hoy, de 00:00 a 00:00 de mañana (exclusive), para el aviso de cierre. */
    public List<DashboardResponse.ProductoMasVendidoDTO> getMasVendidosHoy(int limite) {
        LocalDateTime inicioDelDia = LocalDate.now().atStartOfDay();
        return getMasVendidosEntre(inicioDelDia, inicioDelDia.plusDays(1), limite);
    }

    /** Ventas completadas de la semana calendario (lunes a domingo) de la fecha dada, o de la actual. */
    @Transactional(readOnly = true)
    public VentasSemanalResponse getVentasSemanal(LocalDate fecha) {
        LocalDate referencia = fecha != null ? fecha : LocalDate.now();
        LocalDate lunes = referencia.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate domingo = lunes.plusDays(6);

        List<Venta> ventasSemana = ventaRepository
                .findByFechaVentaBetweenOrderByFechaVentaDesc(lunes.atStartOfDay(), domingo.plusDays(1).atStartOfDay())
                .stream().filter(v -> v.getEstado() == EstadoVenta.COMPLETADA).toList();

        List<DashboardResponse.VentaPorDiaDTO> porDia = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < 7; i++) {
            LocalDate dia = lunes.plusDays(i);
            List<Venta> ventasDia = ventasSemana.stream()
                    .filter(v -> v.getFechaVenta().toLocalDate().equals(dia))
                    .toList();
            porDia.add(new DashboardResponse.VentaPorDiaDTO(
                    dia.format(formatter),
                    (long) ventasDia.size(),
                    ventasDia.stream().map(Venta::getMontoTotal).reduce(BigDecimal.ZERO, BigDecimal::add)
            ));
        }

        BigDecimal montoTotal = ventasSemana.stream().map(Venta::getMontoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate hoy = LocalDate.now();

        return new VentasSemanalResponse(
                lunes.format(formatter),
                domingo.format(formatter),
                lunes.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                !hoy.isBefore(lunes) && !hoy.isAfter(domingo),
                (long) ventasSemana.size(),
                montoTotal,
                porDia
        );
    }

    private List<DashboardResponse.VentaPorDiaDTO> getVentasUltimosDias(int dias) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate primerDia = LocalDate.now().minusDays(dias - 1L);

        // Una sola consulta para todos los días (antes era una por día).
        List<Object[]> ventas = ventaRepository.findResumenVentasEntre(
                primerDia.atStartOfDay(), LocalDate.now().atTime(23, 59, 59));

        List<DashboardResponse.VentaPorDiaDTO> ventasPorDia = new ArrayList<>();
        for (int i = 0; i < dias; i++) {
            LocalDate fecha = primerDia.plusDays(i);
            long cantidadVentas = 0;
            BigDecimal montoTotal = BigDecimal.ZERO;
            for (Object[] v : ventas) {
                if (((LocalDateTime) v[0]).toLocalDate().equals(fecha) && v[1] == EstadoVenta.COMPLETADA) {
                    cantidadVentas++;
                    montoTotal = montoTotal.add((BigDecimal) v[2]);
                }
            }
            ventasPorDia.add(new DashboardResponse.VentaPorDiaDTO(
                    fecha.format(formatter),
                    cantidadVentas,
                    montoTotal
            ));
        }

        return ventasPorDia;
    }
}