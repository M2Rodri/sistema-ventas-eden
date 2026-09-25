package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.CompraRequest;
import com.mitienda.ecommerce.dto.CompraResponse;
import com.mitienda.ecommerce.models.*;
import com.mitienda.ecommerce.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de compras
 */
@Service
// Lectura dentro de transacción por defecto: con spring.jpa.open-in-view=false
// no hay sesión de Hibernate fuera de la transacción, y los DTO de respuesta se
// arman recorriendo relaciones perezosas. Sin esto, los endpoints de lectura
// fallaban con LazyInitializationException.
// Los métodos que escriben llevan su propio @Transactional, que tiene precedencia.
@Transactional(readOnly = true)
public class CompraService {

    private final CompraRepository compraRepository;

    private final DetalleCompraRepository detalleCompraRepository;

    private final ProveedorRepository proveedorRepository;

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
    public CompraService(CompraRepository compraRepository,
                         DetalleCompraRepository detalleCompraRepository,
                         ProveedorRepository proveedorRepository,
                         ProductoRepository productoRepository,
                         UsuarioRepository usuarioRepository,
                         InventarioService inventarioService,
                         InventarioRepository inventarioRepository,
                         RegistroAuditoria registroAuditoria,
                         UsuarioActualService usuarioActualService) {
        this.compraRepository = compraRepository;
        this.detalleCompraRepository = detalleCompraRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.inventarioService = inventarioService;
        this.inventarioRepository = inventarioRepository;
        this.registroAuditoria = registroAuditoria;
        this.usuarioActualService = usuarioActualService;
    }


    /**
     * Listar todas las compras
     */
    public List<CompraResponse> getAllCompras() {
        return compraRepository.findAll()
                .stream()
                .map(CompraResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener compra por ID
     */
    public CompraResponse getCompraById(Long id) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada con ID: " + id));
        return new CompraResponse(compra);
    }

    /**
     * Crear nueva compra
     */
    @Transactional
    public CompraResponse createCompra(CompraRequest request) {
        // Validar proveedor
        Proveedor proveedor = proveedorRepository.findById(request.getIdProveedor())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + request.getIdProveedor()));

        // La base bloquea repetir número de factura para el mismo proveedor
        // (uq_compras_proveedor_factura); se avisa antes para no mostrar el
        // error técnico de la base.
        if (request.getNumeroFactura() != null && !request.getNumeroFactura().isBlank()
                && compraRepository.existsByProveedorIdAndNumeroFactura(
                        request.getIdProveedor(), request.getNumeroFactura())) {
            throw new RuntimeException("Ya existe una compra con esa factura para este proveedor");
        }

        // El usuario sale del token, no de lo que mande el cliente: ver
        // UsuarioActualService.
        Usuario usuario = usuarioActualService.obtenerRequerido();

        // Crear compra
        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setUsuario(usuario);
        compra.setNotas(request.getNotas());
        compra.setEstado(EstadoCompra.POR_CONFIRMAR);
        compra.setSubtotal(BigDecimal.ZERO);
        compra.setDescuento(BigDecimal.ZERO);
        compra.setMontoTotal(BigDecimal.ZERO);
        compra.setNumeroFactura(request.getNumeroFactura());

        // Guardar compra primero
        Compra savedCompra = compraRepository.save(compra);

