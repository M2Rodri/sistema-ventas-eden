-- =====================================================================
--  FASE 9 - RENOMBRAR users A usuarios
--  Proyecto : Sistema de Ventas - Muebleria Eden
--  Base     : muebleria_eden_db
--  Fecha    : 2026-08-21
-- =====================================================================
--
--  QUE HACE
--  --------
--  Renombra la tabla 'users' a 'usuarios', junto con su secuencia, sus
--  restricciones, sus indices y las claves foraneas que la referencian.
--
--  POR QUE
--  -------
--  Era la unica tabla del esquema con nombre en ingles. Las otras 24
--  estan en español (productos, ventas, clientes, proveedores...), y
--  hasta el endpoint que la expone ya era /api/usuarios. La tabla
--  quedaba como la excepcion sin ninguna razon tecnica: solo se llamo
--  asi porque la genero Hibernate a partir de la clase User.
--
--  Un esquema donde 24 tablas siguen una convencion y una la rompe
--  invita exactamente a la pregunta "por que esta es distinta", y no
--  hay una respuesta buena mas alla de "quedo asi".
--
--  Tambien se normalizan dos nombres autogenerados por Hibernate que
--  eran ilegibles:
--    uk6dotkott2kjsp8vw4d0m25fb7        -> uq_usuarios_email
--    fk23iv2aensmqoiivprlx5pxfco        -> fk_ventas_usuario
--    fkfriploufiwgvtbus9een74x4d        -> fk_auditorias_usuario
--
--  SEGURIDAD
--  ---------
--  RENAME no toca los datos: solo cambia el nombre del objeto. Las
--  claves foraneas de otras tablas siguen apuntando a la misma tabla
--  automaticamente, porque PostgreSQL las resuelve por identificador
--  interno y no por nombre.
--  Todo dentro de una transaccion.
--
--  IMPACTO EN BACKEND
--  ------------------
--    models/User.java              -> renombrar clase a Usuario,
--                                     @Table(name = "usuarios")
--    repositories/UserRepository   -> UsuarioRepository
--    services/UserService          -> UsuarioService
--    controllers/UserController    -> UsuarioController
--    dto/UserRequest, UserResponse -> UsuarioRequest, UsuarioResponse
--    security/UserDetailsServiceImpl -> mantiene su nombre (implementa
--                                     el contrato UserDetailsService de
--                                     Spring Security), pero pasa a usar
--                                     la entidad Usuario.
--
--  ATENCION: la clase org.springframework.security.core.userdetails.User
--  es de Spring Security y NO debe renombrarse.
--
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 9.1  La tabla y su secuencia
-- ---------------------------------------------------------------------
ALTER TABLE users RENAME TO usuarios;
ALTER SEQUENCE users_id_seq RENAME TO usuarios_id_seq;


-- ---------------------------------------------------------------------
-- 9.2  Clave primaria e indices
-- ---------------------------------------------------------------------
ALTER INDEX users_pkey RENAME TO usuarios_pkey;
ALTER INDEX idx_users_rol RENAME TO idx_usuarios_rol;

-- Nombre autogenerado por Hibernate, ilegible
ALTER INDEX uk6dotkott2kjsp8vw4d0m25fb7 RENAME TO uq_usuarios_email;


-- ---------------------------------------------------------------------
-- 9.3  Restricciones propias
-- ---------------------------------------------------------------------
ALTER TABLE usuarios RENAME CONSTRAINT fk_users_rol TO fk_usuarios_rol;


-- ---------------------------------------------------------------------
-- 9.4  Claves foraneas de otras tablas hacia usuarios
-- ---------------------------------------------------------------------
-- Estas dos tenian nombres autogenerados por Hibernate.
ALTER TABLE ventas     RENAME CONSTRAINT fk23iv2aensmqoiivprlx5pxfco TO fk_ventas_usuario;
ALTER TABLE auditorias RENAME CONSTRAINT fkfriploufiwgvtbus9een74x4d TO fk_auditorias_usuario;

-- Las de compras, envios y movimientos_inventario ya tenian nombre
-- descriptivo y no necesitan cambio:
--   compras.fk_compras_usuario
--   envios.fk_envios_usuario_responsable
--   movimientos_inventario.fk_movimientos_inv_usuario

COMMENT ON TABLE usuarios IS
    'Personal del negocio: los unicos que inician sesion. Quien compra se registra en clientes, que no tiene credenciales.';

COMMIT;

-- =====================================================================
--  FIN
--  El esquema queda con las 25 tablas nombradas en español.
-- =====================================================================
