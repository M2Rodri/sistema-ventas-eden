-- =============================================================================
-- 26. Se quita el estado de entrega DESPACHADO
-- =============================================================================
--
-- POR QUÉ
--
-- El script 24 agregó DESPACHADO como paso intermedio de las ventas por
-- transportadora. En la práctica no hace falta: lo que importa es si el
-- cliente ya recibió el producto o no. Quedan solo dos estados:
--
--   PENDIENTE  el cliente todavía no recibió el producto.
--   ENTREGADO  el cliente ya lo recibió (también en TRANSPORTADORA).
--
-- QUE HACE
--
-- A) Pasa a PENDIENTE las ventas que estén en DESPACHADO: el producto salió
--    pero el cliente todavía no lo recibió, así que siguen "por entregar".
-- B) Deja la restricción CHECK de ventas.estado_entrega con PENDIENTE y
--    ENTREGADO.
--
-- ORDEN DE DESPLIEGUE
--
-- Correr este script ANTES de publicar la versión del backend que ya no tiene
-- DESPACHADO. Si el backend nuevo arranca con filas en DESPACHADO, falla al
-- leerlas.
--
-- El script 24 no se modifica: ya se corrió y queda como historia. Este lo
-- deja sin efecto en lo que toca a DESPACHADO. Es seguro correrlo más de una
-- vez.
--
-- VERIFICACION (antes y después)
--
--   SELECT estado_entrega, COUNT(*) FROM ventas GROUP BY estado_entrega ORDER BY 1;
--
--   SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint
--   WHERE conname = 'ventas_estado_entrega_check';
--
-- =============================================================================

BEGIN;

UPDATE ventas
SET estado_entrega = 'PENDIENTE'
WHERE estado_entrega = 'DESPACHADO';

ALTER TABLE ventas
    DROP CONSTRAINT IF EXISTS ventas_estado_entrega_check,
    ADD CONSTRAINT ventas_estado_entrega_check
    CHECK (estado_entrega IN ('PENDIENTE', 'ENTREGADO'));

COMMIT;
