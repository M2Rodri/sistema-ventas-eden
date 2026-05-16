package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Cupon;
import com.mitienda.ecommerce.models.TipoCupon;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para respuestas de cupón
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuponResponse {

    private Long id;
    private String codigo;
    private String descripcion;
    private TipoCupon tipoCupon;
    private BigDecimal valorDescuento;
    private BigDecimal montoMinimo;
    private BigDecimal descuentoMaximo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Integer usoMaximo;
    private Integer vecesUsado;
    private Boolean activo;
    private Boolean vigente;
    private Boolean primeraCompra;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    // Constructor desde entidad
    public CuponResponse(Cupon cupon) {
        this.id = cupon.getId();
        this.codigo = cupon.getCodigo();
        this.descripcion = cupon.getDescripcion();
        this.tipoCupon = cupon.getTipoCupon();
        this.valorDescuento = cupon.getValorDescuento();
        this.montoMinimo = cupon.getMontoMinimo();
        this.descuentoMaximo = cupon.getDescuentoMaximo();
        this.fechaInicio = cupon.getFechaInicio();
        this.fechaFin = cupon.getFechaFin();
        this.usoMaximo = cupon.getUsoMaximo();
        this.vecesUsado = cupon.getVecesUsado();
        this.activo = cupon.getActivo();
        this.vigente = cupon.estaVigente();
        this.primeraCompra = cupon.getPrimeraCompra();
        this.fechaCreacion = cupon.getFechaCreacion();
        this.fechaActualizacion = cupon.getFechaActualizacion();
    }
}