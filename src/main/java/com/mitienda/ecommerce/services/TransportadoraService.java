package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.TransportadoraRequest;
import com.mitienda.ecommerce.dto.TransportadoraResponse;
import com.mitienda.ecommerce.models.Transportadora;
import com.mitienda.ecommerce.repositories.TransportadoraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de transportadoras
 */
@Service
public class TransportadoraService {

    @Autowired
    private TransportadoraRepository transportadoraRepository;

    /**
     * Listar todas las transportadoras
     */
    public List<TransportadoraResponse> getAllTransportadoras() {
        return transportadoraRepository.findAll()
                .stream()
                .map(TransportadoraResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Listar solo transportadoras activas
     */
    public List<TransportadoraResponse> getActiveTransportadoras() {
        return transportadoraRepository.findByActivoTrue()
                .stream()
                .map(TransportadoraResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener transportadora por ID
     */
    public TransportadoraResponse getTransportadoraById(Long id) {
        Transportadora transportadora = transportadoraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transportadora no encontrada con ID: " + id));
        return new TransportadoraResponse(transportadora);
    }

    /**
     * Crear nueva transportadora
     */
    @Transactional
    public TransportadoraResponse createTransportadora(TransportadoraRequest request) {
        Transportadora transportadora = new Transportadora();
        transportadora.setNombre(request.getNombre());
        transportadora.setTelefono(request.getTelefono());
        transportadora.setCorreo(request.getCorreo());
        transportadora.setTarifaBase(request.getTarifaBase());
        transportadora.setTiempoEstimadoDias(request.getTiempoEstimadoDias());
        transportadora.setActivo(request.getActivo());

        Transportadora savedTransportadora = transportadoraRepository.save(transportadora);
        return new TransportadoraResponse(savedTransportadora);
    }

    /**
     * Actualizar transportadora existente
     */
    @Transactional
    public TransportadoraResponse updateTransportadora(Long id, TransportadoraRequest request) {
        Transportadora transportadora = transportadoraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transportadora no encontrada con ID: " + id));

        transportadora.setNombre(request.getNombre());
        transportadora.setTelefono(request.getTelefono());
        transportadora.setCorreo(request.getCorreo());
        transportadora.setTarifaBase(request.getTarifaBase());
        transportadora.setTiempoEstimadoDias(request.getTiempoEstimadoDias());
        transportadora.setActivo(request.getActivo());

        Transportadora updatedTransportadora = transportadoraRepository.save(transportadora);
        return new TransportadoraResponse(updatedTransportadora);
    }

    /**
     * Eliminar transportadora (desactivar)
     */
    @Transactional
    public void deleteTransportadora(Long id) {
        Transportadora transportadora = transportadoraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transportadora no encontrada con ID: " + id));

        transportadora.setActivo(false);
        transportadoraRepository.save(transportadora);
    }

    /**
     * Activar/Desactivar transportadora
     */
    @Transactional
    public TransportadoraResponse toggleTransportadoraStatus(Long id) {
        Transportadora transportadora = transportadoraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transportadora no encontrada con ID: " + id));

        transportadora.setActivo(!transportadora.getActivo());
        Transportadora updatedTransportadora = transportadoraRepository.save(transportadora);
        return new TransportadoraResponse(updatedTransportadora);
    }

    /**
     * Contar transportadoras activas
     */
    public Long countActiveTransportadoras() {
        return transportadoraRepository.countByActivo(true);
    }
}