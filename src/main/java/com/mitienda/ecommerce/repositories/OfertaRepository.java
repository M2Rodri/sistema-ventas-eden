package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Oferta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio para operaciones con ofertas
 */
@Repository
public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    /**
     * Listar solo ofertas activas
     */
    List<Oferta> findByActivoTrue();

    /**
     * Ofertas vigentes (activas y dentro del rango de fechas)
     */
    @Query("SELECT o FROM Oferta o WHERE o.activo = true AND :fecha BETWEEN o.fechaInicio AND o.fechaFin")
    List<Oferta> findOfertasVigentes(LocalDate fecha);

    /**
     * Contar ofertas activas
     */
    Long countByActivo(Boolean activo);
}