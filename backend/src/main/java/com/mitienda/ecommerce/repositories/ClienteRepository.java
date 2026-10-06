package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * Buscar clientes por nombre (búsqueda parcial)
     */
    @Query("SELECT c FROM Cliente c WHERE LOWER(c.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR LOWER(c.apellido) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Cliente> searchByNombre(String nombre);

    /**
     * Contar clientes activos. Usado por DashboardService para el panel de Inicio.
     */
    Long countByActivo(Boolean activo);

    /**
     * Cifras de clientes del panel de Inicio en una sola consulta.
     * Una fila: [total, activos, nuevosHoy, nuevosMes].
     */
    @Query("SELECT COUNT(c), COUNT(CASE WHEN c.activo = true THEN 1 END), "
            + "COUNT(CASE WHEN c.fechaRegistro > :hoy AND c.fechaRegistro < :finHoy THEN 1 END), "
            + "COUNT(CASE WHEN c.fechaRegistro > :inicioMes THEN 1 END) FROM Cliente c")
    List<Object[]> resumenClientes(@Param("hoy") java.time.LocalDateTime hoy,
                                   @Param("finHoy") java.time.LocalDateTime finHoy,
                                   @Param("inicioMes") java.time.LocalDateTime inicioMes);
}
