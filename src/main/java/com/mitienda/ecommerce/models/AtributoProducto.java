package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad AtributoProducto - Atributos personalizables de productos
 * Ejemplo: Cama → {tamaño: "King", material: "Madera", color: "Negro"}
 */
@Entity
@Table(name = "atributos_producto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtributoProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @NotBlank(message = "El nombre del atributo es obligatorio")
    @Size(max = 50, message = "El nombre del atributo no puede exceder 50 caracteres")
    @Column(nullable = false, length = 50)
    private String nombreAtributo; // Ej: "tamaño", "material", "color", "firmeza"

    @NotBlank(message = "El valor del atributo es obligatorio")
    @Size(max = 100, message = "El valor del atributo no puede exceder 100 caracteres")
    @Column(nullable = false, length = 100)
    private String valorAtributo; // Ej: "King", "Madera", "Negro", "Media"

    // Constructor personalizado
    public AtributoProducto(Producto producto, String nombreAtributo, String valorAtributo) {
        this.producto = producto;
        this.nombreAtributo = nombreAtributo;
        this.valorAtributo = valorAtributo;
    }
}