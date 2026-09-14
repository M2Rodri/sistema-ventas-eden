package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Promocion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO para respuestas de promocion
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromocionResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal descuento;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Boolean activo;
    private Boolean vigente;
    private List<ProductoSimpleDTO> productos;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    // Constructor desde entidad
    public PromocionResponse(Promocion promocion) {
        this.id = promocion.getId();
        this.nombre = promocion.getNombre();
        this.descripcion = promocion.getDescripcion();
        this.descuento = promocion.getDescuento();
        this.fechaInicio = promocion.getFechaInicio();
        this.fechaFin = promocion.getFechaFin();
        this.activo = promocion.getActivo();
        this.vigente = promocion.estaVigente();
        this.productos = promocion.getProductos().stream()
                .map(p -> new ProductoSimpleDTO(p.getId(), p.getNombre(), p.getSku()))
                .collect(Collectors.toList());
        this.fechaCreacion = promocion.getFechaCreacion();
        this.fechaActualizacion = promocion.getFechaActualizacion();
    }

    /**
     * DTO simple para productos dentro de promociones
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoSimpleDTO {
        private Long id;
        private String nombre;
        private String sku;
    }
}