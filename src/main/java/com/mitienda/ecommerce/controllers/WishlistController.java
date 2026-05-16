package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.WishlistResponse;
import com.mitienda.ecommerce.services.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para gestión de wishlist
 */
@RestController
@RequestMapping("/api/wishlist")
@CrossOrigin(origins = "http://localhost:3000")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    /**
     * GET /api/wishlist/cliente/{idCliente}
     * Obtener wishlist de un cliente
     */
    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<?> getWishlistCliente(@PathVariable Long idCliente) {
        try {
            WishlistResponse wishlist = wishlistService.getOrCreateWishlist(idCliente);
            return ResponseEntity.ok(wishlist);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/wishlist/{id}
     * Obtener wishlist por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getWishlistById(@PathVariable Long id) {
        try {
            WishlistResponse wishlist = wishlistService.getWishlistById(id);
            return ResponseEntity.ok(wishlist);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/wishlist/cliente/{idCliente}/producto/{idProducto}
     * Agregar producto a la wishlist
     */
    @PostMapping("/cliente/{idCliente}/producto/{idProducto}")
    public ResponseEntity<?> agregarProducto(@PathVariable Long idCliente,
                                            @PathVariable Long idProducto) {
        try {
            WishlistResponse wishlist = wishlistService.agregarProducto(idCliente, idProducto);
            return ResponseEntity.ok(wishlist);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/wishlist/cliente/{idCliente}/producto/{idProducto}
     * Eliminar producto de la wishlist
     */
    @DeleteMapping("/cliente/{idCliente}/producto/{idProducto}")
    public ResponseEntity<?> eliminarProducto(@PathVariable Long idCliente,
                                             @PathVariable Long idProducto) {
        try {
            WishlistResponse wishlist = wishlistService.eliminarProducto(idCliente, idProducto);
            return ResponseEntity.ok(wishlist);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/wishlist/cliente/{idCliente}/vaciar
     * Vaciar wishlist
     */
    @DeleteMapping("/cliente/{idCliente}/vaciar")
    public ResponseEntity<?> vaciarWishlist(@PathVariable Long idCliente) {
        try {
            WishlistResponse wishlist = wishlistService.vaciarWishlist(idCliente);
            return ResponseEntity.ok(wishlist);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/wishlist/cliente/{idCliente}/verificar/{idProducto}
     * Verificar si un producto está en la wishlist
     */
    @GetMapping("/cliente/{idCliente}/verificar/{idProducto}")
    public ResponseEntity<?> verificarProductoEnWishlist(@PathVariable Long idCliente,
                                                         @PathVariable Long idProducto) {
        Boolean enWishlist = wishlistService.verificarProductoEnWishlist(idCliente, idProducto);
        return ResponseEntity.ok(Map.of("enWishlist", enWishlist));
    }
}