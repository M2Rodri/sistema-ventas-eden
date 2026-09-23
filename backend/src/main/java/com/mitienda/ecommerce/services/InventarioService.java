// src/main/java/com/mitienda/ecommerce/services/InventarioService.java
package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.MovimientoInventarioRequest;
import com.mitienda.ecommerce.dto.MovimientoInventarioResponse;
import com.mitienda.ecommerce.dto.AlertaInventarioResponse;
import com.mitienda.ecommerce.dto.CatalogoProductoResponse;
import com.mitienda.ecommerce.dto.InventarioRequest;
import com.mitienda.ecommerce.dto.InventarioResponse;
import com.mitienda.ecommerce.models.*;
import com.mitienda.ecommerce.repositories.MovimientoInventarioRepository;
import com.mitienda.ecommerce.repositories.AlertaInventarioRepository;
import com.mitienda.ecommerce.repositories.InventarioRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importar esta anotación

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de inventario
 */
@Service
// Lectura dentro de transacción por defecto: con spring.jpa.open-in-view=false
// no hay sesión de Hibernate fuera de la transacción, y los DTO de respuesta se
// arman recorriendo relaciones perezosas. Sin esto, los endpoints de lectura
// fallaban con LazyInitializationException.
// Los métodos que escriben llevan su propio @Transactional, que tiene precedencia.
@Transactional(readOnly = true)
public class InventarioService {

    private final InventarioRepository inventarioRepository;

    private final AlertaInventarioRepository alertaInventarioRepository;

    private final ProductoRepository productoRepository;

    private final MovimientoInventarioRepository movimientoInventarioRepository;

    private final UsuarioRepository usuarioRepository;

    private final RegistroAuditoria registroAuditoria;

    private final UsuarioActualService usuarioActualService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public InventarioService(InventarioRepository inventarioRepository,
                             AlertaInventarioRepository alertaInventarioRepository,
                             ProductoRepository productoRepository,
                             MovimientoInventarioRepository movimientoInventarioRepository,
                             UsuarioRepository usuarioRepository,
                             RegistroAuditoria registroAuditoria,
                             UsuarioActualService usuarioActualService) {
        this.inventarioRepository = inventarioRepository;
        this.alertaInventarioRepository = alertaInventarioRepository;
        this.productoRepository = productoRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.registroAuditoria = registroAuditoria;
        this.usuarioActualService = usuarioActualService;
    }

    /**
     * GET /api/inventario y sus variantes las puede llamar EMPLEADO.
     * costoReferencial no debe llegarle: el rol EMPLEADO existe justamente
     * para no ver costos ni margenes.
     */
    private InventarioResponse ocultarCostoSiNoEsAdmin(InventarioResponse response) {
        if (!usuarioActualService.esAdmin()) {
            response.setCostoReferencial(null);
        }
        return response;
    }

    /**
     * Listar todo el inventario
     * ✅ CORREGIDO: Añadido @Transactional(readOnly = true)
     */
    @Transactional(readOnly = true) // <--- AÑADIDO ESTA ANOTACIÓN
    public List<InventarioResponse> getAllInventario() {
        return inventarioRepository.findAll()
                .stream()
                .map(i -> ocultarCostoSiNoEsAdmin(new InventarioResponse(i)))
                .collect(Collectors.toList());
    }

