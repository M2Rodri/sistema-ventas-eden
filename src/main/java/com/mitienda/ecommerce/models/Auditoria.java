package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad Auditoria - Registro de acciones del sistema
 */
@Entity
@Table(name = "auditorias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @NotBlank(message = "La acción es obligatoria")
    @Size(max = 100, message = "La acción no puede exceder 100 caracteres")
    @Column(nullable = false, length = 100)
    private String accion; // Ej: "CREAR_PRODUCTO", "ELIMINAR_USUARIO", "ACTUALIZAR_VENTA"

    @NotBlank(message = "La tabla afectada es obligatoria")
    @Size(max = 50, message = "La tabla no puede exceder 50 caracteres")
    @Column(nullable = false, length = 50)
    private String tablaAfectada;

    @Column(length = 20)
    private String idRegistro; // ID del registro afectado

    @Column(length = 1000)
    private String detalles; // Detalles adicionales en JSON

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(length = 50)
    private String ipDispositivo;

    // Constructor personalizado
    public Auditoria(Usuario usuario, String accion, String tablaAfectada, String idRegistro, String ipDispositivo) {
        this.usuario = usuario;
        this.accion = accion;
        this.tablaAfectada = tablaAfectada;
        this.idRegistro = idRegistro;
        this.ipDispositivo = ipDispositivo;
    }
}