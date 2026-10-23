package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.exception.ConflictoEstadoException;
import com.mitienda.ecommerce.exception.RecursoNoEncontradoException;
import com.mitienda.ecommerce.dto.ClienteRequest;
import com.mitienda.ecommerce.dto.ClienteResponse;
import com.mitienda.ecommerce.dto.ClienteConEstadisticasResponse;
import com.mitienda.ecommerce.dto.HistorialComprasResponse;
import com.mitienda.ecommerce.dto.VentaResponse;
import com.mitienda.ecommerce.models.Cliente;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.models.DetalleVenta;
import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.repositories.ClienteRepository;
import com.mitienda.ecommerce.repositories.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Gestión de clientes (CU6). */
@Service
// Lectura en transacción: con open-in-view=false no hay sesión de Hibernate fuera de ella.
@Transactional(readOnly = true)
public class ClienteService {
    private final ClienteRepository clienteRepository;

    private final VentaRepository ventaRepository;

    private final RegistroAuditoria registroAuditoria;

    public ClienteService(ClienteRepository clienteRepository,
                          VentaRepository ventaRepository,
                          RegistroAuditoria registroAuditoria) {
        this.clienteRepository = clienteRepository;
        this.ventaRepository = ventaRepository;
        this.registroAuditoria = registroAuditoria;
    }

    /** Listar todos los clientes */
    @Transactional(readOnly = true)
    public List<ClienteResponse> getAllClientes() {
        return clienteRepository.findAll()
                .stream()
                .map(ClienteResponse::new)
                .collect(Collectors.toList());
    }

    /** Clientes con sus estadísticas de compra, en una sola consulta agregada. */
    @Transactional(readOnly = true)
    public List<ClienteConEstadisticasResponse> getAllClientesConEstadisticas() {
        Map<Long, Object[]> statsPorCliente = ventaRepository.aggregarEstadisticasPorCliente().stream()
                .collect(Collectors.toMap(fila -> (Long) fila[0], fila -> fila));

        return clienteRepository.findAll().stream()
                .map(cliente -> {
                    Object[] stats = statsPorCliente.get(cliente.getId());
                    Long numeroCompras = stats != null ? (Long) stats[1] : 0L;
                    BigDecimal montoTotal = stats != null ? (BigDecimal) stats[2] : BigDecimal.ZERO;
                    LocalDateTime ultimaCompra = stats != null ? (LocalDateTime) stats[3] : null;
                    return new ClienteConEstadisticasResponse(cliente, numeroCompras, montoTotal, ultimaCompra);
                })
                .collect(Collectors.toList());
    }

