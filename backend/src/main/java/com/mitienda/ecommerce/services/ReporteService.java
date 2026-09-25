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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para generación de reportes
 */
@Service
// Lectura dentro de transacción por defecto: con spring.jpa.open-in-view=false
// no hay sesión de Hibernate fuera de la transacción, y los DTO de respuesta se
// arman recorriendo relaciones perezosas. Sin esto, los endpoints de lectura
// fallaban con LazyInitializationException.
// Los métodos que escriben llevan su propio @Transactional, que tiene precedencia.
@Transactional(readOnly = true)
public class ReporteService {

    private final VentaRepository ventaRepository;

    private final DetalleVentaRepository detalleVentaRepository;

    private final InventarioRepository inventarioRepository;

    private final CompraRepository compraRepository;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public ReporteService(VentaRepository ventaRepository,
                          DetalleVentaRepository detalleVentaRepository,
                          InventarioRepository inventarioRepository,
                          CompraRepository compraRepository) {
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.inventarioRepository = inventarioRepository;
        this.compraRepository = compraRepository;
    }


    /**
     * Resume en un texto los métodos de pago usados en una venta.
     * El método dejó de ser un campo de 'ventas' y vive en 'pagos', porque una
     * venta admite varios cobros con métodos distintos.
     */
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

    /**
     * Reporte de ventas por período
     */
    public ReporteVentasResponse getReporteVentas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        // Una venta CANCELADA devuelve el stock al inventario, como si nunca
        // hubiera pasado: no cuenta como venta real en ningún reporte.
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
                        venta.getNombreClienteCompleto(),  // ✅ Corregido: maneja cliente nulo
                        venta.getMontoTotal(),
                        resumirMetodoPago(venta),
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
        // Mismo criterio que el resto de reportes: una venta CANCELADA
        // devolvió el stock, no se vendió de verdad.
        List<DetalleVenta> detalles = detalleVentaRepository.findAll()
                .stream()
                .filter(d -> d.getVenta().getEstado() != EstadoVenta.CANCELADA)
                .collect(Collectors.toList());

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

        // Filtrar ventas con cliente != null antes de agrupar, y las
        // CANCELADA: devolvieron el stock, no cuentan como compra real del
        // cliente.
        List<Venta> ventasConCliente = ventas.stream()
                .filter(venta -> venta.getCliente() != null)
                .filter(venta -> venta.getEstado() != EstadoVenta.CANCELADA)
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

    /**
     * Reporte de inventario valorizado
     */
    public Map<String, Object> getReporteInventarioValorizado() {
        List<Map<String, Object>> inventarios = inventarioRepository.findAll().stream()
                .map(inv -> {
                    // Map.of no admite valores null, y un producto sin precio
                    // de compra cargado sí puede darse: por eso un mapa
                    // mutable en vez de Map.of, y el cálculo solo si hay dato.
                    BigDecimal precioCompra = inv.getProducto().getPrecioCompra();
                    BigDecimal valorItem = precioCompra != null
                            ? precioCompra.multiply(BigDecimal.valueOf(inv.getCantidadDisponible()))
                            : null;
                    Map<String, Object> item = new java.util.HashMap<>();
                    item.put("idProducto", inv.getProducto().getId());
                    item.put("nombreProducto", inv.getProducto().getNombre());
                    item.put("skuProducto", inv.getProducto().getSku());
                    item.put("cantidadDisponible", inv.getCantidadDisponible());
                    item.put("precioUnitario", precioCompra);
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

    /**
     * Reporte de ventas por categoría
     */
    public List<Map<String, Object>> getReporteVentasPorCategoria(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        // Mismo criterio: una venta CANCELADA devolvió el stock, no cuenta.
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

    /**
     * Reporte de cobros por método de pago.
     *
     * Se calcula sobre los pagos y no sobre las ventas. Antes cada venta tenía
     * un único metodo_pago, lo que obligaba a elegir uno cuando el cliente
     * pagaba parte en efectivo y parte por transferencia, y el reporte salía
     * mal. Recorriendo los pagos, cada cobro suma en su propio método y los
     * totales cuadran contra la caja.
     */
    public List<Map<String, Object>> getReporteVentasPorMetodoPago(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Venta> ventas = ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(fechaInicio, fechaFin);

        // Cancelar una venta no toca el registro del pago (sigue COMPLETADO):
        // sin este filtro, la plata de una venta anulada seguia sumando acá.
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

    /**
     * Reporte de cuentas por cobrar: ventas con saldo pendiente.
     *
     * No es un reporte por periodo -- es una foto del momento (a quien le
     * falta cobrar hoy), asi que no recibe rango de fechas. Solo cuentan
     * las ventas PENDIENTE_PAGO: una CANCELADA no genero una deuda real
     * (se anulo entera) y una COMPLETADA ya no tiene saldo pendiente.
     */
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
                        java.time.temporal.ChronoUnit.DAYS.between(v.getFechaVenta(), ahora)
                ))
                .sorted((a, b) -> Long.compare(b.getDiasTranscurridos(), a.getDiasTranscurridos()))
                .collect(Collectors.toList());

        BigDecimal totalPorCobrar = ventasPendientes.stream()
                .map(Venta::getSaldoPendiente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReporteCuentasPorCobrarResponse(
                (long) ventasPendientes.size(), totalPorCobrar, detalle);
    }

    /**
     * Reporte financiero del periodo.
     *
     * Separa dos cosas que antes se mezclaban en un solo numero
     * (ventas menos compras del periodo, mostrado como "ganancia"):
     *
     * - gananciaVentas: ganancia real de lo vendido. Por cada linea de
     *   venta, precio unitario menos costo unitario (el costo que quedo
     *   fijo al momento de esa venta), por la cantidad.
     * - ingresosTotales / gastosTotales / saldoPeriodo: flujo de caja del
     *   periodo (ventas contra compras). No es ganancia: un mes de compra
     *   grande para abastecerse puede dar saldo negativo aunque lo vendido
     *   haya dejado margen positivo.
     *
     * Solo ADMIN llega a este metodo (el controller exige ese rol): el
     * costo por producto no debe llegarle a un EMPLEADO en ninguna
     * respuesta, por eso el calculo vive aca y no se expone
     * detalle_venta.costo_unitario en ningun DTO de venta.
     */
    public ReporteFinancieroResponse getReporteFinanciero(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Venta> ventas = ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(fechaInicio, fechaFin)
                .stream()
                .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA)
                .collect(Collectors.toList());

        List<Compra> compras = compraRepository.findByFechaCompraBetweenOrderByFechaCompraDesc(fechaInicio, fechaFin)
                .stream()
                .filter(c -> c.getEstado() == EstadoCompra.CONFIRMADA)
                .collect(Collectors.toList());

        BigDecimal gananciaVentas = ventas.stream()
                .flatMap(v -> v.getDetalles().stream())
                .map(d -> d.getPrecioUnitario()
                        .subtract(d.getCostoUnitario())
                        .multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
}