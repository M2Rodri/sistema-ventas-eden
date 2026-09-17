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
 * Deja constancia de quién hizo cada operación sensible y desde dónde.
 *
 * Existía AuditoriaService con todos sus métodos listos, pero ningún servicio
 * lo llamaba nunca: la tabla auditorias estaba vacía y la pantalla de auditoría
 * no mostraba nada. Esta clase es el enganche que faltaba.
 *
 * La idea es que registrar una acción cueste una sola línea en el servicio de
 * negocio, sin tener que arrastrar el id del usuario ni la IP por todas las
 * firmas de los métodos: ambos se sacan solos del contexto de la petición.
 *
 * Dos decisiones importantes:
 *
 * 1. Nada de lo que pase acá adentro puede tumbar la operación de negocio.
 *    Si falla el registro de auditoría, se deja el error en el log y la venta,
 *    la compra o el ajuste siguen su curso. Auditar es importante, pero no al
 *    punto de perder una venta real del negocio.
 *
 * 2. Se usa REQUIRES_NEW para que el insert vaya en su propia transacción. Si
 *    fuera en la misma, un fallo al auditar marcaría la transacción entera como
 *    rollback-only y revertiría la operación aunque atrapáramos la excepción.
 */
@Service
public class RegistroAuditoria {

    private final AuditoriaRepository auditoriaRepository;

    private final UsuarioRepository usuarioRepository;

    private final UsuarioActualService usuarioActualService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public RegistroAuditoria(AuditoriaRepository auditoriaRepository, UsuarioRepository usuarioRepository,
                              UsuarioActualService usuarioActualService) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioActualService = usuarioActualService;
    }


    /**
     * Registra una acción sobre una tabla.
     *
     * @param accion         qué se hizo, en mayúsculas (CREAR_PRODUCTO, CANCELAR_VENTA)
     * @param tablaAfectada  nombre de la tabla tocada
     * @param idRegistro     id del registro afectado, o null
     * @param detalles       texto libre para que la pantalla sea legible
     */
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

    /**
     * Variante para cuando el usuario ya se conoce y no está en el contexto de
     * seguridad, como en el login: en ese momento la autenticación todavía no
     * quedó guardada en el SecurityContext.
     */
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

    /**
     * IP desde la que llegó la petición.
     *
     * Se mira primero X-Forwarded-For por si algún día el sistema queda detrás
     * de un proxy o de nginx; sin eso se registraría siempre la IP del proxy y
     * no la del equipo que realmente hizo la operación.
     */
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
