package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para items de wishlist
 */
@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    /**
     * Obtener items de una wishlist
     */
    List<WishlistItem> findByWishlistIdOrderByFechaAgregadoDesc(Long wishlistId);

    /**
     * Buscar item específico en una wishlist
     */
    Optional<WishlistItem> findByWishlistIdAndProductoId(Long wishlistId, Long productoId);

    /**
     * Verificar si un producto está en la wishlist
     */
    Boolean existsByWishlistIdAndProductoId(Long wishlistId, Long productoId);

    /**
     * Eliminar items de una wishlist
     */
    void deleteByWishlistId(Long wishlistId);
}