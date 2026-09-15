-- =====================================================================
--  FASE 2 (ejecucion) / FASE 4 (plan) - INTEGRIDAD E INDICES
--  Proyecto : Sistema de Ventas - Muebleria Eden
--  Base     : muebleria_eden_db
--  Fecha    : 2026-08-20
-- =====================================================================
--
--  QUE HACE
--  --------
--  A) Crea indices sobre las claves foraneas.
--  B) Agrega la FK faltante en movimientos_inventario.id_usuario.
--  C) Restringe con CHECK las columnas de estado/tipo.
--  D) Fuerza inventario 1:1 con producto (un solo almacen).
--
--  POR QUE
--  -------
--  A) PostgreSQL indexa automaticamente las PRIMARY KEY, pero NO las
--     FOREIGN KEY. Sin indice, cada consulta por FK es scan secuencial,
--     y borrar un producto obliga a escanear 8 tablas completas para
--     validar la integridad referencial.
--  B) id_usuario dice QUIEN movio el stock: es la columna que sostiene
--     la trazabilidad del inventario. Sin FK puede apuntar a un usuario
--     que no existe.
--  C) Hoy son varchar libre. Con spring.jpa.hibernate.ddl-auto=none la
--     base es la ultima linea de defensa, y no esta defendiendo nada:
--     cualquier error de tipeo del backend entra y despues no cuadran
--     los reportes.
--  D) Decision tomada: un solo almacen, una fila de inventario por
--     producto.
--
--  SEGURIDAD
--  ---------
--  Verificado antes de escribir este script:
--    - Todas las tablas afectadas por los CHECK estan vacias, salvo
--      productos.tipo_producto = 'CAMA', que es un valor valido.
--    - movimientos_inventario esta vacia: la FK no puede fallar por
--      datos huerfanos.
--    - inventario tiene 1 fila: el UNIQUE no puede fallar.
--  Todo dentro de una transaccion: si algo falla, no cambia nada.
--
--  IMPACTO EN BACKEND
--  ------------------
--  Ninguno. Este script no cambia nombres ni elimina columnas.
--  Solo agrega restricciones que el backend YA respeta (los valores
--  de los CHECK se extrajeron de los propios enums del backend).
--
-- =====================================================================

BEGIN;

-- =====================================================================
-- A) INDICES SOBRE CLAVES FORANEAS
-- =====================================================================
-- Nomenclatura: idx_<tabla>_<columna>
-- No se indexa promociones_producto.id_promocion porque ya es la
-- columna principal de la PRIMARY KEY compuesta (id_promocion, id_producto)
-- y por lo tanto ya esta indexada.

-- --- Referencias a productos ---
CREATE INDEX idx_alertas_inventario_producto     ON alertas_inventario     (id_producto);
CREATE INDEX idx_detalle_venta_producto          ON detalle_venta          (id_producto);
CREATE INDEX idx_imagenes_producto_producto      ON imagenes_producto      (id_producto);
CREATE INDEX idx_movimientos_inv_producto        ON movimientos_inventario (id_producto);
CREATE INDEX idx_multimedia_producto_producto    ON multimedia_producto    (id_producto);
CREATE INDEX idx_productos_proveedor_producto    ON productos_proveedor    (id_producto);
CREATE INDEX idx_promociones_producto_producto   ON promociones_producto   (id_producto);

-- --- Referencias a ventas ---
CREATE INDEX idx_comprobantes_venta              ON comprobantes           (id_venta);
CREATE INDEX idx_detalle_venta_venta             ON detalle_venta          (id_venta);
CREATE INDEX idx_envios_venta                    ON envios                 (id_venta);
CREATE INDEX idx_pagos_venta                     ON pagos                  (id_venta);

-- --- Referencias a users ---
CREATE INDEX idx_auditorias_usuario              ON auditorias             (id_usuario);
CREATE INDEX idx_movimientos_inv_usuario         ON movimientos_inventario (id_usuario);
CREATE INDEX idx_ventas_usuario                  ON ventas                 (id_usuario);
CREATE INDEX idx_envios_usuario_responsable      ON envios                 (id_usuario_responsable);

-- --- Otras referencias ---
CREATE INDEX idx_direcciones_cliente_cliente     ON direcciones_cliente    (id_cliente);
CREATE INDEX idx_envios_transportadora           ON envios                 (id_transportadora);
CREATE INDEX idx_productos_categoria             ON productos              (id_categoria);
CREATE INDEX idx_productos_proveedor_proveedor   ON productos_proveedor    (id_proveedor);
CREATE INDEX idx_ventas_cliente                  ON ventas                 (id_cliente);


