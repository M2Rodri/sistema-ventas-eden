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
 * Entidad Proveedor - Representa los proveedores de productos
 */
@Entity
@Table(name = "proveedores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(min = 2, max = 200, message = "El nombre debe tener entre 2 y 200 caracteres")
    @Column(nullable = false, length = 200)
    private String nombreEmpresa;

    @NotBlank(message = "El NIT es obligatorio")
    @Column(nullable = false, unique = true, length = 20)
    private String nit;

    @NotBlank(message = "El contacto es obligatorio")
    @Size(max = 100, message = "El contacto no puede exceder 100 caracteres")
    @Column(nullable = false, length = 100)
    private String contacto;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 15, message = "El teléfono no puede exceder 15 caracteres")
    @Column(nullable = false, length = 15)
    private String telefono;

    @Column(length = 300)
    private String direccion;

    @Email(message = "El email debe ser válido")
    @Column(length = 100)
    private String email;

    @Column(length = 500)
    private String notas;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    // Relación con compras
    @OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Compra> compras = new ArrayList<>();

    // Constructor personalizado
    public Proveedor(String nombreEmpresa, String nit, String contacto, String telefono) {
        this.nombreEmpresa = nombreEmpresa;
        this.nit = nit;
        this.contacto = contacto;
        this.telefono = telefono;
        this.activo = true;
    }
}