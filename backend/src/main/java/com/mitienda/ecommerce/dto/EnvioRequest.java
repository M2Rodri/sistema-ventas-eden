package com.mitienda.ecommerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para crear o actualizar envíos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnvioRequest {


    private Long idVenta;

    @NotBlank(message = "La dirección de destino es obligatoria")
    @Size(max = 300, message = "La dirección no puede exceder 300 caracteres")
    private String direccionDestino;

    @Size(max = 50, message = "La ciudad no puede exceder 50 caracteres")
    private String ciudad;

    @Size(max = 50, message = "El departamento no puede exceder 50 caracteres")
    private String departamento;

    @NotNull(message = "La fecha de entrega estimada es obligatoria")
    private LocalDate fechaEntregaEstimada;

    @Size(max = 100, message = "La guía de remisión no puede exceder 100 caracteres")
    private String guiaRemision;

    @DecimalMin(value = "0.0", message = "El costo de envío no puede ser negativo")
    private BigDecimal costoEnvio = BigDecimal.ZERO;

    private Long idTransportadora;

    /** Empleado responsable del envío ante el cliente. Opcional. */
    private Long idUsuarioResponsable;

    @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
    private String notas;
}