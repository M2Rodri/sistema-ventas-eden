// src/main/java/com/mitienda/ecommerce/services/InventarioService.java
package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.AjusteInventarioRequest;
import com.mitienda.ecommerce.dto.AjusteInventarioResponse;
import com.mitienda.ecommerce.dto.AlertaInventarioResponse;
import com.mitienda.ecommerce.dto.InventarioRequest;
import com.mitienda.ecommerce.dto.InventarioResponse;
import com.mitienda.ecommerce.models.*;
import com.mitienda.ecommerce.repositories.AjusteInventarioRepository;
import com.mitienda.ecommerce.repositories.AlertaInventarioRepository;
import com.mitienda.ecommerce.repositories.InventarioRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import com.mitienda.ecommerce.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importar esta anotación

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de inventario
 */
@Service
public class InventarioService {

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private AlertaInventarioRepository alertaInventarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private AjusteInventarioRepository ajusteInventarioRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Listar todo el inventario
     * ✅ CORREGIDO: Añadido @Transactional(readOnly = true)
     */
    @Transactional(readOnly = true) // <--- AÑADIDO ESTA ANOTACIÓN
    public List<InventarioResponse> getAllInventario() {
        return inventarioRepository.findAll()
                .stream()
                .map(InventarioResponse::new) // <-- Este constructor accede a .getProducto().getNombre(), etc.
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
        return new InventarioResponse(inventario); // <-- Este constructor también accede a .getProducto()
    }

    /**
     * Obtener inventario por producto
     * ✅ CORREGIDO: Añadido @Transactional(readOnly = true)
     */
    @Transactional(readOnly = true) // <--- AÑADIDO ESTA ANOTACIÓN
    public InventarioResponse getInventarioByProducto(Long idProducto) {
        Inventario inventario = inventarioRepository.findByProductoId(idProducto)
                .orElseThrow(() -> new RuntimeException("No existe inventario para el producto con ID: " + idProducto));
        return new InventarioResponse(inventario); // <-- Este constructor también accede a .getProducto()
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

        return new InventarioResponse(savedInventario);
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

        return new InventarioResponse(updatedInventario);
    }

    /**
     * Ajustar inventario (entrada o salida manual)
     * (Ya tenía @Transactional implícito en el método de escritura)
     */
    @Transactional
    public InventarioResponse ajustarInventario(AjusteInventarioRequest request) {
        Inventario inventario = inventarioRepository.findByProductoId(request.getIdProducto())
                .orElseThrow(() -> new RuntimeException("No existe inventario para el producto con ID: " + request.getIdProducto()));

        // Aplicar ajuste según tipo
        if ("ENTRADA".equalsIgnoreCase(request.getTipoAjuste())) {
            inventario.aumentarStock(request.getCantidad());
        } else if ("SALIDA".equalsIgnoreCase(request.getTipoAjuste())) {
            inventario.reducirStock(request.getCantidad());
        } else {
            throw new RuntimeException("Tipo de ajuste inválido. Use 'ENTRADA' o 'SALIDA'");
        }

        Inventario updatedInventario = inventarioRepository.save(inventario);

        // Verificar si requiere alerta
        verificarYCrearAlerta(updatedInventario);

        return new InventarioResponse(updatedInventario);
    }

    /**
     * Ajustar inventario con registro de auditoría
     * (Ya tenía @Transactional implícito en el método de escritura)
     */
    @Transactional
    public InventarioResponse ajustarInventarioConAuditoria(AjusteInventarioRequest request, Long idUsuario) {
        Inventario inventario = inventarioRepository.findByProductoId(request.getIdProducto())
                .orElseThrow(() -> new RuntimeException("No existe inventario para el producto con ID: " + request.getIdProducto()));
        Integer cantidadAnterior = inventario.getCantidadDisponible();

        // Aplicar ajuste según tipo
        if ("ENTRADA".equalsIgnoreCase(request.getTipoAjuste())) {
            inventario.aumentarStock(request.getCantidad());
        } else if ("SALIDA".equalsIgnoreCase(request.getTipoAjuste())) {
            inventario.reducirStock(request.getCantidad());
        } else {
            throw new RuntimeException("Tipo de ajuste inválido. Use 'ENTRADA' o 'SALIDA'");
        }
        Inventario updatedInventario = inventarioRepository.save(inventario);

        // Registrar auditoría
        User usuario = idUsuario != null ? userRepository.findById(idUsuario).orElse(null) : null;
        String tipoAjuste = request.getTipoAjuste().toUpperCase();

        AjusteInventario ajuste = new AjusteInventario(
            inventario.getProducto(),
            cantidadAnterior,
            inventario.getCantidadDisponible(),
            tipoAjuste,
            request.getMotivo(),
            usuario
        );
        ajusteInventarioRepository.save(ajuste);

        // Verificar si requiere alerta
        verificarYCrearAlerta(updatedInventario);

        return new InventarioResponse(updatedInventario);
    }

    /**
     * Registrar ajuste automático (usado en compras/ventas)
     * (Ya tenía @Transactional implícito en el método de escritura)
     */
    @Transactional
    public void registrarAjusteAutomatico(Long idProducto, Integer cantidadAnterior, Integer cantidadNueva,
                                      String tipoAjuste, String motivo, Long idUsuario) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + idProducto));

        User usuario = idUsuario != null ? userRepository.findById(idUsuario).orElse(null) : null;

        AjusteInventario ajuste = new AjusteInventario(
            producto,
            cantidadAnterior,
            cantidadNueva,
            tipoAjuste,
            motivo,
            usuario
        );
        ajusteInventarioRepository.save(ajuste);
    }

    /**
     * Obtener historial de ajustes de un producto
     * ✅ CORREGIDO: Añadido @Transactional(readOnly = true)
     */
    @Transactional(readOnly = true) // <--- AÑADIDO ESTA ANOTACIÓN
    public List<AjusteInventarioResponse> getHistorialAjustes(Long idProducto) {
        return ajusteInventarioRepository.findByProductoIdOrderByFechaDesc(idProducto)
                .stream()
                .map(AjusteInventarioResponse::new) // <-- Verificar si este constructor accede a .getProducto() o .getUser() perezosamente
                .collect(Collectors.toList());
    }

    /**
     * Obtener últimos ajustes
     * ✅ CORREGIDO: Añadido @Transactional(readOnly = true)
     */
    @Transactional(readOnly = true) // <--- AÑADIDO ESTA ANOTACIÓN
    public List<AjusteInventarioResponse> getUltimosAjustes() {
        return ajusteInventarioRepository.findTop50ByOrderByFechaDesc()
                .stream()
                .map(AjusteInventarioResponse::new) // <-- Verificar si este constructor accede a .getProducto() o .getUser() perezosamente
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
                .map(InventarioResponse::new) // <-- Este constructor accede a .getProducto()
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
                .map(InventarioResponse::new) // <-- Este constructor accede a .getProducto()
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
