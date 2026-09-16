package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.PagoDTO;
import com.mitienda.ecommerce.dto.PagoRequest;
import com.mitienda.ecommerce.models.EstadoPago;
import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.Pago;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.repositories.PagoRepository;
import com.mitienda.ecommerce.repositories.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
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

    private final FileStorageService fileStorageService;

    private static final List<String> EXTENSIONES_COMPROBANTE = Arrays.asList(".jpg", ".jpeg", ".png", ".webp");

    private static final long TAMANO_MAXIMO_COMPROBANTE = 10 * 1024 * 1024; // 10MB, igual que las demas fotos

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public PagoService(PagoRepository pagoRepository, VentaRepository ventaRepository,
                        FileStorageService fileStorageService) {
        this.pagoRepository = pagoRepository;
        this.ventaRepository = ventaRepository;
        this.fileStorageService = fileStorageService;
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

        if (venta.getEstado() == EstadoVenta.CANCELADA) {
            throw new RuntimeException("No se puede registrar un pago sobre una venta cancelada");
        }
        if (request.getMonto().compareTo(venta.getSaldoPendiente()) > 0) {
            throw new RuntimeException("El monto supera el saldo pendiente de la venta (Bs. "
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
     * TODO: esto guarda el archivo en disco local (misma carpeta uploads/ que
     * usan las imágenes de producto). En Railway el filesystem no es
     * persistente entre despliegues, así que un redeploy borra los
     * comprobantes ya subidos. Pendiente: mover a almacenamiento externo
     * (S3, Supabase Storage, etc.) antes de depender de esto en producción.
     */
    @Transactional
    public PagoDTO adjuntarComprobante(Long idPago, MultipartFile file) throws IOException {
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con ID: " + idPago));

        String urlAnterior = pago.getUrlComprobante();

        String urlNueva = fileStorageService.saveFile(
                file, "comprobantes-pago", EXTENSIONES_COMPROBANTE, TAMANO_MAXIMO_COMPROBANTE);
        pago.setUrlComprobante(urlNueva);

        Pago savedPago = pagoRepository.save(pago);

        if (urlAnterior != null && !urlAnterior.isBlank()) {
            fileStorageService.deleteFile(urlAnterior);
        }

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