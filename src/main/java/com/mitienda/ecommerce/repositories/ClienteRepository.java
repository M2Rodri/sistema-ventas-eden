package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Cliente;
import com.mitienda.ecommerce.models.TipoCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones con clientes
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Buscar cliente por email
     */
    Optional<Cliente> findByEmail(String email);

    /**
     * Buscar cliente por NIT/CI
     */
    Optional<Cliente> findByNitCi(String nitCi);

    /**
     * Buscar cliente por telefono
     */
    Optional<Cliente> findByTelefono(String telefono);

    /**
     * Verificar si existe un cliente con ese email
     */
    Boolean existsByEmail(String email);

    /**
     * Listar solo clientes activos
     */
    List<Cliente> findByActivoTrue();

    /**
     * Filtrar clientes por tipo
     */
    List<Cliente> findByTipoCliente(TipoCliente tipo);

    /**
     * Buscar clientes por nombre (búsqueda parcial)
     */
    @Query("SELECT c FROM Cliente c WHERE LOWER(c.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR LOWER(c.apellido) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Cliente> searchByNombre(String nombre);

    /**
     * Contar clientes activos
     */
    Long countByActivo(Boolean activo);

    /**
     * Contar clientes por tipo
     */
    Long countByTipoCliente(TipoCliente tipo);
}