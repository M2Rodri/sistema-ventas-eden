package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Proveedor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de proveedor
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorResponse {

    private Long id;
    private String nombreEmpresa;
    private String nit;
    private String contacto;
    private String telefono;
    private String direccion;
    private String email;
    private String notas;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;

    // Constructor desde entidad
    public ProveedorResponse(Proveedor proveedor) {
        this.id = proveedor.getId();
        this.nombreEmpresa = proveedor.getNombreEmpresa();
        this.nit = proveedor.getNit();
        this.contacto = proveedor.getContacto();
        this.telefono = proveedor.getTelefono();
        this.direccion = proveedor.getDireccion();
        this.email = proveedor.getEmail();
        this.notas = proveedor.getNotas();
        this.activo = proveedor.getActivo();
        this.fechaRegistro = proveedor.getFechaRegistro();
        this.fechaActualizacion = proveedor.getFechaActualizacion();
    }
}