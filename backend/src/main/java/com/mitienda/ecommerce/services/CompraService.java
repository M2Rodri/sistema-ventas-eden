package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.exception.ConflictoEstadoException;
import com.mitienda.ecommerce.exception.RecursoNoEncontradoException;
import com.mitienda.ecommerce.exception.ReglaNegocioException;
import com.mitienda.ecommerce.dto.CompraRequest;
import com.mitienda.ecommerce.dto.CompraResponse;
import com.mitienda.ecommerce.models.*;
import com.mitienda.ecommerce.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/** Servicio para gestión de compras */
@Service
// Lectura en transacción: con open-in-view=false no hay sesión de Hibernate fuera de ella.
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

    /** Listar todas las compras */
    public List<CompraResponse> getAllCompras() {
        return compraRepository.findAll()
                .stream()
                .map(CompraResponse::new)
                .collect(Collectors.toList());
    }

    /** Obtener compra por ID */
    public CompraResponse getCompraById(Long id) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("COMPRA_NO_ENCONTRADA", "Compra no encontrada con ID: " + id));
        return new CompraResponse(compra);
    }

    /** Registra una compra ya hecha (entra la mercadería y se actualiza el costo). */
    @Transactional
    public CompraResponse createCompra(CompraRequest request) {
        // El proveedor es opcional; si viene, tiene que existir.
        Proveedor proveedor = null;
        if (request.getIdProveedor() != null) {
            proveedor = proveedorRepository.findById(request.getIdProveedor())
                    .orElseThrow(() -> new RecursoNoEncontradoException("PROVEEDOR_NO_ENCONTRADO", "Proveedor no encontrado con ID: " + request.getIdProveedor()));
        }

        // Se avisa antes de que la base rechace una factura repetida del mismo proveedor (uq_compras_proveedor_factura).
        if (proveedor != null && request.getNumeroFactura() != null && !request.getNumeroFactura().isBlank()
                && compraRepository.existsByProveedorIdAndNumeroFactura(
                        request.getIdProveedor(), request.getNumeroFactura())) {
            throw new ConflictoEstadoException("FACTURA_DUPLICADA", "Ya existe una compra con esa factura para este proveedor");
        }

        // El usuario sale del token (ver UsuarioActualService).
        Usuario usuario = usuarioActualService.obtenerRequerido();

        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setUsuario(usuario);
        compra.setNotas(request.getNotas());
        compra.setEstado(EstadoCompra.CONFIRMADA);
        compra.setSubtotal(BigDecimal.ZERO);
        compra.setDescuento(BigDecimal.ZERO);
        compra.setMontoTotal(BigDecimal.ZERO);
        compra.setNumeroFactura(request.getNumeroFactura());

        Compra savedCompra = compraRepository.save(compra);

        // Agregar detalles
        BigDecimal total = BigDecimal.ZERO;
        for (CompraRequest.ItemCompraRequest item : request.getItems()) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RecursoNoEncontradoException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado con ID: " + item.getIdProducto()));

            DetalleCompra detalle = new DetalleCompra();
            detalle.setCompra(savedCompra);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(item.getPrecioUnitario());
            detalle.calcularSubtotal();

            detalleCompraRepository.save(detalle);
            savedCompra.getDetalles().add(detalle);
            entrarAlInventario(savedCompra, detalle, usuario.getId());

            total = total.add(detalle.getSubtotal());
        }

        savedCompra.setSubtotal(total);
        savedCompra.setMontoTotal(total);
        Compra finalCompra = compraRepository.save(savedCompra);

        registroAuditoria.registrar("CREAR_COMPRA", "compras", finalCompra.getId(),
                "Compra por Bs " + finalCompra.getMontoTotal()
                        + " a " + (finalCompra.getProveedor() != null
                                ? finalCompra.getProveedor().getNombreEmpresa() : "proveedor sin nombre"));

        return new CompraResponse(finalCompra);
    }

    /** Suma lo comprado al stock, lo deja como movimiento y actualiza el costo del producto. */
    private void entrarAlInventario(Compra compra, DetalleCompra detalle, Long idUsuario) {
        Long idProducto = detalle.getProducto().getId();

        Integer cantidadAnterior = inventarioRepository.findByProductoId(idProducto)
                .map(Inventario::getCantidadDisponible)
                .orElse(0);

        inventarioService.aumentarStock(idProducto, detalle.getCantidad());

        // Cada entrada queda como movimiento, igual que las salidas por venta.
        inventarioService.registrarAjusteAutomatico(
                idProducto,
                cantidadAnterior,
                cantidadAnterior + detalle.getCantidad(),
                "ENTRADA",
                "Compra #" + compra.getId()
                        + (compra.getNumeroFactura() != null ? " - Factura " + compra.getNumeroFactura() : ""),
                idUsuario
        );

        // El costo del producto pasa a ser lo pagado esta vez, para que el valor del inventario no use el precio viejo.
        Producto producto = detalle.getProducto();
        producto.setPrecioCompra(detalle.getPrecioUnitario());
        productoRepository.save(producto);
    }

    /** Anula una compra y descuenta lo ingresado; si parte ya se vendió, no se puede anular. */
    @Transactional
    public CompraResponse cancelarCompra(Long id) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("COMPRA_NO_ENCONTRADA", "Compra no encontrada con ID: " + id));

        if (compra.getEstado() == EstadoCompra.CANCELADA) {
            throw new ConflictoEstadoException("COMPRA_YA_ANULADA", "Esta compra ya está anulada");
        }

        for (DetalleCompra detalle : compra.getDetalles()) {
            Producto producto = detalle.getProducto();
            Integer disponible = inventarioRepository.findByProductoId(producto.getId())
                    .map(Inventario::getCantidadDisponible)
                    .orElse(0);
            if (disponible < detalle.getCantidad()) {
                throw new ReglaNegocioException("COMPRA_STOCK_VENDIDO", "No se puede anular: de '" + producto.getNombre()
                        + "' esta compra trajo " + detalle.getCantidad() + " y hoy quedan " + disponible
                        + " en stock (el resto ya se vendió)");
            }
        }

        Long idUsuario = usuarioActualService.obtenerRequerido().getId();
        for (DetalleCompra detalle : compra.getDetalles()) {
            Long idProducto = detalle.getProducto().getId();
            Integer cantidadAnterior = inventarioRepository.findByProductoId(idProducto)
                    .map(Inventario::getCantidadDisponible)
                    .orElse(0);

            inventarioService.reducirStock(idProducto, detalle.getCantidad());
            inventarioService.registrarAjusteAutomatico(idProducto, cantidadAnterior,
                    cantidadAnterior - detalle.getCantidad(), "SALIDA",
                    "Anulación de Compra #" + compra.getId(), idUsuario);
        }

        compra.setEstado(EstadoCompra.CANCELADA);
        Compra anulada = compraRepository.save(compra);

        registroAuditoria.registrar("ANULAR_COMPRA", "compras", anulada.getId(),
                "Anulación de compra #" + anulada.getId() + " por Bs " + anulada.getMontoTotal());

        return new CompraResponse(anulada);
    }
}