package com.mitienda.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultimediaProductoDTO {
    private Long id;
    private Long productoId;
    private String urlModelo3d;
    private String urlVistaPrevia;
    private Boolean habilitadoRa;
    private Boolean activo;
}