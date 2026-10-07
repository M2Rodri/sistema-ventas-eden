package com.mitienda.ecommerce.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Poner o cambiar la fecha límite del pago pendiente de una venta ya
 * registrada (ADMIN y EMPLEADO).
 *
 * Se manda UNA de las dos cosas:
 *   - plazoDiasPago: "dentro de N días". El servidor calcula la fecha con su
 *     propio reloj, así que no depende de la hora del teléfono ni del
 *     navegador.
 *   - fechaLimitePago: una fecha exacta (yyyy-MM-dd), que no puede ser
 *     anterior a hoy.
 * Si vienen las dos, manda plazoDiasPago.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FechaLimitePagoRequest {

    private LocalDate fechaLimitePago;

    @Min(value = 1, message = "El plazo debe ser de al menos 1 día")
    @Max(value = 365, message = "El plazo no puede superar 365 días")
    private Integer plazoDiasPago;
}
