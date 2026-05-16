package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Transportadora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para operaciones con transportadoras
 */
@Repository
public interface TransportadoraRepository extends JpaRepository<Transportadora, Long> {

    /**
     * Listar solo transportadoras activas
     */
    List<Transportadora> findByActivoTrue();

    /**
     * Contar transportadoras activas
     */
    Long countByActivo(Boolean activo);
}