package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.EnvioRequest;
import com.mitienda.ecommerce.dto.EnvioResponse;
import com.mitienda.ecommerce.models.*;
import com.mitienda.ecommerce.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EnvioService {

    @Autowired
    private EnvioRepository envioRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private TransportadoraRepository transportadoraRepository;

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