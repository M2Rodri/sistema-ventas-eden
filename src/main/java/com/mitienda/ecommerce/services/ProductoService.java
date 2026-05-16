// src/main/java/com/mitienda/ecommerce/services/ProductoService.java
package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ProductoRequest;
import com.mitienda.ecommerce.dto.ProductoResponse;
import com.mitienda.ecommerce.models.Categoria;
import com.mitienda.ecommerce.models.Inventario;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.models.TipoProducto;
import com.mitienda.ecommerce.repositories.CategoriaRepository;
import com.mitienda.ecommerce.repositories.InventarioRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de productos
 */
@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    // ✅ AGREGADO: Inyección de InventarioRepository
    @Autowired
    private InventarioRepository inventarioRepository;

    /**
     * Listar todos los productos
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> getAllProductos() {
        return productoRepository.findAll()
                .stream()
                .map(ProductoResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Listar solo productos activos
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> getActiveProductos() {
        return productoRepository.findByActivoTrue()
                .stream()
                .map(ProductoResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener producto por ID
     */
    @Transactional(readOnly = true)
    public ProductoResponse getProductoById(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
        return new ProductoResponse(producto);
    }

    /**
     * Obtener producto por SKU
     */
    @Transactional(readOnly = true)
    public ProductoResponse getProductoBySku(String sku) {
        Producto producto = productoRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con SKU: " + sku));
        return new ProductoResponse(producto);
    }

    /**
     * Crear nuevo producto con inventario automático
     * ✅ MODIFICADO: Ahora crea inventario automáticamente
     */
    @Transactional
    public ProductoResponse createProducto(ProductoRequest request) {
        // Validar que el SKU no exista
        if (productoRepository.existsBySku(request.getSku())) {
            throw new RuntimeException("Ya existe un producto con el SKU: " + request.getSku());
        }

        // Validar que la categoría exista
        Categoria categoria = categoriaRepository.findById(request.getIdCategoria())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + request.getIdCategoria()));

        // Validar que la categoría esté activa
        if (!categoria.getActivo()) {
            throw new RuntimeException("No se puede crear un producto en una categoría inactiva");
        }

        // Crear producto
        Producto producto = new Producto();
        producto.setSku(request.getSku());
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setModelo(request.getModelo());
        producto.setCategoria(categoria);
        producto.setCalidad(request.getCalidad());
        producto.setPrecioUnitario(request.getPrecioUnitario());
        producto.setPrecioVenta(request.getPrecioVenta());
        producto.setPeso(request.getPeso());
        producto.setDimensiones(request.getDimensiones());
        producto.setStockMinimo(request.getStockMinimo());
        producto.setTipoProducto(request.getTipoProducto());
        producto.setActivo(request.getActivo());

        Producto savedProducto = productoRepository.save(producto);

        // ✅ CREAR INVENTARIO AUTOMÁTICAMENTE
        Inventario inventario = new Inventario();
        inventario.setProducto(savedProducto);
        inventario.setCantidadDisponible(0); // Stock inicial en 0
        inventario.setUbicacion("Sin asignar"); // Ubicación por defecto
        inventarioRepository.save(inventario);

        return new ProductoResponse(savedProducto);
    }

    /**
     * Actualizar producto existente
     */
    @Transactional
    public ProductoResponse updateProducto(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        // Validar SKU único (si cambió)
        if (!producto.getSku().equals(request.getSku()) &&
            productoRepository.existsBySku(request.getSku())) {
            throw new RuntimeException("Ya existe un producto con el SKU: " + request.getSku());
        }

        // Validar categoría
        Categoria categoria = categoriaRepository.findById(request.getIdCategoria())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + request.getIdCategoria()));

        // Actualizar campos
        producto.setSku(request.getSku());
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setModelo(request.getModelo());
        producto.setCategoria(categoria);
        producto.setCalidad(request.getCalidad());
        producto.setPrecioUnitario(request.getPrecioUnitario());
        producto.setPrecioVenta(request.getPrecioVenta());
        producto.setPeso(request.getPeso());
        producto.setDimensiones(request.getDimensiones());
        producto.setStockMinimo(request.getStockMinimo());
        producto.setTipoProducto(request.getTipoProducto());
        producto.setActivo(request.getActivo());

        Producto updatedProducto = productoRepository.save(producto);
        return new ProductoResponse(updatedProducto);
    }

    /**
     * Eliminar producto (desactivar)
     */
    @Transactional
    public void deleteProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        producto.setActivo(false);
        productoRepository.save(producto);
    }

    /**
     * Activar/Desactivar producto
     */
    @Transactional
    public ProductoResponse toggleProductoStatus(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        producto.setActivo(!producto.getActivo());
        Producto updatedProducto = productoRepository.save(producto);
        return new ProductoResponse(updatedProducto);
    }

    /**
     * Filtrar productos por categoría
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> getProductosByCategoria(Long categoriaId) {
        return productoRepository.findByCategoriaId(categoriaId)
                .stream()
                .map(ProductoResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Filtrar productos por tipo
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> getProductosByTipo(TipoProducto tipo) {
        return productoRepository.findByTipoProducto(tipo)
                .stream()
                .map(ProductoResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Buscar productos por nombre
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> searchProductos(String nombre) {
        return productoRepository.searchByNombre(nombre)
                .stream()
                .map(ProductoResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Contar productos activos
     */
    @Transactional(readOnly = true)
    public Long countActiveProductos() {
        return productoRepository.countByActivo(true);
    }

    /**
     * Contar productos por categoría
     */
    @Transactional(readOnly = true)
    public Long countProductosByCategoria(Long categoriaId) {
        return productoRepository.countByCategoriaId(categoriaId);
    }
}