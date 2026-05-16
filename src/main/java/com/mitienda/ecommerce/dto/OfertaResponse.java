package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Oferta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO para respuestas de oferta
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfertaResponse {

    private Long id;
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
    public OfertaResponse(Oferta oferta) {
        this.id = oferta.getId();
        this.descripcion = oferta.getDescripcion();
        this.descuento = oferta.getDescuento();
        this.fechaInicio = oferta.getFechaInicio();
        this.fechaFin = oferta.getFechaFin();
        this.activo = oferta.getActivo();
        this.vigente = oferta.estaVigente();
        this.productos = oferta.getProductos().stream()
                .map(p -> new ProductoSimpleDTO(p.getId(), p.getNombre(), p.getSku()))
                .collect(Collectors.toList());
        this.fechaCreacion = oferta.getFechaCreacion();
        this.fechaActualizacion = oferta.getFechaActualizacion();
    }

    /**
     * DTO simple para productos dentro de ofertas
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