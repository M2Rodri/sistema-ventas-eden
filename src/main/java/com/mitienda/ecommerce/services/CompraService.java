package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.CompraRequest;
import com.mitienda.ecommerce.dto.CompraResponse;
import com.mitienda.ecommerce.models.*;
import com.mitienda.ecommerce.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
                         RegistroAuditoria registroAuditoria) {
        this.compraRepository = compraRepository;
        this.detalleCompraRepository = detalleCompraRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.inventarioService = inventarioService;
        this.inventarioRepository = inventarioRepository;
        this.registroAuditoria = registroAuditoria;
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
    public CompraResponse createCompra(CompraRequest request, Long idUsuario) {
        // Validar proveedor
        Proveedor proveedor = proveedorRepository.findById(request.getIdProveedor())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + request.getIdProveedor()));

        // Validar usuario
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));

        // Crear compra
        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setUsuario(usuario);
        compra.setNotas(request.getNotas());
        compra.setEstado(EstadoCompra.PENDIENTE);
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
     * Cambiar estado de la compra
     */
    @Transactional
    public CompraResponse cambiarEstadoCompra(Long id, EstadoCompra nuevoEstado) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada con ID: " + id));

        // Si el estado cambia a RECIBIDA, la mercadería entra al inventario.
        // Cada entrada queda registrada como movimiento, igual que hace la
        // venta con las salidas: si mañana el stock no cuadra, la entrada por
        // compra tiene que ser rastreable. Sin esto, el stock subía en silencio.
        if (nuevoEstado == EstadoCompra.RECIBIDA && compra.getEstado() != EstadoCompra.RECIBIDA) {
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
            }
        }

        EstadoCompra estadoAnterior = compra.getEstado();
        compra.setEstado(nuevoEstado);
        Compra updatedCompra = compraRepository.save(compra);

        // Interesa el cambio de estado y no solo el estado final, porque pasar a
        // RECIBIDA es lo que sube el stock: si manana el inventario no cuadra,
        // aca queda quien lo dio por recibido y cuando.
        registroAuditoria.registrar("CAMBIAR_ESTADO_COMPRA", "compras", updatedCompra.getId(),
                "Compra #" + updatedCompra.getId() + ": " + estadoAnterior + " -> " + nuevoEstado);

        return new CompraResponse(updatedCompra);
    }

    /**
     * Recibir compra y actualizar inventario
     */
    @Transactional
    public CompraResponse recibirCompra(Long id) {
        return cambiarEstadoCompra(id, EstadoCompra.RECIBIDA);
    }

    /**
     * Cancelar compra
     */
    @Transactional
    public CompraResponse cancelarCompra(Long id) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada con ID: " + id));

        // Solo se puede cancelar si está PENDIENTE o CONFIRMADA
        if (compra.getEstado() != EstadoCompra.PENDIENTE && 
            compra.getEstado() != EstadoCompra.CONFIRMADA) {
            throw new RuntimeException("No se puede cancelar una compra en estado: " + compra.getEstado());
        }

        compra.setEstado(EstadoCompra.CANCELADA);
        Compra updatedCompra = compraRepository.save(compra);
        return new CompraResponse(updatedCompra);
    }

    /**
     * Listar compras por proveedor
     */
    public List<CompraResponse> getComprasByProveedor(Long proveedorId) {
        return compraRepository.findByProveedorIdOrderByFechaCompraDesc(proveedorId)
                .stream()
                .map(CompraResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Filtrar compras por estado
     */
    public List<CompraResponse> getComprasByEstado(EstadoCompra estado) {
        return compraRepository.findByEstadoOrderByFechaCompraDesc(estado)
                .stream()
                .map(CompraResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Últimas compras
     */
    public List<CompraResponse> getUltimasCompras() {
        return compraRepository.findTop10ByOrderByFechaCompraDesc()
                .stream()
                .map(CompraResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Compras entre fechas
     */
    public List<CompraResponse> getComprasByFechas(LocalDateTime inicio, LocalDateTime fin) {
        return compraRepository.findByFechaCompraBetweenOrderByFechaCompraDesc(inicio, fin)
                .stream()
                .map(CompraResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Total de compras en un rango de fechas
     */
    public BigDecimal getTotalComprasByFechas(LocalDateTime inicio, LocalDateTime fin) {
        BigDecimal total = compraRepository.sumMontoTotalByFechaCompraBetween(inicio, fin);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Contar compras por estado
     */
    public Long countComprasByEstado(EstadoCompra estado) {
        return compraRepository.countByEstado(estado);
    }
}