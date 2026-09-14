-- =====================================================================
--  FASE 1 - LIMPIEZA DE RESTOS DEL DISENIO ANTERIOR
--  Proyecto : Sistema de Ventas - Muebleria Eden
--  Base     : muebleria_eden_db
--  Fecha    : 2026-08-20
-- =====================================================================
--
--  QUE HACE
--  --------
--  Elimina columnas que quedaron de un disenio anterior (flujo de
--  pedidos online que se descarto) y columnas que duplican informacion
--  ya modelada en otras tablas. Define la semantica de envios.id_usuario.
--
--  POR QUE
--  -------
--  La tienda web es una vitrina publica: sin login, sin carrito y sin
--  pedidos. Las columnas id_pedido apuntan a una tabla 'pedidos' que no
--  existe y ademas tienen indices UNIQUE hacia la nada.
--
--  SEGURIDAD
--  ---------
--  Verificado antes de escribir este script: las 7 columnas afectadas
--  tienen 0 filas con datos, y no existen vistas dependientes.
--  Todo va dentro de una transaccion: si una sentencia falla, la base
--  queda exactamente como estaba.
--
--  IMPACTO EN BACKEND (para la etapa siguiente)
--  --------------------------------------------
--    models/Venta.java     -> ya no mapea nombreClienteDirecto ni
--                             celularClienteDirecto (revisar DTOs y
--                             VentaService)
--    models/Cliente.java   -> ya no mapea direccion; usar la tabla
--                             direcciones_cliente
--    models/Envio.java     -> agregar relacion usuarioResponsable
--    models/User.java      -> el rol CLIENTE deja de ser valido
--    models/Role.java      -> quitar el valor CLIENTE
--
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1.1  Restos del flujo de pedidos online (descartado)
-- ---------------------------------------------------------------------
-- Al eliminar la columna, PostgreSQL elimina tambien su indice UNIQUE
-- (uk3hseenfpcew653899qnr73qmq y uklc4ahgse8qryrpuoge3cdasd4).

ALTER TABLE ventas DROP COLUMN id_pedido;
ALTER TABLE envios DROP COLUMN id_pedido;


-- ---------------------------------------------------------------------
-- 1.2  Datos del cliente duplicados dentro de la venta
-- ---------------------------------------------------------------------
-- La venta de mostrador sin cliente registrado ya se modela con
-- clientes.tipo = 'INVITADO'. Tener ademas estos campos sueltos permite
-- que el mismo dato viva en dos lugares distintos.

ALTER TABLE ventas DROP COLUMN nombre_cliente_directo;
ALTER TABLE ventas DROP COLUMN celular_cliente_directo;


-- ---------------------------------------------------------------------
-- 1.3  Direccion duplicada del cliente
-- ---------------------------------------------------------------------
-- Existe la tabla direcciones_cliente (con es_principal, ciudad,
-- departamento y referencia). El campo suelto en clientes sobra.

ALTER TABLE clientes DROP COLUMN direccion;


-- ---------------------------------------------------------------------
-- 1.4  Atributo de producto duplicado
-- ---------------------------------------------------------------------
-- productos.tipo_producto ya distingue CAMA / COLCHON.
-- tipo_cama nunca se uso (0 filas con dato).

ALTER TABLE productos DROP COLUMN tipo_cama;


-- ---------------------------------------------------------------------
-- 1.5  envios.id_usuario -> responsable del envio
-- ---------------------------------------------------------------------
-- La columna existia sin nombre claro y sin clave foranea.
-- Se conserva porque responde una pregunta de NEGOCIO distinta de la
-- que responde la tabla auditorias:
--
--   auditorias           = quien toco el registro en el sistema
--   id_usuario_responsable = quien responde por el envio ante el cliente
--
-- El traslado lo hace un tercero (id_transportadora), por eso importa
-- registrar quien, del lado del negocio, verifico la direccion y
-- entrego la mercaderia al transportista.
--
-- Queda NULL-able: un envio puede registrarse antes de asignarle
-- responsable.

ALTER TABLE envios RENAME COLUMN id_usuario TO id_usuario_responsable;

ALTER TABLE envios
    ADD CONSTRAINT fk_envios_usuario_responsable
    FOREIGN KEY (id_usuario_responsable) REFERENCES users (id);

COMMENT ON COLUMN envios.id_usuario_responsable IS
    'Empleado responsable del envio ante el cliente. Distinto de ventas.id_usuario (quien registro la venta).';


-- ---------------------------------------------------------------------
-- 1.6  El rol CLIENTE deja de existir
-- ---------------------------------------------------------------------
-- La tienda web no tiene login. Los unicos usuarios del sistema son
-- el dueño (ADMIN) y los vendedores (EMPLEADO). Quien compra se
-- registra en la tabla clientes, que no tiene credenciales.
--
-- NOTA: la Fase 2 reemplaza users.role por users.id_rol -> roles.
-- Este CHECK es transitorio, pero deja la Fase 1 coherente por si
-- la Fase 2 se posterga.

ALTER TABLE users DROP CONSTRAINT users_role_check;

ALTER TABLE users
    ADD CONSTRAINT users_role_check
    CHECK (role IN ('ADMIN', 'EMPLEADO'));

COMMIT;

-- =====================================================================
--  FIN FASE 1
--  Siguiente: 02_integridad_indices.sql (indices sobre FKs,
--  CHECK constraints y FK faltante en movimientos_inventario)
-- =====================================================================