    /** Obtener cliente por ID */
    @Transactional(readOnly = true)
    public ClienteResponse getClienteById(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("CLIENTE_NO_ENCONTRADO", "Cliente no encontrado con ID: " + id));
        return new ClienteResponse(cliente);
    }

    /** Buscar cliente por email */
    @Transactional(readOnly = true)
    public ClienteResponse getClienteByCorreo(String email) {
        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("CLIENTE_NO_ENCONTRADO", "Cliente no encontrado con email: " + email));
        return new ClienteResponse(cliente);
    }

    /** Crear nuevo cliente */
    @Transactional
    public ClienteResponse createCliente(ClienteRequest request) {
        if (request.getEmail() != null && !request.getEmail().isEmpty() 
            && clienteRepository.existsByEmail(request.getEmail())) {
            throw new ConflictoEstadoException("EMAIL_DUPLICADO", "Ya existe un cliente con el email: " + request.getEmail());
        }

        if (request.getNitCi() != null && !request.getNitCi().isEmpty()
            && clienteRepository.findByNitCi(request.getNitCi()).isPresent()) {
            throw new ConflictoEstadoException("NIT_CI_DUPLICADO", "El NIT o CI ya está registrado para otro cliente");
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setNitCi(request.getNitCi());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setActivo(request.getActivo());

        Cliente savedCliente = clienteRepository.save(cliente);

        registroAuditoria.registrar("CREAR_CLIENTE", "clientes", savedCliente.getId(),
                "Alta de " + savedCliente.getNombreCompleto());

        return new ClienteResponse(savedCliente);
    }

    @Transactional
    public ClienteResponse updateCliente(Long id, ClienteRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("CLIENTE_NO_ENCONTRADO", "Cliente no encontrado con ID: " + id));

        if (request.getEmail() != null && !request.getEmail().isEmpty()
            && !request.getEmail().equals(cliente.getEmail())
            && clienteRepository.existsByEmail(request.getEmail())) {
            throw new ConflictoEstadoException("EMAIL_DUPLICADO", "Ya existe un cliente con el email: " + request.getEmail());
        }

        if (request.getNitCi() != null && !request.getNitCi().isEmpty()
            && !request.getNitCi().equals(cliente.getNitCi())) {
            clienteRepository.findByNitCi(request.getNitCi()).ifPresent(c -> {
                if (!c.getId().equals(id)) {
                    throw new ConflictoEstadoException("NIT_CI_DUPLICADO", "El NIT o CI ya está registrado para otro cliente");
                }
            });
        }

        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setNitCi(request.getNitCi());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setActivo(request.getActivo());

        Cliente updatedCliente = clienteRepository.save(cliente);

        registroAuditoria.registrar("ACTUALIZAR_CLIENTE", "clientes", updatedCliente.getId(),
                "Edicion de " + updatedCliente.getNombreCompleto());

        return new ClienteResponse(updatedCliente);
    }

    /** Busca por nombre, teléfono o NIT/CI. */
    @Transactional(readOnly = true)
    public List<ClienteResponse> searchClientes(String query) {
        List<Cliente> resultados = clienteRepository.searchByNombre(query);
        
        // También buscar por telefono
        clienteRepository.findByTelefono(query).ifPresent(c -> {
            if (!resultados.contains(c)) {
                resultados.add(c);
            }
        });

        // También buscar por NIT/CI
        clienteRepository.findByNitCi(query).ifPresent(c -> {
            if (!resultados.contains(c)) {
                resultados.add(c);
            }
        });

        return resultados.stream()
                .map(ClienteResponse::new)
                .collect(Collectors.toList());
    }

    /** Historial completo de compras de un cliente. */
    @Transactional(readOnly = true)
    public HistorialComprasResponse getHistorialCompras(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("CLIENTE_NO_ENCONTRADO", "Cliente no encontrado con ID: " + clienteId));

        List<Venta> ventas = ventaRepository.findByClienteIdOrderByFechaVentaDesc(clienteId);

        return construirHistorialCompras(cliente, ventas);
    }

    /** Historial de compras de un cliente entre dos fechas. */
    @Transactional(readOnly = true)
    public HistorialComprasResponse getHistorialComprasFiltrado(Long clienteId, LocalDateTime inicio, LocalDateTime fin) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("CLIENTE_NO_ENCONTRADO", "Cliente no encontrado con ID: " + clienteId));

        List<Venta> todasVentas = ventaRepository.findByClienteIdOrderByFechaVentaDesc(clienteId);
        List<Venta> ventasFiltradas = todasVentas.stream()
                .filter(v -> !v.getFechaVenta().isBefore(inicio) && !v.getFechaVenta().isAfter(fin))
                .collect(Collectors.toList());

        return construirHistorialCompras(cliente, ventasFiltradas);
    }

    /** Construir respuesta de historial de compras con estadísticas */
    private HistorialComprasResponse construirHistorialCompras(Cliente cliente, List<Venta> ventas) {
        HistorialComprasResponse response = new HistorialComprasResponse();
        
        // Información del cliente
        response.setCliente(new ClienteResponse(cliente));

        // Ventas del cliente
        List<VentaResponse> ventasResponse = ventas.stream()
                .map(VentaResponse::new)
                .collect(Collectors.toList());
        response.setVentas(ventasResponse);

        // Las ventas canceladas se listan, pero no cuentan en las estadísticas.
        List<Venta> ventasValidas = ventas.stream()
                .filter(v -> v.getEstado() != EstadoVenta.CANCELADA)
                .collect(Collectors.toList());

        // Estadísticas de compra
        HistorialComprasResponse.EstadisticasCompra stats = new HistorialComprasResponse.EstadisticasCompra();
        
        stats.setTotalCompras((long) ventasValidas.size());
        
        BigDecimal montoTotal = ventasValidas.stream()
                .map(Venta::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setMontoTotal(montoTotal);

        if (!ventasValidas.isEmpty()) {
            BigDecimal ticketPromedio = montoTotal.divide(
                BigDecimal.valueOf(ventasValidas.size()), 
                2, 
                RoundingMode.HALF_UP
            );
            stats.setTicketPromedio(ticketPromedio);

            // Última fecha de compra
            stats.setUltimaCompra(ventasValidas.get(0).getFechaVenta());

            // Producto más comprado
            Map<String, Long> productosCantidad = ventasValidas.stream()
                .flatMap(v -> v.getDetalles().stream())
                .collect(Collectors.groupingBy(
                    d -> d.getProducto() != null ? d.getProducto().getNombre() : "Producto desconocido",
                    Collectors.summingLong(d -> d.getCantidad().longValue())
                ));

            if (!productosCantidad.isEmpty()) {
                Map.Entry<String, Long> productoTop = productosCantidad.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .orElse(null);

                if (productoTop != null) {
                    stats.setProductoMasComprado(productoTop.getKey());
                    stats.setCantidadProductoMasComprado(productoTop.getValue().intValue());
                }
            }
        } else {
            stats.setTicketPromedio(BigDecimal.ZERO);
            stats.setUltimaCompra(null);
            stats.setProductoMasComprado(null);
            stats.setCantidadProductoMasComprado(0);
        }

        response.setEstadisticas(stats);

        return response;
    }
}