        // Agregar detalles
        BigDecimal total = BigDecimal.ZERO;
        for (CompraRequest.ItemCompraRequest item : request.getItems()) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + item.getIdProducto()));

            // Crear detalle
            DetalleCompra detalle = new DetalleCompra();
            detalle.setCompra(savedCompra);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(item.getPrecioUnitario());
            detalle.calcularSubtotal();

            detalleCompraRepository.save(detalle);

            total = total.add(detalle.getSubtotal());
        }

        // Actualizar total de la compra
        savedCompra.setSubtotal(total);
        savedCompra.setMontoTotal(total);
        Compra finalCompra = compraRepository.save(savedCompra);

        registroAuditoria.registrar("CREAR_COMPRA", "compras", finalCompra.getId(),
                "Compra por Bs " + finalCompra.getMontoTotal()
                        + " a " + (finalCompra.getProveedor() != null
                                ? finalCompra.getProveedor().getNombreEmpresa() : "proveedor sin nombre"));

        return new CompraResponse(finalCompra);
    }

    /**
     * Editar una compra que todavía no fue confirmada. Una vez CONFIRMADA ya
     * actualizó stock y costo del producto, y CANCELADA es un callejón sin
     * salida — en los dos casos, editar la desincronizaría de lo que ya
     * pasó. Reemplaza proveedor, factura, notas y todos los productos.
     */
    @Transactional
    public CompraResponse updateCompra(Long id, CompraRequest request) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada con ID: " + id));

        if (compra.getEstado() != EstadoCompra.POR_CONFIRMAR) {
            throw new RuntimeException("Solo se puede editar una compra que esté sin confirmar. Estado actual: " + compra.getEstado());
        }

        Proveedor proveedor = proveedorRepository.findById(request.getIdProveedor())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + request.getIdProveedor()));

        if (request.getNumeroFactura() != null && !request.getNumeroFactura().isBlank()
                && compraRepository.existsByProveedorIdAndNumeroFacturaAndIdNot(
                        request.getIdProveedor(), request.getNumeroFactura(), id)) {
            throw new RuntimeException("Ya existe una compra con esa factura para este proveedor");
        }

        compra.setProveedor(proveedor);
        compra.setNumeroFactura(request.getNumeroFactura());
        compra.setNotas(request.getNotas());

        // Se reemplazan los productos enteros: se borran los de antes y se
        // cargan los nuevos, más simple y menos propenso a error que tratar
        // de calcular qué línea cambió, cuál es nueva y cuál se borró.
        // flush() fuerza el DELETE a la base ya, porque si no Hibernate lo
        // deja para el final (después de los INSERT de abajo) y choca con la
        // restricción de unicidad cuando un producto se mantiene en la lista.
        detalleCompraRepository.deleteByCompraId(id);
        detalleCompraRepository.flush();

        BigDecimal total = BigDecimal.ZERO;
        for (CompraRequest.ItemCompraRequest item : request.getItems()) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + item.getIdProducto()));

            DetalleCompra detalle = new DetalleCompra();
            detalle.setCompra(compra);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(item.getPrecioUnitario());
            detalle.calcularSubtotal();

            detalleCompraRepository.save(detalle);
            total = total.add(detalle.getSubtotal());
        }

        compra.setSubtotal(total);
        compra.setMontoTotal(total);
        Compra updatedCompra = compraRepository.save(compra);

        registroAuditoria.registrar("EDITAR_COMPRA", "compras", updatedCompra.getId(),
                "Edición de compra #" + updatedCompra.getId());

        return new CompraResponse(updatedCompra);
    }

    /**
     * Cambiar estado de la compra
     */
    @Transactional
    public CompraResponse cambiarEstadoCompra(Long id, EstadoCompra nuevoEstado) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada con ID: " + id));

        // Solo se puede confirmar una compra que sigue POR_CONFIRMAR. Sin este
        // chequeo, una compra ya CANCELADA se podía "confirmar" igual: sumaba
        // stock y pisaba el precio de compra con los datos de una compra
        // que se suponía anulada. Confirmar de nuevo una ya CONFIRMADA tampoco
        // tiene sentido (duplicaría el stock).
        if (nuevoEstado == EstadoCompra.CONFIRMADA && compra.getEstado() != EstadoCompra.POR_CONFIRMAR) {
            throw new RuntimeException("Solo se puede confirmar una compra que esté sin confirmar. Estado actual: " + compra.getEstado());
        }

        // Si el estado cambia a CONFIRMADA, la mercadería entra al inventario.
        // Cada entrada queda registrada como movimiento, igual que hace la
        // venta con las salidas: si mañana el stock no cuadra, la entrada por
        // compra tiene que ser rastreable. Sin esto, el stock subía en silencio.
        if (nuevoEstado == EstadoCompra.CONFIRMADA) {
            Long idUsuario = compra.getUsuario() != null ? compra.getUsuario().getId() : null;

            for (DetalleCompra detalle : compra.getDetalles()) {
                Long idProducto = detalle.getProducto().getId();

                Integer cantidadAnterior = inventarioRepository.findByProductoId(idProducto)
                        .map(Inventario::getCantidadDisponible)
                        .orElse(0);

                inventarioService.aumentarStock(idProducto, detalle.getCantidad());

                inventarioService.registrarAjusteAutomatico(
                        idProducto,
                        cantidadAnterior,
                        cantidadAnterior + detalle.getCantidad(),
                        "COMPRA",
                        "Compra #" + compra.getId()
                                + (compra.getNumeroFactura() != null
                                        ? " - Factura " + compra.getNumeroFactura() : ""),
                        idUsuario
                );

                // El precio de compra del producto se actualiza con lo que
                // realmente se pagó esta vez. Sin esto, "Valor Total del
                // Inventario" y el costo que se ve en Productos se quedan
                // con el precio viejo para siempre, aunque el proveedor
                // suba o baje sus precios y eso se registre acá.
                Producto producto = detalle.getProducto();
                producto.setPrecioCompra(detalle.getPrecioUnitario());
                productoRepository.save(producto);
            }
        }

        EstadoCompra estadoAnterior = compra.getEstado();
        compra.setEstado(nuevoEstado);
        Compra updatedCompra = compraRepository.save(compra);

        // Interesa el cambio de estado y no solo el estado final, porque pasar a
        // CONFIRMADA es lo que sube el stock: si manana el inventario no cuadra,
        // aca queda quien lo confirmo y cuando.
        registroAuditoria.registrar("CAMBIAR_ESTADO_COMPRA", "compras", updatedCompra.getId(),
                "Compra #" + updatedCompra.getId() + ": " + estadoAnterior + " -> " + nuevoEstado);

        return new CompraResponse(updatedCompra);
    }

    /**
     * Confirmar compra y actualizar inventario
     */
    @Transactional
    public CompraResponse recibirCompra(Long id) {
        return cambiarEstadoCompra(id, EstadoCompra.CONFIRMADA);
    }

    /**
     * Cancelar compra
     */
    @Transactional
    public CompraResponse cancelarCompra(Long id) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada con ID: " + id));

        // Solo se puede cancelar si está POR_CONFIRMAR
        if (compra.getEstado() != EstadoCompra.POR_CONFIRMAR) {
            throw new RuntimeException("No se puede cancelar una compra en estado: " + compra.getEstado());
        }

        compra.setEstado(EstadoCompra.CANCELADA);
        Compra updatedCompra = compraRepository.save(compra);
        return new CompraResponse(updatedCompra);
    }

}