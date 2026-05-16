package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Resenia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para operaciones con reseñas
 */
@Repository
public interface ReseniaRepository extends JpaRepository<Resenia, Long> {

    /**
     * Reseñas de un producto (aprobadas)
     */
    List<Resenia> findByProductoIdAndAprobadoTrueOrderByFechaDesc(Long productoId);

    /**
     * Todas las reseñas de un producto (incluso no aprobadas)
     */
    List<Resenia> findByProductoIdOrderByFechaDesc(Long productoId);

    /**
     * Reseñas de un cliente
     */
    List<Resenia> findByClienteIdOrderByFechaDesc(Long clienteId);

    /**
     * Reseñas pendientes de aprobación
     */
    List<Resenia> findByAprobadoFalseOrderByFechaDesc();

    /**
     * Calificación promedio de un producto
     */
    @Query("SELECT AVG(r.calificacion) FROM Resenia r WHERE r.producto.id = :productoId AND r.aprobado = true")
    Double getCalificacionPromedio(Long productoId);

    /**
     * Contar reseñas aprobadas de un producto
     */
    Long countByProductoIdAndAprobadoTrue(Long productoId);
}