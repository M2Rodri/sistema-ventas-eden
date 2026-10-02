-- =====================================================================
--  27 - Compras sin estado previo ("Por confirmar") y sin edicion
-- =====================================================================
--  QUE HACE
--  --------
--  Una compra es algo que ya se compro. Al registrarla, la mercaderia entra
--  al inventario y el costo del producto se actualiza (antes habia que
--  "confirmarla" despues). Para corregir un error se anula la compra, y se
--  registra de nuevo. Por eso el estado POR_CONFIRMAR deja de existir:
--  quedan CONFIRMADA (registrada) y CANCELADA (anulada).
--
--  Reemplaza el CHECK y el DEFAULT de compras.estado.
--
--  DATOS EXISTENTES
--  ----------------
--  Una compra en POR_CONFIRMAR nunca sumo stock. El script NO decide por vos:
--  si encuentra alguna, se detiene y las lista. Hay que resolver cada una
--  antes de volver a correrlo:
--    - Si la mercaderia llego de verdad: cargarle el stock con un ajuste de
--      entrada en Inventario y pasarla a CONFIRMADA
--        (UPDATE compras SET estado = 'CONFIRMADA' WHERE id = ...).
--    - Si fue un error o no se hizo: pasarla a CANCELADA.
--
--  EJECUCION
--  ---------
--  Dentro de una transaccion. Correr contra la base local y despues contra
--  produccion (ver README.md de esta carpeta).
-- =====================================================================

BEGIN;

DO $$
DECLARE
    pendientes TEXT;
BEGIN
    SELECT string_agg('#' || id, ', ' ORDER BY id) INTO pendientes
      FROM compras WHERE estado = 'POR_CONFIRMAR';
    IF pendientes IS NOT NULL THEN
        RAISE EXCEPTION 'Hay compras en POR_CONFIRMAR (%). Pasalas a CONFIRMADA o CANCELADA antes de correr este script.', pendientes;
    END IF;
END $$;

ALTER TABLE compras DROP CONSTRAINT IF EXISTS chk_compras_estado;
ALTER TABLE compras ALTER COLUMN estado SET DEFAULT 'CONFIRMADA';
ALTER TABLE compras
    ADD CONSTRAINT chk_compras_estado
    CHECK (estado IN ('CONFIRMADA', 'CANCELADA'));

COMMIT;
