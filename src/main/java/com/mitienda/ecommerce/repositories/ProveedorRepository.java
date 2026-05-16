package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones con proveedores
 */
@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    /**
     * Buscar proveedor por NIT
     */
    Optional<Proveedor> findByNit(String nit);

    /**
     * Verificar si existe un proveedor con ese NIT
     */
    Boolean existsByNit(String nit);

    /**
     * Listar solo proveedores activos
     */
    List<Proveedor> findByActivoTrue();

    /**
     * Buscar proveedores por nombre (búsqueda parcial)
     */
    @Query("SELECT p FROM Proveedor p WHERE LOWER(p.nombreEmpresa) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Proveedor> searchByNombre(String nombre);

    /**
     * Contar proveedores activos
     */
    Long countByActivo(Boolean activo);
}