package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Comprobante;
import com.mitienda.ecommerce.models.TipoComprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para comprobantes
 */
public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    /**
     * Buscar comprobante por número
     */
    Optional<Comprobante> findByNumeroComprobante(String numeroComprobante);

    /**
     * Buscar comprobante por venta
     */
    Optional<Comprobante> findByVentaId(Long ventaId);

    /**
     * Verificar si existe comprobante con ese número
     */
    Boolean existsByNumeroComprobante(String numeroComprobante);

    /**
     * Comprobantes por tipo
     */
    List<Comprobante> findByTipoComprobanteOrderByFechaEmisionDesc(TipoComprobante tipoComprobante);

    /**
     * Comprobantes entre fechas
     */
    List<Comprobante> findByFechaEmisionBetweenOrderByFechaEmisionDesc(LocalDateTime inicio, LocalDateTime fin);

    /**
     * Comprobantes no anulados
     */
    List<Comprobante> findByAnuladoFalseOrderByFechaEmisionDesc();

    /**
     * Comprobantes anulados
     */
    List<Comprobante> findByAnuladoTrueOrderByFechaEmisionDesc();

    /**
     * Último comprobante emitido (para generar número correlativo)
     */
    @Query("SELECT c FROM Comprobante c ORDER BY c.id DESC LIMIT 1")
    Optional<Comprobante> findUltimoComprobante();

    /**
     * Contar comprobantes del mes actual
     */
    @Query("SELECT COUNT(c) FROM Comprobante c WHERE YEAR(c.fechaEmision) = YEAR(CURRENT_DATE) AND MONTH(c.fechaEmision) = MONTH(CURRENT_DATE)")
    Long countComprobantesDelMes();

    /**
     * Último número usado para un prefijo dado (ej: "REC-2026-"), el más
     * alto por orden alfabético. Como el numero va con ceros a la
     * izquierda (00001, 00002...), el orden alfabético coincide con el
     * numérico mientras no se pasen de 5 dígitos.
     */
    @Query("SELECT c.numeroComprobante FROM Comprobante c " +
           "WHERE c.numeroComprobante LIKE CONCAT(:prefijo, '%') " +
           "ORDER BY c.numeroComprobante DESC LIMIT 1")
    Optional<String> findUltimoNumeroConPrefijo(@Param("prefijo") String prefijo);

    /**
     * Últimos comprobantes
     */
    List<Comprobante> findTop20ByOrderByFechaEmisionDesc();
}