package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.AlertaInventario;
import com.mitienda.ecommerce.models.EstadoAlerta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de alertas de inventario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertaInventarioResponse {

    private Long id;
    private Long idProducto;
    private String nombreProducto;
    private String skuProducto;
    private Integer cantidadActual;
    private Integer cantidadMinima;
    private LocalDateTime fechaAlerta;
    private EstadoAlerta estado;

    // Constructor desde entidad
    public AlertaInventarioResponse(AlertaInventario alerta) {
        this.id = alerta.getId();
        this.idProducto = alerta.getProducto().getId();
        this.nombreProducto = alerta.getProducto().getNombre();
        this.skuProducto = alerta.getProducto().getSku();
        this.cantidadActual = alerta.getCantidadActual();
        this.cantidadMinima = alerta.getCantidadMinima();
        this.fechaAlerta = alerta.getFechaAlerta();
        this.estado = alerta.getEstado();
    }
}