    /**
     * Catálogo con precio y stock para la app móvil (Catálogo, Alertas de
     * stock y Nueva venta). Ver InventarioRepository#findCatalogoApp.
     */
    public List<CatalogoProductoResponse> getCatalogoApp(String nombre, boolean soloBajoMinimo) {
        return inventarioRepository.findCatalogoApp(nombre, soloBajoMinimo)
                .stream()
                .map(CatalogoProductoResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener inventario por ID
     * ✅ CORREGIDO: Añadido @Transactional(readOnly = true)
     */
    @Transactional(readOnly = true) // <--- AÑADIDO ESTA ANOTACIÓN
    public InventarioResponse getInventarioById(Long id) {
        Inventario inventario = inventarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado con ID: " + id));
        return ocultarCostoSiNoEsAdmin(new InventarioResponse(inventario));
    }

    /**
     * Obtener inventario por producto
     * ✅ CORREGIDO: Añadido @Transactional(readOnly = true)
     */
    @Transactional(readOnly = true) // <--- AÑADIDO ESTA ANOTACIÓN
    public InventarioResponse getInventarioByProducto(Long idProducto) {
        Inventario inventario = inventarioRepository.findByProductoId(idProducto)
                .orElseThrow(() -> new RuntimeException("No existe inventario para el producto con ID: " + idProducto));
        return ocultarCostoSiNoEsAdmin(new InventarioResponse(inventario));
    }

    /**
     * Crear inventario para un producto
     * (Ya tenía @Transactional implícito en el método de escritura)
     */
    @Transactional
    public InventarioResponse createInventario(InventarioRequest request) {
        // Validar que el producto exista
        Producto producto = productoRepository.findById(request.getIdProducto())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + request.getIdProducto()));

        // Validar que no exista inventario previo
        if (inventarioRepository.existsByProductoId(request.getIdProducto())) {
            throw new RuntimeException("Ya existe inventario para este producto");
        }

        // Crear inventario
        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setCantidadDisponible(request.getCantidadDisponible());
        inventario.setUbicacion(request.getUbicacion());

        Inventario savedInventario = inventarioRepository.save(inventario);

        // Verificar si requiere alerta
        verificarYCrearAlerta(savedInventario);

        return ocultarCostoSiNoEsAdmin(new InventarioResponse(savedInventario));
    }

    /**
     * Actualizar inventario existente
     * (Ya tenía @Transactional implícito en el método de escritura)
     */
    @Transactional
    public InventarioResponse updateInventario(Long id, InventarioRequest request) {
        Inventario inventario = inventarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado con ID: " + id));

        inventario.setCantidadDisponible(request.getCantidadDisponible());
        inventario.setUbicacion(request.getUbicacion());

        Inventario updatedInventario = inventarioRepository.save(inventario);

        // Verificar si requiere alerta
        verificarYCrearAlerta(updatedInventario);

        return ocultarCostoSiNoEsAdmin(new InventarioResponse(updatedInventario));
    }

    /**
     * Ajustar inventario manualmente (entrada o salida).
     *
     * Antes existían dos caminos: este, que no dejaba rastro, y
     * ajustarInventarioConAuditoria, que sí lo hacía pero confiaba en un
     * idUsuario mandado por el cliente. Ahora hay uno solo: todo ajuste
     * manual queda en movimientos_inventario, con el usuario que sale del
     * token (ver UsuarioActualService), nunca de un parámetro.
     */
    @Transactional
    public InventarioResponse ajustarInventario(MovimientoInventarioRequest request) {
        Inventario inventario = inventarioRepository.findByProductoId(request.getIdProducto())
                .orElseThrow(() -> new RuntimeException("No existe inventario para el producto con ID: " + request.getIdProducto()));
        Integer cantidadAnterior = inventario.getCantidadDisponible();

        // Aplicar movimiento según tipo
        if ("ENTRADA".equalsIgnoreCase(request.getTipoMovimiento())) {
            inventario.aumentarStock(request.getCantidad());
        } else if ("SALIDA".equalsIgnoreCase(request.getTipoMovimiento())) {
            inventario.reducirStock(request.getCantidad());
        } else {
            throw new RuntimeException("Tipo de movimiento inválido. Use 'ENTRADA' o 'SALIDA'");
        }
        Inventario updatedInventario = inventarioRepository.save(inventario);

        // Registrar auditoría
        Usuario usuario = usuarioActualService.obtenerRequerido();
        String tipoMovimiento = request.getTipoMovimiento().toUpperCase();

        MovimientoInventario movimiento = new MovimientoInventario(
            inventario.getProducto(),
            cantidadAnterior,
            inventario.getCantidadDisponible(),
            tipoMovimiento,
            request.getMotivo(),
            usuario
        );
        movimientoInventarioRepository.save(movimiento);

        // Verificar si requiere alerta
        verificarYCrearAlerta(updatedInventario);

        // Solo se audita el ajuste manual. Los movimientos automaticos por venta
        // o por compra ya quedan registrados en movimientos_inventario y ligados
        // a su venta o compra, que a su vez estan auditadas; duplicarlos aca solo
        // llenaria la auditoria de ruido.
        registroAuditoria.registrar("AJUSTAR_INVENTARIO", "inventario", updatedInventario.getId(),
                "Ajuste manual de " + inventario.getProducto().getSku() + ": " + cantidadAnterior
                        + " -> " + updatedInventario.getCantidadDisponible()
                        + (request.getMotivo() != null ? ". Motivo: " + request.getMotivo() : ""));

        return ocultarCostoSiNoEsAdmin(new InventarioResponse(updatedInventario));
    }

