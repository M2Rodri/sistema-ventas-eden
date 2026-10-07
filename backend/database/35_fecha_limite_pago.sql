-- =====================================================================
--  35 - Fecha límite del pago pendiente en las ventas
-- =====================================================================
--  QUE HACE
--  --------
--  Agrega ventas.fecha_limite_pago (DATE, opcional): hasta cuándo el cliente
--  prometió pagar lo que queda pendiente. Se llena al registrar una venta
--  con pago pendiente (web y app) y se muestra en "por cobrar".
--
--  POR QUE
--  -------
--  Hasta ahora una venta con saldo pendiente no decía hasta cuándo se
--  esperaba el pago, y el dueño tenía que acordarse. Con la fecha se puede
--  ver de un vistazo qué cobros se están atrasando.
--
--  DATOS EXISTENTES
--  ----------------
--  Las ventas que ya existen quedan con la fecha vacía (NULL): no se inventa
--  ninguna fecha.
--
--  ORDEN
--  -----
--  Correrlo ANTES de publicar el backend nuevo: la entidad Venta ya lee
--  esta columna y, con ddl-auto en validate, fallaría si no existe. Es
--  seguro correrlo antes: el backend viejo simplemente la ignora.
-- =====================================================================

BEGIN;

ALTER TABLE ventas ADD COLUMN IF NOT EXISTS fecha_limite_pago DATE;

COMMENT ON COLUMN ventas.fecha_limite_pago IS
    'Fecha hasta la que el cliente prometió pagar el saldo pendiente. Opcional.';

COMMIT;
