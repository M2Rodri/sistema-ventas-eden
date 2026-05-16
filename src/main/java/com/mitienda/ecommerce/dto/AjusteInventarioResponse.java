package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.AjusteInventario;
import com.mitienda.ecommerce.models.TipoAjuste;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de ajuste de inventario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AjusteInventarioResponse {

    private Long id;
    private Long idProducto;
    private String nombreProducto;
    private String skuProducto;
    private Integer cantidadAnterior;
    private Integer cantidadNueva;
    private Integer diferencia;
    private String tipoAjuste;
    private String motivo;
    private Long idUsuario;
    private String nombreUsuario;
    private LocalDateTime fecha;

    // Constructor desde entidad
    public AjusteInventarioResponse(AjusteInventario ajuste) {
        this.id = ajuste.getId();
        this.idProducto = ajuste.getProducto().getId();
        this.nombreProducto = ajuste.getProducto().getNombre();
        this.skuProducto = ajuste.getProducto().getSku();
        this.cantidadAnterior = ajuste.getCantidadAnterior();
        this.cantidadNueva = ajuste.getCantidadNueva();
        this.diferencia = ajuste.getDiferencia();
        this.tipoAjuste = ajuste.getTipoAjuste();
        this.motivo = ajuste.getMotivo();
        this.idUsuario = ajuste.getUsuario() != null ? ajuste.getUsuario().getId() : null;
        this.nombreUsuario = ajuste.getUsuario() != null ? ajuste.getUsuario().getNombreCompleto() : "Sistema";
        this.fecha = ajuste.getFecha();
    }
}