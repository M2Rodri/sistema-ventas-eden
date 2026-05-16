package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad WishlistItem - Items dentro de la wishlist
 */
@Entity
@Table(name = "wishlist_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La wishlist es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_wishlist", nullable = false)
    private Wishlist wishlist;

    @NotNull(message = "El producto es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaAgregado;

    // Constructor personalizado
    public WishlistItem(Wishlist wishlist, Producto producto) {
        this.wishlist = wishlist;
        this.producto = producto;
    }
}