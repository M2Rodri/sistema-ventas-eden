package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ComprobanteRequest;
import com.mitienda.ecommerce.dto.VentaRequest;
import com.mitienda.ecommerce.dto.VentaResponse;
import com.mitienda.ecommerce.models.*;
import com.mitienda.ecommerce.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private ComprobanteService comprobanteService;

    @Transactional(readOnly = true)
    public List<VentaResponse> getAllVentas() {
        return ventaRepository.findAll()
                .stream()
                .map(VentaResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VentaResponse getVentaById(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id));
        return new VentaResponse(venta);
    }

    @Transactional
    public VentaResponse createVentaDirecta(VentaRequest request, Long idUsuario) {
        if (!request.tieneCliente()) {
            throw new RuntimeException(
                    "Debe proporcionar un cliente registrado (idCliente) o datos del cliente (nombreClienteDirecto)");
        }

        User usuario = userRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));

        if (request.getMetodoPago() != MetodoPago.EFECTIVO) {
            if (request.getReferenciaPago() == null || request.getReferenciaPago().trim().isEmpty()) {
                throw new RuntimeException("La referencia de pago es obligatoria para métodos de pago digitales");
            }
        }

        Venta venta = new Venta();

        if (request.esClienteRegistrado()) {
            Cliente cliente = clienteRepository.findById(request.getIdCliente())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + request.getIdCliente()));
            venta.setCliente(cliente);
        } else if (request.esClienteRapido()) {
            venta.setNombreClienteDirecto(request.getNombreClienteDirecto().trim());
            venta.setCelularClienteDirecto(
                    request.getCelularClienteDirecto() != null ? request.getCelularClienteDirecto().trim() : null);
        }

        venta.setMetodoPago(request.getMetodoPago());
        venta.setUsuario(usuario);
        venta.setEstado(EstadoVenta.COMPLETADA);

        // PASO 1: CALCULAR TOTAL
        BigDecimal total = BigDecimal.ZERO;

        for (VentaRequest.ItemVentaRequest item : request.getItems()) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + item.getIdProducto()));

            if (!producto.getActivo()) {
                throw new RuntimeException("El producto '" + producto.getNombre() + "' no está disponible");
            }

            if (!inventarioService.verificarDisponibilidad(producto.getId(), item.getCantidad())) {
                throw new RuntimeException("Stock insuficiente para el producto '" + producto.getNombre() + "'");
            }

            BigDecimal precioOriginal = producto.getPrecioVenta();
            BigDecimal precioFinal;

            if (item.getPrecioUnitarioConDescuento() != null &&
                    item.getPrecioUnitarioConDescuento().compareTo(precioOriginal) < 0) {
                precioFinal = item.getPrecioUnitarioConDescuento();
                if (precioFinal.compareTo(producto.getPrecioUnitario()) < 0) {
                    throw new RuntimeException("No se puede vender '" + producto.getNombre() +
                            "' por debajo del costo (Bs. " + producto.getPrecioUnitario() + ")");
                }
            } else {
                precioFinal = precioOriginal;
            }

            total = total.add(precioFinal.multiply(BigDecimal.valueOf(item.getCantidad())));
        }

        // PASO 2: GUARDAR VENTA
        venta.setMontoTotal(total);
        Venta savedVenta = ventaRepository.save(venta);

        // PASO 3: CREAR DETALLES Y REDUCIR INVENTARIO
        for (VentaRequest.ItemVentaRequest item : request.getItems()) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            Inventario inventario = inventarioRepository.findByProductoId(producto.getId())
                    .orElseThrow(() -> new RuntimeException(
                            "No existe inventario para el producto con ID: " + producto.getId()));

            Integer cantidadAnterior = inventario.getCantidadDisponible();

            BigDecimal precioOriginal = producto.getPrecioVenta();
            BigDecimal precioFinal;
            BigDecimal descuentoUnitario;
            BigDecimal descuentoPorcentaje;

            if (item.getPrecioUnitarioConDescuento() != null &&
                    item.getPrecioUnitarioConDescuento().compareTo(precioOriginal) < 0) {
                precioFinal = item.getPrecioUnitarioConDescuento();
                descuentoUnitario = precioOriginal.subtract(precioFinal);
                descuentoPorcentaje = item.getDescuentoPorcentaje() != null ? item.getDescuentoPorcentaje()
                        : BigDecimal.ZERO;
            } else {
                precioFinal = precioOriginal;
                descuentoUnitario = BigDecimal.ZERO;
                descuentoPorcentaje = BigDecimal.ZERO;
            }

            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(savedVenta);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitarioOriginal(precioOriginal);
            detalle.setPrecioUnitario(precioFinal);
            detalle.setDescuentoUnitario(descuentoUnitario);
            detalle.setDescuentoPorcentaje(descuentoPorcentaje);
            detalle.calcularSubtotal();
            detalleVentaRepository.save(detalle);

            inventarioService.reducirStock(producto.getId(), item.getCantidad());

            Inventario inventarioActualizado = inventarioRepository.findByProductoId(producto.getId())
                    .orElseThrow(() -> new RuntimeException("Error al obtener inventario actualizado"));

            inventarioService.registrarAjusteAutomatico(
                    producto.getId(),
                    cantidadAnterior,
                    inventarioActualizado.getCantidadDisponible(),
                    "SALIDA",
                    "Venta #" + savedVenta.getId(),
                    idUsuario);
        }

        // PASO 4: REGISTRAR PAGO
        Pago pago = new Pago();
        pago.setVenta(savedVenta);
        pago.setMonto(total);
        pago.setMetodoPago(request.getMetodoPago());
        pago.setReferencia(request.getReferenciaPago());
        pago.setEstado(EstadoPago.COMPLETADO);
        pagoRepository.save(pago);

        // PASO 5: GENERAR COMPROBANTE
        try {
            ComprobanteRequest comprobanteRequest = new ComprobanteRequest();
            comprobanteRequest.setIdVenta(savedVenta.getId());
            comprobanteRequest.setTipoComprobante(TipoComprobante.RECIBO);
            comprobanteRequest.setNombreCliente(savedVenta.getNombreClienteCompleto());
            comprobanteService.createComprobante(comprobanteRequest);
        } catch (Exception e) {
            System.err.println("⚠️ Error al generar comprobante: " + e.getMessage());
        }

        return new VentaResponse(savedVenta);
    }

    @Transactional
    public VentaResponse cancelarVenta(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id));

        if (venta.getEstado() == EstadoVenta.CANCELADA) {
            throw new RuntimeException("Esta venta ya está cancelada");
        }

        for (DetalleVenta detalle : venta.getDetalles()) {
            Inventario inventario = inventarioRepository.findByProductoId(detalle.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("No existe inventario para el producto"));

            Integer cantidadAnterior = inventario.getCantidadDisponible();

            inventarioService.aumentarStock(detalle.getProducto().getId(), detalle.getCantidad());

            Inventario inventarioActualizado = inventarioRepository.findByProductoId(detalle.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Error al obtener inventario actualizado"));

            inventarioService.registrarAjusteAutomatico(
                    detalle.getProducto().getId(),
                    cantidadAnterior,
                    inventarioActualizado.getCantidadDisponible(),
                    "ENTRADA",
                    "Cancelación de Venta #" + id,
                    venta.getUsuario() != null ? venta.getUsuario().getId() : null);
        }

        venta.setEstado(EstadoVenta.CANCELADA);
        return new VentaResponse(ventaRepository.save(venta));
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> getVentasByCliente(Long clienteId) {
        return ventaRepository.findByClienteIdOrderByFechaVentaDesc(clienteId)
                .stream()
                .map(VentaResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> getVentasByEstado(EstadoVenta estado) {
        return ventaRepository.findByEstadoOrderByFechaVentaDesc(estado)
                .stream()
                .map(VentaResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> getVentasDelDia() {
        return ventaRepository.findVentasDelDia()
                .stream()
                .map(VentaResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> getUltimasVentas() {
        return ventaRepository.findTop10ByOrderByFechaVentaDesc()
                .stream()
                .map(VentaResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> getVentasByFechas(LocalDateTime inicio, LocalDateTime fin) {
        return ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(inicio, fin)
                .stream()
                .map(VentaResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalVentasByFechas(LocalDateTime inicio, LocalDateTime fin) {
        BigDecimal total = ventaRepository.sumMontoTotalByFechaVentaBetween(inicio, fin);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public Long countVentasByEstado(EstadoVenta estado) {
        return ventaRepository.countByEstado(estado);
    }
}