package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.exception.ConflictoEstadoException;
import com.mitienda.ecommerce.exception.PeticionInvalidaException;
import com.mitienda.ecommerce.exception.RecursoNoEncontradoException;
import com.mitienda.ecommerce.exception.ReglaNegocioException;
import com.mitienda.ecommerce.dto.ProductoRequest;
import com.mitienda.ecommerce.dto.ProductoResponse;
import com.mitienda.ecommerce.models.Categoria;
import com.mitienda.ecommerce.models.EstadoCompra;
import com.mitienda.ecommerce.models.Inventario;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.models.TipoProducto;
import com.mitienda.ecommerce.repositories.CategoriaRepository;
import com.mitienda.ecommerce.repositories.DetalleCompraRepository;
import com.mitienda.ecommerce.repositories.InventarioRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Servicio para gestión de productos */
@Service
// Lectura en transacción: con open-in-view=false no hay sesión de Hibernate fuera de ella.
@Transactional(readOnly = true)
public class ProductoService {
    private final ProductoRepository productoRepository;

    private final CategoriaRepository categoriaRepository;

    private final InventarioRepository inventarioRepository;

    private final RegistroAuditoria registroAuditoria;

    private final UsuarioActualService usuarioActualService;

    private final InventarioService inventarioService;

    private final DetalleCompraRepository detalleCompraRepository;

    public ProductoService(ProductoRepository productoRepository,
                           CategoriaRepository categoriaRepository,
                           InventarioRepository inventarioRepository,
                           RegistroAuditoria registroAuditoria,
                           UsuarioActualService usuarioActualService,
                           InventarioService inventarioService,
                           DetalleCompraRepository detalleCompraRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.inventarioRepository = inventarioRepository;
        this.registroAuditoria = registroAuditoria;
        this.usuarioActualService = usuarioActualService;
        this.inventarioService = inventarioService;
        this.detalleCompraRepository = detalleCompraRepository;
    }

    /** Las lecturas no envían precioCompra; marca además si el producto ya tuvo compras confirmadas. */
    private ProductoResponse ocultarCostoSiNoEsAdmin(ProductoResponse response) {
        response.setTieneComprasConfirmadas(
                detalleCompraRepository.existsByProducto_IdAndCompra_Estado(response.getId(), EstadoCompra.CONFIRMADA));
        if (!usuarioActualService.esAdmin()) {
            response.setPrecioCompra(null);
        }
        return response;
    }

    /** Convierte la lista a respuestas sin consultas por fila. */
    private List<ProductoResponse> aResponses(List<Producto> productos) {
        boolean esAdmin = usuarioActualService.esAdmin();
        Set<Long> conCompraConfirmada = new HashSet<>(
                detalleCompraRepository.findIdsProductoPorEstadoCompra(EstadoCompra.CONFIRMADA));
        return productos.stream().map(p -> {
            ProductoResponse response = new ProductoResponse(p);
            response.setTieneComprasConfirmadas(conCompraConfirmada.contains(p.getId()));
            if (!esAdmin) {
                response.setPrecioCompra(null);
            }
            return response;
        }).collect(Collectors.toList());
    }

    /** Listar todos los productos */
    @Transactional(readOnly = true)
    public List<ProductoResponse> getAllProductos() {
        return aResponses(productoRepository.findAll());
    }

    /** Listar solo productos activos */
    @Transactional(readOnly = true)
    public List<ProductoResponse> getActiveProductos() {
        return aResponses(productoRepository.findByActivoTrue());
    }

    /** Obtener producto por ID */
    @Transactional(readOnly = true)
    public ProductoResponse getProductoById(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado con ID: " + id));
        return ocultarCostoSiNoEsAdmin(new ProductoResponse(producto));
    }

