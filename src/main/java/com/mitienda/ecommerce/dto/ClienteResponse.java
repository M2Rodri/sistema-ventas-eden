package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Cliente;
import com.mitienda.ecommerce.models.TipoCliente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de cliente
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponse {

    private Long id;
    private String nombre;
    private String apellido;
    private String nombreCompleto;
    private String nitCi;
    private String celular;
    private String correo;
    private String direccion;
    private TipoCliente tipo;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;

    // Constructor desde entidad
    public ClienteResponse(Cliente cliente) {
        this.id = cliente.getId();
        this.nombre = cliente.getNombre();
        this.apellido = cliente.getApellido();
        this.nombreCompleto = cliente.getNombreCompleto();
        this.nitCi = cliente.getNitCi();
        this.celular = cliente.getCelular();
        this.correo = cliente.getCorreo();
        this.direccion = cliente.getDireccion();
        this.tipo = cliente.getTipo();
        this.activo = cliente.getActivo();
        this.fechaRegistro = cliente.getFechaRegistro();
        this.fechaActualizacion = cliente.getFechaActualizacion();
    }
}