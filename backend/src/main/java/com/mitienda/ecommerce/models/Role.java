package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Perfil del personal (tabla 'roles'); al ser una tabla se puede agregar un perfil sin cambiar el código. */
@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre técnico del rol: ADMIN, EMPLEADO. Es la clave de búsqueda. */
    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    /** Descripción legible: "Administrador / Dueño", "Vendedor". */
    @Column(length = 200)
    private String descripcion;
}