    /** Obtener producto por SKU */
    @Transactional(readOnly = true)
    public ProductoResponse getProductoBySku(String sku) {
        Producto producto = productoRepository.findBySku(sku)
                .orElseThrow(() -> new RecursoNoEncontradoException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado con SKU: " + sku));
        return ocultarCostoSiNoEsAdmin(new ProductoResponse(producto));
    }

    /** Crear nuevo producto con inventario automático */
    @Transactional
    public ProductoResponse createProducto(ProductoRequest request) {
        if (productoRepository.existsBySku(request.getSku())) {
            throw new ConflictoEstadoException("SKU_DUPLICADO", "Ya existe un producto con el SKU: " + request.getSku());
        }

        Categoria categoria = categoriaRepository.findById(request.getIdCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException("CATEGORIA_NO_ENCONTRADA", "Categoría no encontrada con ID: " + request.getIdCategoria()));

        if (!categoria.getActivo()) {
            throw new ReglaNegocioException("CATEGORIA_INACTIVA", "No se puede crear un producto en una categoría inactiva");
        }

        Producto producto = new Producto();
        producto.setSku(request.getSku());
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setModelo(request.getModelo());
        producto.setMarca(request.getMarca());
        producto.setFirmeza(request.getFirmeza());
        producto.setMaterialNucleo(request.getMaterialNucleo());
        producto.setColor(request.getColor());
        producto.setMaterialArmazon(request.getMaterialArmazon());
        producto.setCategoria(categoria);
        producto.setCalidad(request.getCalidad());
        producto.setPrecioCompra(request.getPrecioCompra());
        producto.setPrecioVenta(request.getPrecioVenta());
        producto.setDimensiones(request.getDimensiones());
        producto.setStockMinimo(request.getStockMinimo());
        // La categoría define el tipo, nunca el cliente: evita productos con categoría y tipo desincronizados.
        producto.setTipoProducto(categoria.getTipoProducto());
        producto.setActivo(request.getActivo());

        Producto savedProducto = productoRepository.save(producto);

        int stockInicial = request.getStockInicial() != null ? request.getStockInicial() : 0;
        Inventario inventario = new Inventario();
        inventario.setProducto(savedProducto);
        inventario.setCantidadDisponible(stockInicial);
        inventarioRepository.save(inventario);

        // Con stock inicial se deja un movimiento AJUSTE_INICIAL a nombre de quien dio de alta el producto.
        if (stockInicial > 0) {
            inventarioService.registrarAjusteAutomatico(
                    savedProducto.getId(), 0, stockInicial, "AJUSTE_INICIAL",
                    "Stock inicial al crear el producto",
                    usuarioActualService.obtenerRequerido().getId());
            inventarioService.sincronizarAlerta(savedProducto.getId());
        }

        registroAuditoria.registrar("CREAR_PRODUCTO", "productos", savedProducto.getId(),
                "Alta de " + savedProducto.getSku() + " - " + savedProducto.getNombre()
                        + (stockInicial > 0 ? " con stock inicial " + stockInicial : ""));

        return ocultarCostoSiNoEsAdmin(new ProductoResponse(savedProducto));
    }

    /** Actualizar producto existente */
    @Transactional
    public ProductoResponse updateProducto(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado con ID: " + id));

        if (!producto.getSku().equals(request.getSku()) &&
            productoRepository.existsBySku(request.getSku())) {
            throw new ConflictoEstadoException("SKU_DUPLICADO", "Ya existe un producto con el SKU: " + request.getSku());
        }

        Categoria categoria = categoriaRepository.findById(request.getIdCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException("CATEGORIA_NO_ENCONTRADA", "Categoría no encontrada con ID: " + request.getIdCategoria()));

        producto.setSku(request.getSku());
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setModelo(request.getModelo());
        producto.setMarca(request.getMarca());
        producto.setFirmeza(request.getFirmeza());
        producto.setMaterialNucleo(request.getMaterialNucleo());
        producto.setColor(request.getColor());
        producto.setMaterialArmazon(request.getMaterialArmazon());
        producto.setCategoria(categoria);
        producto.setCalidad(request.getCalidad());
        // Tras una compra confirmada, precioCompra lo maneja Compras; esto cubre las llamadas directas a la API.
        boolean yaConfirmoCompra = detalleCompraRepository
                .existsByProducto_IdAndCompra_Estado(producto.getId(), EstadoCompra.CONFIRMADA);
        if (!yaConfirmoCompra) {
            producto.setPrecioCompra(request.getPrecioCompra());
        }
        producto.setPrecioVenta(request.getPrecioVenta());
        producto.setDimensiones(request.getDimensiones());
        producto.setStockMinimo(request.getStockMinimo());
        // Igual que al crear: el tipo lo define la categoría.
        producto.setTipoProducto(categoria.getTipoProducto());
        producto.setActivo(request.getActivo());

        Producto updatedProducto = productoRepository.save(producto);

        registroAuditoria.registrar("ACTUALIZAR_PRODUCTO", "productos", updatedProducto.getId(),
                "Edicion de " + updatedProducto.getSku() + " - " + updatedProducto.getNombre());

        return ocultarCostoSiNoEsAdmin(new ProductoResponse(updatedProducto));
    }

    /** Cambia solo el stock mínimo (0 = sin alerta); es lo único que necesita la pantalla de inventario. */
    @Transactional
    public ProductoResponse actualizarStockMinimo(Long id, Integer stockMinimo) {
        if (stockMinimo == null || stockMinimo < 0) {
            throw new PeticionInvalidaException("STOCK_MINIMO_INVALIDO", "El stock mínimo no puede ser negativo");
        }

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado con ID: " + id));

        Integer anterior = producto.getStockMinimo();
        producto.setStockMinimo(stockMinimo);
        Producto guardado = productoRepository.save(producto);

        // El mínimo decide si hay alerta: se revisa para crearla o cerrarla de inmediato.
        inventarioService.sincronizarAlerta(guardado.getId());

        registroAuditoria.registrar("ACTUALIZAR_STOCK_MINIMO", "productos", guardado.getId(),
                "Stock minimo de " + guardado.getSku() + ": " + anterior + " -> " + stockMinimo);

        return ocultarCostoSiNoEsAdmin(new ProductoResponse(guardado));
    }

    /** Eliminar producto (desactivar) */
    @Transactional
    public void deleteProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado con ID: " + id));

        producto.setActivo(false);
        productoRepository.save(producto);

        // Baja lógica: ventas viejas referencian el producto. Se audita como eliminación.
        registroAuditoria.registrar("ELIMINAR_PRODUCTO", "productos", producto.getId(),
                "Baja de " + producto.getSku() + " - " + producto.getNombre());
    }

