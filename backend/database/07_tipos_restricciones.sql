-- =====================================================================
--  FASE 7 - UNIFICACION DE TIPOS Y RESTRICCIONES DE NEGOCIO
--  Proyecto : Sistema de Ventas - Muebleria Eden
--  Base     : muebleria_eden_db
--  Fecha    : 2026-08-20
-- =====================================================================
--
--  QUE HACE
--  --------
--  A) Unifica en BIGINT todas las claves primarias y foraneas.
--  B) Unifica en BIGINT todas las secuencias.
--  C) Agrega restricciones de negocio faltantes.
--  D) Elimina ventas.metodo_pago (redundante con pagos).
--  E) Agrega promociones.fecha_actualizacion.
--
--  POR QUE
--  -------
--  A) 9 claves foraneas eran INTEGER apuntando a claves primarias
--     BIGINT, y 8 tablas tenian PK INTEGER mientras las otras 17
--     usaban BIGINT. PostgreSQL lo tolera con conversion implicita,
--     pero obliga a que las entidades JPA declaren Integer en unas
--     tablas y Long en otras para el MISMO concepto (el id de un
--     producto), lo que genera errores de tipo en el backend.
--     No hay razon tecnica para la diferencia: las tablas se crearon
--     en momentos distintos.
--
--  B) 21 de 24 secuencias eran INTEGER, incluso las de tablas cuya PK
--     ya era BIGINT. Una columna BIGINT alimentada por una secuencia
--     INTEGER se agota igual en 2.147.483.647: el tipo ancho no sirve
--     de nada si el generador es angosto.
--
--  C) Restricciones que el modelo permitia violar:
--       - un cliente podia tener varias direcciones marcadas como
--         principal, y el sistema no sabria a cual enviar
--       - el mismo producto/proveedor podia cargarse dos veces con la
--         misma vigencia y precios distintos
--       - una venta podia registrarse sin vendedor
--
--  D) El metodo de pago vivia en dos lugares: ventas.metodo_pago (uno
--     por venta) y pagos.metodo_pago (uno por pago). Como una venta
--     admite varios pagos, el campo de ventas no puede representar el
--     caso real (parte en efectivo, parte por transferencia) y queda
--     desactualizado en silencio. Se aplica el mismo criterio que con
--     productos.stock: una sola fuente de verdad.
--
--  E) promociones era la unica tabla del esquema sin fecha_actualizacion.
--
--  IMPACTO EN BACKEND
--  ------------------
--    A/B) Cambiar Integer por Long en las entidades:
--           AlertaInventario, AjusteInventario (movimientos_inventario),
--           MultimediaProducto, Oferta (promociones), Role
--         y en los DTOs y servicios que los usan.
--         Es un cambio que estas entidades iban a requerir de todos
--         modos por otros motivos.
--
--    C)   VentaService debe asignar siempre el usuario que registra la
--         venta (id_usuario pasa a ser obligatorio).
--
--    D)   models/Venta.java        -> eliminar campo metodoPago
--         dto/VentaRequest.java, VentaResponse.java
--         services/VentaService.java
--         services/ReporteService.java -> los reportes por metodo de
--                                         pago deben leer de pagos
--         frontend: types/venta.ts, RegistrarVentaModal.tsx
--
--    E)   models/Oferta.java -> ya declaraba fechaActualizacion como
--         NOT NULL, y la columna no existia. Este cambio la agrega.
--
--  SEGURIDAD
--  ---------
--  INTEGER cabe siempre en BIGINT: la conversion no pierde datos.
--  Todas las tablas afectadas estan vacias, salvo users (1 fila),
--  productos (1), categorias (2) e inventario (1), ninguna de las
--  cuales pierde informacion.
--  Con tablas vacias estos ALTER son instantaneos; con datos cargados
--  reescribirian la tabla entera y tomarian bloqueo exclusivo.
--  Todo dentro de una transaccion.
--
-- =====================================================================

BEGIN;

-- =====================================================================
-- A) UNIFICAR CLAVES EN BIGINT
-- =====================================================================
-- PostgreSQL admite claves foraneas entre INTEGER y BIGINT, por lo que
-- cada ALTER es valido de forma independiente y el orden no importa.

-- --- A.1  Claves primarias que eran INTEGER ---
ALTER TABLE alertas_inventario     ALTER COLUMN id TYPE BIGINT;
ALTER TABLE direcciones_cliente    ALTER COLUMN id TYPE BIGINT;
ALTER TABLE movimientos_inventario ALTER COLUMN id TYPE BIGINT;
ALTER TABLE multimedia_producto    ALTER COLUMN id TYPE BIGINT;
ALTER TABLE productos_proveedor    ALTER COLUMN id TYPE BIGINT;
ALTER TABLE promociones            ALTER COLUMN id TYPE BIGINT;
ALTER TABLE roles                  ALTER COLUMN id TYPE BIGINT;

