package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.EstadoEntrega;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A qué estado vuelve una venta al deshacer su entrega (solo ADMIN).
 *
 * Solo importa en una venta por TRANSPORTADORA que está ENTREGADA: puede
 * haberse entregado sin pasar por el despacho, así que el ADMIN elige si
 * vuelve a PENDIENTE o a DESPACHADO. Si no elige, vuelve a DESPACHADO (un
 * paso atrás). En los demás casos el destino está fijo y este campo se ignora.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeshacerEntregaRequest {

    private EstadoEntrega estadoEntrega;
}