    /** Activar/Desactivar producto */
    @Transactional
    public ProductoResponse toggleProductoStatus(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado con ID: " + id));

        producto.setActivo(!producto.getActivo());
        Producto updatedProducto = productoRepository.save(producto);

        registroAuditoria.registrar(
                Boolean.TRUE.equals(updatedProducto.getActivo()) ? "ACTIVAR_PRODUCTO" : "DESACTIVAR_PRODUCTO",
                "productos", updatedProducto.getId(),
                updatedProducto.getSku() + " - " + updatedProducto.getNombre());

        return ocultarCostoSiNoEsAdmin(new ProductoResponse(updatedProducto));
    }

    /** Filtrar productos por categoría */
    @Transactional(readOnly = true)
    public List<ProductoResponse> getProductosByCategoria(Long categoriaId) {
        return aResponses(productoRepository.findByCategoriaId(categoriaId));
    }

    /** Buscar productos por nombre */
    @Transactional(readOnly = true)
    public List<ProductoResponse> searchProductos(String nombre) {
        return aResponses(productoRepository.searchByNombre(nombre));
    }

    /** Contar productos activos */
    @Transactional(readOnly = true)
    public Long countActiveProductos() {
        return productoRepository.countByActivo(true);
    }

    /** Contar productos por categoría */
    @Transactional(readOnly = true)
    public Long countProductosByCategoria(Long categoriaId) {
        return productoRepository.countByCategoriaId(categoriaId);
    }
}