package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio para operaciones con promociones
 */
public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    /**
     * Listar solo promociones activas
     */
    List<Promocion> findByActivoTrue();

    /**
     * Promociones vigentes (activas y dentro del rango de fechas)
     */
    @Query("SELECT o FROM Promocion o WHERE o.activo = true AND :fecha BETWEEN o.fechaInicio AND o.fechaFin")
    List<Promocion> findPromocionesVigentes(LocalDate fecha);

    /**
     * Contar promociones activas
     */
    Long countByActivo(Boolean activo);
}