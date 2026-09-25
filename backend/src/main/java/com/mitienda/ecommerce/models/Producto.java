package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Producto - Representa los productos del catálogo
 */
@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El SKU es obligatorio")
    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 200, message = "El nombre debe tener entre 2 y 200 caracteres")
    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 2000)
    private String descripcion;

    @Column(length = 100)
    private String modelo;

    /**
     * Campos propios del rubro que la base ya tenía y la entidad no mapeaba.
     * En camas y colchones son criterios de compra reales: el cliente pregunta
     * por la marca, por qué tan firme es y de qué está hecho el núcleo.
     */
    @Column(length = 100)
    private String marca;

    @Column(length = 50)
    private String firmeza;

    @Column(name = "material_nucleo", length = 100)
    private String materialNucleo;

    @Column(length = 50)
    private String color;

    @Column(name = "material_armazon", length = 50)
    private String materialArmazon;

    @NotNull(message = "La categoría es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @Column(length = 50)
    private String calidad;

    // Opcional a propósito: a veces se carga el producto rápido (compra por
    // mayor, costo todavía sin definir) y se completa después editando. Sin
    // este dato, la validación de "no vender por debajo del costo" y el
    // valor total del inventario simplemente lo dejan afuera del cálculo.
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio de compra debe ser mayor a 0")
    @Column(precision = 10, scale = 2)
    private BigDecimal precioCompra;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio de venta debe ser mayor a 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioVenta;

    @Column(length = 100)
    private String dimensiones; // ej: "200x180x30 cm"

    @Column(nullable = false)
    private Integer stockMinimo = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoProducto tipoProducto;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    // Relación con imágenes
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ImagenProducto> imagenes = new ArrayList<>();

    // Constructor personalizado
    public Producto(String sku, String nombre, Categoria categoria, BigDecimal precioVenta, TipoProducto tipoProducto) {
        this.sku = sku;
        this.nombre = nombre;
        this.categoria = categoria;
        this.precioVenta = precioVenta;
        this.tipoProducto = tipoProducto;
        this.activo = true;
    }
}