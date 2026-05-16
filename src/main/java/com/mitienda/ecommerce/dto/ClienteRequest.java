package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.TipoCliente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear o actualizar clientes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    private String apellido;

    @Size(max = 20, message = "El NIT/CI no puede exceder 20 caracteres")
    private String nitCi;

    @NotBlank(message = "El celular es obligatorio")
    @Size(max = 15, message = "El celular no puede exceder 15 caracteres")
    private String celular;

    @Email(message = "El correo debe ser válido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String correo;

    @Size(max = 300, message = "La dirección no puede exceder 300 caracteres")
    private String direccion;

    private TipoCliente tipo = TipoCliente.INVITADO;

    private Boolean activo = true;
}