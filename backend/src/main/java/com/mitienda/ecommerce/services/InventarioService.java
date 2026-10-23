package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.exception.ConflictoEstadoException;
import com.mitienda.ecommerce.exception.PeticionInvalidaException;
import com.mitienda.ecommerce.exception.RecursoNoEncontradoException;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Reglas de stock, ajustes manuales y alertas de inventario. */
@Service
// Lectura en transacción: con open-in-view=false no hay sesión de Hibernate fuera de ella.
@Transactional(readOnly = true)
public class InventarioService {
    private final InventarioRepository inventarioRepository;

    private final AlertaInventarioRepository alertaInventarioRepository;

    private final ProductoRepository productoRepository;

    private final MovimientoInventarioRepository movimientoInventarioRepository;

    private final UsuarioRepository usuarioRepository;

    private final RegistroAuditoria registroAuditoria;

    private final UsuarioActualService usuarioActualService;

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

    /** EMPLEADO también consulta el inventario, pero no debe recibir precioCompra. */
    private InventarioResponse ocultarCostoSiNoEsAdmin(InventarioResponse response) {
        return ocultarCostoSiNoEsAdmin(response, usuarioActualService.esAdmin());
    }

    /** Versión para listados: resuelve esAdmin() una sola vez en vez de una consulta por fila. */
    private InventarioResponse ocultarCostoSiNoEsAdmin(InventarioResponse response, boolean esAdmin) {
        if (!esAdmin) {
            response.setPrecioCompra(null);
        }
        return response;
    }

