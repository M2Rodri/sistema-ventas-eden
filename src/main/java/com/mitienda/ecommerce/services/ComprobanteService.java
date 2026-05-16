package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ComprobanteRequest;
import com.mitienda.ecommerce.dto.ComprobanteResponse;
import com.mitienda.ecommerce.models.Comprobante;
import com.mitienda.ecommerce.models.TipoComprobante;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.repositories.ComprobanteRepository;
import com.mitienda.ecommerce.repositories.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de comprobantes
 */
@Service
public class ComprobanteService {

    @Autowired
    private ComprobanteRepository comprobanteRepository;

    @Autowired
    private VentaRepository ventaRepository;

    /**
     * Listar todos los comprobantes
     */
    public List<ComprobanteResponse> getAllComprobantes() {
        return comprobanteRepository.findAll()
                .stream()
                .map(ComprobanteResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener comprobante por ID
     */
    public ComprobanteResponse getComprobanteById(Long id) {
        Comprobante comprobante = comprobanteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comprobante no encontrado con ID: " + id));
        return new ComprobanteResponse(comprobante);
    }

    /**
     * Obtener comprobante por número
     */
    public ComprobanteResponse getComprobanteByNumero(String numeroComprobante) {
        Comprobante comprobante = comprobanteRepository.findByNumeroComprobante(numeroComprobante)
                .orElseThrow(() -> new RuntimeException("Comprobante no encontrado con número: " + numeroComprobante));
        return new ComprobanteResponse(comprobante);
    }

    /**
     * Obtener comprobante por venta
     */
    public ComprobanteResponse getComprobanteByVenta(Long idVenta) {
        Comprobante comprobante = comprobanteRepository.findByVentaId(idVenta)
                .orElseThrow(() -> new RuntimeException("No existe comprobante para la venta con ID: " + idVenta));
        return new ComprobanteResponse(comprobante);
    }

    /**
     * Crear comprobante para una venta
     */
    @Transactional
    public ComprobanteResponse createComprobante(ComprobanteRequest request) {
        // Validar venta
        Venta venta = ventaRepository.findById(request.getIdVenta())
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + request.getIdVenta()));

        // Validar que la venta no tenga comprobante
        if (comprobanteRepository.findByVentaId(request.getIdVenta()).isPresent()) {
            throw new RuntimeException("Esta venta ya tiene un comprobante asociado");
        }

        // Generar número de comprobante
        String numeroComprobante = generarNumeroComprobante(request.getTipoComprobante());

        // Crear comprobante
        Comprobante comprobante = new Comprobante();
        comprobante.setVenta(venta);
        comprobante.setNumeroComprobante(numeroComprobante);
        comprobante.setTipoComprobante(request.getTipoComprobante());
        comprobante.setMontoTotal(venta.getMontoTotal());
        comprobante.setNombreCliente(request.getNombreCliente());
        comprobante.setObservaciones(request.getObservaciones());

        Comprobante savedComprobante = comprobanteRepository.save(comprobante);
        return new ComprobanteResponse(savedComprobante);
    }

    /**
     * Anular comprobante
     */
    @Transactional
    public ComprobanteResponse anularComprobante(Long id, String motivo) {
        Comprobante comprobante = comprobanteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comprobante no encontrado con ID: " + id));

        if (comprobante.getAnulado()) {
            throw new RuntimeException("Este comprobante ya está anulado");
        }

        comprobante.setAnulado(true);
        comprobante.setFechaAnulacion(LocalDateTime.now());
        comprobante.setMotivoAnulacion(motivo);

        Comprobante updatedComprobante = comprobanteRepository.save(comprobante);
        return new ComprobanteResponse(updatedComprobante);
    }

    /**
     * Comprobantes activos (no anulados)
     */
    public List<ComprobanteResponse> getComprobantesActivos() {
        return comprobanteRepository.findByAnuladoFalseOrderByFechaEmisionDesc()
                .stream()
                .map(ComprobanteResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Comprobantes anulados
     */
    public List<ComprobanteResponse> getComprobantesAnulados() {
        return comprobanteRepository.findByAnuladoTrueOrderByFechaEmisionDesc()
                .stream()
                .map(ComprobanteResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Comprobantes por tipo
     */
    public List<ComprobanteResponse> getComprobantesByTipo(TipoComprobante tipo) {
        return comprobanteRepository.findByTipoComprobanteOrderByFechaEmisionDesc(tipo)
                .stream()
                .map(ComprobanteResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Comprobantes entre fechas
     */
    public List<ComprobanteResponse> getComprobantesByFechas(LocalDateTime inicio, LocalDateTime fin) {
        return comprobanteRepository.findByFechaEmisionBetweenOrderByFechaEmisionDesc(inicio, fin)
                .stream()
                .map(ComprobanteResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Últimos comprobantes
     */
    public List<ComprobanteResponse> getUltimosComprobantes() {
        return comprobanteRepository.findTop20ByOrderByFechaEmisionDesc()
                .stream()
                .map(ComprobanteResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Generar número de comprobante correlativo
     */
    private String generarNumeroComprobante(TipoComprobante tipo) {
        String prefijo = obtenerPrefijoTipo(tipo);
        String año = String.valueOf(LocalDateTime.now().getYear());
        
        // Obtener el último número
        Long contadorMes = comprobanteRepository.countComprobantesDelMes();
        String numero = String.format("%05d", contadorMes + 1);
        
        return prefijo + "-" + año + "-" + numero;
    }

    /**
     * Obtener prefijo según tipo de comprobante
     */
    private String obtenerPrefijoTipo(TipoComprobante tipo) {
        switch (tipo) {
            case RECIBO: return "REC";
            case COMPROBANTE: return "COM";
            case NOTA_VENTA: return "NV";
            default: return "DOC";
        }
    }

    /**
     * Contar comprobantes del mes
     */
    public Long countComprobantesDelMes() {
        return comprobanteRepository.countComprobantesDelMes();
    }
}