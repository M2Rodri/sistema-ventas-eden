package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Repositorio para operaciones con proveedores */
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    /** Verificar si existe un proveedor con ese NIT */
    Boolean existsByNit(String nit);

    /** Solo proveedores activos (los que se pueden elegir en una compra nueva). */
    List<Proveedor> findByActivoTrue();

    /** Buscar proveedores por nombre (búsqueda parcial) */
    @Query("SELECT p FROM Proveedor p WHERE LOWER(p.nombreEmpresa) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Proveedor> searchByNombre(String nombre);
}