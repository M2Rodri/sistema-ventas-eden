package com.mitienda.ecommerce.models;

import com.mitienda.ecommerce.config.GeneradorIdSinHuecos;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
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

/** Personal del negocio, los únicos que inician sesión (tabla 'usuarios'). */
@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(generator = "id_usuarios")
    @GenericGenerator(name = "id_usuarios", type = GeneradorIdSinHuecos.class,
            parameters = @Parameter(name = "tabla", value = "usuarios"))
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Column(nullable = false, length = 50)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    @Column(nullable = false, length = 50)
    private String apellido;

    /** Nombre para iniciar sesión (no es un correo); siempre en minúsculas, ver UsuarioService. */
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

    /** Rol del usuario (tabla 'roles'); EAGER porque se necesita en cada autenticación. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Role rol;

    @Column(nullable = false)
    private Boolean activo = true; // Usuario activo por defecto

    /** Corte de sesiones: instante (ms UTC) desde el que valen las sesiones; vacío = sin corte. */
    @Column(name = "sesiones_validas_desde")
    private Long sesionesValidasDesde;

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

    /** Nombre técnico del rol (ADMIN, EMPLEADO), expuesto como texto plano. */
    public String getRoleName() {
        return this.rol != null ? this.rol.getNombre() : null;
    }

    // Método para obtener el nombre completo
    public String getNombreCompleto() {
        return this.nombre + " " + this.apellido;
    }
}