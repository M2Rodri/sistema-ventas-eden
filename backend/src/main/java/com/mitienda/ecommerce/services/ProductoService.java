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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de productos
 */
@Service
// Lectura dentro de transacción por defecto: con spring.jpa.open-in-view=false
// no hay sesión de Hibernate fuera de la transacción, y los DTO de respuesta se
// arman recorriendo relaciones perezosas. Sin esto, los endpoints de lectura
// fallaban con LazyInitializationException.
// Los métodos que escriben llevan su propio @Transactional, que tiene precedencia.
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;

    private final CategoriaRepository categoriaRepository;

    // ✅ AGREGADO: Inyección de InventarioRepository
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
    public ProductoService(ProductoRepository productoRepository,
                           CategoriaRepository categoriaRepository,
                           InventarioRepository inventarioRepository,
                           RegistroAuditoria registroAuditoria,
                           UsuarioActualService usuarioActualService) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.inventarioRepository = inventarioRepository;
        this.registroAuditoria = registroAuditoria;
        this.usuarioActualService = usuarioActualService;
    }

    /**
     * GET /api/productos y sus variantes son de lectura publica (los usa la
     * tienda sin login) o EMPLEADO. Ninguno de los dos debe recibir
     * costoReferencial: el rol EMPLEADO existe justamente para no ver
     * costos, y un visitante anonimo mucho menos.
     */
    private ProductoResponse ocultarCostoSiNoEsAdmin(ProductoResponse response) {
        if (!usuarioActualService.esAdmin()) {
            response.setCostoReferencial(null);
        }
        return response;
    }


    /**
     * Listar todos los productos
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> getAllProductos() {
        return productoRepository.findAll()
                .stream()
                .map(p -> ocultarCostoSiNoEsAdmin(new ProductoResponse(p)))
                .collect(Collectors.toList());
    }

    /**
     * Listar solo productos activos
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> getActiveProductos() {
        return productoRepository.findByActivoTrue()
                .stream()
                .map(p -> ocultarCostoSiNoEsAdmin(new ProductoResponse(p)))
                .collect(Collectors.toList());
    }

    /**
     * Obtener producto por ID
     */
    @Transactional(readOnly = true)
    public ProductoResponse getProductoById(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
        return ocultarCostoSiNoEsAdmin(new ProductoResponse(producto));
    }

    /**
     * Obtener producto por SKU
     */
    @Transactional(readOnly = true)
    public ProductoResponse getProductoBySku(String sku) {
        Producto producto = productoRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con SKU: " + sku));
        return ocultarCostoSiNoEsAdmin(new ProductoResponse(producto));
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
        producto.setMarca(request.getMarca());
        producto.setFirmeza(request.getFirmeza());
        producto.setMaterialNucleo(request.getMaterialNucleo());
        producto.setCategoria(categoria);
        producto.setCalidad(request.getCalidad());
        producto.setCostoReferencial(request.getCostoReferencial());
        producto.setPrecioVenta(request.getPrecioVenta());
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

        registroAuditoria.registrar("CREAR_PRODUCTO", "productos", savedProducto.getId(),
                "Alta de " + savedProducto.getSku() + " - " + savedProducto.getNombre());

        return ocultarCostoSiNoEsAdmin(new ProductoResponse(savedProducto));
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
        producto.setMarca(request.getMarca());
        producto.setFirmeza(request.getFirmeza());
        producto.setMaterialNucleo(request.getMaterialNucleo());
        producto.setCategoria(categoria);
        producto.setCalidad(request.getCalidad());
        producto.setCostoReferencial(request.getCostoReferencial());
        producto.setPrecioVenta(request.getPrecioVenta());
        producto.setDimensiones(request.getDimensiones());
        producto.setStockMinimo(request.getStockMinimo());
        producto.setTipoProducto(request.getTipoProducto());
        producto.setActivo(request.getActivo());

        Producto updatedProducto = productoRepository.save(producto);

        registroAuditoria.registrar("ACTUALIZAR_PRODUCTO", "productos", updatedProducto.getId(),
                "Edicion de " + updatedProducto.getSku() + " - " + updatedProducto.getNombre());

        return ocultarCostoSiNoEsAdmin(new ProductoResponse(updatedProducto));
    }

    /**
     * Cambia solo el stock mínimo de un producto.
     *
     * Existe como operación aparte porque es lo único que necesita la pantalla
     * de inventario: obligarla a enviar el producto completo (SKU, precios,
     * categoría) para tocar un número sería frágil y arriesga pisar datos.
     *
     * Este valor es el que dispara las alertas de stock bajo.
     */
    @Transactional
    public ProductoResponse actualizarStockMinimo(Long id, Integer stockMinimo) {
        if (stockMinimo == null || stockMinimo < 0) {
            throw new RuntimeException("El stock mínimo debe ser un número positivo");
        }

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        Integer anterior = producto.getStockMinimo();
        producto.setStockMinimo(stockMinimo);
        Producto guardado = productoRepository.save(producto);

        registroAuditoria.registrar("ACTUALIZAR_STOCK_MINIMO", "productos", guardado.getId(),
                "Stock minimo de " + guardado.getSku() + ": " + anterior + " -> " + stockMinimo);

        return ocultarCostoSiNoEsAdmin(new ProductoResponse(guardado));
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

        // Es una baja logica, no un DELETE: el producto sigue en la base porque
        // lo referencian ventas viejas. Igual se audita como eliminacion, que es
        // lo que el usuario percibe.
        registroAuditoria.registrar("ELIMINAR_PRODUCTO", "productos", producto.getId(),
                "Baja de " + producto.getSku() + " - " + producto.getNombre());
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

        registroAuditoria.registrar(
                Boolean.TRUE.equals(updatedProducto.getActivo()) ? "ACTIVAR_PRODUCTO" : "DESACTIVAR_PRODUCTO",
                "productos", updatedProducto.getId(),
                updatedProducto.getSku() + " - " + updatedProducto.getNombre());

        return ocultarCostoSiNoEsAdmin(new ProductoResponse(updatedProducto));
    }

    /**
     * Filtrar productos por categoría
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> getProductosByCategoria(Long categoriaId) {
        return productoRepository.findByCategoriaId(categoriaId)
                .stream()
                .map(p -> ocultarCostoSiNoEsAdmin(new ProductoResponse(p)))
                .collect(Collectors.toList());
    }

    /**
     * Buscar productos por nombre
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> searchProductos(String nombre) {
        return productoRepository.searchByNombre(nombre)
                .stream()
                .map(p -> ocultarCostoSiNoEsAdmin(new ProductoResponse(p)))
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