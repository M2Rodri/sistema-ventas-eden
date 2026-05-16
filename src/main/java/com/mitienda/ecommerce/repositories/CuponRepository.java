package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Cupon;
import com.mitienda.ecommerce.models.TipoCupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para cupones
 */
@Repository
public interface CuponRepository extends JpaRepository<Cupon, Long> {

    /**
     * Buscar cupón por código
     */
    Optional<Cupon> findByCodigo(String codigo);

    /**
     * Verificar si existe cupón con ese código
     */
    Boolean existsByCodigo(String codigo);

    /**
     * Listar cupones activos
     */
    List<Cupon> findByActivoTrue();

    /**
     * Cupones vigentes (activos y dentro del rango de fechas)
     */
    @Query("SELECT c FROM Cupon c WHERE c.activo = true AND :fecha BETWEEN c.fechaInicio AND c.fechaFin")
    List<Cupon> findCuponesVigentes(LocalDate fecha);

    /**
     * Cupones por tipo
     */
    List<Cupon> findByTipoCupon(TipoCupon tipoCupon);

    /**
     * Contar cupones activos
     */
    Long countByActivo(Boolean activo);
}