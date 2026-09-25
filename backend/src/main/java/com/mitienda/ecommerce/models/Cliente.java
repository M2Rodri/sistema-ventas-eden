package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Cliente - Quien compra en el negocio (tabla 'clientes').
 *
 * No tiene credenciales: los clientes no inician sesión, los registra el
 * vendedor en el mostrador. El personal del sistema vive en la entidad Usuario.
 *
 * Campos alineados con el esquema tras el ajuste de la base:
 *   correo    -> email      (unificado con usuarios, proveedores, transportadoras)
 *   celular   -> telefono   (unificado con el resto del esquema)
 *   tipo      -> tipoCliente (una columna llamada solo "tipo" era ambigua)
 *   direccion -> eliminada  (las direcciones viven en direcciones_cliente,
 *                            que admite varias y marca una como principal)
 */
@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nombre;

    /** Opcional: una empresa no tiene apellido. Coincide con la base, que lo permite nulo. */
    @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
    @Column(length = 100)
    private String apellido;

    /** NIT o CI, para la facturación. */
    @Column(name = "nit_ci", length = 20)
    private String nitCi;

    @Size(max = 15, message = "El teléfono no puede exceder 15 caracteres")
    @Column(length = 15)
    private String telefono;

    @Email(message = "El email debe ser válido")
    @Column(length = 100)
    private String email;

    /**
     * El sistema ya no distingue INVITADO de REGISTRADO (decisión del
     * negocio: no tenía sentido mostrarle esa diferencia a nadie). El campo
     * se deja porque sigue siendo parte del modelo de datos, pero todo
     * cliente nuevo queda REGISTRADO por igual.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cliente", length = 20)
    private TipoCliente tipoCliente = TipoCliente.REGISTRADO;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    // Relación con ventas. Sin cascade: las ventas las administra VentaService
    // de forma independiente, y un cliente nunca se borra de verdad (solo se
    // desactiva, ver activo). Con CascadeType.ALL, si alguna vez alguien
    // borrara un Cliente por error, Hibernate se hubiera llevado puesto todo
    // su historial de ventas con él.
    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    private List<Venta> ventas = new ArrayList<>();

    public Cliente(String nombre, String apellido, String telefono, String email) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
        this.email = email;
        this.tipoCliente = TipoCliente.REGISTRADO;
        this.activo = true;
    }

    public String getNombreCompleto() {
        return this.apellido != null && !this.apellido.isBlank()
                ? this.nombre + " " + this.apellido
                : this.nombre;
    }
}
