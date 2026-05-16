package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para wishlists
 */
@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    /**
     * Buscar wishlist de un cliente
     */
    Optional<Wishlist> findByClienteId(Long clienteId);

    /**
     * Verificar si un cliente tiene wishlist
     */
    Boolean existsByClienteId(Long clienteId);
}