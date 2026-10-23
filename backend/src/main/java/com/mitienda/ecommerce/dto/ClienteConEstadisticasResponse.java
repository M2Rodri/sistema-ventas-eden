package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Cliente;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cliente con sus estadísticas de compra, armado con una sola consulta agregada (ver VentaRepository). */
@Getter
@Setter
public class ClienteConEstadisticasResponse extends ClienteResponse {
    private Long numeroCompras;
    private BigDecimal montoTotalComprado;
    private LocalDateTime ultimaFechaCompra;

    public ClienteConEstadisticasResponse(Cliente cliente, Long numeroCompras,
                                           BigDecimal montoTotalComprado, LocalDateTime ultimaFechaCompra) {
        super(cliente);
        this.numeroCompras = numeroCompras;
        this.montoTotalComprado = montoTotalComprado;
        this.ultimaFechaCompra = ultimaFechaCompra;
    }
}
