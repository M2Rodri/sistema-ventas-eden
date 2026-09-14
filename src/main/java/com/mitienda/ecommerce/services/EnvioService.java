package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.EnvioRequest;
import com.mitienda.ecommerce.dto.EnvioResponse;
import com.mitienda.ecommerce.models.*;
import com.mitienda.ecommerce.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
// Lectura dentro de transacción por defecto: con spring.jpa.open-in-view=false
// no hay sesión de Hibernate fuera de la transacción, y los DTO de respuesta se
// arman recorriendo relaciones perezosas. Sin esto, los endpoints de lectura
// fallaban con LazyInitializationException.
// Los métodos que escriben llevan su propio @Transactional, que tiene precedencia.
@Transactional(readOnly = true)
public class EnvioService {

    private final EnvioRepository envioRepository;

    private final VentaRepository ventaRepository;

    private final TransportadoraRepository transportadoraRepository;

    private final UsuarioRepository usuarioRepository;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public EnvioService(EnvioRepository envioRepository,
                        VentaRepository ventaRepository,
                        TransportadoraRepository transportadoraRepository,
                        UsuarioRepository usuarioRepository) {
        this.envioRepository = envioRepository;
        this.ventaRepository = ventaRepository;
        this.transportadoraRepository = transportadoraRepository;
        this.usuarioRepository = usuarioRepository;
    }


    /** Asigna el empleado responsable del envío, si el request lo trae. */
    private void asignarResponsable(Envio envio, EnvioRequest request) {
        if (request.getIdUsuarioResponsable() != null) {
            Usuario responsable = usuarioRepository.findById(request.getIdUsuarioResponsable())
                    .orElseThrow(() -> new RuntimeException(
                            "Usuario no encontrado con ID: " + request.getIdUsuarioResponsable()));
            envio.setUsuarioResponsable(responsable);
        }
    }

    @Transactional(readOnly = true)
    public List<EnvioResponse> getAllEnvios() {
        return envioRepository.findAll()
                .stream()
                .map(EnvioResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EnvioResponse getEnvioById(Long id) {
        Envio envio = envioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Envío no encontrado con ID: " + id));
        return new EnvioResponse(envio);
    }

    @Transactional(readOnly = true)
    public EnvioResponse getEnvioByVenta(Long idVenta) {
        Envio envio = envioRepository.findByVentaId(idVenta)
                .orElseThrow(() -> new RuntimeException("No existe envío para la venta con ID: " + idVenta));
        return new EnvioResponse(envio);
    }

    @Transactional(readOnly = true)
    public EnvioResponse getEnvioByGuia(String guiaRemision) {
        Envio envio = envioRepository.findByGuiaRemision(guiaRemision)
                .orElseThrow(() -> new RuntimeException("No existe envío con guía de remisión: " + guiaRemision));
        return new EnvioResponse(envio);
    }

    @Transactional
    public EnvioResponse createEnvio(EnvioRequest request) {
        Envio envio = new Envio();

        if (request.getIdVenta() != null) {
            Venta venta = ventaRepository.findByIdWithCliente(request.getIdVenta())
                    .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + request.getIdVenta()));
            envio.setVenta(venta);
        } else {
            throw new RuntimeException("Debe especificar una venta para el envío");
        }

        if (request.getIdTransportadora() != null) {
            Transportadora transportadora = transportadoraRepository.findById(request.getIdTransportadora())
                    .orElseThrow(() -> new RuntimeException("Transportadora no encontrada con ID: " + request.getIdTransportadora()));
            envio.setTransportadora(transportadora);
        }
        asignarResponsable(envio, request);
        envio.setDireccionDestino(request.getDireccionDestino());
        envio.setCiudad(request.getCiudad());
        envio.setDepartamento(request.getDepartamento());
        envio.setFechaEntregaEstimada(request.getFechaEntregaEstimada());
        envio.setGuiaRemision(request.getGuiaRemision());
        envio.setCostoEnvio(request.getCostoEnvio());
        envio.setNotas(request.getNotas());
        envio.setEstadoSeguimiento(EstadoEnvio.PENDIENTE);

        return new EnvioResponse(envioRepository.save(envio));
    }

    @Transactional
    public EnvioResponse updateEnvio(Long id, EnvioRequest request) {
        Envio envio = envioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Envío no encontrado con ID: " + id));

        if (request.getIdTransportadora() != null) {
            Transportadora transportadora = transportadoraRepository.findById(request.getIdTransportadora())
                    .orElseThrow(() -> new RuntimeException("Transportadora no encontrada con ID: " + request.getIdTransportadora()));
            envio.setTransportadora(transportadora);
        }
        asignarResponsable(envio, request);
        envio.setDireccionDestino(request.getDireccionDestino());
        envio.setCiudad(request.getCiudad());
        envio.setDepartamento(request.getDepartamento());
        envio.setFechaEntregaEstimada(request.getFechaEntregaEstimada());
        envio.setGuiaRemision(request.getGuiaRemision());
        envio.setCostoEnvio(request.getCostoEnvio());
        envio.setNotas(request.getNotas());

        return new EnvioResponse(envioRepository.save(envio));
    }

    @Transactional
    public EnvioResponse cambiarEstadoEnvio(Long id, EstadoEnvio nuevoEstado) {
        Envio envio = envioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Envío no encontrado con ID: " + id));

        envio.setEstadoSeguimiento(nuevoEstado);

        if (nuevoEstado == EstadoEnvio.ENTREGADO) {
            envio.setFechaEntregaReal(LocalDate.now());
        }

        return new EnvioResponse(envioRepository.save(envio));
    }

    @Transactional
    public EnvioResponse marcarComoEntregado(Long id) {
        return cambiarEstadoEnvio(id, EstadoEnvio.ENTREGADO);
    }

    @Transactional(readOnly = true)
    public List<EnvioResponse> getEnviosByEstado(EstadoEnvio estado) {
        return envioRepository.findByEstadoSeguimientoOrderByFechaCreacionDesc(estado)
                .stream()
                .map(EnvioResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EnvioResponse> getEnviosByTransportadora(Long idTransportadora) {
        return envioRepository.findByTransportadoraIdOrderByFechaCreacionDesc(idTransportadora)
                .stream()
                .map(EnvioResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EnvioResponse> getEnviosPendientes() {
        return envioRepository.findEnviosPendientes()
                .stream()
                .map(EnvioResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EnvioResponse> getEnviosEnCamino() {
        return envioRepository.findEnviosEnCamino()
                .stream()
                .map(EnvioResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EnvioResponse> getEnviosPorEntregar() {
        return envioRepository.findEnviosPorEntregar(LocalDate.now())
                .stream()
                .map(EnvioResponse::new)
                .collect(Collectors.toList());
    }

    public Long countEnviosByEstado(EstadoEnvio estado) {
        return envioRepository.countByEstadoSeguimiento(estado);
    }
}