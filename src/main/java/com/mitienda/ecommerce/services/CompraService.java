package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.CompraRequest;
import com.mitienda.ecommerce.dto.CompraResponse;
import com.mitienda.ecommerce.models.*;
import com.mitienda.ecommerce.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
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
public class CompraService {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private DetalleCompraRepository detalleCompraRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InventarioService inventarioService;

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
        User usuario = userRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));

        // Crear compra
        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setUsuario(usuario);
        compra.setNotas(request.getNotas());
        compra.setEstado(EstadoCompra.PENDIENTE);
        compra.setCostoTotal(BigDecimal.ZERO);

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
        savedCompra.setCostoTotal(total);
        Compra finalCompra = compraRepository.save(savedCompra);

        return new CompraResponse(finalCompra);
    }

    /**
     * Cambiar estado de la compra
     */
    @Transactional
    public CompraResponse cambiarEstadoCompra(Long id, EstadoCompra nuevoEstado) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada con ID: " + id));

        // Si el estado cambia a RECIBIDA, actualizar inventario
        if (nuevoEstado == EstadoCompra.RECIBIDA && compra.getEstado() != EstadoCompra.RECIBIDA) {
            for (DetalleCompra detalle : compra.getDetalles()) {
                inventarioService.aumentarStock(detalle.getProducto().getId(), detalle.getCantidad());
            }
        }

        compra.setEstado(nuevoEstado);
        Compra updatedCompra = compraRepository.save(compra);
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
        BigDecimal total = compraRepository.sumCostoTotalByFechaCompraBetween(inicio, fin);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Contar compras por estado
     */
    public Long countComprasByEstado(EstadoCompra estado) {
        return compraRepository.countByEstado(estado);
    }
}