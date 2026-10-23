package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.models.Auditoria;
import com.mitienda.ecommerce.repositories.AuditoriaRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Deja constancia de quién hizo cada operación sensible y desde dónde (usuario e IP salen de la petición).
 * Un fallo al auditar no tumba la operación de negocio y se guarda en su propia transacción (REQUIRES_NEW).
 */
@Service
public class RegistroAuditoria {
    private final AuditoriaRepository auditoriaRepository;

    private final UsuarioRepository usuarioRepository;

    private final UsuarioActualService usuarioActualService;

    public RegistroAuditoria(AuditoriaRepository auditoriaRepository, UsuarioRepository usuarioRepository,
                              UsuarioActualService usuarioActualService) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioActualService = usuarioActualService;
    }

    /** Registra una acción sobre una tabla: acción en mayúsculas, tabla, id del registro (o null) y detalle legible. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String accion, String tablaAfectada, Object idRegistro, String detalles) {
        try {
            Auditoria auditoria = new Auditoria();
            auditoria.setUsuario(usuarioActualService.obtener());
            auditoria.setAccion(accion);
            auditoria.setTablaAfectada(tablaAfectada);
            auditoria.setIdRegistro(idRegistro != null ? recortar(idRegistro.toString(), 20) : null);
            auditoria.setDetalles(recortar(detalles, 1000));
            auditoria.setIpDispositivo(recortar(ipActual(), 50));
            auditoriaRepository.save(auditoria);
        } catch (Exception e) {
            // A propósito no se relanza: ver el punto 1 del comentario de arriba.
            System.err.println("No se pudo registrar la auditoría de " + accion + ": " + e.getMessage());
        }
    }

    /** Variante para cuando el usuario no está en el SecurityContext, como en el login. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarPara(Long idUsuario, String accion, String tablaAfectada,
                              Object idRegistro, String detalles) {
        try {
            Auditoria auditoria = new Auditoria();
            if (idUsuario != null) {
                auditoria.setUsuario(usuarioRepository.findById(idUsuario).orElse(null));
            }
            auditoria.setAccion(accion);
            auditoria.setTablaAfectada(tablaAfectada);
            auditoria.setIdRegistro(idRegistro != null ? recortar(idRegistro.toString(), 20) : null);
            auditoria.setDetalles(recortar(detalles, 1000));
            auditoria.setIpDispositivo(recortar(ipActual(), 50));
            auditoriaRepository.save(auditoria);
        } catch (Exception e) {
            System.err.println("No se pudo registrar la auditoría de " + accion + ": " + e.getMessage());
        }
    }

    /** IP de la petición; mira primero X-Forwarded-For por si hay un proxy. */
    private String ipActual() {
        try {
            ServletRequestAttributes atributos =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (atributos == null) {
                return null;
            }
            HttpServletRequest request = atributos.getRequest();

            String reenviada = request.getHeader("X-Forwarded-For");
            if (reenviada != null && !reenviada.isBlank()) {
                // Puede venir como "cliente, proxy1, proxy2": la primera es la del cliente.
                return reenviada.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    /** Evita que un detalle largo rompa el insert por exceder la columna. */
    private String recortar(String texto, int maximo) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 3) + "...";
    }
}
