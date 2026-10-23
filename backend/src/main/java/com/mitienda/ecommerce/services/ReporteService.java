package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ReporteClientesResponse;
import com.mitienda.ecommerce.dto.ReporteCuentasPorCobrarResponse;
import com.mitienda.ecommerce.dto.ReporteFinancieroResponse;
import com.mitienda.ecommerce.dto.ReporteProductosResponse;
import com.mitienda.ecommerce.dto.ReporteVentasResponse;
import com.mitienda.ecommerce.models.Cliente;
import com.mitienda.ecommerce.models.Compra;
import com.mitienda.ecommerce.models.DetalleVenta;
import com.mitienda.ecommerce.models.EstadoCompra;
import com.mitienda.ecommerce.models.EstadoPago;
import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.Pago;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.repositories.CompraRepository;
import com.mitienda.ecommerce.repositories.DetalleVentaRepository;
import com.mitienda.ecommerce.repositories.InventarioRepository;
import com.mitienda.ecommerce.repositories.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
// Lectura en transacción: con open-in-view=false no hay sesión de Hibernate fuera de ella.
@Transactional(readOnly = true)
public class ReporteService {
    private final VentaRepository ventaRepository;

    private final DetalleVentaRepository detalleVentaRepository;

    private final InventarioRepository inventarioRepository;

    private final CompraRepository compraRepository;

    public ReporteService(VentaRepository ventaRepository,
                          DetalleVentaRepository detalleVentaRepository,
                          InventarioRepository inventarioRepository,
                          CompraRepository compraRepository) {
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.inventarioRepository = inventarioRepository;
        this.compraRepository = compraRepository;
    }

    /** Métodos de pago de una venta en un texto; viven en 'pagos', no en 'ventas'. */
    private String resumirMetodoPago(Venta venta) {
        if (venta.getPagos() == null || venta.getPagos().isEmpty()) {
            return "SIN PAGOS";
        }
        List<String> metodos = venta.getPagos().stream()
                .filter(p -> p.getMetodoPago() != null)
                .map(p -> p.getMetodoPago().name())
                .distinct()
                .collect(Collectors.toList());

        if (metodos.isEmpty()) {
            return "SIN PAGOS";
        }
        return metodos.size() == 1 ? metodos.get(0) : "VARIOS";
    }

    /** Ganancia real: ventas COMPLETADA, (precio - costo guardado en detalle_venta) x cantidad. */
    private BigDecimal calcularGananciaVentas(List<Venta> ventas) {
        return ventas.stream()
                .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA)
                .flatMap(v -> v.getDetalles().stream())
                .filter(d -> d.getCostoUnitario() != null)
                .map(d -> d.getPrecioUnitario()
                        .subtract(d.getCostoUnitario())
                        .multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Costo total de una venta; null si está CANCELADA o alguna línea no tiene costo. */
    private BigDecimal costoTotalDeUnaVenta(Venta venta) {
        if (venta.getEstado() == EstadoVenta.CANCELADA || venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            return null;
        }
        BigDecimal costoAcumulado = BigDecimal.ZERO;
        for (DetalleVenta d : venta.getDetalles()) {
            if (d.getCostoUnitario() == null) {
                return null;
            }
            costoAcumulado = costoAcumulado.add(d.getCostoUnitario().multiply(BigDecimal.valueOf(d.getCantidad())));
        }
        return costoAcumulado;
    }

    /** Ganancia de una venta (puede ser pérdida); null si está CANCELADA o falta algún costo. */
    private BigDecimal gananciaDeUnaVenta(Venta venta) {
        BigDecimal costoTotal = costoTotalDeUnaVenta(venta);
        if (costoTotal == null) {
            return null;
        }
        return venta.getMontoTotal().subtract(costoTotal);
    }

    /** Ganancia del reporte de ventas: suma las positivas de las ventas no canceladas. */
    private BigDecimal calcularGananciaReporteVentas(List<Venta> ventas) {
        return ventas.stream()
                .filter(v -> v.getEstado() != EstadoVenta.CANCELADA)
                .map(this::gananciaDeUnaVenta)
                .filter(g -> g != null && g.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public ReporteVentasResponse getReporteVentas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        // Una venta CANCELADA devolvió el stock: no cuenta en ningún reporte.
        List<Venta> ventas = ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(fechaInicio, fechaFin)
                .stream()
                .filter(v -> v.getEstado() != EstadoVenta.CANCELADA)
                .collect(Collectors.toList());

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
                        venta.getNombreClienteCompleto(),
                        venta.getMontoTotal(),
                        resumirMetodoPago(venta),
                        venta.getEstado().name(),
                        costoTotalDeUnaVenta(venta),
                        gananciaDeUnaVenta(venta)
                ))
                .collect(Collectors.toList());

        return new ReporteVentasResponse(
                fechaInicio, fechaFin,
                totalVentas, montoTotalVentas,
                ticketPromedio, calcularGananciaReporteVentas(ventas), ventasDetalle
        );
    }

