package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.exception.AccesoProhibidoException;
import com.mitienda.ecommerce.exception.ConflictoEstadoException;
import com.mitienda.ecommerce.exception.PeticionInvalidaException;
import com.mitienda.ecommerce.exception.RecursoNoEncontradoException;
import com.mitienda.ecommerce.exception.ReglaNegocioException;
import com.mitienda.ecommerce.dto.UsuarioRequest;
import com.mitienda.ecommerce.dto.UsuarioResponse;
import com.mitienda.ecommerce.models.Role;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.RoleRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/** Servicio para gestión de usuarios (CRUD completo) */
@Service
// Lectura en transacción: con open-in-view=false no hay sesión de Hibernate fuera de ella.
@Transactional(readOnly = true)
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final RegistroAuditoria registroAuditoria;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          RegistroAuditoria registroAuditoria) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.registroAuditoria = registroAuditoria;
    }

    /** Traduce el nombre de rol que envía el frontend ("ADMIN", "EMPLEADO") a su fila de 'roles'. */
    private Role resolverRol(String nombreRol) {
        if (nombreRol == null || nombreRol.isBlank()) {
            throw new PeticionInvalidaException("ROL_REQUERIDO", "El rol es obligatorio");
        }
        return roleRepository.findByNombre(nombreRol.trim().toUpperCase())
                .orElseThrow(() -> new PeticionInvalidaException("ROL_INVALIDO", "Rol no válido: " + nombreRol));
    }

    /** Nombre de usuario del administrador principal: solo él puede modificarse a sí mismo. */
    public static final String ADMIN_PRINCIPAL = "admin";

    /** Solo el administrador principal puede modificarse a sí mismo; otro ADMIN recibe 403. */
    private void validarNoEsAdminPrincipalAjeno(Usuario objetivo) {
        if (!ADMIN_PRINCIPAL.equalsIgnoreCase(objetivo.getUsuario())) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esElMismo = auth != null && auth.isAuthenticated()
                && ADMIN_PRINCIPAL.equalsIgnoreCase(auth.getName());
        if (!esElMismo) {
            throw new AccesoProhibidoException("ADMIN_PRINCIPAL_PROTEGIDO",
                    "Solo el administrador principal puede modificar su propia cuenta.");
        }
    }

    /** Impide dejar el sistema sin ningún administrador activo. */
    private void validarQuedeUnAdminActivo(Usuario usuario, boolean seguiraSiendoAdminActivo) {
        if (seguiraSiendoAdminActivo) {
            return;
        }
        boolean eraAdminActivo = "ADMIN".equalsIgnoreCase(usuario.getRoleName())
                && Boolean.TRUE.equals(usuario.getActivo());
        if (!eraAdminActivo) {
            return;
        }
        boolean quedaOtroAdminActivo = usuarioRepository.findAll().stream()
                .anyMatch(u -> !u.getId().equals(usuario.getId())
                        && "ADMIN".equalsIgnoreCase(u.getRoleName())
                        && Boolean.TRUE.equals(u.getActivo()));
        if (!quedaOtroAdminActivo) {
            throw new ReglaNegocioException("ULTIMO_ADMINISTRADOR", "Es el único administrador activo. No se puede desactivar.");
        }
    }

    /** Listar todos los usuarios */
    public List<UsuarioResponse> getAllUsers() {
        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioResponse::new)
                .collect(Collectors.toList());
    }

    /** Obtener usuario por ID */
    public UsuarioResponse getUserById(Long id) {
        Usuario user = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado con ID: " + id));
        return new UsuarioResponse(user);
    }

    /** Crear nuevo usuario */
    @Transactional
    public UsuarioResponse createUser(UsuarioRequest request) {
        // Siempre en minúsculas: la base distingue mayúsculas y evita duplicados ("Juan" y "juan").
        String usuarioNormalizado = request.getUsuario().trim().toLowerCase();

        if (usuarioRepository.existsByUsuario(usuarioNormalizado)) {
            throw new ConflictoEstadoException("USUARIO_DUPLICADO", "El usuario ya está registrado: " + usuarioNormalizado);
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new PeticionInvalidaException("PASSWORD_REQUERIDA", "La contraseña es obligatoria al crear un usuario");
        }

        Usuario user = new Usuario();
        user.setNombre(request.getNombre());
        user.setApellido(request.getApellido());
        user.setUsuario(usuarioNormalizado);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setTelefono(request.getTelefono());
        user.setDireccion(request.getDireccion());
        // Sin rol en el pedido, el usuario nuevo es EMPLEADO: el rol con menos permisos.
        String rolPedido = request.getRole() == null || request.getRole().isBlank()
                ? "EMPLEADO" : request.getRole();
        user.setRol(resolverRol(rolPedido));
        user.setActivo(request.getActivo());

        Usuario savedUser = usuarioRepository.save(user);

        // Nunca se registra la contraseña ni su hash.
        registroAuditoria.registrar("CREAR_USUARIO", "usuarios", savedUser.getId(),
                "Alta de " + savedUser.getUsuario() + " con rol " + savedUser.getRoleName());

        return new UsuarioResponse(savedUser);
    }

    /** Actualizar usuario existente */
    @Transactional
    public UsuarioResponse updateUser(Long id, UsuarioRequest request) {
        Usuario user = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado con ID: " + id));
        validarNoEsAdminPrincipalAjeno(user);

        String usuarioNormalizado = request.getUsuario().trim().toLowerCase();

        // Renombrarlo dejaría el nombre "admin" libre y sin protección.
        if (ADMIN_PRINCIPAL.equalsIgnoreCase(user.getUsuario()) && !ADMIN_PRINCIPAL.equals(usuarioNormalizado)) {
            throw new ReglaNegocioException("ADMIN_PRINCIPAL_NO_RENOMBRABLE", "El administrador principal no puede cambiar su nombre de usuario.");
        }

        if (!user.getUsuario().equals(usuarioNormalizado) &&
            usuarioRepository.existsByUsuario(usuarioNormalizado)) {
            throw new ConflictoEstadoException("USUARIO_DUPLICADO", "El usuario ya está registrado: " + usuarioNormalizado);
        }

        Role nuevoRol = resolverRol(request.getRole());
        boolean seguiraSiendoAdminActivo = "ADMIN".equalsIgnoreCase(nuevoRol.getNombre())
                && Boolean.TRUE.equals(request.getActivo());
        validarQuedeUnAdminActivo(user, seguiraSiendoAdminActivo);

        user.setNombre(request.getNombre());
        user.setApellido(request.getApellido());
        user.setUsuario(usuarioNormalizado);
        user.setTelefono(request.getTelefono());
        user.setDireccion(request.getDireccion());
        user.setRol(nuevoRol);
        user.setActivo(request.getActivo());

        // Solo actualizar password si se proporcionó uno nuevo
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            // Cambiar la contraseña corta las sesiones abiertas: si otro la conocía, su token deja de servir.
            user.setSesionesValidasDesde(System.currentTimeMillis());
        }

        boolean cambioPassword = request.getPassword() != null
                && !request.getPassword().trim().isEmpty();

        Usuario updatedUser = usuarioRepository.save(user);

        registroAuditoria.registrar("ACTUALIZAR_USUARIO", "usuarios", updatedUser.getId(),
                "Edicion de " + updatedUser.getUsuario() + ", rol " + updatedUser.getRoleName()
                        + (cambioPassword ? ", con cambio de contrasena" : ""));

        return new UsuarioResponse(updatedUser);
    }

    /** Eliminar usuario (desactivar) */
    @Transactional
    public void deleteUser(Long id) {
        Usuario user = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado con ID: " + id));
        validarNoEsAdminPrincipalAjeno(user);

        // Desactivar en lugar de eliminar
        validarQuedeUnAdminActivo(user, false);
        user.setActivo(false);
        usuarioRepository.save(user);

        registroAuditoria.registrar("ELIMINAR_USUARIO", "usuarios", user.getId(),
                "Baja de " + user.getUsuario());
    }

    /** Activar/Desactivar usuario */
    @Transactional
    public UsuarioResponse toggleUserStatus(Long id) {
        Usuario user = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado con ID: " + id));
        validarNoEsAdminPrincipalAjeno(user);

        boolean nuevoActivo = !user.getActivo();
        validarQuedeUnAdminActivo(user, nuevoActivo);
        user.setActivo(nuevoActivo);
        Usuario updatedUser = usuarioRepository.save(user);

        registroAuditoria.registrar(
                Boolean.TRUE.equals(updatedUser.getActivo()) ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO",
                "usuarios", updatedUser.getId(), updatedUser.getUsuario());

        return new UsuarioResponse(updatedUser);
    }

    /** Corta las sesiones abiertas de un usuario: sus tokens anteriores se rechazan y no se pueden renovar. */
    @Transactional
    public void cerrarSesiones(Long id) {
        Usuario user = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado con ID: " + id));
        validarNoEsAdminPrincipalAjeno(user);

        user.setSesionesValidasDesde(System.currentTimeMillis());
        usuarioRepository.save(user);

        registroAuditoria.registrar("CERRAR_SESIONES", "usuarios", user.getId(),
                "Cierre de todas las sesiones de " + user.getUsuario());
    }

    /** Listar usuarios por rol */
    public List<UsuarioResponse> getUsersByRole(String role) {
        return usuarioRepository.findAll()
                .stream()
                .filter(user -> role != null && role.equalsIgnoreCase(user.getRoleName()))
                .map(UsuarioResponse::new)
                .collect(Collectors.toList());
    }

    /** Listar solo usuarios activos */
    public List<UsuarioResponse> getActiveUsers() {
        return usuarioRepository.findAll()
                .stream()
                .filter(Usuario::getActivo)
                .map(UsuarioResponse::new)
                .collect(Collectors.toList());
    }

    /** Contar usuarios por rol */
    public Long countUsersByRole(String role) {
        return usuarioRepository.findAll()
                .stream()
                .filter(user -> role != null && role.equalsIgnoreCase(user.getRoleName()))
                .count();
    }

    /** Contar usuarios activos */
    public Long countActiveUsers() {
        return usuarioRepository.countByActivo(true);
    }
}