    /**
     * Registrar movimiento automático (usado en compras/ventas)
     * (Ya tenía @Transactional implícito en el método de escritura)
     */
    @Transactional
    public void registrarAjusteAutomatico(Long idProducto, Integer cantidadAnterior, Integer cantidadNueva,
                                      String tipoMovimiento, String motivo, Long idUsuario) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + idProducto));

        Usuario usuario = idUsuario != null ? usuarioRepository.findById(idUsuario).orElse(null) : null;

        MovimientoInventario movimiento = new MovimientoInventario(
            producto,
            cantidadAnterior,
            cantidadNueva,
            tipoMovimiento,
            motivo,
            usuario
        );
        movimientoInventarioRepository.save(movimiento);
    }

    /**
     * Obtener historial de ajustes de un producto
     * ✅ CORREGIDO: Añadido @Transactional(readOnly = true)
     */
    @Transactional(readOnly = true) // <--- AÑADIDO ESTA ANOTACIÓN
    public List<MovimientoInventarioResponse> getHistorialAjustes(Long idProducto) {
        return movimientoInventarioRepository.findByProductoIdOrderByFechaDesc(idProducto)
                .stream()
                .map(MovimientoInventarioResponse::new) // <-- Verificar si este constructor accede a .getProducto() o .getUser() perezosamente
                .collect(Collectors.toList());
    }

    /**
     * Obtener últimos ajustes
     * ✅ CORREGIDO: Añadido @Transactional(readOnly = true)
     */
    @Transactional(readOnly = true) // <--- AÑADIDO ESTA ANOTACIÓN
    public List<MovimientoInventarioResponse> getUltimosAjustes() {
        return movimientoInventarioRepository.findTop50ByOrderByFechaDesc()
                .stream()
                .map(MovimientoInventarioResponse::new) // <-- Verificar si este constructor accede a .getProducto() o .getUser() perezosamente
                .collect(Collectors.toList());
    }

    /**
     * Reducir stock (usado en ventas)
     * (Ya tenía @Transactional implícito en el método de escritura)
     */
    @Transactional
    public void reducirStock(Long idProducto, Integer cantidad) {
        Inventario inventario = inventarioRepository.findByProductoId(idProducto)
                .orElseThrow(() -> new RuntimeException("No existe inventario para el producto con ID: " + idProducto));

        inventario.reducirStock(cantidad);
        inventarioRepository.save(inventario);

        // Verificar si requiere alerta
        verificarYCrearAlerta(inventario);
    }

    /**
     * Aumentar stock (usado en compras)
     * (Ya tenía @Transactional implícito en el método de escritura)
     */
    @Transactional
    public void aumentarStock(Long idProducto, Integer cantidad) {
        Inventario inventario = inventarioRepository.findByProductoId(idProducto)
                .orElseThrow(() -> new RuntimeException("No existe inventario para el producto con ID: " + idProducto));

        inventario.aumentarStock(cantidad);
        inventarioRepository.save(inventario);
    }

    /**
     * Productos con stock bajo
     * ✅ CORREGIDO: Añadido @Transactional(readOnly = true)
     */
    @Transactional(readOnly = true) // <--- AÑADIDO ESTA ANOTACIÓN
    public List<InventarioResponse> getProductosConStockBajo() {
        return inventarioRepository.findProductosConStockBajo()
                .stream()
                .map(i -> ocultarCostoSiNoEsAdmin(new InventarioResponse(i)))
                .collect(Collectors.toList());
    }

    /**
     * Productos sin stock
     * ✅ CORREGIDO: Añadido @Transactional(readOnly = true)
     */
    @Transactional(readOnly = true) // <--- AÑADIDO ESTA ANOTACIÓN
    public List<InventarioResponse> getProductosSinStock() {
        return inventarioRepository.findProductosSinStock()
                .stream()
                .map(i -> ocultarCostoSiNoEsAdmin(new InventarioResponse(i)))
                .collect(Collectors.toList());
    }

    /**
     * Verificar disponibilidad de stock
     * (Método de solo lectura que devuelve un booleano, no necesita @Transactional si no accede a relaciones perezosas para serializarlas)
     * PERO, como accede a inventario, y si se llama dentro de un contexto donde se va a serializar, es seguro tenerlo.
     */
    @Transactional(readOnly = true) // <--- AÑADIDO ESTA ANOTACIÓN (buen práctica para métodos de solo lectura que acceden a entidades)
    public boolean verificarDisponibilidad(Long idProducto, Integer cantidad) {
        Inventario inventario = inventarioRepository.findByProductoId(idProducto)
                .orElseThrow(() -> new RuntimeException("No existe inventario para el producto con ID: " + idProducto));
        return inventario.tieneStock(cantidad);
    }

    /**
     * Contar productos con stock bajo
     * (Método de solo lectura que devuelve un Long, no necesita @Transactional)
     */
    public Long countProductosConStockBajo() {
        return inventarioRepository.countProductosConStockBajo();
    }

    /**
     * Listar alertas pendientes
     * ✅ CORREGIDO: Añadido @Transactional(readOnly = true)
     */
    @Transactional(readOnly = true) // <--- AÑADIDO ESTA ANOTACIÓN
    public List<AlertaInventarioResponse> getAlertasPendientes() {
        return alertaInventarioRepository.findByEstadoOrderByFechaAlertaDesc(EstadoAlerta.PENDIENTE)
                .stream()
                .map(AlertaInventarioResponse::new) // <-- Verificar si este constructor accede a .getProducto() perezosamente
                .collect(Collectors.toList());
    }

    /**
     * Marcar alerta como atendida
     * (Ya tenía @Transactional implícito en el método de escritura)
     */
    @Transactional
    public AlertaInventarioResponse marcarAlertaAtendida(Long idAlerta) {
        AlertaInventario alerta = alertaInventarioRepository.findById(idAlerta)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada con ID: " + idAlerta));

        alerta.setEstado(EstadoAlerta.ATENDIDA);
        AlertaInventario updatedAlerta = alertaInventarioRepository.save(alerta);
        return new AlertaInventarioResponse(updatedAlerta);
    }

    /**
     * Verificar y crear alerta si es necesario
     * (Método privado, no es un CU hijo, pero es parte de la lógica de negocio de escritura)
     * No necesita @Transactional, ya que se llama desde un método que sí lo tiene (@Transactional en createInventario, updateInventario, ajustarInventario, etc.).
     */
    private void verificarYCrearAlerta(Inventario inventario) {
        if (inventario.estaBajoStockMinimo()) {
            AlertaInventario alerta = new AlertaInventario(
                inventario.getProducto(),
                inventario.getCantidadDisponible(),
                inventario.getProducto().getStockMinimo()
            );
            alertaInventarioRepository.save(alerta);
        }
    }
}