-- --- A.2  Claves foraneas que eran INTEGER ---
ALTER TABLE alertas_inventario     ALTER COLUMN id_producto            TYPE BIGINT;
ALTER TABLE direcciones_cliente    ALTER COLUMN id_cliente             TYPE BIGINT;
ALTER TABLE envios                 ALTER COLUMN id_usuario_responsable TYPE BIGINT;
ALTER TABLE movimientos_inventario ALTER COLUMN id_producto            TYPE BIGINT;
ALTER TABLE movimientos_inventario ALTER COLUMN id_usuario             TYPE BIGINT;
ALTER TABLE multimedia_producto    ALTER COLUMN id_producto            TYPE BIGINT;
ALTER TABLE productos_proveedor    ALTER COLUMN id_producto            TYPE BIGINT;
ALTER TABLE productos_proveedor    ALTER COLUMN id_proveedor           TYPE BIGINT;
ALTER TABLE promociones_producto   ALTER COLUMN id_promocion           TYPE BIGINT;
ALTER TABLE promociones_producto   ALTER COLUMN id_producto            TYPE BIGINT;
ALTER TABLE users                  ALTER COLUMN id_rol                 TYPE BIGINT;


-- =====================================================================
-- B) UNIFICAR SECUENCIAS EN BIGINT
-- =====================================================================
-- Una columna BIGINT alimentada por una secuencia INTEGER se agota en
-- 2.147.483.647 igual que antes. Se normalizan todas.

ALTER SEQUENCE alertas_inventario_id_seq     AS BIGINT;
ALTER SEQUENCE auditorias_id_seq             AS BIGINT;
ALTER SEQUENCE categorias_id_seq             AS BIGINT;
ALTER SEQUENCE clientes_id_seq               AS BIGINT;
ALTER SEQUENCE comprobantes_id_seq           AS BIGINT;
ALTER SEQUENCE configuracion_sistema_id_seq  AS BIGINT;
ALTER SEQUENCE detalle_venta_id_seq          AS BIGINT;
ALTER SEQUENCE direcciones_cliente_id_seq    AS BIGINT;
ALTER SEQUENCE envios_id_seq                 AS BIGINT;
ALTER SEQUENCE imagenes_producto_id_seq      AS BIGINT;
ALTER SEQUENCE inventario_id_seq             AS BIGINT;
ALTER SEQUENCE movimientos_inventario_id_seq AS BIGINT;
ALTER SEQUENCE multimedia_producto_id_seq    AS BIGINT;
ALTER SEQUENCE pagos_id_seq                  AS BIGINT;
ALTER SEQUENCE productos_id_seq              AS BIGINT;
ALTER SEQUENCE productos_proveedor_id_seq    AS BIGINT;
ALTER SEQUENCE promociones_id_seq            AS BIGINT;
ALTER SEQUENCE proveedores_id_seq            AS BIGINT;
ALTER SEQUENCE roles_id_seq                  AS BIGINT;
ALTER SEQUENCE transportadoras_id_seq        AS BIGINT;
ALTER SEQUENCE ventas_id_seq                 AS BIGINT;


-- =====================================================================
-- C) RESTRICCIONES DE NEGOCIO
-- =====================================================================

-- C.1  Una sola direccion principal por cliente.
--      Indice unico PARCIAL: la condicion WHERE hace que la unicidad
--      aplique solo a las filas con es_principal = true. El cliente
--      puede tener N direcciones, pero solo una marcada como principal.
CREATE UNIQUE INDEX uq_direcciones_cliente_principal
    ON direcciones_cliente (id_cliente)
    WHERE es_principal;

-- C.2  No repetir el mismo producto/proveedor con la misma vigencia.
ALTER TABLE productos_proveedor
    ADD CONSTRAINT uq_productos_proveedor_vigencia
    UNIQUE (id_producto, id_proveedor, fecha_inicio);

-- C.3  Toda venta tiene un vendedor.
--      Regla de negocio: no existe una venta sin responsable.
ALTER TABLE ventas
    ALTER COLUMN id_usuario SET NOT NULL;


-- =====================================================================
-- D) ELIMINAR ventas.metodo_pago
-- =====================================================================
-- Una venta puede tener varios pagos con metodos distintos (parte en
-- efectivo, parte por transferencia). Un unico campo en ventas no puede
-- representar ese caso y queda desactualizado sin que nadie lo note.
-- La fuente de verdad pasa a ser pagos.metodo_pago.
--
-- Al eliminar la columna desaparece tambien chk_ventas_metodo_pago.

ALTER TABLE ventas DROP COLUMN metodo_pago;

COMMENT ON TABLE pagos IS
    'Pagos de una venta. Unica fuente de verdad del metodo de pago: una venta admite varios pagos con metodos distintos.';


-- =====================================================================
-- E) promociones.fecha_actualizacion
-- =====================================================================
-- Era la unica tabla del esquema sin este campo, y la entidad Oferta
-- del backend ya lo declaraba como obligatorio.

ALTER TABLE promociones
    ADD COLUMN fecha_actualizacion TIMESTAMP DEFAULT now();

COMMIT;

-- =====================================================================
--  FIN DEL PLAN DE AJUSTE DE BASE DE DATOS  (fases 0 a 7)
--
--  El esquema queda cerrado:
--    - sin columnas hacia tablas inexistentes
--    - sin datos duplicados sin sincronizar
--    - tipos de clave uniformes
--    - todas las FK indexadas
--    - reglas de negocio expresadas como restricciones
--
--  Siguiente etapa: ajuste del backend contra este esquema.
-- =====================================================================
