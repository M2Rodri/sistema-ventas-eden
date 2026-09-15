package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.PagoDTO;
import com.mitienda.ecommerce.dto.PagoRequest;
import com.mitienda.ecommerce.models.EstadoPago;
import com.mitienda.ecommerce.models.Pago;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.repositories.PagoRepository;
import com.mitienda.ecommerce.repositories.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de pagos
 */
@Service
// Lectura dentro de transacción por defecto: con spring.jpa.open-in-view=false
// no hay sesión de Hibernate fuera de la transacción, y los DTO de respuesta se
// arman recorriendo relaciones perezosas. Sin esto, los endpoints de lectura
// fallaban con LazyInitializationException.
// Los métodos que escriben llevan su propio @Transactional, que tiene precedencia.
@Transactional(readOnly = true)
public class PagoService {

    private final PagoRepository pagoRepository;

    private final VentaRepository ventaRepository;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public PagoService(PagoRepository pagoRepository, VentaRepository ventaRepository) {
        this.pagoRepository = pagoRepository;
        this.ventaRepository = ventaRepository;
    }


    /**
     * Listar todos los pagos
     */
    public List<PagoDTO> getAllPagos() {
        return pagoRepository.findAll()
                .stream()
                .map(PagoDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener pago por ID
     */
    public PagoDTO getPagoById(Long id) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con ID: " + id));
        return new PagoDTO(pago);
    }

    /**
     * Registrar un pago
     */
    @Transactional
    public PagoDTO registrarPago(PagoRequest request) {
        // Validar venta
        Venta venta = ventaRepository.findById(request.getIdVenta())
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + request.getIdVenta()));

        // Crear pago
        Pago pago = new Pago();
        pago.setVenta(venta);
        pago.setMonto(request.getMonto());
        pago.setMetodoPago(request.getMetodoPago());
        pago.setReferencia(request.getReferencia());
        pago.setObservacion(request.getObservacion());
        pago.setEstado(EstadoPago.COMPLETADO);

        Pago savedPago = pagoRepository.save(pago);
        return new PagoDTO(savedPago);
    }

    /**
     * Obtener pagos de una venta
     */
    public List<PagoDTO> getPagosByVenta(Long ventaId) {
        return pagoRepository.findByVentaIdOrderByFechaPagoDesc(ventaId)
                .stream()
                .map(PagoDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Filtrar pagos por estado
     */
    public List<PagoDTO> getPagosByEstado(EstadoPago estado) {
        return pagoRepository.findByEstadoOrderByFechaPagoDesc(estado)
                .stream()
                .map(PagoDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Contar pagos por estado
     */
    public Long countPagosByEstado(EstadoPago estado) {
        return pagoRepository.countByEstado(estado);
    }
}