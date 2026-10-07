package com.mitienda.ecommerce.config;

import com.mitienda.ecommerce.models.Role;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.RoleRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import com.mitienda.ecommerce.services.RegistroAuditoria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Deja la base lista para usarse al arrancar: los roles del sistema y, si hace falta, el
 * primer administrador.
 *
 * ROLES
 *
 * Los roles son estructura, no datos de negocio: sin ADMIN y EMPLEADO en la
 * tabla, nadie puede iniciar sesión y el sistema queda inutilizable. Por eso se
 * aseguran en cada arranque, y por eso se hace sin pisar lo que ya exista.
 *
 * ADMINISTRADOR INICIAL
 *
 * Una base creada desde cero (backend/database/00_esquema.sql) no tiene usuarios, y sin
 * usuarios nadie puede entrar. Si la tabla usuarios está VACÍA y están definidas las
 * variables de entorno ADMIN_INICIAL_USUARIO y ADMIN_INICIAL_CLAVE, se crea con ellas un
 * administrador (la contraseña se guarda con BCrypt, como en cualquier alta de usuario).
 * Si falta alguna de las dos variables, o ya hay usuarios, no se hace nada: nunca se crea
 * ni se modifica un usuario en una base que ya está en uso. La contraseña nunca se escribe
 * en el registro.
 *
 * Esto NO es lo que había antes, y conviene no confundirlos. Antes esta clase creaba en cada
 * arranque el usuario admin@mitienda.com con una contraseña escrita en el código fuente y,
 * si el usuario ya existía, le reescribía la contraseña: una cuenta de administrador con
 * clave conocida, imposible de cerrar. Se quitó. Ahora la clave sale del entorno del
 * servidor, solo sirve para la primera vez y no vuelve a tocar nada una vez que existe un
 * usuario. Una vez que se entró, conviene sacar la clave del entorno.
 *
 * Las altas de usuarios siguientes se hacen desde la pantalla de Usuarios, que exige rol
 * ADMIN y deja registro en la auditoría.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** Mismas reglas que valida la entidad Usuario (usuario de 3 a 30 caracteres, clave de 6 o más). */
    private static final Pattern FORMATO_USUARIO = Pattern.compile("^[a-zA-Z0-9._]{3,30}$");
    private static final int LARGO_MINIMO_CLAVE = 6;

    private final RoleRepository roleRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistroAuditoria registroAuditoria;
    private final String adminInicialUsuario;
    private final String adminInicialClave;

    public DataInitializer(RoleRepository roleRepository,
                           UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           RegistroAuditoria registroAuditoria,
                           @Value("${ADMIN_INICIAL_USUARIO:}") String adminInicialUsuario,
                           @Value("${ADMIN_INICIAL_CLAVE:}") String adminInicialClave) {
        this.roleRepository = roleRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.registroAuditoria = registroAuditoria;
        this.adminInicialUsuario = adminInicialUsuario;
        this.adminInicialClave = adminInicialClave;
    }

    @Override
    public void run(String... args) {
        try {
            asegurarRol("ADMIN", "Administrador / Dueño");
            asegurarRol("EMPLEADO", "Vendedor / Personal");
        } catch (Exception e) {
            // No se corta el arranque: si los roles ya están en la base, el
            // sistema funciona igual. Solo se deja constancia.
            log.error("No se pudieron verificar los roles del sistema: {}", e.getMessage());
        }

        crearAdministradorInicial();
    }

    /** Crea el rol solo si falta. Nunca modifica uno existente. */
    private void asegurarRol(String nombre, String descripcion) {
        if (roleRepository.findByNombre(nombre).isEmpty()) {
            Role rol = new Role();
            rol.setNombre(nombre);
            rol.setDescripcion(descripcion);
            roleRepository.save(rol);
            log.info("Rol {} creado", nombre);
        }
    }

    /**
     * Crea el administrador inicial solo si hay variables definidas y la tabla usuarios está
     * vacía. Nunca imprime ni deja en un error la contraseña.
     */
    private void crearAdministradorInicial() {
        if (adminInicialUsuario == null || adminInicialUsuario.isBlank()
                || adminInicialClave == null || adminInicialClave.isBlank()) {
            return;
        }

        try {
            if (usuarioRepository.count() > 0) {
                return;
            }

            String usuario = adminInicialUsuario.trim().toLowerCase();
            if (!FORMATO_USUARIO.matcher(usuario).matches()) {
                log.error("No se creó el administrador inicial: ADMIN_INICIAL_USUARIO debe tener de 3 a 30 "
                        + "caracteres (letras, números, puntos y guiones bajos)");
                return;
            }
            if (adminInicialClave.length() < LARGO_MINIMO_CLAVE) {
                log.error("No se creó el administrador inicial: ADMIN_INICIAL_CLAVE debe tener al menos {} caracteres",
                        LARGO_MINIMO_CLAVE);
                return;
            }

            Role rolAdmin = roleRepository.findByNombre("ADMIN")
                    .orElseThrow(() -> new IllegalStateException("falta el rol ADMIN"));

            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setApellido("Inicial");
            admin.setUsuario(usuario);
            admin.setPassword(passwordEncoder.encode(adminInicialClave));
            admin.setRol(rolAdmin);
            admin.setActivo(true);
            Usuario guardado = usuarioRepository.save(admin);

            log.info("Administrador inicial creado: usuario {}", usuario);
            registroAuditoria.registrarPara(guardado.getId(), "CREAR_ADMIN_INICIAL", "usuarios", guardado.getId(),
                    "Administrador inicial creado al arrancar, con las variables de entorno");
        } catch (Exception e) {
            // Solo el tipo de error: el mensaje de una excepción podría arrastrar datos del usuario.
            log.error("No se pudo crear el administrador inicial ({})", e.getClass().getSimpleName());
        }
    }
}
