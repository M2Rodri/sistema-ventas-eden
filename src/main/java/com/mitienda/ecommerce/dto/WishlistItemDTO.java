package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.WishlistItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para items de la wishlist
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemDTO {

    private Long id;
    private Long idProducto;
    private String nombreProducto;
    private String skuProducto;
    private String imagenPrincipal;
    private BigDecimal precio;
    private Boolean disponible;
    private Boolean enStock;
    private LocalDateTime fechaAgregado;

    // Constructor desde entidad
    public WishlistItemDTO(WishlistItem item) {
        this.id = item.getId();
        this.idProducto = item.getProducto().getId();
        this.nombreProducto = item.getProducto().getNombre();
        this.skuProducto = item.getProducto().getSku();
        // Obtener imagen principal si existe
        this.imagenPrincipal = item.getProducto().getImagenes().stream()
                .filter(img -> img.getEsPrincipal())
                .findFirst()
                .map(img -> img.getUrlImagen())
                .orElse(null);
        this.precio = item.getProducto().getPrecioVenta();
        this.disponible = item.getProducto().getActivo();
        // Verificar stock (simplificado - necesitarías InventarioService)
        this.enStock = true; // Implementar lógica real
        this.fechaAgregado = item.getFechaAgregado();
    }
}