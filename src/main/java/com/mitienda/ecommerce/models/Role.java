package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad Role - Perfiles del personal del sistema (tabla 'roles')
 *
 * Antes era un enum con los valores ADMIN, EMPLEADO y CLIENTE. Se convirtió en
 * entidad por dos motivos:
 *
 *  1. El rol vivía duplicado: el valor real en users.role y una tabla 'roles'
 *     vacía y sin relacionar.
 *  2. Normalizado, agregar un perfil nuevo (por ejemplo "Almacenero") es un
 *     INSERT desde el sistema, y no modificar el código y volver a desplegar.
 *
 * El valor CLIENTE se eliminó: la tienda web es una vitrina pública sin inicio
 * de sesión. Quien compra se registra en la tabla 'clientes', que no tiene
 * credenciales.
 */
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
