package com.mitienda.ecommerce.config;

import com.mitienda.ecommerce.models.Role;
import com.mitienda.ecommerce.repositories.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Garantiza que existan los roles del sistema al arrancar.
 *
 * Los roles son estructura, no datos de negocio: sin ADMIN y EMPLEADO en la
 * tabla, nadie puede iniciar sesión y el sistema queda inutilizable. Por eso se
 * aseguran en cada arranque, y por eso se hace sin pisar lo que ya exista.
 *
 * ANTES ESTA CLASE HACÍA ALGO MÁS, Y SE QUITÓ
 *
 * También creaba en cada arranque el usuario admin@mitienda.com con la
 * contraseña REDACTADO, ambas escritas en el código fuente. Peor todavía: si el
 * usuario ya existía, le reescribía la contraseña. Es decir que cambiarla no
 * servía de nada, porque el siguiente reinicio la devolvía a REDACTADO.
 *
 * Era una cuenta de administrador con contraseña conocida, imposible de cerrar
 * mientras ese código estuviera, y con la marca vieja del proyecto. Se agregó
 * durante la migración a Supabase para no quedar afuera del sistema; cumplida
 * esa función, es solo una puerta abierta.
 *
 * Las altas de usuarios se hacen desde la pantalla de Usuarios, que exige rol
 * ADMIN y deja registro en la auditoría. Un usuario creado así tiene dueño y
 * fecha; uno creado por código no tiene ni una cosa ni la otra.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
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
}
