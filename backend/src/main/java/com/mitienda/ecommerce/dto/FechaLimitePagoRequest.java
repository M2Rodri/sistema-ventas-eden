package com.mitienda.ecommerce.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Fecha límite del pago pendiente: en N días (plazoDiasPago) o una fecha exacta; si vienen las dos, manda el plazo. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FechaLimitePagoRequest {
    private LocalDate fechaLimitePago;

    @Min(value = 1, message = "El plazo debe ser de al menos 1 día")
    @Max(value = 365, message = "El plazo no puede superar 365 días")
    private Integer plazoDiasPago;
}
