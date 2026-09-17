package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.VentaRequest;
import com.mitienda.ecommerce.dto.VentaResponse;
import com.mitienda.ecommerce.models.*;
import com.mitienda.ecommerce.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
// Lectura dentro de transacción por defecto: con spring.jpa.open-in-view=false
// no hay sesión de Hibernate fuera de la transacción, y los DTO de respuesta se
// arman recorriendo relaciones perezosas. Sin esto, los endpoints de lectura
// fallaban con LazyInitializationException.
// Los métodos que escriben llevan su propio @Transactional, que tiene precedencia.
@Transactional(readOnly = true)
public class VentaService {

    private final VentaRepository ventaRepository;

    private final DetalleVentaRepository detalleVentaRepository;

    private final PagoRepository pagoRepository;

    private final ClienteRepository clienteRepository;

    private final ProductoRepository productoRepository;

    private final UsuarioRepository usuarioRepository;

    private final InventarioService inventarioService;

    private final InventarioRepository inventarioRepository;

    private final RegistroAuditoria registroAuditoria;

    private final UsuarioActualService usuarioActualService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public VentaService(VentaRepository ventaRepository,
                        DetalleVentaRepository detalleVentaRepository,
                        PagoRepository pagoRepository,
                        ClienteRepository clienteRepository,
                        ProductoRepository productoRepository,
                        UsuarioRepository usuarioRepository,
                        InventarioService inventarioService,
                        InventarioRepository inventarioRepository,
                        RegistroAuditoria registroAuditoria,
                        UsuarioActualService usuarioActualService) {
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.pagoRepository = pagoRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.inventarioService = inventarioService;
        this.inventarioRepository = inventarioRepository;
        this.registroAuditoria = registroAuditoria;
        this.usuarioActualService = usuarioActualService;
    }


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
    public VentaResponse createVentaDirecta(VentaRequest request) {
        if (!request.tieneCliente()) {
            throw new RuntimeException(
                    "Debe proporcionar un cliente registrado (idCliente) o el nombre del cliente de mostrador");
        }

        // El vendedor sale del token, no de lo que mande el cliente: ver
        // UsuarioActualService.
        Usuario usuario = usuarioActualService.obtenerRequerido();

        ModalidadEntrega modalidadEntrega = request.getModalidadEntrega() != null
                ? request.getModalidadEntrega() : ModalidadEntrega.RETIRO;

        if (modalidadEntrega != ModalidadEntrega.RETIRO) {
            if (request.getDireccionDestino() == null || request.getDireccionDestino().trim().isEmpty()) {
                throw new RuntimeException("La dirección de destino es obligatoria para esta modalidad de entrega");
            }
            if (request.getCiudad() == null || request.getCiudad().trim().isEmpty()) {
                throw new RuntimeException("La ciudad es obligatoria para esta modalidad de entrega");
            }
        }
        if (modalidadEntrega == ModalidadEntrega.TRANSPORTADORA) {
            if (request.getTransportadora() == null || request.getTransportadora().trim().isEmpty()) {
                throw new RuntimeException("La transportadora es obligatoria para esta modalidad de entrega");
            }
            if (request.getGuiaRemision() == null || request.getGuiaRemision().trim().isEmpty()) {
                throw new RuntimeException("La guía de remisión es obligatoria para esta modalidad de entrega");
            }
        }

        Venta venta = new Venta();

        if (request.esClienteRegistrado()) {
            Cliente cliente = clienteRepository.findById(request.getIdCliente())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + request.getIdCliente()));
            venta.setCliente(cliente);
        } else if (request.esClienteRapido()) {
            // Venta de mostrador: en lugar de guardar el nombre suelto dentro de
            // la venta, se crea un cliente tipo INVITADO. Así hay un solo
            // mecanismo para identificar al comprador y el dato queda disponible
            // para el resto del sistema (comprobante, envío, historial).
            Cliente invitado = new Cliente();
            invitado.setNombre(request.getNombreClienteInvitado().trim());
            invitado.setTelefono(request.getTelefonoClienteInvitado() != null
                    ? request.getTelefonoClienteInvitado().trim() : null);
            invitado.setTipoCliente(TipoCliente.INVITADO);
            invitado.setActivo(true);
            venta.setCliente(clienteRepository.save(invitado));
        }

        // El método de pago ya no se guarda en la venta: viaja al Pago (PASO 4),
        // porque una venta admite varios cobros con métodos distintos.
        venta.setUsuario(usuario);

        venta.setModalidadEntrega(modalidadEntrega);
        venta.setEstadoEntrega(EstadoEntrega.PENDIENTE);
        if (modalidadEntrega != ModalidadEntrega.RETIRO) {
            venta.setDireccionDestino(request.getDireccionDestino().trim());
            venta.setCiudad(request.getCiudad().trim());
        }
        if (modalidadEntrega == ModalidadEntrega.TRANSPORTADORA) {
            venta.setTransportadora(request.getTransportadora().trim());
            venta.setGuiaRemision(request.getGuiaRemision().trim());
        }

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
                if (precioFinal.compareTo(producto.getCostoReferencial()) < 0) {
                    throw new RuntimeException("No se puede vender '" + producto.getNombre() +
                            "' por debajo del costo (Bs. " + producto.getCostoReferencial() + ")");
                }
            } else {
                precioFinal = precioOriginal;
            }

            total = total.add(precioFinal.multiply(BigDecimal.valueOf(item.getCantidad())));
        }

        // PASO 2: GUARDAR VENTA
        // subtotal es NOT NULL en la base. Sin descuento general, subtotal y
        // montoTotal coinciden; el descuento por línea ya está aplicado en
        // cada precio unitario y queda registrado en detalle_venta.
        venta.setSubtotal(total);
        venta.setDescuento(BigDecimal.ZERO);
        venta.setMontoTotal(total);

        // Si no se indica montoPagado, se asume que se cobró todo (comportamiento
        // previo a la venta a crédito).
        BigDecimal montoPagado = request.getMontoPagado() != null ? request.getMontoPagado() : total;
        if (montoPagado.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("El monto pagado no puede ser negativo");
        }
        if (montoPagado.compareTo(total) > 0) {
            throw new RuntimeException("El monto pagado no puede superar el total de la venta");
        }

        BigDecimal saldoPendiente = total.subtract(montoPagado);
        venta.setSaldoPendiente(saldoPendiente);
        venta.setEstado(saldoPendiente.compareTo(BigDecimal.ZERO) == 0
                ? EstadoVenta.COMPLETADA : EstadoVenta.PENDIENTE_PAGO);

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
                    usuario.getId());
        }

        // PASO 4: REGISTRAR PAGO
        // Si montoPagado vino en 0 (venta enteramente a crédito), no hay pago
        // que registrar: Pago exige un monto mayor a 0.
        if (montoPagado.compareTo(BigDecimal.ZERO) > 0) {
            Pago pago = new Pago();
            pago.setVenta(savedVenta);
            pago.setMonto(montoPagado);
            pago.setMetodoPago(request.getMetodoPago());
            pago.setReferencia(request.getReferenciaPago());
            pago.setUsuario(usuario);
            pago.setEstado(EstadoPago.COMPLETADO);
            pagoRepository.save(pago);
        }

        // El comprobante se genera aparte, después de que esta transacción
        // confirme (ver VentaController). Antes se generaba acá adentro
        // envuelto en try/catch: como createComprobante es @Transactional y
        // comparte esta misma transacción, si fallaba (por ejemplo un
        // numeroComprobante repetido), Spring marcaba TODA la transacción
        // como rollback-only. El catch de acá no evitaba nada: la venta
        // igual se revertía entera al terminar el método, con un error
        // "Transaction silently rolled back" que no explicaba la causa real.

        registroAuditoria.registrar("CREAR_VENTA", "ventas", savedVenta.getId(),
                "Venta por Bs " + savedVenta.getMontoTotal()
                        + " a " + savedVenta.getNombreClienteCompleto());

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
        Venta cancelada = ventaRepository.save(venta);

        // Cancelar una venta devuelve mercaderia al stock y anula plata cobrada:
        // es de las operaciones que mas conviene poder rastrear despues.
        registroAuditoria.registrar("CANCELAR_VENTA", "ventas", cancelada.getId(),
                "Anulacion de la venta #" + cancelada.getId()
                        + " por Bs " + cancelada.getMontoTotal());

        return new VentaResponse(cancelada);
    }

    /**
     * Marca la entrega de una venta como completada.
     *
     * No se puede entregar mercadería de una venta que todavía debe plata:
     * primero se cobra, después se entrega.
     */
    @Transactional
    public VentaResponse marcarEntregado(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id));

        if (venta.getEstado() == EstadoVenta.CANCELADA) {
            throw new RuntimeException("No se puede entregar una venta cancelada");
        }
        if (venta.getEstadoEntrega() == EstadoEntrega.ENTREGADO) {
            throw new RuntimeException("Esta venta ya está marcada como entregada");
        }
        if (venta.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("No se puede entregar una venta con saldo pendiente");
        }

        venta.setEstadoEntrega(EstadoEntrega.ENTREGADO);
        Venta entregada = ventaRepository.save(venta);

        registroAuditoria.registrar("ENTREGAR_VENTA", "ventas", entregada.getId(),
                "Entrega marcada para la venta #" + entregada.getId());

        return new VentaResponse(entregada);
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