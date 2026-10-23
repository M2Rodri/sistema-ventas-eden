package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.MensajeContactoRequest;
import com.mitienda.ecommerce.dto.MensajeContactoResponse;
import com.mitienda.ecommerce.models.MensajeContacto;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.MensajeContactoRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** Consultas recibidas desde la tienda. */
@Service
// Lectura en transacción: con open-in-view=false no hay sesión de Hibernate fuera de ella.
@Transactional(readOnly = true)
public class MensajeContactoService {
    private final MensajeContactoRepository mensajeContactoRepository;

    private final UsuarioRepository usuarioRepository;

    public MensajeContactoService(MensajeContactoRepository mensajeContactoRepository, UsuarioRepository usuarioRepository) {
        this.mensajeContactoRepository = mensajeContactoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Recibe una consulta desde la tienda. No requiere sesión. */
    @Transactional
    public MensajeContactoResponse registrarMensaje(MensajeContactoRequest request, String ipOrigen) {
        MensajeContacto mensaje = new MensajeContacto();
        mensaje.setNombre(request.getNombre().trim());
        mensaje.setEmail(request.getEmail().trim());
        mensaje.setTelefono(request.getTelefono() != null ? request.getTelefono().trim() : null);
        mensaje.setAsunto(request.getAsunto().trim());
        mensaje.setMensaje(request.getMensaje().trim());
        mensaje.setAtendido(false);
        mensaje.setIpOrigen(ipOrigen);

        return new MensajeContactoResponse(mensajeContactoRepository.save(mensaje));
    }

    public List<MensajeContactoResponse> getAllMensajes() {
        return mensajeContactoRepository.findAllByOrderByFechaEnvioDesc()
                .stream().map(MensajeContactoResponse::new).collect(Collectors.toList());
    }

    public List<MensajeContactoResponse> getMensajesPendientes() {
        return mensajeContactoRepository.findByAtendidoOrderByFechaEnvioDesc(false)
                .stream().map(MensajeContactoResponse::new).collect(Collectors.toList());
    }

    public Long countPendientes() {
        return mensajeContactoRepository.countByAtendido(false);
    }

    /** Marca la consulta como atendida y registra quién se hizo cargo. */
    @Transactional
    public MensajeContactoResponse marcarAtendido(Long id, Long idUsuario) {
        MensajeContacto mensaje = mensajeContactoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mensaje no encontrado con ID: " + id));

        if (Boolean.TRUE.equals(mensaje.getAtendido())) {
            return new MensajeContactoResponse(mensaje);
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));

        mensaje.setAtendido(true);
        mensaje.setFechaAtencion(LocalDateTime.now());
        mensaje.setUsuarioAtiende(usuario);

        return new MensajeContactoResponse(mensajeContactoRepository.save(mensaje));
    }

    @Transactional
    public void deleteMensaje(Long id) {
        if (!mensajeContactoRepository.existsById(id)) {
            throw new RuntimeException("Mensaje no encontrado con ID: " + id);
        }
        mensajeContactoRepository.deleteById(id);
    }
}
