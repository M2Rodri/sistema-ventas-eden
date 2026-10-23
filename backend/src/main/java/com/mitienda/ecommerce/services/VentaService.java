package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.exception.ConflictoEstadoException;
import com.mitienda.ecommerce.exception.PeticionInvalidaException;
import com.mitienda.ecommerce.exception.RecursoNoEncontradoException;
import com.mitienda.ecommerce.exception.ReglaNegocioException;
import com.mitienda.ecommerce.dto.DatosEntregaRequest;
import com.mitienda.ecommerce.dto.FechaLimitePagoRequest;
import com.mitienda.ecommerce.dto.VentaRequest;
import com.mitienda.ecommerce.dto.VentaResponse;
import com.mitienda.ecommerce.models.*;
import com.mitienda.ecommerce.repositories.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
// Lectura en transacción: con open-in-view=false no hay sesión de Hibernate fuera de ella.
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

    /** Respuesta de la venta según el rol: solo el Admin recibe costo y ganancia; al Empleado no le llegan ni ocultos. */
    private VentaResponse respuesta(Venta venta) {
        return new VentaResponse(venta, usuarioActualService.esAdmin());
    }

    /** Igual que respuesta(), para listas: se pregunta por el rol una sola vez y no por cada venta. */
    private Function<Venta, VentaResponse> aRespuesta() {
        boolean esAdmin = usuarioActualService.esAdmin();
        return venta -> new VentaResponse(venta, esAdmin);
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> getAllVentas() {
        return ventaRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaVenta"))
                .stream()
                .map(aRespuesta())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VentaResponse getVentaById(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("VENTA_NO_ENCONTRADA", "Venta no encontrada con ID: " + id));
        return respuesta(venta);
    }

    @Transactional
    public VentaResponse createVentaDirecta(VentaRequest request) {
        if (!request.tieneCliente()) {
            throw new PeticionInvalidaException("CLIENTE_REQUERIDO",
                    "Debe proporcionar un cliente registrado (idCliente) o el nombre del cliente de mostrador");
        }

        // El vendedor sale del token, no del cliente (ver UsuarioActualService).
        Usuario usuario = usuarioActualService.obtenerRequerido();

        ModalidadEntrega modalidadEntrega = request.getModalidadEntrega() != null
                ? request.getModalidadEntrega() : ModalidadEntrega.RETIRO;

        // Entrega: RETIRO sin datos; DOMICILIO con dirección opcional; TRANSPORTADORA con ciudad obligatoria.
        if (modalidadEntrega == ModalidadEntrega.TRANSPORTADORA && esVacio(request.getCiudad())) {
            throw new PeticionInvalidaException("CIUDAD_REQUERIDA", "La ciudad es obligatoria para el envío por transportadora");
        }

        EstadoEntrega estadoEntrega = resolverEstadoInicial(modalidadEntrega, request.getEstadoEntrega());

        Venta venta = new Venta();

        if (request.esClienteRegistrado()) {
            Cliente cliente = clienteRepository.findById(request.getIdCliente())
                    .orElseThrow(() -> new RecursoNoEncontradoException("CLIENTE_NO_ENCONTRADO", "Cliente no encontrado con ID: " + request.getIdCliente()));
            venta.setCliente(cliente);
        } else if (request.esClienteRapido()) {
            // Venta de mostrador: se crea un cliente para tener un único mecanismo de comprador.
            Cliente invitado = new Cliente();
            invitado.setNombre(request.getNombreClienteInvitado().trim());
            invitado.setTelefono(request.getTelefonoClienteInvitado() != null
                    ? request.getTelefonoClienteInvitado().trim() : null);
            invitado.setNitCi(request.getCiClienteInvitado() != null
                    ? request.getCiClienteInvitado().trim() : null);
            invitado.setActivo(true);
            venta.setCliente(clienteRepository.save(invitado));
        }

        // El método de pago vive en Pago (PASO 4): una venta admite varios cobros.
        venta.setUsuario(usuario);

        venta.setModalidadEntrega(modalidadEntrega);
        venta.setEstadoEntrega(estadoEntrega);
        if (modalidadEntrega != ModalidadEntrega.RETIRO) {
            venta.setDireccionDestino(textoOpcional(request.getDireccionDestino()));
        }
        if (modalidadEntrega == ModalidadEntrega.TRANSPORTADORA) {
            venta.setCiudad(request.getCiudad().trim());
            venta.setTransportadora(textoOpcional(request.getTransportadora()));
            venta.setGuiaRemision(textoOpcional(request.getGuiaRemision()));
        }

        // PASO 1: calcular total
        BigDecimal total = BigDecimal.ZERO;

        for (VentaRequest.ItemVentaRequest item : request.getItems()) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RecursoNoEncontradoException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado con ID: " + item.getIdProducto()));

            if (!producto.getActivo()) {
                throw new ReglaNegocioException("PRODUCTO_NO_DISPONIBLE", "El producto '" + producto.getNombre() + "' no está disponible");
            }

            if (!inventarioService.verificarDisponibilidad(producto.getId(), item.getCantidad())) {
                throw new ReglaNegocioException("STOCK_INSUFICIENTE", "Stock insuficiente para el producto '" + producto.getNombre() + "'");
            }

            BigDecimal precioOriginal = producto.getPrecioVenta();
            BigDecimal precioFinal;

            if (item.getPrecioUnitarioConDescuento() != null) {
                if (item.getPrecioUnitarioConDescuento().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new PeticionInvalidaException("PRECIO_INVALIDO", "El precio unitario debe ser mayor a 0");
                }
                // El precio pactado es independiente del de catálogo y no lo altera.
                precioFinal = item.getPrecioUnitarioConDescuento();
            } else {
                precioFinal = precioOriginal;
            }

            total = total.add(precioFinal.multiply(BigDecimal.valueOf(item.getCantidad())));
        }

        // PASO 2: guardar venta. Sin descuento general, subtotal y montoTotal coinciden.
        venta.setSubtotal(total);
        venta.setDescuento(BigDecimal.ZERO);
        venta.setMontoTotal(total);

        // Sin montoPagado se asume cobro total.
        BigDecimal montoPagado = request.getMontoPagado() != null ? request.getMontoPagado() : total;
        if (montoPagado.compareTo(BigDecimal.ZERO) < 0) {
            throw new PeticionInvalidaException("MONTO_INVALIDO", "El monto pagado no puede ser negativo");
        }
        if (montoPagado.compareTo(total) > 0) {
            throw new ReglaNegocioException("PAGO_EXCEDE_TOTAL", "El monto pagado no puede superar el total de la venta");
        }

        BigDecimal saldoPendiente = total.subtract(montoPagado);
        venta.setSaldoPendiente(saldoPendiente);
        venta.setEstado(saldoPendiente.compareTo(BigDecimal.ZERO) == 0
                ? EstadoVenta.COMPLETADA : EstadoVenta.PENDIENTE_PAGO);

        // Fecha límite del pago pendiente: opcional y solo si queda saldo.
        if (saldoPendiente.compareTo(BigDecimal.ZERO) > 0) {
            venta.setFechaLimitePago(resolverFechaLimitePago(request.getPlazoDiasPago(), request.getFechaLimitePago()));
        }

        Venta savedVenta = ventaRepository.save(venta);

        // PASO 3: crear detalles y reducir inventario
        for (VentaRequest.ItemVentaRequest item : request.getItems()) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RecursoNoEncontradoException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado"));

            Inventario inventario = inventarioRepository.findByProductoId(producto.getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("INVENTARIO_NO_ENCONTRADO",
                            "No existe inventario para el producto con ID: " + producto.getId()));

            Integer cantidadAnterior = inventario.getCantidadDisponible();

            BigDecimal precioOriginal = producto.getPrecioVenta();
            BigDecimal precioFinal;
            BigDecimal descuentoUnitario;
            BigDecimal descuentoPorcentaje;

            if (item.getPrecioUnitarioConDescuento() != null) {
                if (item.getPrecioUnitarioConDescuento().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new PeticionInvalidaException("PRECIO_INVALIDO", "El precio unitario debe ser mayor a 0");
                }
                precioFinal = item.getPrecioUnitarioConDescuento();
                if (precioFinal.compareTo(precioOriginal) < 0) {
                    descuentoUnitario = precioOriginal.subtract(precioFinal);
                    descuentoPorcentaje = item.getDescuentoPorcentaje() != null ? item.getDescuentoPorcentaje()
                            : BigDecimal.ZERO;
                } else {
                    descuentoUnitario = BigDecimal.ZERO;
                    descuentoPorcentaje = BigDecimal.ZERO;
                }
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
            // Costo congelado al vender; sin precio de compra queda en 0 (la columna no admite null).
            detalle.setCostoUnitario(producto.getPrecioCompra() != null
                    ? producto.getPrecioCompra() : BigDecimal.ZERO);
            detalle.calcularSubtotal();
            // También en la lista en memoria: la respuesta se arma desde ahí.
            savedVenta.getDetalles().add(detalleVentaRepository.save(detalle));

            inventarioService.reducirStock(producto.getId(), item.getCantidad());

            Inventario inventarioActualizado = inventarioRepository.findByProductoId(producto.getId())
                    .orElseThrow(() -> new IllegalStateException("Error al obtener inventario actualizado"));

            inventarioService.registrarAjusteAutomatico(
                    producto.getId(),
                    cantidadAnterior,
                    inventarioActualizado.getCantidadDisponible(),
                    "SALIDA",
                    "Venta #" + savedVenta.getId(),
                    usuario.getId());
        }

        // PASO 4: registrar pago (Pago exige monto mayor a 0, así que una venta toda a crédito no lo crea).
        if (montoPagado.compareTo(BigDecimal.ZERO) > 0) {
            Pago pago = new Pago();
            pago.setVenta(savedVenta);
            pago.setMonto(montoPagado);
            pago.setMetodoPago(request.getMetodoPago());
            pago.setReferencia(request.getReferenciaPago());
            pago.setUsuario(usuario);
            pago.setEstado(EstadoPago.COMPLETADO);
            // También en la lista en memoria: la web sube la foto del comprobante a pagos[0].
            savedVenta.getPagos().add(pagoRepository.save(pago));
        }

        // El comprobante se genera aparte, tras confirmar la transacción (ver VentaController).

        registroAuditoria.registrar("CREAR_VENTA", "ventas", savedVenta.getId(),
                "Venta por Bs " + savedVenta.getMontoTotal()
                        + " a " + savedVenta.getNombreClienteCompleto());

        return respuesta(savedVenta);
    }

    @Transactional
    public VentaResponse cancelarVenta(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("VENTA_NO_ENCONTRADA", "Venta no encontrada con ID: " + id));

        if (venta.getEstado() == EstadoVenta.CANCELADA) {
            throw new ConflictoEstadoException("VENTA_YA_CANCELADA", "Esta venta ya está cancelada");
        }

        for (DetalleVenta detalle : venta.getDetalles()) {
            Inventario inventario = inventarioRepository.findByProductoId(detalle.getProducto().getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("INVENTARIO_NO_ENCONTRADO", "No existe inventario para el producto"));

            Integer cantidadAnterior = inventario.getCantidadDisponible();

            inventarioService.aumentarStock(detalle.getProducto().getId(), detalle.getCantidad());

            Inventario inventarioActualizado = inventarioRepository.findByProductoId(detalle.getProducto().getId())
                    .orElseThrow(() -> new IllegalStateException("Error al obtener inventario actualizado"));

            inventarioService.registrarAjusteAutomatico(
                    detalle.getProducto().getId(),
                    cantidadAnterior,
                    inventarioActualizado.getCantidadDisponible(),
                    "ENTRADA",
                    "Cancelación de Venta #" + id,
                    venta.getUsuario() != null ? venta.getUsuario().getId() : null);
        }

        venta.setEstado(EstadoVenta.CANCELADA);
        // Una venta cancelada no debe quedar con saldo pendiente.
        venta.setSaldoPendiente(BigDecimal.ZERO);
        venta.setFechaLimitePago(null);
        Venta cancelada = ventaRepository.save(venta);

        registroAuditoria.registrar("CANCELAR_VENTA", "ventas", cancelada.getId(),
                "Anulacion de la venta #" + cancelada.getId()
                        + " por Bs " + cancelada.getMontoTotal());

        return respuesta(cancelada);
    }

    /** Marca la entrega como completada; el saldo pendiente no la bloquea (decisión del dueño). */
    @Transactional
    public VentaResponse marcarEntregado(Long id) {
        Venta venta = buscarVenta(id);

        if (venta.getEstado() == EstadoVenta.CANCELADA) {
            throw new ConflictoEstadoException("VENTA_CANCELADA", "No se puede entregar una venta cancelada");
        }
        if (venta.getEstadoEntrega() == EstadoEntrega.ENTREGADO) {
            throw new ConflictoEstadoException("VENTA_YA_ENTREGADA", "Esta venta ya está marcada como entregada");
        }

        venta.setEstadoEntrega(EstadoEntrega.ENTREGADO);
        Venta entregada = ventaRepository.save(venta);

        registroAuditoria.registrar("ENTREGAR_VENTA", "ventas", entregada.getId(),
                "Entrega marcada para la venta #" + entregada.getId());

        return respuesta(entregada);
    }

    /** Corrige una entrega marcada (ENTREGADO a PENDIENTE); solo ADMIN. Una venta RETIRO siempre es ENTREGADO. */
    @Transactional
    public VentaResponse deshacerEntrega(Long id) {
        Venta venta = buscarVenta(id);

        if (venta.getEstado() == EstadoVenta.CANCELADA) {
            throw new ConflictoEstadoException("VENTA_CANCELADA", "No se puede corregir la entrega de una venta cancelada");
        }
        if (venta.getModalidadEntrega() == ModalidadEntrega.RETIRO) {
            throw new ReglaNegocioException("VENTA_EN_TIENDA_SIN_ENTREGA", "En una venta en tienda no se puede corregir la entrega");
        }
        if (venta.getEstadoEntrega() != EstadoEntrega.ENTREGADO) {
            throw new ConflictoEstadoException("VENTA_ENTREGA_PENDIENTE", "No hay nada que corregir: la venta está pendiente de entrega");
        }

        venta.setEstadoEntrega(EstadoEntrega.PENDIENTE);
        Venta actualizada = ventaRepository.save(venta);

        registroAuditoria.registrar("DESHACER_ENTREGA", "ventas", actualizada.getId(),
                "Entrega corregida en la venta #" + actualizada.getId() + ": de ENTREGADO a PENDIENTE");

        return respuesta(actualizada);
    }

    /** Completa o corrige dirección, transportadora y guía de la entrega; un valor vacío borra el dato. Solo ADMIN. */
    @Transactional
    public VentaResponse actualizarDatosEntrega(Long id, DatosEntregaRequest datos) {
        Venta venta = buscarVenta(id);

        if (venta.getEstado() == EstadoVenta.CANCELADA) {
            throw new ConflictoEstadoException("VENTA_CANCELADA", "No se pueden editar los datos de entrega de una venta cancelada");
        }
        if (venta.getModalidadEntrega() == ModalidadEntrega.RETIRO) {
            throw new ReglaNegocioException("VENTA_EN_TIENDA_SIN_ENTREGA", "Una venta en tienda no tiene datos de entrega");
        }

        venta.setDireccionDestino(textoOpcional(datos.getDireccionDestino()));
        if (venta.getModalidadEntrega() == ModalidadEntrega.TRANSPORTADORA) {
            venta.setTransportadora(textoOpcional(datos.getTransportadora()));
            venta.setGuiaRemision(textoOpcional(datos.getGuiaRemision()));
        }
        Venta actualizada = ventaRepository.save(venta);

        registroAuditoria.registrar("EDITAR_ENTREGA", "ventas", actualizada.getId(),
                "Datos de entrega editados en la venta #" + actualizada.getId());

        return respuesta(actualizada);
    }

    /** Pone o cambia la fecha límite del pago pendiente, solo si queda saldo. */
    @Transactional
    public VentaResponse actualizarFechaLimitePago(Long id, FechaLimitePagoRequest datos) {
        Venta venta = buscarVenta(id);

        if (venta.getEstado() == EstadoVenta.CANCELADA) {
            throw new ConflictoEstadoException("VENTA_CANCELADA", "No se puede cambiar la fecha de una venta cancelada");
        }
        if (venta.getEstado() != EstadoVenta.PENDIENTE_PAGO
                || venta.getSaldoPendiente() == null
                || venta.getSaldoPendiente().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("SIN_PAGO_PENDIENTE", "La venta no tiene pago pendiente");
        }
        LocalDate fecha = resolverFechaLimitePago(datos.getPlazoDiasPago(), datos.getFechaLimitePago());
        if (fecha == null) {
            throw new PeticionInvalidaException("FECHA_REQUERIDA", "Indica la fecha límite o los días de plazo");
        }

        venta.setFechaLimitePago(fecha);
        Venta actualizada = ventaRepository.save(venta);

        registroAuditoria.registrar("EDITAR_FECHA_LIMITE_PAGO", "ventas", actualizada.getId(),
                "Fecha límite de pago de la venta #" + actualizada.getId() + ": " + fecha);

        return respuesta(actualizada);
    }

    /** La fecha límite sale del reloj del servidor: hoy más N días, o una fecha no anterior a hoy. */
    private LocalDate resolverFechaLimitePago(Integer plazoDias, LocalDate fecha) {
        if (plazoDias != null) {
            return LocalDate.now().plusDays(plazoDias);
        }
        if (fecha != null) {
            if (fecha.isBefore(LocalDate.now())) {
                throw new PeticionInvalidaException("FECHA_LIMITE_INVALIDA",
                        "La fecha límite de pago no puede ser anterior a hoy");
            }
            return fecha;
        }
        return null;
    }

    private Venta buscarVenta(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("VENTA_NO_ENCONTRADA", "Venta no encontrada con ID: " + id));
    }

    private boolean esVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    /** Texto sin espacios sobrantes, o null si viene vacío. */
    private String textoOpcional(String texto) {
        return esVacio(texto) ? null : texto.trim();
    }

    /** Estado de entrega inicial: RETIRO siempre ENTREGADO; en otros casos, el indicado o PENDIENTE. */
    private EstadoEntrega resolverEstadoInicial(ModalidadEntrega modalidad, EstadoEntrega pedido) {
        if (modalidad == ModalidadEntrega.RETIRO) {
            return EstadoEntrega.ENTREGADO;
        }
        if (pedido == null || pedido == EstadoEntrega.PENDIENTE) {
            return EstadoEntrega.PENDIENTE;
        }
        return pedido;
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> getVentasByCliente(Long clienteId) {
        return ventaRepository.findByClienteIdOrderByFechaVentaDesc(clienteId)
                .stream()
                .map(aRespuesta())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> getVentasByEstado(EstadoVenta estado) {
        return ventaRepository.findByEstadoOrderByFechaVentaDesc(estado)
                .stream()
                .map(aRespuesta())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> getVentasDelDia() {
        return ventaRepository.findVentasDelDia()
                .stream()
                .map(aRespuesta())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> getUltimasVentas() {
        return ventaRepository.findTop10ByOrderByFechaVentaDesc()
                .stream()
                .map(aRespuesta())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> getVentasByFechas(LocalDateTime inicio, LocalDateTime fin) {
        return ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(inicio, fin)
                .stream()
                .map(aRespuesta())
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