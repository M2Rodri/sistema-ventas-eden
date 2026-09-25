package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ClienteRequest;
import com.mitienda.ecommerce.dto.ClienteResponse;
import com.mitienda.ecommerce.dto.ClienteConEstadisticasResponse;
import com.mitienda.ecommerce.dto.HistorialComprasResponse;
import com.mitienda.ecommerce.dto.VentaResponse;
import com.mitienda.ecommerce.models.Cliente;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.models.DetalleVenta;
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

/**
 * Servicio para gestión de clientes
 * Cumple con CU6 - Gestionar Clientes
 */
@Service
// Lectura dentro de transacción por defecto: con spring.jpa.open-in-view=false
// no hay sesión de Hibernate fuera de la transacción, y los DTO de respuesta se
// arman recorriendo relaciones perezosas. Sin esto, los endpoints de lectura
// fallaban con LazyInitializationException.
// Los métodos que escriben llevan su propio @Transactional, que tiene precedencia.
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;

    private final VentaRepository ventaRepository;

    private final RegistroAuditoria registroAuditoria;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public ClienteService(ClienteRepository clienteRepository,
                          VentaRepository ventaRepository,
                          RegistroAuditoria registroAuditoria) {
        this.clienteRepository = clienteRepository;
        this.ventaRepository = ventaRepository;
        this.registroAuditoria = registroAuditoria;
    }


    /**
     * Listar todos los clientes
     */
    @Transactional(readOnly = true)
    public List<ClienteResponse> getAllClientes() {
        return clienteRepository.findAll()
                .stream()
                .map(ClienteResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Listar todos los clientes con sus estadísticas de compra (número de
     * compras, monto total, última compra), para la tabla de Clientes.
     * Una sola consulta agregada en vez de pedir el historial de cada
     * cliente por separado.
     */
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

    /**
     * Obtener cliente por ID
     */
    @Transactional(readOnly = true)
    public ClienteResponse getClienteById(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
        return new ClienteResponse(cliente);
    }

    /**
     * Buscar cliente por email
     */
    @Transactional(readOnly = true)
    public ClienteResponse getClienteByCorreo(String email) {
        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con email: " + email));
        return new ClienteResponse(cliente);
    }

    /**
     * Crear nuevo cliente
     */
    @Transactional
    public ClienteResponse createCliente(ClienteRequest request) {
        // Validar que el email no exista (si se proporcionó)
        if (request.getEmail() != null && !request.getEmail().isEmpty() 
            && clienteRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Ya existe un cliente con el email: " + request.getEmail());
        }

        // Validar NIT/CI único (si se proporcionó)
        if (request.getNitCi() != null && !request.getNitCi().isEmpty()
            && clienteRepository.findByNitCi(request.getNitCi()).isPresent()) {
            throw new RuntimeException("El NIT o CI ya está registrado para otro cliente");
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setNitCi(request.getNitCi());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setTipoCliente(request.getTipoCliente());
        cliente.setActivo(request.getActivo());

        Cliente savedCliente = clienteRepository.save(cliente);

        registroAuditoria.registrar("CREAR_CLIENTE", "clientes", savedCliente.getId(),
                "Alta de " + savedCliente.getNombreCompleto());

        return new ClienteResponse(savedCliente);
    }

    /**
     * Actualizar cliente existente
     * CU: Modificar Cliente
     */
    @Transactional
    public ClienteResponse updateCliente(Long id, ClienteRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));

        // Validar email único (si cambió y no es nulo)
        if (request.getEmail() != null && !request.getEmail().isEmpty()
            && !request.getEmail().equals(cliente.getEmail())
            && clienteRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Ya existe un cliente con el email: " + request.getEmail());
        }

        // Validar NIT/CI único (si cambió y no es nulo)
        if (request.getNitCi() != null && !request.getNitCi().isEmpty()
            && !request.getNitCi().equals(cliente.getNitCi())) {
            clienteRepository.findByNitCi(request.getNitCi()).ifPresent(c -> {
                if (!c.getId().equals(id)) {
                    throw new RuntimeException("El NIT o CI ya está registrado para otro cliente");
                }
            });
        }

        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setNitCi(request.getNitCi());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setTipoCliente(request.getTipoCliente());
        cliente.setActivo(request.getActivo());

        Cliente updatedCliente = clienteRepository.save(cliente);

        registroAuditoria.registrar("ACTUALIZAR_CLIENTE", "clientes", updatedCliente.getId(),
                "Edicion de " + updatedCliente.getNombreCompleto());

        return new ClienteResponse(updatedCliente);
    }

    /**
     * Buscar clientes por nombre, teléfono o NIT/CI
     * CU: Buscar/Consultar Cliente
     */
    @Transactional(readOnly = true)
    public List<ClienteResponse> searchClientes(String query) {
        // Buscar por nombre, apellido, telefono o NIT/CI
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

    /**
     * Obtener historial completo de compras de un cliente
     * CU: Ver Historial de Compras del Cliente
     */
    @Transactional(readOnly = true)
    public HistorialComprasResponse getHistorialCompras(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + clienteId));

        List<Venta> ventas = ventaRepository.findByClienteIdOrderByFechaVentaDesc(clienteId);

        return construirHistorialCompras(cliente, ventas);
    }

    /**
     * Obtener historial de compras filtrado por fechas
     * CU: Ver Historial de Compras del Cliente - Filtro de fechas
     */
    @Transactional(readOnly = true)
    public HistorialComprasResponse getHistorialComprasFiltrado(Long clienteId, LocalDateTime inicio, LocalDateTime fin) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + clienteId));

        List<Venta> todasVentas = ventaRepository.findByClienteIdOrderByFechaVentaDesc(clienteId);
        List<Venta> ventasFiltradas = todasVentas.stream()
                .filter(v -> !v.getFechaVenta().isBefore(inicio) && !v.getFechaVenta().isAfter(fin))
                .collect(Collectors.toList());

        return construirHistorialCompras(cliente, ventasFiltradas);
    }

    /**
     * Construir respuesta de historial de compras con estadísticas
     */
    private HistorialComprasResponse construirHistorialCompras(Cliente cliente, List<Venta> ventas) {
        HistorialComprasResponse response = new HistorialComprasResponse();
        
        // Información del cliente
        response.setCliente(new ClienteResponse(cliente));

        // Ventas del cliente
        List<VentaResponse> ventasResponse = ventas.stream()
                .map(VentaResponse::new)
                .collect(Collectors.toList());
        response.setVentas(ventasResponse);

        // Estadísticas de compra
        HistorialComprasResponse.EstadisticasCompra stats = new HistorialComprasResponse.EstadisticasCompra();
        
        stats.setTotalCompras((long) ventas.size());
        
        BigDecimal montoTotal = ventas.stream()
                .map(Venta::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setMontoTotal(montoTotal);

        if (!ventas.isEmpty()) {
            BigDecimal ticketPromedio = montoTotal.divide(
                BigDecimal.valueOf(ventas.size()), 
                2, 
                RoundingMode.HALF_UP
            );
            stats.setTicketPromedio(ticketPromedio);

            // Última fecha de compra
            stats.setUltimaCompra(ventas.get(0).getFechaVenta());

            // Producto más comprado
            // Producto más comprado
            Map<String, Long> productosCantidad = ventas.stream()
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