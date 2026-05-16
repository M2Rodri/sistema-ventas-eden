package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ProveedorRequest;
import com.mitienda.ecommerce.dto.ProveedorResponse;
import com.mitienda.ecommerce.models.Proveedor;
import com.mitienda.ecommerce.repositories.ProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de proveedores
 */
@Service
public class ProveedorService {

    @Autowired
    private ProveedorRepository proveedorRepository;

    /**
     * Listar todos los proveedores
     */
    public List<ProveedorResponse> getAllProveedores() {
        return proveedorRepository.findAll()
                .stream()
                .map(ProveedorResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Listar solo proveedores activos
     */
    public List<ProveedorResponse> getActiveProveedores() {
        return proveedorRepository.findByActivoTrue()
                .stream()
                .map(ProveedorResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener proveedor por ID
     */
    public ProveedorResponse getProveedorById(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));
        return new ProveedorResponse(proveedor);
    }

    /**
     * Buscar proveedor por NIT
     */
    public ProveedorResponse getProveedorByNit(String nit) {
        Proveedor proveedor = proveedorRepository.findByNit(nit)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con NIT: " + nit));
        return new ProveedorResponse(proveedor);
    }

    /**
     * Crear nuevo proveedor
     */
    @Transactional
    public ProveedorResponse createProveedor(ProveedorRequest request) {
        // Validar que el NIT no exista
        if (proveedorRepository.existsByNit(request.getNit())) {
            throw new RuntimeException("Ya existe un proveedor con el NIT: " + request.getNit());
        }

        Proveedor proveedor = new Proveedor();
        proveedor.setNombreEmpresa(request.getNombreEmpresa());
        proveedor.setNit(request.getNit());
        proveedor.setContacto(request.getContacto());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setDireccion(request.getDireccion());
        proveedor.setCorreo(request.getCorreo());
        proveedor.setNotas(request.getNotas());
        proveedor.setActivo(request.getActivo());

        Proveedor savedProveedor = proveedorRepository.save(proveedor);
        return new ProveedorResponse(savedProveedor);
    }

    /**
     * Actualizar proveedor existente
     */
    @Transactional
    public ProveedorResponse updateProveedor(Long id, ProveedorRequest request) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));

        // Validar NIT único (si cambió)
        if (!proveedor.getNit().equals(request.getNit()) && 
            proveedorRepository.existsByNit(request.getNit())) {
            throw new RuntimeException("Ya existe un proveedor con el NIT: " + request.getNit());
        }

        proveedor.setNombreEmpresa(request.getNombreEmpresa());
        proveedor.setNit(request.getNit());
        proveedor.setContacto(request.getContacto());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setDireccion(request.getDireccion());
        proveedor.setCorreo(request.getCorreo());
        proveedor.setNotas(request.getNotas());
        proveedor.setActivo(request.getActivo());

        Proveedor updatedProveedor = proveedorRepository.save(proveedor);
        return new ProveedorResponse(updatedProveedor);
    }

    /**
     * Eliminar proveedor (desactivar)
     */
    @Transactional
    public void deleteProveedor(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));

        proveedor.setActivo(false);
        proveedorRepository.save(proveedor);
    }

    /**
     * Activar/Desactivar proveedor
     */
    @Transactional
    public ProveedorResponse toggleProveedorStatus(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));

        proveedor.setActivo(!proveedor.getActivo());
        Proveedor updatedProveedor = proveedorRepository.save(proveedor);
        return new ProveedorResponse(updatedProveedor);
    }

    /**
     * Buscar proveedores por nombre
     */
    public List<ProveedorResponse> searchProveedores(String nombre) {
        return proveedorRepository.searchByNombre(nombre)
                .stream()
                .map(ProveedorResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Contar proveedores activos
     */
    public Long countActiveProveedores() {
        return proveedorRepository.countByActivo(true);
    }
}