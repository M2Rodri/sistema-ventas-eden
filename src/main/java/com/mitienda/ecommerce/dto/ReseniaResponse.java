package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Resenia;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de reseña
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReseniaResponse {

    private Long id;
    private Long idProducto;
    private String nombreProducto;
    private Long idCliente;
    private String nombreCliente;
    private Integer calificacion;
    private String comentario;
    private Boolean aprobado;
    private LocalDateTime fecha;

    // Constructor desde entidad
    public ReseniaResponse(Resenia resenia) {
        this.id = resenia.getId();
        this.idProducto = resenia.getProducto().getId();
        this.nombreProducto = resenia.getProducto().getNombre();
        this.idCliente = resenia.getCliente().getId();
        this.nombreCliente = resenia.getCliente().getNombreCompleto();
        this.calificacion = resenia.getCalificacion();
        this.comentario = resenia.getComentario();
        this.aprobado = resenia.getAprobado();
        this.fecha = resenia.getFecha();
    }
}