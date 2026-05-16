package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Auditoria;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de auditoría
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaResponse {

    private Long id;
    private Long idUsuario;
    private String nombreUsuario;
    private String accion;
    private String tablaAfectada;
    private String idRegistro;
    private String detalles;
    private LocalDateTime fechaHora;
    private String ipDispositivo;

    // Constructor desde entidad
    public AuditoriaResponse(Auditoria auditoria) {
        this.id = auditoria.getId();
        this.idUsuario = auditoria.getUsuario() != null ? auditoria.getUsuario().getId() : null;
        this.nombreUsuario = auditoria.getUsuario() != null ? auditoria.getUsuario().getNombreCompleto() : "Sistema";
        this.accion = auditoria.getAccion();
        this.tablaAfectada = auditoria.getTablaAfectada();
        this.idRegistro = auditoria.getIdRegistro();
        this.detalles = auditoria.getDetalles();
        this.fechaHora = auditoria.getFechaHora();
        this.ipDispositivo = auditoria.getIpDispositivo();
    }
}