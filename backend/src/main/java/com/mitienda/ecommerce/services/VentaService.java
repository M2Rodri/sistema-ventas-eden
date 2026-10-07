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
        // El resto de las consultas del repositorio ya ordenan por fecha
        // descendente (ventas del día, últimas ventas, por cliente...);
        // esta era la única que quedaba sin orden, por eso la lista salía
        // mezclada sin relación con el ID ni con la fecha.
        return ventaRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaVenta"))
                .stream()
                .map(VentaResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VentaResponse getVentaById(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("VENTA_NO_ENCONTRADA", "Venta no encontrada con ID: " + id));
        return new VentaResponse(venta);
    }

    @Transactional
    public VentaResponse createVentaDirecta(VentaRequest request) {
        if (!request.tieneCliente()) {
            throw new PeticionInvalidaException("CLIENTE_REQUERIDO",
                    "Debe proporcionar un cliente registrado (idCliente) o el nombre del cliente de mostrador");
        }

        // El vendedor sale del token, no de lo que mande el cliente: ver
        // UsuarioActualService.
        Usuario usuario = usuarioActualService.obtenerRequerido();

        ModalidadEntrega modalidadEntrega = request.getModalidadEntrega() != null
                ? request.getModalidadEntrega() : ModalidadEntrega.RETIRO;

        // Datos de entrega según la modalidad:
        //   RETIRO:         ninguno.
        //   DOMICILIO:      dirección opcional (sirve para coordinar); no se pide ciudad.
        //   TRANSPORTADORA: ciudad obligatoria; dirección, transportadora y
        //                   guía opcionales (se completan después si hace falta).
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
            // Venta de mostrador: en lugar de guardar el nombre suelto dentro de
            // la venta, se crea un cliente. Así hay un solo mecanismo para
            // identificar al comprador y el dato queda disponible para el
            // resto del sistema (comprobante, envío, historial).
            // Queda como cualquier otro cliente: no se distingue de los demás.
            Cliente invitado = new Cliente();
            invitado.setNombre(request.getNombreClienteInvitado().trim());
            invitado.setTelefono(request.getTelefonoClienteInvitado() != null
                    ? request.getTelefonoClienteInvitado().trim() : null);
            invitado.setNitCi(request.getCiClienteInvitado() != null
                    ? request.getCiClienteInvitado().trim() : null);
            invitado.setActivo(true);
            venta.setCliente(clienteRepository.save(invitado));
        }

        // El método de pago ya no se guarda en la venta: viaja al Pago (PASO 4),
        // porque una venta admite varios cobros con métodos distintos.
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

        // PASO 1: CALCULAR TOTAL
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
                // Un precio "con descuento" que en realidad es igual o mayor
                // al de catálogo no es un descuento: antes esto se aceptaba
                // en silencio y se terminaba cobrando el precio normal sin
                // avisar. Ahora se rechaza, como pide CA-04.2.
                if (item.getPrecioUnitarioConDescuento().compareTo(precioOriginal) > 0) {
                    throw new ReglaNegocioException("PRECIO_EXCEDE_CATALOGO", "No se puede vender '" + producto.getNombre() +
                            "' por encima del precio de catálogo (máximo Bs. " + precioOriginal + ")");
                }
                // Vender por debajo del costo no se bloquea: es decisión del dueño.
                precioFinal = item.getPrecioUnitarioConDescuento();
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

        // PASO 3: CREAR DETALLES Y REDUCIR INVENTARIO
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
                // Misma validación que en el cálculo del total: un precio
                // por encima del catálogo se rechaza, no se corrige solo.
                if (item.getPrecioUnitarioConDescuento().compareTo(precioOriginal) > 0) {
                    throw new ReglaNegocioException("PRECIO_EXCEDE_CATALOGO", "No se puede vender '" + producto.getNombre() +
                            "' por encima del precio de catálogo (máximo Bs. " + precioOriginal + ")");
                }
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
            // Costo al momento de la venta: si el precio de compra del
            // producto cambia despues, la ganancia de esta venta no se mueve.
            // costo_unitario no admite null en la base: sin precio de compra
            // cargado, queda en 0 (el margen de esa venta no se puede saber,
            // pero la venta no se bloquea por eso).
            detalle.setCostoUnitario(producto.getPrecioCompra() != null
                    ? producto.getPrecioCompra() : BigDecimal.ZERO);
            detalle.calcularSubtotal();
            // Se agrega también a la lista de la venta en memoria: la respuesta se arma
            // desde ahí y, si no, saldría sin los productos recién guardados.
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
            // Se agrega también a la lista de la venta en memoria: la respuesta se arma
            // desde ahí. Sin esto salía con pagos vacíos y la web, que sube la foto del
            // comprobante a pagos[0] al registrar la venta, nunca la subía.
            savedVenta.getPagos().add(pagoRepository.save(pago));
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
        // Una venta cancelada no le debe nada a nadie: sin esto, una
        // PENDIENTE_PAGO cancelada quedaba con su saldoPendiente viejo
        // pegado en la base para siempre, como si la deuda siguiera viva.
        venta.setSaldoPendiente(BigDecimal.ZERO);
        venta.setFechaLimitePago(null);
        Venta cancelada = ventaRepository.save(venta);

        // Cancelar una venta devuelve mercaderia al stock y anula plata cobrada:
        // es de las operaciones que mas conviene poder rastrear despues.
        registroAuditoria.registrar("CANCELAR_VENTA", "ventas", cancelada.getId(),
                "Anulacion de la venta #" + cancelada.getId()
                        + " por Bs " + cancelada.getMontoTotal());

        return new VentaResponse(cancelada);
    }

    /**
     * Marca la entrega de una venta como completada. La pueden hacer ADMIN y
     * EMPLEADO, en cualquier venta no cancelada y sin importar desde qué
     * estado.
     *
     * El saldo pendiente NO bloquea la entrega: el sistema registra lo que
     * pasa en el negocio, y la decisión de entregar con saldo es del dueño.
     * (Antes, RF-07 lo impedía; se quitó por decisión del negocio.)
     */
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

        return new VentaResponse(entregada);
    }

    /**
     * Corrige una entrega ya marcada: ENTREGADO -> PENDIENTE. Solo ADMIN: lo
     * exige el controlador. Queda registrado en la auditoría. Si la venta
     * vuelve a PENDIENTE, vuelve a ofrecerse marcarla como entregada.
     *
     * En una venta en tienda (RETIRO) no se ofrece: siempre es ENTREGADO.
     */
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

        return new VentaResponse(actualizada);
    }

    /**
     * Completa o corrige los datos de entrega de una venta ya registrada:
     * dirección (DOMICILIO o TRANSPORTADORA) y transportadora y guía (solo
     * TRANSPORTADORA). Solo ADMIN: lo exige el controlador.
     *
     * Un valor vacío borra el dato. No toca la ciudad ni el estado.
     */
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

        return new VentaResponse(actualizada);
    }

    /**
     * Pone o cambia la fecha límite del pago pendiente de una venta ya
     * registrada (ADMIN y EMPLEADO). Solo si todavía hay saldo pendiente.
     */
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

        return new VentaResponse(actualizada);
    }

    /**
     * La fecha límite sale del reloj del SERVIDOR, nunca del aparato del
     * usuario: si vienen días, es hoy más esos días; si viene una fecha, no
     * puede ser anterior a hoy. Si no viene nada, no hay fecha (null).
     */
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

    /**
     * Estado de entrega con el que nace una venta.
     *
     *   RETIRO:         siempre ENTREGADO (el cliente se lleva el producto en el momento).
     *   sin indicar:    PENDIENTE.
     *   PENDIENTE:      cualquier modalidad.
     *   ENTREGADO:      DOMICILIO o TRANSPORTADORA, ADMIN y EMPLEADO.
     */
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