package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.exception.ConflictoEstadoException;
import com.mitienda.ecommerce.exception.RecursoNoEncontradoException;
import com.mitienda.ecommerce.exception.ReglaNegocioException;
import com.mitienda.ecommerce.dto.PagoDTO;
import com.mitienda.ecommerce.dto.PagoRequest;
import com.mitienda.ecommerce.models.EstadoPago;
import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.Pago;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.repositories.PagoRepository;
import com.mitienda.ecommerce.repositories.VentaRepository;
import com.mitienda.ecommerce.storage.AlmacenArchivos;
import com.mitienda.ecommerce.storage.ValidadorImagen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
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

    private final AlmacenArchivos almacen;

    private final UsuarioActualService usuarioActualService;

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public PagoService(PagoRepository pagoRepository, VentaRepository ventaRepository,
                        AlmacenArchivos almacen, UsuarioActualService usuarioActualService) {
        this.pagoRepository = pagoRepository;
        this.ventaRepository = ventaRepository;
        this.almacen = almacen;
        this.usuarioActualService = usuarioActualService;
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
                .orElseThrow(() -> new RecursoNoEncontradoException("PAGO_NO_ENCONTRADO", "Pago no encontrado con ID: " + id));
        return new PagoDTO(pago);
    }

    /**
     * Registrar un pago
     */
    @Transactional
    public PagoDTO registrarPago(PagoRequest request) {
        // Validar venta
        Venta venta = ventaRepository.findById(request.getIdVenta())
                .orElseThrow(() -> new RecursoNoEncontradoException("VENTA_NO_ENCONTRADA", "Venta no encontrada con ID: " + request.getIdVenta()));

        if (venta.getEstado() == EstadoVenta.CANCELADA) {
            throw new ConflictoEstadoException("VENTA_CANCELADA", "No se puede registrar un pago sobre una venta cancelada");
        }
        if (request.getMonto().compareTo(venta.getSaldoPendiente()) > 0) {
            throw new ReglaNegocioException("PAGO_EXCEDE_SALDO", "El monto supera el saldo pendiente de la venta (Bs. "
                    + venta.getSaldoPendiente() + ")");
        }

        // Crear pago
        Pago pago = new Pago();
        pago.setVenta(venta);
        pago.setMonto(request.getMonto());
        pago.setMetodoPago(request.getMetodoPago());
        pago.setReferencia(request.getReferencia());
        pago.setObservacion(request.getObservacion());
        pago.setEstado(EstadoPago.COMPLETADO);
        pago.setUsuario(usuarioActualService.obtenerRequerido());

        Pago savedPago = pagoRepository.save(pago);

        BigDecimal nuevoSaldo = venta.getSaldoPendiente().subtract(request.getMonto());
        venta.setSaldoPendiente(nuevoSaldo);
        if (nuevoSaldo.compareTo(BigDecimal.ZERO) == 0) {
            venta.setEstado(EstadoVenta.COMPLETADA);
        }
        ventaRepository.save(venta);

        return new PagoDTO(savedPago);
    }

    /**
     * Adjunta (o reemplaza) la foto de comprobante de un pago ya registrado.
     *
     * La foto va al bucket privado "comprobantes" de Supabase Storage; en la base
     * se guarda el nombre del objeto y la URL firmada se genera al responder.
     */
    @Transactional
    public PagoDTO adjuntarComprobante(Long idPago, MultipartFile file) throws IOException {
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new RecursoNoEncontradoException("PAGO_NO_ENCONTRADO", "Pago no encontrado con ID: " + idPago));

        // Se valida antes de tocar el almacenamiento: tipo real, contenido y tamaño (imagen o PDF).
        ValidadorImagen.ImagenValida imagen = ValidadorImagen.validarComprobante(file, ValidadorImagen.MAXIMO_COMPROBANTE);

        String urlAnterior = pago.getUrlComprobante();

        String referencia = almacen.guardar(AlmacenArchivos.BUCKET_COMPROBANTES,
                imagen.nombreObjeto(), imagen.contenido(), imagen.tipoContenido());
        pago.setUrlComprobante(referencia);

        Pago savedPago;
        try {
            savedPago = pagoRepository.save(pago);
        } catch (RuntimeException e) {
            // Si la base falla, el archivo recién subido quedaría huérfano en el bucket.
            eliminarSinFallar(referencia);
            throw e;
        }

        if (urlAnterior != null && !urlAnterior.isBlank()) {
            eliminarSinFallar(urlAnterior);
        }

        return new PagoDTO(savedPago);
    }

    /** Borrar un comprobante que ya no se usa no debe tumbar la operación principal. */
    private void eliminarSinFallar(String referencia) {
        try {
            almacen.eliminar(AlmacenArchivos.BUCKET_COMPROBANTES, referencia);
        } catch (IOException e) {
            log.warn("No se pudo eliminar un comprobante del almacenamiento: {}", e.getMessage());
        }
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