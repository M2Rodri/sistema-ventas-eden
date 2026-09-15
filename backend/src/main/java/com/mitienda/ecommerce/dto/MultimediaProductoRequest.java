package com.mitienda.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultimediaProductoRequest {

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    @Size(max = 500, message = "La URL del modelo 3D no puede exceder 500 caracteres")
    private String urlModelo3d;

    @Size(max = 500, message = "La URL de vista previa no puede exceder 500 caracteres")
    private String urlVistaPrevia;

    private Boolean habilitadoRa = false;
}