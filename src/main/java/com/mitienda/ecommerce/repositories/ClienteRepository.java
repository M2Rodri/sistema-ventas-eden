package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Cliente;
import com.mitienda.ecommerce.models.TipoCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones con clientes
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Buscar cliente por correo
     */
    Optional<Cliente> findByCorreo(String correo);

    /**
     * Buscar cliente por NIT/CI
     */
    Optional<Cliente> findByNitCi(String nitCi);

    /**
     * Buscar cliente por celular
     */
    Optional<Cliente> findByCelular(String celular);

    /**
     * Verificar si existe un cliente con ese correo
     */
    Boolean existsByCorreo(String correo);

    /**
     * Listar solo clientes activos
     */
    List<Cliente> findByActivoTrue();

    /**
     * Filtrar clientes por tipo
     */
    List<Cliente> findByTipo(TipoCliente tipo);

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
    Long countByTipo(TipoCliente tipo);
}