-- =====================================================================
--  FASE 3 - RESOLUCION DE AMBIGUEDADES
--  Proyecto : Sistema de Ventas - Muebleria Eden
--  Base     : muebleria_eden_db
--  Fecha    : 2026-08-20
-- =====================================================================
--
--  QUE HACE
--  --------
--  A) Elimina productos.stock: el stock pasa a vivir unicamente en
--     inventario.cantidad_disponible.
--  B) Normaliza los roles: users.role (varchar) pasa a ser
--     users.id_rol -> roles(id).
--  C) Renombra productos.precio_unitario a costo_referencial.
--
--  POR QUE
--  -------
--  A) El mismo dato vivia en dos lugares (productos.stock e
--     inventario.cantidad_disponible) sin ningun mecanismo que los
--     mantuviera sincronizados. Tarde o temprano difieren y no hay
--     forma de saber cual es el correcto. Gana inventario porque es
--     donde apuntan movimientos_inventario y alertas_inventario.
--  B) El rol vivia en dos lugares: el valor real en users.role y una
--     tabla roles vacia y sin relacionar. Normalizarlo permite que el
--     dueño agregue un rol nuevo (ej. "Almacenero") con un INSERT, en
--     lugar de alterar un CHECK constraint y volver a desplegar.
--  C) El nombre "precio_unitario" sugiere precio de venta, pero el
--     campo guarda el costo (1400 frente a precio_venta 2100). El
--     historial real de costos vive en productos_proveedor y, a partir
--     de la fase siguiente, en las compras.
--
--  ATENCION - ESTE ES EL PRIMER SCRIPT QUE ROMPE EL BACKEND
--  --------------------------------------------------------
--  Los scripts anteriores solo agregaban restricciones que el backend
--  ya cumplia. Este renombra y elimina columnas: el backend NO va a
--  funcionar hasta que se ajuste. Es esperado y forma parte del plan.
--
--  IMPACTO EN BACKEND (lista de trabajo para la etapa siguiente)
--  -------------------------------------------------------------
--    A) productos.stock
--         models/Producto.java        -> no lo mapeaba, sin cambios
--         (el stock ya se leia de Inventario)
--
--    B) roles  [ este es el cambio de mayor alcance ]
--         models/Role.java            -> de enum a entidad @Entity
--         models/User.java            -> campo role pasa a @ManyToOne rol
--         security/UserDetailsServiceImpl.java
--                                     -> "ROLE_" + user.getRol().getNombre()
--         security/JwtUtil.java       -> generateToken / extractRole
--         services/AuthService.java   -> ver nota importante abajo
--         services/UserService.java   -> alta y edicion de usuarios
--         dto/AuthResponse.java, UserRequest.java, UserResponse.java
--
--       NOTA IMPORTANTE: AuthService.register() asigna Role.CLIENTE,
--       un rol que ya no existe (la tienda web no tiene login). Ese
--       metodo, junto con las paginas /register y /tienda/perfil del
--       frontend, debe revisarse: o se elimina el registro publico, o
--       se convierte en alta de empleados hecha por el ADMIN.
--
--    C) precio_unitario -> costo_referencial
--         models/Producto.java        -> renombrar campo precioUnitario
--         dto/ProductoRequest.java, ProductoResponse.java
--         services/ProductoService.java
--         frontend: types/producto.ts, ProductoModal.tsx
--
--  SEGURIDAD
--  ---------
--  El paso A.1 es una red de proteccion: antes de eliminar
--  productos.stock, crea la fila de inventario faltante para cualquier
--  producto que no la tenga, arrastrando el valor de stock. Hoy no hace
--  falta (1 producto, ambos valores en 0), pero deja el script correcto
--  aunque se ejecute sobre una base con datos.
--  Todo dentro de una transaccion: si algo falla, no cambia nada.
--
-- =====================================================================

BEGIN;

-- =====================================================================
-- A) STOCK: UNA SOLA FUENTE DE VERDAD
-- =====================================================================

-- A.1  Red de proteccion: ningun producto debe quedarse sin su fila de
--      inventario. Si alguno no la tiene, se crea arrastrando el valor
--      que tenia en productos.stock para no perder el dato.
INSERT INTO inventario (id_producto, cantidad_disponible, ubicacion, fecha_actualizacion)
SELECT p.id, COALESCE(p.stock, 0), 'Sin asignar', now()
FROM productos p
WHERE NOT EXISTS (
    SELECT 1 FROM inventario i WHERE i.id_producto = p.id
);

-- A.2  Eliminar la columna duplicada.
ALTER TABLE productos DROP COLUMN stock;

COMMENT ON COLUMN inventario.cantidad_disponible IS
    'Unica fuente de verdad del stock. Se modifica a traves de movimientos_inventario.';


-- =====================================================================
-- B) NORMALIZACION DE ROLES
-- =====================================================================

-- B.1  El nombre del rol debe ser unico: es la clave por la que se
--      busca desde la aplicacion.
ALTER TABLE roles
    ADD CONSTRAINT uq_roles_nombre UNIQUE (nombre);

-- B.2  Poblar el catalogo de roles.
INSERT INTO roles (nombre, descripcion) VALUES
    ('ADMIN',    'Administrador / Dueño'),
    ('EMPLEADO', 'Vendedor');

-- B.3  Nueva columna, todavia nullable para poder migrar.
ALTER TABLE users ADD COLUMN id_rol INTEGER;

-- B.4  Migrar el valor existente de cada usuario.
UPDATE users u
SET id_rol = r.id
FROM roles r
WHERE r.nombre = u.role;

-- B.5  Verificacion: si algun usuario quedo sin rol, aborta todo.
--      Mas vale fallar aca que dejar un usuario sin poder autenticarse.
DO $$
DECLARE
    sin_rol INTEGER;
BEGIN
    SELECT count(*) INTO sin_rol FROM users WHERE id_rol IS NULL;
    IF sin_rol > 0 THEN
        RAISE EXCEPTION 'Hay % usuario(s) sin rol asignado. Se cancela la migracion.', sin_rol;
    END IF;
END $$;

-- B.6  Ahora si, obligatorio y con clave foranea.
ALTER TABLE users ALTER COLUMN id_rol SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_rol
    FOREIGN KEY (id_rol) REFERENCES roles (id);

CREATE INDEX idx_users_rol ON users (id_rol);

-- B.7  Eliminar la columna vieja. Al hacerlo desaparece tambien
--      users_role_check, que era transitorio desde la Fase 1.
ALTER TABLE users DROP COLUMN role;

COMMENT ON TABLE roles IS
    'Roles del personal del sistema. Los clientes no tienen credenciales: se registran en la tabla clientes.';


-- =====================================================================
-- C) COSTO DEL PRODUCTO
-- =====================================================================
-- precio_unitario sugiere precio de venta, pero guarda el costo.
-- Se renombra para que el nombre diga la verdad.

ALTER TABLE productos RENAME COLUMN precio_unitario TO costo_referencial;

COMMENT ON COLUMN productos.costo_referencial IS
    'Costo de referencia para calculo rapido de margen. El historial real de costos vive en productos_proveedor y en las compras.';

COMMIT;

-- =====================================================================
--  FIN
--  Siguiente: 04_compras.sql (tablas compras y detalle_compra)
--
--  RECORDATORIO: a partir de aqui el backend requiere ajustes para
--  volver a funcionar. Ver el bloque "IMPACTO EN BACKEND" del encabezado.
-- =====================================================================
