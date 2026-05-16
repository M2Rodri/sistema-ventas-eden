package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Envio;
import com.mitienda.ecommerce.models.EstadoEnvio;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnvioResponse {

    private Long id;
    private Long idVenta;
    private String nombreCliente;
    private String celularCliente;
    private String direccionDestino;
    private String ciudad;
    private String departamento;
    private LocalDate fechaEntregaEstimada;
    private LocalDate fechaEntregaReal;
    private EstadoEnvio estadoSeguimiento;
    private String guiaRemision;
    private BigDecimal costoEnvio;
    private Long idTransportadora;
    private String nombreTransportadora;
    private String notas;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public EnvioResponse(Envio envio) {
        this.id = envio.getId();
        this.idVenta = envio.getVenta() != null ? envio.getVenta().getId() : null;

        if (envio.getVenta() != null) {
            var venta = envio.getVenta();
            this.nombreCliente = venta.getNombreClienteCompleto();
            this.celularCliente = venta.getCelularClienteCompleto();
        }

        this.direccionDestino = envio.getDireccionDestino();
        this.ciudad = envio.getCiudad();
        this.departamento = envio.getDepartamento();
        this.fechaEntregaEstimada = envio.getFechaEntregaEstimada();
        this.fechaEntregaReal = envio.getFechaEntregaReal();
        this.estadoSeguimiento = envio.getEstadoSeguimiento();
        this.guiaRemision = envio.getGuiaRemision();
        this.costoEnvio = envio.getCostoEnvio();
        this.idTransportadora = envio.getTransportadora() != null ? envio.getTransportadora().getId() : null;
        this.nombreTransportadora = envio.getTransportadora() != null ? envio.getTransportadora().getNombre() : null;
        this.notas = envio.getNotas();
        this.fechaCreacion = envio.getFechaCreacion();
        this.fechaActualizacion = envio.getFechaActualizacion();
    }
}