package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.UsuarioRequest;
import com.mitienda.ecommerce.dto.UsuarioResponse;
import com.mitienda.ecommerce.models.Role;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.RoleRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de usuarios (CRUD completo)
 */
@Service
// Lectura dentro de transacción por defecto: con spring.jpa.open-in-view=false
// no hay sesión de Hibernate fuera de la transacción, y los DTO de respuesta se
// arman recorriendo relaciones perezosas. Sin esto, los endpoints de lectura
// fallaban con LazyInitializationException.
// Los métodos que escriben llevan su propio @Transactional, que tiene precedencia.
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final RegistroAuditoria registroAuditoria;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public UsuarioService(UsuarioRepository usuarioRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          RegistroAuditoria registroAuditoria) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.registroAuditoria = registroAuditoria;
    }


    /**
     * Busca el rol por su nombre. El frontend envía "ADMIN" o "EMPLEADO"
     * como texto; acá se traduce a la fila correspondiente de la tabla 'roles'.
     */
    private Role resolverRol(String nombreRol) {
        if (nombreRol == null || nombreRol.isBlank()) {
            throw new RuntimeException("El rol es obligatorio");
        }
        return roleRepository.findByNombre(nombreRol.trim().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Rol no válido: " + nombreRol));
    }

    /**
     * Evita que una edición, baja o cambio de estado deje al sistema sin
     * ningún administrador activo. Sin esto, un admin puede desactivarse a
     * sí mismo (o cambiarse el rol) y quedar afuera sin que nadie, ni él
     * mismo, pueda revertirlo desde la interfaz: la pantalla de Usuarios ya
     * no aparece para un EMPLEADO, y el login rechaza cuentas inactivas.
     */
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
            throw new RuntimeException("Es el único administrador activo. No se puede desactivar.");
        }
    }

    /**
     * Listar todos los usuarios
     */
    public List<UsuarioResponse> getAllUsers() {
        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener usuario por ID
     */
    public UsuarioResponse getUserById(Long id) {
        Usuario user = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        return new UsuarioResponse(user);
    }

    /**
     * Crear nuevo usuario
     */
    @Transactional
    public UsuarioResponse createUser(UsuarioRequest request) {
        // Se guarda siempre en minúsculas: la comparación en la base es
        // sensible a mayúsculas/minúsculas y esto evita duplicados como
        // "Juan" y "juan".
        String usuarioNormalizado = request.getUsuario().trim().toLowerCase();

        // Validar que el usuario no exista
        if (usuarioRepository.existsByUsuario(usuarioNormalizado)) {
            throw new RuntimeException("El usuario ya está registrado: " + usuarioNormalizado);
        }

        // Validar que la contraseña no esté vacía al crear
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("La contraseña es obligatoria al crear un usuario");
        }

        // Crear nuevo usuario
        Usuario user = new Usuario();
        user.setNombre(request.getNombre());
        user.setApellido(request.getApellido());
        user.setUsuario(usuarioNormalizado);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setTelefono(request.getTelefono());
        user.setDireccion(request.getDireccion());
        user.setRol(resolverRol(request.getRole()));
        user.setActivo(request.getActivo());

        Usuario savedUser = usuarioRepository.save(user);

        // Nunca se registra la contrasena ni su hash, solo quien creo la cuenta
        // y con que rol.
        registroAuditoria.registrar("CREAR_USUARIO", "usuarios", savedUser.getId(),
                "Alta de " + savedUser.getUsuario() + " con rol " + savedUser.getRoleName());

        return new UsuarioResponse(savedUser);
    }

    /**
     * Actualizar usuario existente
     */
    @Transactional
    public UsuarioResponse updateUser(Long id, UsuarioRequest request) {
        Usuario user = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        String usuarioNormalizado = request.getUsuario().trim().toLowerCase();

        // Validar usuario único (si cambió)
        if (!user.getUsuario().equals(usuarioNormalizado) &&
            usuarioRepository.existsByUsuario(usuarioNormalizado)) {
            throw new RuntimeException("El usuario ya está registrado: " + usuarioNormalizado);
        }

        Role nuevoRol = resolverRol(request.getRole());
        boolean seguiraSiendoAdminActivo = "ADMIN".equalsIgnoreCase(nuevoRol.getNombre())
                && Boolean.TRUE.equals(request.getActivo());
        validarQuedeUnAdminActivo(user, seguiraSiendoAdminActivo);

        // Actualizar campos
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
        }

        boolean cambioPassword = request.getPassword() != null
                && !request.getPassword().trim().isEmpty();

        Usuario updatedUser = usuarioRepository.save(user);

        registroAuditoria.registrar("ACTUALIZAR_USUARIO", "usuarios", updatedUser.getId(),
                "Edicion de " + updatedUser.getUsuario() + ", rol " + updatedUser.getRoleName()
                        + (cambioPassword ? ", con cambio de contrasena" : ""));

        return new UsuarioResponse(updatedUser);
    }

    /**
     * Eliminar usuario (desactivar)
     */
    @Transactional
    public void deleteUser(Long id) {
        Usuario user = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        // Desactivar en lugar de eliminar
        validarQuedeUnAdminActivo(user, false);
        user.setActivo(false);
        usuarioRepository.save(user);

        registroAuditoria.registrar("ELIMINAR_USUARIO", "usuarios", user.getId(),
                "Baja de " + user.getUsuario());
    }

    /**
     * Activar/Desactivar usuario
     */
    @Transactional
    public UsuarioResponse toggleUserStatus(Long id) {
        Usuario user = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        boolean nuevoActivo = !user.getActivo();
        validarQuedeUnAdminActivo(user, nuevoActivo);
        user.setActivo(nuevoActivo);
        Usuario updatedUser = usuarioRepository.save(user);

        registroAuditoria.registrar(
                Boolean.TRUE.equals(updatedUser.getActivo()) ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO",
                "usuarios", updatedUser.getId(), updatedUser.getUsuario());

        return new UsuarioResponse(updatedUser);
    }

    /**
     * Listar usuarios por rol
     */
    public List<UsuarioResponse> getUsersByRole(String role) {
        return usuarioRepository.findAll()
                .stream()
                .filter(user -> role != null && role.equalsIgnoreCase(user.getRoleName()))
                .map(UsuarioResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Listar solo usuarios activos
     */
    public List<UsuarioResponse> getActiveUsers() {
        return usuarioRepository.findAll()
                .stream()
                .filter(Usuario::getActivo)
                .map(UsuarioResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Contar usuarios por rol
     */
    public Long countUsersByRole(String role) {
        return usuarioRepository.findAll()
                .stream()
                .filter(user -> role != null && role.equalsIgnoreCase(user.getRoleName()))
                .count();
    }

    /**
     * Contar usuarios activos
     */
    public Long countActiveUsers() {
        return usuarioRepository.countByActivo(true);
    }
}