-- =====================================================================
-- B) CLAVE FORANEA FALTANTE
-- =====================================================================
-- movimientos_inventario.id_usuario -> users(id)
-- Es la unica columna id_* del esquema que referenciaba una tabla sin
-- declararlo. Queda NULL-able: un movimiento puede ser generado por el
-- sistema (ej. descuento automatico por venta) y no por una persona.

ALTER TABLE movimientos_inventario
    ADD CONSTRAINT fk_movimientos_inv_usuario
    FOREIGN KEY (id_usuario) REFERENCES users (id);


-- =====================================================================
-- C) CHECK CONSTRAINTS EN COLUMNAS DE ESTADO / TIPO
-- =====================================================================
-- Los valores permitidos se extrajeron de los enums del backend, para
-- que base y aplicacion validen exactamente lo mismo:
--
--   TipoProducto, EstadoVenta, MetodoPago, EstadoPago, EstadoEnvio,
--   TipoCliente, TipoComprobante, TipoAjuste, EstadoAlerta
--
-- Nota: un CHECK con IN (...) se evalua como NULL cuando la columna es
-- NULL, y un CHECK que da NULL se considera satisfecho. Por eso las
-- columnas nullable siguen aceptando NULL sin necesitar tratamiento
-- especial.

-- --- productos ---
ALTER TABLE productos
    ADD CONSTRAINT chk_productos_tipo_producto
    CHECK (tipo_producto IN ('CAMA', 'COLCHON', 'ALMOHADA', 'ACCESORIO'));

-- --- ventas ---
ALTER TABLE ventas
    ADD CONSTRAINT chk_ventas_estado
    CHECK (estado IN ('COMPLETADA', 'PENDIENTE_PAGO', 'CANCELADA'));

ALTER TABLE ventas
    ADD CONSTRAINT chk_ventas_metodo_pago
    CHECK (metodo_pago IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'QR'));

-- --- pagos ---
ALTER TABLE pagos
    ADD CONSTRAINT chk_pagos_estado
    CHECK (estado IN ('PENDIENTE', 'COMPLETADO', 'RECHAZADO'));

ALTER TABLE pagos
    ADD CONSTRAINT chk_pagos_metodo_pago
    CHECK (metodo_pago IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'QR'));

-- --- envios ---
ALTER TABLE envios
    ADD CONSTRAINT chk_envios_estado_seguimiento
    CHECK (estado_seguimiento IN ('PENDIENTE', 'EN_PREPARACION', 'EN_CAMINO',
                                  'ENTREGADO', 'DEVUELTO', 'CANCELADO'));

-- --- clientes ---
-- REGISTRADO = cliente con datos completos cargados por el empleado
-- INVITADO   = venta de mostrador sin datos del comprador
ALTER TABLE clientes
    ADD CONSTRAINT chk_clientes_tipo
    CHECK (tipo IN ('REGISTRADO', 'INVITADO'));

-- --- comprobantes ---
ALTER TABLE comprobantes
    ADD CONSTRAINT chk_comprobantes_tipo
    CHECK (tipo_comprobante IN ('RECIBO', 'COMPROBANTE', 'NOTA_VENTA'));

-- --- movimientos_inventario ---
ALTER TABLE movimientos_inventario
    ADD CONSTRAINT chk_movimientos_inv_tipo
    CHECK (tipo IN ('ENTRADA', 'SALIDA', 'COMPRA', 'VENTA',
                    'DEVOLUCION', 'MERMA', 'AJUSTE_INICIAL'));

-- --- alertas_inventario ---
ALTER TABLE alertas_inventario
    ADD CONSTRAINT chk_alertas_inv_estado
    CHECK (estado IN ('PENDIENTE', 'ATENDIDA'));

-- NOTA: imagenes_producto.tipo queda SIN restringir a proposito.
-- No existe enum para ella en el backend, la entidad ImagenProducto ni
-- siquiera la mapea, y su unico valor actual es 'normal' en minuscula.
-- Definir sus valores validos, o eliminar la columna, queda pendiente.


-- =====================================================================
-- D) INVENTARIO 1:1 CON PRODUCTO
-- =====================================================================
-- Decision tomada: un solo almacen. Una fila de inventario por producto.
-- Sin esta restriccion el esquema permitia N filas por producto, lo que
-- volvia ambiguo cual es el stock real.
-- El indice UNIQUE resultante sirve ademas como indice de la FK
-- inventario.id_producto (por eso no se creo uno en el bloque A).

ALTER TABLE inventario
    ADD CONSTRAINT uq_inventario_producto UNIQUE (id_producto);

COMMIT;

-- =====================================================================
--  FIN
--  Siguiente: 03_ambiguedades.sql
--    - eliminar productos.stock (manda inventario.cantidad_disponible)
--    - normalizar roles: users.id_rol -> roles(id)
--    - renombrar productos.precio_unitario -> costo_referencial
-- =====================================================================
