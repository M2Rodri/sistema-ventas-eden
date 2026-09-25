package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad Usuario - Representa los usuarios del sistema
 * Personal del negocio: los únicos que inician sesión (tabla 'usuarios')
 */
@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Column(nullable = false, length = 50)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    @Column(nullable = false, length = 50)
    private String apellido;

    /**
     * Nombre de usuario para iniciar sesión. No es un correo: el sistema no
     * manda ni recibe nada ahí, es solo la llave para entrar. Se guarda
     * siempre en minúsculas (ver UsuarioService) porque la comparación en
     * la base es sensible a mayúsculas y minúsculas.
     */
    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 3, max = 30, message = "El usuario debe tener entre 3 y 30 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "El usuario solo puede tener letras, números, puntos y guiones bajos")
    @Column(nullable = false, unique = true, length = 30)
    private String usuario;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    @Column(nullable = false)
    private String password;

    @Column(length = 15)
    private String telefono;

    @Column(length = 200)
    private String direccion;

    /**
     * Rol del usuario. Antes era un enum guardado como texto en users.role;
     * ahora es una relación a la tabla 'roles'.
     * EAGER porque el rol se necesita en cada autenticación.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Role rol;

    @Column(nullable = false)
    private Boolean activo = true; // Usuario activo por defecto

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    // Constructor personalizado para alta de personal
    public Usuario(String nombre, String apellido, String usuario, String password, Role rol) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.usuario = usuario;
        this.password = password;
        this.rol = rol;
        this.activo = true;
    }

    /**
     * Nombre técnico del rol (ADMIN, EMPLEADO).
     * Mantiene el contrato de la API igual que cuando el rol era un enum:
     * hacia afuera se sigue exponiendo como texto plano, no como objeto.
     */
    public String getRoleName() {
        return this.rol != null ? this.rol.getNombre() : null;
    }

    // Método para obtener el nombre completo
    public String getNombreCompleto() {
        return this.nombre + " " + this.apellido;
    }
}