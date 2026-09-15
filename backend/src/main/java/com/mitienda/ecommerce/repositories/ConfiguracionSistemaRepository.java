package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.ConfiguracionSistema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio para configuraciones del sistema
 */
public interface ConfiguracionSistemaRepository extends JpaRepository<ConfiguracionSistema, Long> {

    /**
     * Buscar configuración por clave
     */
    Optional<ConfiguracionSistema> findByClave(String clave);

    /**
     * Verificar si existe una configuración con esa clave
     */
    Boolean existsByClave(String clave);
}