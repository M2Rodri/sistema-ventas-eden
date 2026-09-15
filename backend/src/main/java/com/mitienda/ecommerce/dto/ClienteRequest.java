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

    /**
     * Opcional: una empresa no tiene apellido. La base lo permite nulo y la
     * entidad también; este DTO era el único que seguía exigiéndolo, y por eso
     * no se podía registrar un cliente como "Hotel Las Palmeras".
     */
    @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
    private String apellido;

    @Size(max = 20, message = "El NIT/CI no puede exceder 20 caracteres")
    private String nitCi;

    @NotBlank(message = "El telefono es obligatorio")
    @Size(max = 15, message = "El telefono no puede exceder 15 caracteres")
    private String telefono;

    @Email(message = "El email debe ser válido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    private String email;

    

    private TipoCliente tipoCliente = TipoCliente.INVITADO;

    private Boolean activo = true;
}