    @Transactional(readOnly = true)
    public List<InventarioResponse> getAllInventario() {
        Set<Long> conAlertaAtendida = new HashSet<>(
                alertaInventarioRepository.findIdsProductoPorEstado(EstadoAlerta.ATENDIDA_MANUAL));
        boolean esAdmin = usuarioActualService.esAdmin();
        return inventarioRepository.findAll()
                .stream()
                .map(i -> {
                    InventarioResponse response = ocultarCostoSiNoEsAdmin(new InventarioResponse(i), esAdmin);
                    response.setAlertaAtendida(conAlertaAtendida.contains(i.getProducto().getId()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    /** Catálogo con precio y stock para la app (ver InventarioRepository#findCatalogoApp). */
    public List<CatalogoProductoResponse> getCatalogoApp(String nombre, boolean soloBajoMinimo) {
        return inventarioRepository.findCatalogoApp(nombre, soloBajoMinimo)
                .stream()
                .map(CatalogoProductoResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventarioResponse getInventarioById(Long id) {
        Inventario inventario = inventarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("INVENTARIO_NO_ENCONTRADO", "Inventario no encontrado con ID: " + id));
        return ocultarCostoSiNoEsAdmin(new InventarioResponse(inventario));
    }

    @Transactional(readOnly = true)
    public InventarioResponse getInventarioByProducto(Long idProducto) {
        Inventario inventario = inventarioRepository.findByProductoId(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException("INVENTARIO_NO_ENCONTRADO", "No existe inventario para el producto con ID: " + idProducto));
        return ocultarCostoSiNoEsAdmin(new InventarioResponse(inventario));
    }

    @Transactional
    public InventarioResponse createInventario(InventarioRequest request) {
        Producto producto = productoRepository.findById(request.getIdProducto())
                .orElseThrow(() -> new RecursoNoEncontradoException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado con ID: " + request.getIdProducto()));

        if (inventarioRepository.existsByProductoId(request.getIdProducto())) {
            throw new ConflictoEstadoException("INVENTARIO_YA_EXISTE", "Ya existe inventario para este producto");
        }

        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setCantidadDisponible(request.getCantidadDisponible());

        Inventario savedInventario = inventarioRepository.save(inventario);

        verificarYCrearAlerta(savedInventario);

        return ocultarCostoSiNoEsAdmin(new InventarioResponse(savedInventario));
    }

    @Transactional
    public InventarioResponse updateInventario(Long id, InventarioRequest request) {
        Inventario inventario = inventarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("INVENTARIO_NO_ENCONTRADO", "Inventario no encontrado con ID: " + id));

        inventario.setCantidadDisponible(request.getCantidadDisponible());

        Inventario updatedInventario = inventarioRepository.save(inventario);

        verificarYCrearAlerta(updatedInventario);

        return ocultarCostoSiNoEsAdmin(new InventarioResponse(updatedInventario));
    }

    /** Ajuste manual de stock; queda en movimientos_inventario con el usuario del token, nunca uno enviado por el cliente. */
    @Transactional
    public InventarioResponse ajustarInventario(MovimientoInventarioRequest request) {
        Inventario inventario = inventarioRepository.findByProductoId(request.getIdProducto())
                .orElseThrow(() -> new RecursoNoEncontradoException("INVENTARIO_NO_ENCONTRADO", "No existe inventario para el producto con ID: " + request.getIdProducto()));
        Integer cantidadAnterior = inventario.getCantidadDisponible();

        if ("ENTRADA".equalsIgnoreCase(request.getTipoMovimiento())) {
            inventario.aumentarStock(request.getCantidad());
        } else if ("SALIDA".equalsIgnoreCase(request.getTipoMovimiento())) {
            inventario.reducirStock(request.getCantidad());
        } else {
            throw new PeticionInvalidaException("MOVIMIENTO_INVALIDO", "Tipo de movimiento inválido. Use 'ENTRADA' o 'SALIDA'");
        }
        Inventario updatedInventario = inventarioRepository.save(inventario);

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

        verificarYCrearAlerta(updatedInventario);

        // Solo se audita el ajuste manual: los movimientos de ventas y compras ya quedan ligados a su operación.
        registroAuditoria.registrar("AJUSTAR_INVENTARIO", "inventario", updatedInventario.getId(),
                "Ajuste manual de " + inventario.getProducto().getSku() + ": " + cantidadAnterior
                        + " -> " + updatedInventario.getCantidadDisponible()
                        + (request.getMotivo() != null ? ". Motivo: " + request.getMotivo() : ""));

        return ocultarCostoSiNoEsAdmin(new InventarioResponse(updatedInventario));
    }

    /** Movimiento automático de compras y ventas. */
    @Transactional
    public void registrarAjusteAutomatico(Long idProducto, Integer cantidadAnterior, Integer cantidadNueva,
                                      String tipoMovimiento, String motivo, Long idUsuario) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado con ID: " + idProducto));

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

    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> getHistorialAjustes(Long idProducto) {
        return movimientoInventarioRepository.findByProductoIdOrderByFechaDesc(idProducto)
                .stream()
                .map(MovimientoInventarioResponse::new) // <-- Verificar si este constructor accede a .getProducto() o .getUser() perezosamente
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> getUltimosAjustes() {
        return movimientoInventarioRepository.findTop50ByOrderByFechaDesc()
                .stream()
                .map(MovimientoInventarioResponse::new) // <-- Verificar si este constructor accede a .getProducto() o .getUser() perezosamente
                .collect(Collectors.toList());
    }

    @Transactional
    public void reducirStock(Long idProducto, Integer cantidad) {
        Inventario inventario = inventarioRepository.findByProductoId(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException("INVENTARIO_NO_ENCONTRADO", "No existe inventario para el producto con ID: " + idProducto));

        inventario.reducirStock(cantidad);
        inventarioRepository.save(inventario);

        verificarYCrearAlerta(inventario);
    }

    @Transactional
    public void aumentarStock(Long idProducto, Integer cantidad) {
        Inventario inventario = inventarioRepository.findByProductoId(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException("INVENTARIO_NO_ENCONTRADO", "No existe inventario para el producto con ID: " + idProducto));

        inventario.aumentarStock(cantidad);
        inventarioRepository.save(inventario);
    }

    @Transactional(readOnly = true)
    public List<InventarioResponse> getProductosConStockBajo() {
        boolean esAdmin = usuarioActualService.esAdmin();
        return inventarioRepository.findProductosConStockBajo()
                .stream()
                .map(i -> ocultarCostoSiNoEsAdmin(new InventarioResponse(i), esAdmin))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventarioResponse> getProductosSinStock() {
        boolean esAdmin = usuarioActualService.esAdmin();
        return inventarioRepository.findProductosSinStock()
                .stream()
                .map(i -> ocultarCostoSiNoEsAdmin(new InventarioResponse(i), esAdmin))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean verificarDisponibilidad(Long idProducto, Integer cantidad) {
        Inventario inventario = inventarioRepository.findByProductoId(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException("INVENTARIO_NO_ENCONTRADO", "No existe inventario para el producto con ID: " + idProducto));
        return inventario.tieneStock(cantidad);
    }

    public Long countProductosConStockBajo() {
        return inventarioRepository.countProductosConStockBajo();
    }

    @Transactional(readOnly = true)
    public List<AlertaInventarioResponse> getAlertasPendientes() {
        return alertaInventarioRepository.findByEstadoOrderByFechaAlertaDesc(EstadoAlerta.PENDIENTE)
                .stream()
                .map(this::aResponseConDatosEnVivo)
                .collect(Collectors.toList());
    }

    /** Marca la alerta como atendida: deja de mostrarse y no avisa de nuevo mientras siga bajo el mínimo. */
    @Transactional
    public void marcarAlertaAtendida(Long idAlerta) {
        AlertaInventario alerta = alertaInventarioRepository.findById(idAlerta)
                .orElseThrow(() -> new RecursoNoEncontradoException("ALERTA_NO_ENCONTRADA", "Alerta no encontrada con ID: " + idAlerta));
        if (alerta.getEstado() != EstadoAlerta.PENDIENTE) {
            throw new ConflictoEstadoException("ALERTA_YA_ATENDIDA", "Esta alerta ya fue atendida");
        }
        alerta.setEstado(EstadoAlerta.ATENDIDA_MANUAL);
        alertaInventarioRepository.save(alerta);
        registroAuditoria.registrar("ATENDER_ALERTA_STOCK", "alertas_inventario", alerta.getId(),
                "Alerta de stock de " + alerta.getProducto().getSku() + " marcada como atendida");
    }

    /** Reactiva una alerta atendida; si el producto ya no está bajo el mínimo, la cierra. */
    @Transactional
    public void reactivarAlerta(Long idProducto) {
        List<AlertaInventario> atendidas = alertaInventarioRepository
                .findByProductoIdAndEstado(idProducto, EstadoAlerta.ATENDIDA_MANUAL);
        if (atendidas.isEmpty()) {
            throw new RecursoNoEncontradoException("ALERTA_NO_ENCONTRADA", "Este producto no tiene una alerta atendida para reactivar");
        }
        atendidas.forEach(a -> a.setEstado(EstadoAlerta.PENDIENTE));
        alertaInventarioRepository.saveAll(atendidas);
        sincronizarAlerta(idProducto);
    }

    /** Refresca las cantidades congeladas de la alerta con el inventario real. */
    private AlertaInventarioResponse aResponseConDatosEnVivo(AlertaInventario alerta) {
        AlertaInventarioResponse response = new AlertaInventarioResponse(alerta);
        inventarioRepository.findByProductoId(alerta.getProducto().getId()).ifPresent(inv -> {
            response.setCantidadActual(inv.getCantidadDisponible());
            response.setCantidadMinima(inv.getProducto().getStockMinimo());
        });
        return response;
    }

    /** Crea una alerta si el producto quedó bajo el mínimo y no tiene una pendiente. */
    private void verificarYCrearAlerta(Inventario inventario) {
        if (!inventario.estaBajoStockMinimo()) {
            // Con el stock repuesto se cierran las alertas pendientes y las atendidas, para que avise si vuelve a bajar.
            resolverAlertasPendientes(inventario);
            return;
        }
        // Marcada como atendida por el dueño y todavía sin reponer: no se vuelve a avisar.
        if (alertaInventarioRepository.existsByProductoIdAndEstado(
                inventario.getProducto().getId(), EstadoAlerta.ATENDIDA_MANUAL)) {
            return;
        }
        // Evita apilar alertas pendientes del mismo producto con cantidades distintas.
        boolean yaHayAlertaPendiente = alertaInventarioRepository
                .existsByProductoIdAndEstado(inventario.getProducto().getId(), EstadoAlerta.PENDIENTE);
        if (yaHayAlertaPendiente) {
            return;
        }
        AlertaInventario alerta = new AlertaInventario(
            inventario.getProducto(),
            inventario.getCantidadDisponible(),
            inventario.getProducto().getStockMinimo()
        );
        alertaInventarioRepository.save(alerta);
    }

    /** Revisa la alerta tras un cambio externo al servicio (p. ej. editar el stock mínimo). */
    @Transactional
    public void sincronizarAlerta(Long idProducto) {
        inventarioRepository.findByProductoId(idProducto)
                .ifPresent(this::verificarYCrearAlerta);
    }

    /** Cierra las alertas pendientes de un producto cuyo stock ya superó el mínimo. */
    private void resolverAlertasPendientes(Inventario inventario) {
        List<AlertaInventario> pendientes = new ArrayList<>(alertaInventarioRepository
                .findByProductoIdAndEstado(inventario.getProducto().getId(), EstadoAlerta.PENDIENTE));
        pendientes.addAll(alertaInventarioRepository
                .findByProductoIdAndEstado(inventario.getProducto().getId(), EstadoAlerta.ATENDIDA_MANUAL));
        if (pendientes.isEmpty()) {
            return;
        }
        pendientes.forEach(alerta -> alerta.setEstado(EstadoAlerta.ATENDIDA));
        alertaInventarioRepository.saveAll(pendientes);
    }
}
