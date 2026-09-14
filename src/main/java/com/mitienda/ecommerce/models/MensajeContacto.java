package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Consulta enviada desde el formulario de la tienda.
 *
 * Antes el formulario simulaba el envío y no guardaba nada. Como la tienda no
 * tiene carrito ni pedidos, este es el único canal por el que un cliente puede
 * comunicarse con el negocio.
 */
@Entity
@Table(name = "mensajes_contacto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MensajeContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false, length = 150)
    private String asunto;

    @Column(nullable = false, columnDefinition = "text")
    private String mensaje;

    @Column(nullable = false)
    private Boolean atendido = false;

    @Column(name = "fecha_atencion")
    private LocalDateTime fechaAtencion;

    /** Empleado que se hizo cargo de responder la consulta. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_atiende")
    private Usuario usuarioAtiende;

    /** Se registra igual que en auditorías: el formulario es público. */
    @Column(name = "ip_origen", length = 50)
    private String ipOrigen;

    @CreationTimestamp
    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private LocalDateTime fechaEnvio;
}
