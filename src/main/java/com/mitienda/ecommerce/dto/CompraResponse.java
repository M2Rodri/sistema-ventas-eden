package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Compra;
import com.mitienda.ecommerce.models.EstadoCompra;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO para respuestas de compra
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompraResponse {

    private Long id;
    private Long idProveedor;
    private String nombreProveedor;
    private String nitProveedor;
    private LocalDateTime fechaCompra;
    private BigDecimal costoTotal;
    private EstadoCompra estado;
    private Long idUsuario;
    private String nombreUsuario;
    private String notas;
    private List<DetalleCompraDTO> detalles;
    private LocalDateTime fechaActualizacion;

    // Constructor desde entidad
    public CompraResponse(Compra compra) {
        this.id = compra.getId();
        this.idProveedor = compra.getProveedor().getId();
        this.nombreProveedor = compra.getProveedor().getNombreEmpresa();
        this.nitProveedor = compra.getProveedor().getNit();
        this.fechaCompra = compra.getFechaCompra();
        this.costoTotal = compra.getCostoTotal();
        this.estado = compra.getEstado();
        this.idUsuario = compra.getUsuario() != null ? compra.getUsuario().getId() : null;
        this.nombreUsuario = compra.getUsuario() != null ? compra.getUsuario().getNombreCompleto() : null;
        this.notas = compra.getNotas();
        this.detalles = compra.getDetalles().stream()
                .map(DetalleCompraDTO::new)
                .collect(Collectors.toList());
        this.fechaActualizacion = compra.getFechaActualizacion();
    }
}