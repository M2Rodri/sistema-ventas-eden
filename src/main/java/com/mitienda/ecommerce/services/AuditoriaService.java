package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.AuditoriaResponse;
import com.mitienda.ecommerce.models.Auditoria;
import com.mitienda.ecommerce.models.User;
import com.mitienda.ecommerce.repositories.AuditoriaRepository;
import com.mitienda.ecommerce.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de auditorías
 */
@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Listar todas las auditorías
     */
    public List<AuditoriaResponse> getAllAuditorias() {
        return auditoriaRepository.findAll()
                .stream()
                .map(AuditoriaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener auditoría por ID
     */
    public AuditoriaResponse getAuditoriaById(Long id) {
        Auditoria auditoria = auditoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auditoría no encontrada con ID: " + id));
        return new AuditoriaResponse(auditoria);
    }

    /**
     * Registrar una auditoría
     */
    @Transactional
    public AuditoriaResponse registrarAuditoria(Long idUsuario, String accion, String tablaAfectada, 
                                               String idRegistro, String detalles, String ipDispositivo) {
        User usuario = null;
        if (idUsuario != null) {
            usuario = userRepository.findById(idUsuario).orElse(null);
        }

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(usuario);
        auditoria.setAccion(accion);
        auditoria.setTablaAfectada(tablaAfectada);
        auditoria.setIdRegistro(idRegistro);
        auditoria.setDetalles(detalles);
        auditoria.setIpDispositivo(ipDispositivo);

        Auditoria savedAuditoria = auditoriaRepository.save(auditoria);
        return new AuditoriaResponse(savedAuditoria);
    }

    /**
     * Auditorías de un usuario
     */
    public List<AuditoriaResponse> getAuditoriasByUsuario(Long idUsuario) {
        return auditoriaRepository.findByUsuarioIdOrderByFechaHoraDesc(idUsuario)
                .stream()
                .map(AuditoriaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Auditorías de una tabla específica
     */
    public List<AuditoriaResponse> getAuditoriasByTabla(String tablaAfectada) {
        return auditoriaRepository.findByTablaAfectadaOrderByFechaHoraDesc(tablaAfectada)
                .stream()
                .map(AuditoriaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Auditorías de una acción específica
     */
    public List<AuditoriaResponse> getAuditoriasByAccion(String accion) {
        return auditoriaRepository.findByAccionOrderByFechaHoraDesc(accion)
                .stream()
                .map(AuditoriaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Auditorías entre fechas
     */
    public List<AuditoriaResponse> getAuditoriasByFechas(LocalDateTime inicio, LocalDateTime fin) {
        return auditoriaRepository.findByFechaHoraBetweenOrderByFechaHoraDesc(inicio, fin)
                .stream()
                .map(AuditoriaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Últimas 100 auditorías
     */
    public List<AuditoriaResponse> getUltimasAuditorias() {
        return auditoriaRepository.findTop100ByOrderByFechaHoraDesc()
                .stream()
                .map(AuditoriaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Auditorías del día
     */
    public List<AuditoriaResponse> getAuditoriasDelDia() {
        return auditoriaRepository.findAuditoriasDelDia()
                .stream()
                .map(AuditoriaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Métodos helper para registrar acciones comunes
     */

    public void registrarCreacion(Long idUsuario, String tabla, String idRegistro, String ip) {
        registrarAuditoria(idUsuario, "CREAR_" + tabla.toUpperCase(), tabla, idRegistro, null, ip);
    }

    public void registrarActualizacion(Long idUsuario, String tabla, String idRegistro, String ip) {
        registrarAuditoria(idUsuario, "ACTUALIZAR_" + tabla.toUpperCase(), tabla, idRegistro, null, ip);
    }

    public void registrarEliminacion(Long idUsuario, String tabla, String idRegistro, String ip) {
        registrarAuditoria(idUsuario, "ELIMINAR_" + tabla.toUpperCase(), tabla, idRegistro, null, ip);
    }

    public void registrarLogin(Long idUsuario, String ip) {
        registrarAuditoria(idUsuario, "LOGIN", "users", idUsuario != null ? idUsuario.toString() : null, "Inicio de sesión", ip);
    }

    public void registrarLogout(Long idUsuario, String ip) {
        registrarAuditoria(idUsuario, "LOGOUT", "users", idUsuario != null ? idUsuario.toString() : null, "Cierre de sesión", ip);
    }
}