package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.OfertaRequest;
import com.mitienda.ecommerce.dto.OfertaResponse;
import com.mitienda.ecommerce.models.Oferta;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.repositories.OfertaRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de ofertas
 */
@Service
public class OfertaService {

    @Autowired
    private OfertaRepository ofertaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    /**
     * Listar todas las ofertas
     */
    public List<OfertaResponse> getAllOfertas() {
        return ofertaRepository.findAll()
                .stream()
                .map(OfertaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Listar solo ofertas activas
     */
    public List<OfertaResponse> getActiveOfertas() {
        return ofertaRepository.findByActivoTrue()
                .stream()
                .map(OfertaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Listar ofertas vigentes (activas y dentro del rango de fechas)
     */
    public List<OfertaResponse> getOfertasVigentes() {
        return ofertaRepository.findOfertasVigentes(LocalDate.now())
                .stream()
                .map(OfertaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener oferta por ID
     */
    public OfertaResponse getOfertaById(Long id) {
        Oferta oferta = ofertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada con ID: " + id));
        return new OfertaResponse(oferta);
    }

    /**
     * Crear nueva oferta
     */
    @Transactional
    public OfertaResponse createOferta(OfertaRequest request) {
        // Validar fechas
        if (request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new RuntimeException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        // Buscar productos
        List<Producto> productos = new ArrayList<>();
        for (Long idProducto : request.getIdsProductos()) {
            Producto producto = productoRepository.findById(idProducto)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + idProducto));
            productos.add(producto);
        }

        // Crear oferta
        Oferta oferta = new Oferta();
        oferta.setDescripcion(request.getDescripcion());
        oferta.setDescuento(request.getDescuento());
        oferta.setFechaInicio(request.getFechaInicio());
        oferta.setFechaFin(request.getFechaFin());
        oferta.setActivo(request.getActivo());
        oferta.setProductos(productos);

        Oferta savedOferta = ofertaRepository.save(oferta);
        return new OfertaResponse(savedOferta);
    }

    /**
     * Actualizar oferta existente
     */
    @Transactional
    public OfertaResponse updateOferta(Long id, OfertaRequest request) {
        Oferta oferta = ofertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada con ID: " + id));

        // Validar fechas
        if (request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new RuntimeException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        // Actualizar productos
        List<Producto> productos = new ArrayList<>();
        for (Long idProducto : request.getIdsProductos()) {
            Producto producto = productoRepository.findById(idProducto)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + idProducto));
            productos.add(producto);
        }

        oferta.setDescripcion(request.getDescripcion());
        oferta.setDescuento(request.getDescuento());
        oferta.setFechaInicio(request.getFechaInicio());
        oferta.setFechaFin(request.getFechaFin());
        oferta.setActivo(request.getActivo());
        oferta.setProductos(productos);

        Oferta updatedOferta = ofertaRepository.save(oferta);
        return new OfertaResponse(updatedOferta);
    }

    /**
     * Eliminar oferta (desactivar)
     */
    @Transactional
    public void deleteOferta(Long id) {
        Oferta oferta = ofertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada con ID: " + id));

        oferta.setActivo(false);
        ofertaRepository.save(oferta);
    }

    /**
     * Activar/Desactivar oferta
     */
    @Transactional
    public OfertaResponse toggleOfertaStatus(Long id) {
        Oferta oferta = ofertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada con ID: " + id));

        oferta.setActivo(!oferta.getActivo());
        Oferta updatedOferta = ofertaRepository.save(oferta);
        return new OfertaResponse(updatedOferta);
    }

    /**
     * Contar ofertas activas
     */
    public Long countActiveOfertas() {
        return ofertaRepository.countByActivo(true);
    }
}