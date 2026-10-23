package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.PromocionRequest;
import com.mitienda.ecommerce.dto.PromocionResponse;
import com.mitienda.ecommerce.models.Promocion;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.repositories.PromocionRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Servicio para gestión de promociones */
@Service
// Lectura en transacción: con open-in-view=false no hay sesión de Hibernate fuera de ella.
@Transactional(readOnly = true)
public class PromocionService {
    private final PromocionRepository promocionRepository;

    private final ProductoRepository productoRepository;

    public PromocionService(PromocionRepository promocionRepository, ProductoRepository productoRepository) {
        this.promocionRepository = promocionRepository;
        this.productoRepository = productoRepository;
    }

    /** Listar todas las promociones */
    public List<PromocionResponse> getAllPromociones() {
        return promocionRepository.findAll()
                .stream()
                .map(PromocionResponse::new)
                .collect(Collectors.toList());
    }

    /** Listar solo promociones activas */
    public List<PromocionResponse> getActivePromociones() {
        return promocionRepository.findByActivoTrue()
                .stream()
                .map(PromocionResponse::new)
                .collect(Collectors.toList());
    }

    /** Listar promociones vigentes (activas y dentro del rango de fechas) */
    public List<PromocionResponse> getPromocionesVigentes() {
        return promocionRepository.findPromocionesVigentes(LocalDate.now())
                .stream()
                .map(PromocionResponse::new)
                .collect(Collectors.toList());
    }

    /** Obtener promocion por ID */
    public PromocionResponse getPromocionById(Long id) {
        Promocion promocion = promocionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promocion no encontrada con ID: " + id));
        return new PromocionResponse(promocion);
    }

    /** Crear nueva promocion */
    @Transactional
    public PromocionResponse createPromocion(PromocionRequest request) {
        if (request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new RuntimeException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        List<Producto> productos = new ArrayList<>();
        for (Long idProducto : request.getIdsProductos()) {
            Producto producto = productoRepository.findById(idProducto)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + idProducto));
            productos.add(producto);
        }

        Promocion promocion = new Promocion();
        promocion.setNombre(request.getNombre());
        promocion.setDescripcion(request.getDescripcion());
        promocion.setDescuento(request.getDescuento());
        promocion.setFechaInicio(request.getFechaInicio());
        promocion.setFechaFin(request.getFechaFin());
        promocion.setActivo(request.getActivo());
        promocion.setProductos(productos);

        Promocion savedPromocion = promocionRepository.save(promocion);
        return new PromocionResponse(savedPromocion);
    }

    /** Actualizar promocion existente */
    @Transactional
    public PromocionResponse updatePromocion(Long id, PromocionRequest request) {
        Promocion promocion = promocionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promocion no encontrada con ID: " + id));

        if (request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new RuntimeException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        List<Producto> productos = new ArrayList<>();
        for (Long idProducto : request.getIdsProductos()) {
            Producto producto = productoRepository.findById(idProducto)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + idProducto));
            productos.add(producto);
        }

        promocion.setNombre(request.getNombre());
        promocion.setDescripcion(request.getDescripcion());
        promocion.setDescuento(request.getDescuento());
        promocion.setFechaInicio(request.getFechaInicio());
        promocion.setFechaFin(request.getFechaFin());
        promocion.setActivo(request.getActivo());
        promocion.setProductos(productos);

        Promocion updatedPromocion = promocionRepository.save(promocion);
        return new PromocionResponse(updatedPromocion);
    }

    /** Eliminar promocion (desactivar) */
    @Transactional
    public void deletePromocion(Long id) {
        Promocion promocion = promocionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promocion no encontrada con ID: " + id));

        promocion.setActivo(false);
        promocionRepository.save(promocion);
    }

    /** Activar/Desactivar promocion */
    @Transactional
    public PromocionResponse togglePromocionStatus(Long id) {
        Promocion promocion = promocionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promocion no encontrada con ID: " + id));

        promocion.setActivo(!promocion.getActivo());
        Promocion updatedPromocion = promocionRepository.save(promocion);
        return new PromocionResponse(updatedPromocion);
    }

    /** Contar promociones activas */
    public Long countActivePromociones() {
        return promocionRepository.countByActivo(true);
    }
}