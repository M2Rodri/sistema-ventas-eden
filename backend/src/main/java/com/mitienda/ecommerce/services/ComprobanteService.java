package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.exception.ConflictoEstadoException;
import com.mitienda.ecommerce.exception.RecursoNoEncontradoException;
import com.mitienda.ecommerce.dto.ComprobanteRequest;
import com.mitienda.ecommerce.dto.ComprobanteResponse;
import com.mitienda.ecommerce.models.Comprobante;
import com.mitienda.ecommerce.models.TipoComprobante;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.repositories.ComprobanteRepository;
import com.mitienda.ecommerce.repositories.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** Servicio para gestión de comprobantes */
@Service
// Lectura en transacción: con open-in-view=false no hay sesión de Hibernate fuera de ella.
@Transactional(readOnly = true)
public class ComprobanteService {
    private final ComprobanteRepository comprobanteRepository;

    private final VentaRepository ventaRepository;

    public ComprobanteService(ComprobanteRepository comprobanteRepository, VentaRepository ventaRepository) {
        this.comprobanteRepository = comprobanteRepository;
        this.ventaRepository = ventaRepository;
    }

    /** Listar todos los comprobantes */
    public List<ComprobanteResponse> getAllComprobantes() {
        return comprobanteRepository.findAll()
                .stream()
                .map(ComprobanteResponse::new)
                .collect(Collectors.toList());
    }

    /** Obtener comprobante por ID */
    public ComprobanteResponse getComprobanteById(Long id) {
        Comprobante comprobante = comprobanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("COMPROBANTE_NO_ENCONTRADO", "Comprobante no encontrado con ID: " + id));
        return new ComprobanteResponse(comprobante);
    }

    /** Obtener comprobante por número */
    public ComprobanteResponse getComprobanteByNumero(String numeroComprobante) {
        Comprobante comprobante = comprobanteRepository.findByNumeroComprobante(numeroComprobante)
                .orElseThrow(() -> new RecursoNoEncontradoException("COMPROBANTE_NO_ENCONTRADO", "Comprobante no encontrado con número: " + numeroComprobante));
        return new ComprobanteResponse(comprobante);
    }

    /** Obtener comprobante por venta */
    public ComprobanteResponse getComprobanteByVenta(Long idVenta) {
        Comprobante comprobante = comprobanteRepository.findByVentaId(idVenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("COMPROBANTE_NO_ENCONTRADO", "No existe comprobante para la venta con ID: " + idVenta));
        return new ComprobanteResponse(comprobante);
    }

    /** Crear comprobante para una venta */
    @Transactional
    public ComprobanteResponse createComprobante(ComprobanteRequest request) {
        Venta venta = ventaRepository.findById(request.getIdVenta())
                .orElseThrow(() -> new RecursoNoEncontradoException("VENTA_NO_ENCONTRADA", "Venta no encontrada con ID: " + request.getIdVenta()));

        if (comprobanteRepository.findByVentaId(request.getIdVenta()).isPresent()) {
            throw new ConflictoEstadoException("COMPROBANTE_YA_EXISTE", "Esta venta ya tiene un comprobante asociado");
        }

        // Generar número de comprobante
        String numeroComprobante = generarNumeroComprobante(request.getTipoComprobante(), venta.getFechaVenta());

        Comprobante comprobante = new Comprobante();
        comprobante.setVenta(venta);
        comprobante.setNumeroComprobante(numeroComprobante);
        comprobante.setTipoComprobante(request.getTipoComprobante());
        comprobante.setMontoTotal(venta.getMontoTotal());
        comprobante.setNombreCliente(request.getNombreCliente());
        comprobante.setObservaciones(request.getObservaciones());

        // saveAndFlush: un numeroComprobante repetido falla aquí y no al confirmar la transacción.
        Comprobante savedComprobante = comprobanteRepository.saveAndFlush(comprobante);
        return new ComprobanteResponse(savedComprobante);
    }

    /** Comprobantes activos (no anulados) */
    public List<ComprobanteResponse> getComprobantesActivos() {
        return comprobanteRepository.findByAnuladoFalseOrderByFechaEmisionDesc()
                .stream()
                .map(ComprobanteResponse::new)
                .collect(Collectors.toList());
    }

    /** Comprobantes anulados */
    public List<ComprobanteResponse> getComprobantesAnulados() {
        return comprobanteRepository.findByAnuladoTrueOrderByFechaEmisionDesc()
                .stream()
                .map(ComprobanteResponse::new)
                .collect(Collectors.toList());
    }

    /** Comprobantes por tipo */
    public List<ComprobanteResponse> getComprobantesByTipo(TipoComprobante tipo) {
        return comprobanteRepository.findByTipoComprobanteOrderByFechaEmisionDesc(tipo)
                .stream()
                .map(ComprobanteResponse::new)
                .collect(Collectors.toList());
    }

    /** Comprobantes entre fechas */
    public List<ComprobanteResponse> getComprobantesByFechas(LocalDateTime inicio, LocalDateTime fin) {
        return comprobanteRepository.findByFechaEmisionBetweenOrderByFechaEmisionDesc(inicio, fin)
                .stream()
                .map(ComprobanteResponse::new)
                .collect(Collectors.toList());
    }

    /** Últimos comprobantes */
    public List<ComprobanteResponse> getUltimosComprobantes() {
        return comprobanteRepository.findTop20ByOrderByFechaEmisionDesc()
                .stream()
                .map(ComprobanteResponse::new)
                .collect(Collectors.toList());
    }

    /** Número correlativo a partir del último real con el mismo prefijo (tipo + año de la venta, no de hoy). */
    private String generarNumeroComprobante(TipoComprobante tipo, LocalDateTime fechaVenta) {
        String prefijo = obtenerPrefijoTipo(tipo);
        LocalDateTime referencia = fechaVenta != null ? fechaVenta : LocalDateTime.now();
        String año = String.valueOf(referencia.getYear());
        String prefijoCompleto = prefijo + "-" + año + "-";

        int siguiente = comprobanteRepository.findUltimoNumeroConPrefijo(prefijoCompleto)
                .map(ultimo -> {
                    String parteNumerica = ultimo.substring(prefijoCompleto.length());
                    try {
                        return Integer.parseInt(parteNumerica) + 1;
                    } catch (NumberFormatException e) {
                        return 1;
                    }
                })
                .orElse(1);

        return prefijoCompleto + String.format("%05d", siguiente);
    }

    /** Obtener prefijo según tipo de comprobante */
    private String obtenerPrefijoTipo(TipoComprobante tipo) {
        switch (tipo) {
            case RECIBO: return "REC";
            case COMPROBANTE: return "COM";
            case NOTA_VENTA: return "NV";
            default: return "DOC";
        }
    }

    /** Contar comprobantes del mes */
    public Long countComprobantesDelMes() {
        return comprobanteRepository.countComprobantesDelMes();
    }
}