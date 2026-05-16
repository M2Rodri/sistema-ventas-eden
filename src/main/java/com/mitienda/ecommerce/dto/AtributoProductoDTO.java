package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.AtributoProducto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para atributos de productos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtributoProductoDTO {

    private Long id;

    @NotBlank(message = "El nombre del atributo es obligatorio")
    @Size(max = 50, message = "El nombre del atributo no puede exceder 50 caracteres")
    private String nombreAtributo;

    @NotBlank(message = "El valor del atributo es obligatorio")
    @Size(max = 100, message = "El valor del atributo no puede exceder 100 caracteres")
    private String valorAtributo;

    // Constructor desde entidad
    public AtributoProductoDTO(AtributoProducto atributo) {
        this.id = atributo.getId();
        this.nombreAtributo = atributo.getNombreAtributo();
        this.valorAtributo = atributo.getValorAtributo();
    }
}