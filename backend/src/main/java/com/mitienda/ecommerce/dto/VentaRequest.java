package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.EstadoEntrega;
import com.mitienda.ecommerce.models.MetodoPago;
import com.mitienda.ecommerce.models.ModalidadEntrega;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;  // ← ESTA LÍNEA FALTABA
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para crear venta directa (sin pedido previo)
 * Soporta dos modos:
 * 1. Cliente registrado: Se envía idCliente
 * 2. Cliente de mostrador: se envian nombre y telefono, y el backend crea un Cliente tipo INVITADO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaRequest {

    // OPCIÓN 1: Cliente registrado (ID de la tabla clientes)
    private Long idCliente;

    // OPCIÓN 2: Cliente rápido (datos directos)
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombreClienteInvitado;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefonoClienteInvitado;

    @Size(max = 20, message = "El CI no puede exceder 20 caracteres")
    private String ciClienteInvitado;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    @Size(max = 100, message = "La referencia no puede exceder 100 caracteres")
    private String referenciaPago;

    @NotNull(message = "Los productos son obligatorios")
    @Size(min = 1, message = "Debe incluir al menos un producto")
    private List<ItemVentaRequest> items;

    /**
     * Monto efectivamente cobrado al registrar la venta. Opcional: si no
     * viene, se asume que se cobró el total completo (comportamiento previo
     * a la venta a crédito).
     */
    private BigDecimal montoPagado;

    /**
     * Hasta cuándo se espera el pago del saldo pendiente (yyyy-MM-dd).
     * Opcional: solo se guarda si la venta queda con saldo pendiente y no
     * puede ser anterior a hoy.
     */
    private LocalDate fechaLimitePago;

    /**
     * Alternativa a fechaLimitePago: "dentro de N días". El servidor calcula la
     * fecha con su propio reloj (no depende de la hora del teléfono ni del
     * navegador). Si vienen las dos, manda este.
     */
    @Min(value = 1, message = "El plazo debe ser de al menos 1 día")
    @Max(value = 365, message = "El plazo no puede superar 365 días")
    private Integer plazoDiasPago;

    /**
     * Modalidad de entrega. Opcional: si no viene, se asume RETIRO (el
     * cliente se lleva la mercadería en el momento).
     */
    private ModalidadEntrega modalidadEntrega;

    /**
     * Estado de entrega con el que se registra la venta. Opcional: si no
     * viene, queda PENDIENTE (en RETIRO siempre queda ENTREGADO).
     *
     * Reglas (las valida VentaService):
     *   PENDIENTE:  cualquier modalidad.
     *   ENTREGADO:  DOMICILIO o TRANSPORTADORA, para ADMIN y EMPLEADO (a
     *               veces la venta se registra cuando todo ya terminó).
     */
    private EstadoEntrega estadoEntrega;

    // DOMICILIO: opcional, sirve para coordinar la entrega.
    // TRANSPORTADORA: opcional.
    @Size(max = 300, message = "La dirección no puede exceder 300 caracteres")
    private String direccionDestino;

    // Obligatoria solo para TRANSPORTADORA. En DOMICILIO no se pide.
    @Size(max = 50, message = "La ciudad no puede exceder 50 caracteres")
    private String ciudad;

    // Opcionales, solo para TRANSPORTADORA (se pueden completar después)
    @Size(max = 100, message = "El nombre de la transportadora no puede exceder 100 caracteres")
    private String transportadora;

    @Size(max = 100, message = "La guía de remisión no puede exceder 100 caracteres")
    private String guiaRemision;

    /**
     * Valida que se haya proporcionado al menos una forma de identificar al cliente
     */
    public boolean tieneCliente() {
        return idCliente != null || (nombreClienteInvitado != null && !nombreClienteInvitado.trim().isEmpty());
    }

    /**
     * Determina si es una venta con cliente registrado
     */
    public boolean esClienteRegistrado() {
        return idCliente != null;
    }

    /**
     * Determina si es una venta con cliente rápido
     */
    public boolean esClienteRapido() {
        return !esClienteRegistrado() && nombreClienteInvitado != null && !nombreClienteInvitado.trim().isEmpty();
    }

    /**
     * Clase interna para items de la venta
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemVentaRequest {

        @NotNull(message = "El ID del producto es obligatorio")
        private Long idProducto;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        private Integer cantidad;

        // Precio con descuento aplicado (opcional)
        private BigDecimal precioUnitarioConDescuento;

        // Porcentaje de descuento aplicado (opcional, para auditoría)
        private BigDecimal descuentoPorcentaje;
    }
}