    public ReporteProductosResponse getReporteProductosMasVendidos(Integer limite) {
        List<DetalleVenta> detalles = detalleVentaRepository.findAll()
                .stream()
                .filter(d -> d.getVenta().getEstado() != EstadoVenta.CANCELADA)
                .collect(Collectors.toList());

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

    public ReporteClientesResponse getReporteClientesFrecuentes(Integer limite) {
        List<Venta> ventas = ventaRepository.findAll();

        // Solo ventas con cliente y no canceladas.
        List<Venta> ventasConCliente = ventas.stream()
                .filter(venta -> venta.getCliente() != null)
                .filter(venta -> venta.getEstado() != EstadoVenta.CANCELADA)
                .collect(Collectors.toList());

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
                            cliente.getTelefono(),
                            cliente.getEmail(),
                            cantidadCompras,
                            montoTotal,
                            promedioCompra
                    );
                })
                .collect(Collectors.toList());

        return new ReporteClientesResponse(limite, clientes);
    }

    public Map<String, Object> getReporteInventarioValorizado() {
        List<Map<String, Object>> inventarios = inventarioRepository.findAll().stream()
                .map(inv -> {
                    // Mapa mutable porque Map.of no admite null (producto sin precio de compra).
                    BigDecimal precioCompra = inv.getProducto().getPrecioCompra();
                    BigDecimal valorItem = precioCompra != null
                            ? precioCompra.multiply(BigDecimal.valueOf(inv.getCantidadDisponible()))
                            : null;
                    Map<String, Object> item = new java.util.HashMap<>();
                    item.put("idProducto", inv.getProducto().getId());
                    item.put("nombreProducto", inv.getProducto().getNombre());
                    item.put("skuProducto", inv.getProducto().getSku());
                    item.put("categoria", inv.getProducto().getCategoria() != null
                            ? inv.getProducto().getCategoria().getNombre() : "Sin categoría");
                    item.put("cantidadDisponible", inv.getCantidadDisponible());
                    item.put("precioCompra", precioCompra);
                    item.put("valorTotal", valorItem);
                    return item;
                })
                .collect(Collectors.toList());

        BigDecimal valorTotalInventario = inventarioRepository.findAll().stream()
                .filter(inv -> inv.getProducto().getPrecioCompra() != null)
                .map(inv -> inv.getProducto().getPrecioCompra()
                        .multiply(BigDecimal.valueOf(inv.getCantidadDisponible())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "inventarios", inventarios,
                "valorTotal", valorTotalInventario,
                "totalProductos", inventarios.size()
        );
    }

    public List<Map<String, Object>> getReporteVentasPorCategoria(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Venta> ventas = ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(fechaInicio, fechaFin)
                .stream()
                .filter(v -> v.getEstado() != EstadoVenta.CANCELADA)
                .collect(Collectors.toList());

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

    /** Una fila por producto vendido en cada venta del período, sin canceladas, de la más reciente a la más antigua. */
    public List<Map<String, Object>> getReporteVentasPorProducto(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Venta venta : ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(fechaInicio, fechaFin)) {
            if (venta.getEstado() == EstadoVenta.CANCELADA) {
                continue;
            }
            for (DetalleVenta detalle : venta.getDetalles()) {
                Producto producto = detalle.getProducto();
                filas.add(Map.of(
                        "idVenta", venta.getId(),
                        "fechaVenta", venta.getFechaVenta(),
                        "nombreCliente", venta.getNombreClienteCompleto(),
                        "idProducto", producto.getId(),
                        "nombreProducto", producto.getNombre(),
                        "skuProducto", producto.getSku(),
                        "categoria", producto.getCategoria() != null ? producto.getCategoria().getNombre() : "Sin categoría",
                        "cantidad", detalle.getCantidad(),
                        "precioUnitario", detalle.getPrecioUnitario(),
                        "subtotal", detalle.getSubtotal()
                ));
            }
        }
        return filas;
    }

    /** Cobros por método de pago, calculados sobre los pagos y no sobre las ventas (un cobro puede mezclar métodos). */
    public List<Map<String, Object>> getReporteVentasPorMetodoPago(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Venta> ventas = ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(fechaInicio, fechaFin);

        // Cancelar una venta no cambia su pago: se filtra para no sumar plata de ventas anuladas.
        List<Pago> pagos = ventas.stream()
                .filter(v -> v.getEstado() != EstadoVenta.CANCELADA)
                .flatMap(v -> v.getPagos().stream())
                .filter(p -> p.getMetodoPago() != null && p.getEstado() == EstadoPago.COMPLETADO)
                .collect(Collectors.toList());

        Map<String, Long> cantidadPorMetodo = pagos.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getMetodoPago().name(),
                        Collectors.counting()
                ));

        Map<String, BigDecimal> montosPorMetodo = pagos.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getMetodoPago().name(),
                        Collectors.reducing(BigDecimal.ZERO, Pago::getMonto, BigDecimal::add)
                ));

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (String metodo : cantidadPorMetodo.keySet()) {
            resultado.add(Map.of(
                    "metodoPago", metodo,
                    "cantidadPagos", cantidadPorMetodo.get(metodo),
                    "montoTotal", montosPorMetodo.get(metodo)
            ));
        }

        return resultado;
    }

    /** Cuentas por cobrar: foto del momento, solo ventas PENDIENTE_PAGO (sin rango de fechas). */
    public ReporteCuentasPorCobrarResponse getReporteCuentasPorCobrar() {
        List<Venta> ventasPendientes = ventaRepository.findAll().stream()
                .filter(v -> v.getEstado() == EstadoVenta.PENDIENTE_PAGO)
                .collect(Collectors.toList());

        LocalDateTime ahora = LocalDateTime.now();
        List<ReporteCuentasPorCobrarResponse.VentaPendienteDTO> detalle = ventasPendientes.stream()
                .map(v -> new ReporteCuentasPorCobrarResponse.VentaPendienteDTO(
                        v.getId(),
                        v.getFechaVenta(),
                        v.getNombreClienteCompleto(),
                        v.getTelefonoClienteCompleto(),
                        v.getMontoTotal(),
                        v.getSaldoPendiente(),
                        java.time.temporal.ChronoUnit.DAYS.between(v.getFechaVenta(), ahora),
                        v.getFechaLimitePago(),
                        v.getFechaLimitePago() != null && v.getFechaLimitePago().isBefore(LocalDate.now())
                ))
                .sorted((a, b) -> Long.compare(b.getDiasTranscurridos(), a.getDiasTranscurridos()))
                .collect(Collectors.toList());

        BigDecimal totalPorCobrar = ventasPendientes.stream()
                .map(Venta::getSaldoPendiente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReporteCuentasPorCobrarResponse(
                (long) ventasPendientes.size(), totalPorCobrar, detalle);
    }

    /** Reporte financiero: gananciaVentas (precio menos costo guardado) y flujo de caja (ingresos, gastos, saldo). */
    public ReporteFinancieroResponse getReporteFinanciero(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Venta> ventas = ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(fechaInicio, fechaFin)
                .stream()
                .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA)
                .collect(Collectors.toList());

        List<Compra> compras = compraRepository.findByFechaCompraBetweenOrderByFechaCompraDesc(fechaInicio, fechaFin)
                .stream()
                .filter(c -> c.getEstado() == EstadoCompra.CONFIRMADA)
                .collect(Collectors.toList());

        BigDecimal gananciaVentas = calcularGananciaVentas(ventas);

        BigDecimal ingresosTotales = ventas.stream()
                .map(Venta::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gastosTotales = compras.stream()
                .map(Compra::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoPeriodo = ingresosTotales.subtract(gastosTotales);

        DateTimeFormatter formatoFecha = DateTimeFormatter.ISO_LOCAL_DATE;
        List<ReporteFinancieroResponse.DetalleFechaMontoDTO> detalleIngresos = ventas.stream()
                .map(v -> new ReporteFinancieroResponse.DetalleFechaMontoDTO(
                        v.getFechaVenta().format(formatoFecha), v.getMontoTotal()))
                .collect(Collectors.toList());

        List<ReporteFinancieroResponse.DetalleFechaMontoDTO> detalleGastos = compras.stream()
                .map(c -> new ReporteFinancieroResponse.DetalleFechaMontoDTO(
                        c.getFechaCompra().format(formatoFecha), c.getMontoTotal()))
                .collect(Collectors.toList());

        return new ReporteFinancieroResponse(
                fechaInicio, fechaFin,
                gananciaVentas,
                ingresosTotales, gastosTotales, saldoPeriodo,
                detalleIngresos, detalleGastos
        );
    }

    /** Ganancia por producto vendido, solo ventas COMPLETADA y con el costo guardado en cada línea. */
    public List<Map<String, Object>> getGananciasPorProducto(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Venta venta : ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(fechaInicio, fechaFin)) {
            if (venta.getEstado() != EstadoVenta.COMPLETADA) continue;
            for (DetalleVenta detalle : venta.getDetalles()) {
                Map<String, Object> fila = new java.util.HashMap<>();
                BigDecimal costoUnitario = detalle.getCostoUnitario();
                BigDecimal ganancia = costoUnitario == null
                        ? null
                        : detalle.getPrecioUnitario().subtract(costoUnitario)
                                .multiply(BigDecimal.valueOf(detalle.getCantidad()));
                fila.put("idVenta", venta.getId());
                fila.put("fechaVenta", venta.getFechaVenta());
                fila.put("idProducto", detalle.getProducto().getId());
                fila.put("nombreProducto", detalle.getProducto().getNombre());
                fila.put("skuProducto", detalle.getProducto().getSku());
                fila.put("cantidad", detalle.getCantidad());
                fila.put("precioUnitario", detalle.getPrecioUnitario());
                fila.put("costoUnitario", costoUnitario);
                fila.put("subtotal", detalle.getSubtotal());
                fila.put("ganancia", ganancia);
                resultado.add(fila);
            }
        }
        return resultado;
    }

    /** Fecha del primer registro de venta disponible para limitar filtros históricos. */
    public LocalDate getFechaInicioRegistros() {
        return ventaRepository.findAll().stream()
                .map(Venta::getFechaVenta)
                .filter(java.util.Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .min(LocalDate::compareTo)
                .orElse(null);
    }
}
