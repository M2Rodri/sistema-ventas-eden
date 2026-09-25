package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Cliente;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cliente junto con sus estadísticas de compra, para la tabla de Clientes.
 *
 * Antes el frontend armaba esto pidiendo el historial de cada cliente por
 * separado (una consulta HTTP por fila de la tabla). Acá se arma con una
 * sola consulta agregada (ver VentaRepository#aggregarEstadisticasPorCliente),
 * así que la pantalla entera sale en dos consultas en vez de N+1.
 */
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
