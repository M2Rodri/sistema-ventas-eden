package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ProveedorRequest;
import com.mitienda.ecommerce.dto.ProveedorResponse;
import com.mitienda.ecommerce.models.Proveedor;
import com.mitienda.ecommerce.repositories.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de proveedores
 */
@Service
// Lectura dentro de transacción por defecto: con spring.jpa.open-in-view=false
// no hay sesión de Hibernate fuera de la transacción, y los DTO de respuesta se
// arman recorriendo relaciones perezosas. Sin esto, los endpoints de lectura
// fallaban con LazyInitializationException.
// Los métodos que escriben llevan su propio @Transactional, que tiene precedencia.
@Transactional(readOnly = true)
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    private final RegistroAuditoria registroAuditoria;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public ProveedorService(ProveedorRepository proveedorRepository, RegistroAuditoria registroAuditoria) {
        this.proveedorRepository = proveedorRepository;
        this.registroAuditoria = registroAuditoria;
    }


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
        proveedor.setEmail(request.getEmail());
        proveedor.setNotas(request.getNotas());
        proveedor.setActivo(request.getActivo());

        Proveedor savedProveedor = proveedorRepository.save(proveedor);
        registroAuditoria.registrar("CREAR_PROVEEDOR", "proveedores", savedProveedor.getId(),
                "Alta de " + savedProveedor.getNombreEmpresa());

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
        proveedor.setEmail(request.getEmail());
        proveedor.setNotas(request.getNotas());
        proveedor.setActivo(request.getActivo());

        Proveedor updatedProveedor = proveedorRepository.save(proveedor);

        registroAuditoria.registrar("ACTUALIZAR_PROVEEDOR", "proveedores", updatedProveedor.getId(),
                "Edicion de " + updatedProveedor.getNombreEmpresa());

        return new ProveedorResponse(updatedProveedor);
    }

    /**
     * Activar/Desactivar proveedor. Es la única baja que existe: no hay un
     * "eliminar" aparte (ver misma decisión en ClienteService).
     */
    @Transactional
    public ProveedorResponse toggleProveedorStatus(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));

        proveedor.setActivo(!proveedor.getActivo());
        Proveedor updatedProveedor = proveedorRepository.save(proveedor);

        registroAuditoria.registrar(
                Boolean.TRUE.equals(updatedProveedor.getActivo()) ? "ACTIVAR_PROVEEDOR" : "DESACTIVAR_PROVEEDOR",
                "proveedores", updatedProveedor.getId(), updatedProveedor.getNombreEmpresa());

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
}