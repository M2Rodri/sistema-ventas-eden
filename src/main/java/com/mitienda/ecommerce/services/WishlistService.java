package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.WishlistResponse;
import com.mitienda.ecommerce.models.Cliente;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.models.Wishlist;
import com.mitienda.ecommerce.models.WishlistItem;
import com.mitienda.ecommerce.repositories.ClienteRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import com.mitienda.ecommerce.repositories.WishlistItemRepository;
import com.mitienda.ecommerce.repositories.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Servicio para gestión de wishlist
 */
@Service
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    /**
     * Obtener wishlist de un cliente (o crear una nueva)
     */
    @Transactional
    public WishlistResponse getOrCreateWishlist(Long idCliente) {
        Optional<Wishlist> wishlistExistente = wishlistRepository.findByClienteId(idCliente);

        if (wishlistExistente.isPresent()) {
            return new WishlistResponse(wishlistExistente.get());
        }

        // Crear nueva wishlist
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + idCliente));

        Wishlist nuevaWishlist = new Wishlist(cliente);
        Wishlist savedWishlist = wishlistRepository.save(nuevaWishlist);
        return new WishlistResponse(savedWishlist);
    }

    /**
     * Obtener wishlist por ID
     */
    public WishlistResponse getWishlistById(Long id) {
        Wishlist wishlist = wishlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Wishlist no encontrada con ID: " + id));
        return new WishlistResponse(wishlist);
    }

    /**
     * Agregar producto a la wishlist
     */
    @Transactional
    public WishlistResponse agregarProducto(Long idCliente, Long idProducto) {
        Wishlist wishlist = wishlistRepository.findByClienteId(idCliente)
                .orElseGet(() -> {
                    Cliente cliente = clienteRepository.findById(idCliente)
                            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + idCliente));
                    return wishlistRepository.save(new Wishlist(cliente));
                });

        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + idProducto));

        // Verificar si el producto ya está en la wishlist
        if (wishlistItemRepository.existsByWishlistIdAndProductoId(wishlist.getId(), idProducto)) {
            throw new RuntimeException("El producto ya está en la wishlist");
        }

        // Agregar nuevo item
        WishlistItem nuevoItem = new WishlistItem(wishlist, producto);
        wishlistItemRepository.save(nuevoItem);

        Wishlist updatedWishlist = wishlistRepository.findById(wishlist.getId()).get();
        return new WishlistResponse(updatedWishlist);
    }

    /**
     * Eliminar producto de la wishlist
     */
    @Transactional
    public WishlistResponse eliminarProducto(Long idCliente, Long idProducto) {
        Wishlist wishlist = wishlistRepository.findByClienteId(idCliente)
                .orElseThrow(() -> new RuntimeException("Wishlist no encontrada para el cliente con ID: " + idCliente));

        WishlistItem item = wishlistItemRepository.findByWishlistIdAndProductoId(wishlist.getId(), idProducto)
                .orElseThrow(() -> new RuntimeException("El producto no está en la wishlist"));

        wishlistItemRepository.delete(item);

        Wishlist updatedWishlist = wishlistRepository.findById(wishlist.getId()).get();
        return new WishlistResponse(updatedWishlist);
    }

    /**
     * Vaciar wishlist
     */
    @Transactional
    public WishlistResponse vaciarWishlist(Long idCliente) {
        Wishlist wishlist = wishlistRepository.findByClienteId(idCliente)
                .orElseThrow(() -> new RuntimeException("Wishlist no encontrada para el cliente con ID: " + idCliente));

        wishlistItemRepository.deleteByWishlistId(wishlist.getId());

        Wishlist updatedWishlist = wishlistRepository.findById(wishlist.getId()).get();
        return new WishlistResponse(updatedWishlist);
    }

    /**
     * Verificar si un producto está en la wishlist
     */
    public Boolean verificarProductoEnWishlist(Long idCliente, Long idProducto) {
        Optional<Wishlist> wishlist = wishlistRepository.findByClienteId(idCliente);
        if (wishlist.isEmpty()) {
            return false;
        }
        return wishlistItemRepository.existsByWishlistIdAndProductoId(wishlist.get().getId(), idProducto);
    }
}