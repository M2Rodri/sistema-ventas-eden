package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Wishlist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO para respuestas de wishlist
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponse {

    private Long id;
    private Long idCliente;
    private String nombreCliente;
    private List<WishlistItemDTO> items;
    private Integer cantidadItems;
    private LocalDateTime fechaCreacion;

    // Constructor desde entidad
    public WishlistResponse(Wishlist wishlist) {
        this.id = wishlist.getId();
        this.idCliente = wishlist.getCliente().getId();
        this.nombreCliente = wishlist.getCliente().getNombreCompleto();
        this.items = wishlist.getItems().stream()
                .map(WishlistItemDTO::new)
                .collect(Collectors.toList());
        this.cantidadItems = wishlist.contarItems();
        this.fechaCreacion = wishlist.getFechaCreacion();
    }
}