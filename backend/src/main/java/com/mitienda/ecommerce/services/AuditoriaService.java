package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.AuditoriaResponse;
import com.mitienda.ecommerce.models.Auditoria;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.AuditoriaRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de auditorías
 */
@Service
// Lectura dentro de transacción por defecto: con spring.jpa.open-in-view=false
// no hay sesión de Hibernate fuera de la transacción, y los DTO de respuesta se
// arman recorriendo relaciones perezosas. Sin esto, los endpoints de lectura
// fallaban con LazyInitializationException.
// Los métodos que escriben llevan su propio @Transactional, que tiene precedencia.
@Transactional(readOnly = true)
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    private final UsuarioRepository usuarioRepository;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public AuditoriaService(AuditoriaRepository auditoriaRepository, UsuarioRepository usuarioRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }


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
        Usuario usuario = null;
        if (idUsuario != null) {
            usuario = usuarioRepository.findById(idUsuario).orElse(null);
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

    @Transactional
    public void registrarCreacion(Long idUsuario, String tabla, String idRegistro, String ip) {
        registrarAuditoria(idUsuario, "CREAR_" + tabla.toUpperCase(), tabla, idRegistro, null, ip);
    }

    @Transactional
    public void registrarActualizacion(Long idUsuario, String tabla, String idRegistro, String ip) {
        registrarAuditoria(idUsuario, "ACTUALIZAR_" + tabla.toUpperCase(), tabla, idRegistro, null, ip);
    }

    @Transactional
    public void registrarEliminacion(Long idUsuario, String tabla, String idRegistro, String ip) {
        registrarAuditoria(idUsuario, "ELIMINAR_" + tabla.toUpperCase(), tabla, idRegistro, null, ip);
    }

    @Transactional
    public void registrarLogin(Long idUsuario, String ip) {
        registrarAuditoria(idUsuario, "LOGIN", "usuarios", idUsuario != null ? idUsuario.toString() : null, "Inicio de sesión", ip);
    }

    @Transactional
    public void registrarLogout(Long idUsuario, String ip) {
        registrarAuditoria(idUsuario, "LOGOUT", "usuarios", idUsuario != null ? idUsuario.toString() : null, "Cierre de sesión", ip);
    }
}