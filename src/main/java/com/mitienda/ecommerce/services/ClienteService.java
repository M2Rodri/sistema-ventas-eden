package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ClienteRequest;
import com.mitienda.ecommerce.dto.ClienteResponse;
import com.mitienda.ecommerce.dto.ClienteEstadisticas;
import com.mitienda.ecommerce.dto.HistorialComprasResponse;
import com.mitienda.ecommerce.dto.VentaResponse;
import com.mitienda.ecommerce.models.Cliente;
import com.mitienda.ecommerce.models.TipoCliente;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.models.DetalleVenta;
import com.mitienda.ecommerce.repositories.ClienteRepository;
import com.mitienda.ecommerce.repositories.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de clientes
 * Cumple con CU6 - Gestionar Clientes
 */
@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VentaRepository ventaRepository;

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
     * Listar solo clientes activos
     */
    @Transactional(readOnly = true)
    public List<ClienteResponse> getActiveClientes() {
        return clienteRepository.findByActivoTrue()
                .stream()
                .map(ClienteResponse::new)
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
     * Buscar cliente por correo
     */
    @Transactional(readOnly = true)
    public ClienteResponse getClienteByCorreo(String correo) {
        Cliente cliente = clienteRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con correo: " + correo));
        return new ClienteResponse(cliente);
    }

    /**
     * Crear nuevo cliente
     */
    @Transactional
    public ClienteResponse createCliente(ClienteRequest request) {
        // Validar que el correo no exista (si se proporcionó)
        if (request.getCorreo() != null && !request.getCorreo().isEmpty() 
            && clienteRepository.existsByCorreo(request.getCorreo())) {
            throw new RuntimeException("Ya existe un cliente con el correo: " + request.getCorreo());
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
        cliente.setCelular(request.getCelular());
        cliente.setCorreo(request.getCorreo());
        cliente.setDireccion(request.getDireccion());
        cliente.setTipo(request.getTipo());
        cliente.setActivo(request.getActivo());

        Cliente savedCliente = clienteRepository.save(cliente);
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

        // Validar correo único (si cambió y no es nulo)
        if (request.getCorreo() != null && !request.getCorreo().isEmpty()
            && !request.getCorreo().equals(cliente.getCorreo())
            && clienteRepository.existsByCorreo(request.getCorreo())) {
            throw new RuntimeException("Ya existe un cliente con el correo: " + request.getCorreo());
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
        cliente.setCelular(request.getCelular());
        cliente.setCorreo(request.getCorreo());
        cliente.setDireccion(request.getDireccion());
        cliente.setTipo(request.getTipo());
        cliente.setActivo(request.getActivo());

        Cliente updatedCliente = clienteRepository.save(cliente);
        return new ClienteResponse(updatedCliente);
    }

    /**
     * Eliminar cliente (desactivar)
     */
    @Transactional
    public void deleteCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));

        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    /**
     * Activar/Desactivar cliente
     */
    @Transactional
    public ClienteResponse toggleClienteStatus(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));

        cliente.setActivo(!cliente.getActivo());
        Cliente updatedCliente = clienteRepository.save(cliente);
        return new ClienteResponse(updatedCliente);
    }

    /**
     * Buscar clientes por nombre, teléfono o NIT/CI
     * CU: Buscar/Consultar Cliente
     */
    @Transactional(readOnly = true)
    public List<ClienteResponse> searchClientes(String query) {
        // Buscar por nombre, apellido, celular o NIT/CI
        List<Cliente> resultados = clienteRepository.searchByNombre(query);
        
        // También buscar por celular
        clienteRepository.findByCelular(query).ifPresent(c -> {
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
     * Filtrar clientes por tipo
     */
    @Transactional(readOnly = true)
    public List<ClienteResponse> getClientesByTipo(TipoCliente tipo) {
        return clienteRepository.findByTipo(tipo)
                .stream()
                .map(ClienteResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Contar clientes activos
     */
    @Transactional(readOnly = true)
    public Long countActiveClientes() {
        return clienteRepository.countByActivo(true);
    }

    /**
     * Contar clientes por tipo
     */
    @Transactional(readOnly = true)
    public Long countClientesByTipo(TipoCliente tipo) {
        return clienteRepository.countByTipo(tipo);
    }

    /**
     * Obtener estadísticas generales de clientes
     * Para Interfaz P6.1 - Indicadores superiores
     */
    @Transactional(readOnly = true)
    public ClienteEstadisticas getEstadisticasGenerales() {
        // Total de clientes registrados
        Long totalClientes = clienteRepository.count();

        // Clientes con compras este mes
        YearMonth mesActual = YearMonth.now();
        LocalDateTime inicioMes = mesActual.atDay(1).atStartOfDay();
        LocalDateTime finMes = mesActual.atEndOfMonth().atTime(23, 59, 59);
        
        Long clientesConComprasEsteMes = clienteRepository.findAll().stream()
                .filter(cliente -> {
                    List<Venta> ventas = ventaRepository.findByClienteIdOrderByFechaVentaDesc(cliente.getId());
                    return ventas.stream().anyMatch(v -> 
                        v.getFechaVenta().isAfter(inicioMes) && v.getFechaVenta().isBefore(finMes)
                    );
                })
                .count();

        // Cliente con mayor monto de compras
        Cliente clienteTop = null;
        BigDecimal montoMaximo = BigDecimal.ZERO;

        for (Cliente cliente : clienteRepository.findAll()) {
            List<Venta> ventas = ventaRepository.findByClienteIdOrderByFechaVentaDesc(cliente.getId());
            BigDecimal montoTotal = ventas.stream()
                    .map(Venta::getMontoTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (montoTotal.compareTo(montoMaximo) > 0) {
                montoMaximo = montoTotal;
                clienteTop = cliente;
            }
        }

        ClienteEstadisticas estadisticas = new ClienteEstadisticas();
        estadisticas.setTotalClientes(totalClientes);
        estadisticas.setClientesConComprasEsteMes(clientesConComprasEsteMes);
        
        if (clienteTop != null) {
            estadisticas.setClienteTopNombre(clienteTop.getNombreCompleto());
            estadisticas.setClienteTopMonto(montoMaximo);
        }

        return estadisticas;
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