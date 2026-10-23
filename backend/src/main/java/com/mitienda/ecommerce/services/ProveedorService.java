package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.exception.ConflictoEstadoException;
import com.mitienda.ecommerce.exception.RecursoNoEncontradoException;
import com.mitienda.ecommerce.dto.ProveedorRequest;
import com.mitienda.ecommerce.dto.ProveedorResponse;
import com.mitienda.ecommerce.models.Proveedor;
import com.mitienda.ecommerce.repositories.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/** Servicio para gestión de proveedores */
@Service
// Lectura en transacción: con open-in-view=false no hay sesión de Hibernate fuera de ella.
@Transactional(readOnly = true)
public class ProveedorService {
    private final ProveedorRepository proveedorRepository;

    private final RegistroAuditoria registroAuditoria;

    public ProveedorService(ProveedorRepository proveedorRepository, RegistroAuditoria registroAuditoria) {
        this.proveedorRepository = proveedorRepository;
        this.registroAuditoria = registroAuditoria;
    }

    /** Listar todos los proveedores */
    public List<ProveedorResponse> getAllProveedores() {
        return proveedorRepository.findAll()
                .stream()
                .map(ProveedorResponse::new)
                .collect(Collectors.toList());
    }

    /** Listar solo proveedores activos */
    public List<ProveedorResponse> getActiveProveedores() {
        return proveedorRepository.findByActivoTrue()
                .stream()
                .map(ProveedorResponse::new)
                .collect(Collectors.toList());
    }

    /** Obtener proveedor por ID */
    public ProveedorResponse getProveedorById(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("PROVEEDOR_NO_ENCONTRADO", "Proveedor no encontrado con ID: " + id));
        return new ProveedorResponse(proveedor);
    }

    /** Crear nuevo proveedor */
    @Transactional
    public ProveedorResponse createProveedor(ProveedorRequest request) {
        if (proveedorRepository.existsByNit(request.getNit())) {
            throw new ConflictoEstadoException("NIT_DUPLICADO", "Ya existe un proveedor con el NIT: " + request.getNit());
        }

        Proveedor proveedor = new Proveedor();
        proveedor.setNombreEmpresa(request.getNombreEmpresa());
        proveedor.setNit(request.getNit());
        proveedor.setContacto(request.getContacto());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setDireccion(request.getDireccion());
        proveedor.setEmail(request.getEmail());
        proveedor.setNotas(request.getNotas());
        proveedor.setActivo(request.getActivo());

        Proveedor savedProveedor = proveedorRepository.save(proveedor);
        registroAuditoria.registrar("CREAR_PROVEEDOR", "proveedores", savedProveedor.getId(),
                "Alta de " + savedProveedor.getNombreEmpresa());

        return new ProveedorResponse(savedProveedor);
    }

    /** Actualizar proveedor existente */
    @Transactional
    public ProveedorResponse updateProveedor(Long id, ProveedorRequest request) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("PROVEEDOR_NO_ENCONTRADO", "Proveedor no encontrado con ID: " + id));

        if (!proveedor.getNit().equals(request.getNit()) && 
            proveedorRepository.existsByNit(request.getNit())) {
            throw new ConflictoEstadoException("NIT_DUPLICADO", "Ya existe un proveedor con el NIT: " + request.getNit());
        }

        proveedor.setNombreEmpresa(request.getNombreEmpresa());
        proveedor.setNit(request.getNit());
        proveedor.setContacto(request.getContacto());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setDireccion(request.getDireccion());
        proveedor.setEmail(request.getEmail());
        proveedor.setNotas(request.getNotas());
        proveedor.setActivo(request.getActivo());

        Proveedor updatedProveedor = proveedorRepository.save(proveedor);

        registroAuditoria.registrar("ACTUALIZAR_PROVEEDOR", "proveedores", updatedProveedor.getId(),
                "Edicion de " + updatedProveedor.getNombreEmpresa());

        return new ProveedorResponse(updatedProveedor);
    }

    /** Activa o desactiva un proveedor; es la única baja que existe. */
    @Transactional
    public ProveedorResponse toggleProveedorStatus(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("PROVEEDOR_NO_ENCONTRADO", "Proveedor no encontrado con ID: " + id));

        proveedor.setActivo(!proveedor.getActivo());
        Proveedor updatedProveedor = proveedorRepository.save(proveedor);

        registroAuditoria.registrar(
                Boolean.TRUE.equals(updatedProveedor.getActivo()) ? "ACTIVAR_PROVEEDOR" : "DESACTIVAR_PROVEEDOR",
                "proveedores", updatedProveedor.getId(), updatedProveedor.getNombreEmpresa());

        return new ProveedorResponse(updatedProveedor);
    }

    /** Buscar proveedores por nombre */
    public List<ProveedorResponse> searchProveedores(String nombre) {
        return proveedorRepository.searchByNombre(nombre)
                .stream()
                .map(ProveedorResponse::new)
                .collect(Collectors.toList());
    }
}