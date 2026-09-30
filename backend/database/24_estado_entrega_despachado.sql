-- =============================================================================
-- 24. Estado de entrega DESPACHADO, y retiro en tienda entregado al registrar
-- =============================================================================
--
-- POR QUÉ
--
-- El script 16 dejó dos estados de entrega (PENDIENTE y ENTREGADO). Con el uso
-- real aparecieron dos cosas que ese modelo no expresa:
--
--   1. En una venta por transportadora hay un paso intermedio: la mercadería
--      ya salió con la transportadora pero todavía no llegó al cliente. Ese
--      paso se llama DESPACHADO.
--   2. Una venta con retiro en tienda no tiene nada que despachar: el cliente
--      se lleva el producto en el momento. Dejarla PENDIENTE obligaba a la web
--      a esconderla con un filtro especial ("En tienda") y hacía que el
--      backend, la web y la app contaran distinto las entregas por hacer.
--
-- Recorrido de cada modalidad desde ahora:
--
--   RETIRO:         queda ENTREGADO al registrar la venta.
--   DOMICILIO:      PENDIENTE -> ENTREGADO.
--   TRANSPORTADORA: PENDIENTE -> DESPACHADO -> ENTREGADO (también se puede
--                   pasar de PENDIENTE a ENTREGADO directamente).
--
-- Con esto "por entregar" tiene una sola definición en todo el sistema: estado
-- de entrega distinto de ENTREGADO y venta no cancelada.
--
-- QUE HACE
--
-- A) Agrega DESPACHADO a la restricción CHECK de ventas.estado_entrega.
-- B) Pasa a ENTREGADO las ventas RETIRO que hoy están PENDIENTE. No toca las
--    canceladas: una venta cancelada no se entregó, y de todos modos no cuenta
--    como "por entregar".
--
-- IMPACTO EN BACKEND
--
--   models/EstadoEntrega.java      -> tercer valor, DESPACHADO
--   services/VentaService.java     -> estado inicial según modalidad; despachar,
--                                     deshacer entrega y editar datos de
--                                     entrega; marcarEntregado ya no mira el
--                                     saldo
--   repositories/VentaRepository   -> countVentasPorEntregar con la definición
--                                     única
--
-- ORDEN DE DESPLIEGUE
--
-- Correr este script ANTES de desplegar el backend nuevo. Con el backend nuevo
-- y la restricción vieja, despachar una venta fallaría contra el CHECK.
-- El script es repetible: volver a correrlo no cambia nada.
--
-- SEGURIDAD
--
-- No borra ni reescribe datos salvo el UPDATE del punto B, que solo toca filas
-- RETIRO en PENDIENTE. Todo va en una transacción: si algo falla, no queda
-- nada a medias.
-- =============================================================================

BEGIN;

-- A) Restricción de estado_entrega con los tres valores.
ALTER TABLE ventas
    DROP CONSTRAINT IF EXISTS ventas_estado_entrega_check,
    ADD CONSTRAINT ventas_estado_entrega_check
        CHECK (estado_entrega IN ('PENDIENTE', 'DESPACHADO', 'ENTREGADO'));

-- B) El retiro en tienda ya está entregado.
UPDATE ventas
   SET estado_entrega = 'ENTREGADO'
 WHERE modalidad_entrega = 'RETIRO'
   AND estado_entrega = 'PENDIENTE'
   AND estado <> 'CANCELADA';

COMMIT;

-- =============================================================================
-- VERIFICACIÓN (opcional, después de correrlo)
-- =============================================================================
--
-- 1) La restricción debe listar los tres valores:
--
--    SELECT pg_get_constraintdef(oid)
--      FROM pg_constraint
--     WHERE conname = 'ventas_estado_entrega_check';
--
-- 2) No debe quedar ninguna venta RETIRO sin entregar, salvo las canceladas
--    (debe devolver 0):
--
--    SELECT COUNT(*) FROM ventas
--     WHERE modalidad_entrega = 'RETIRO' AND estado_entrega <> 'ENTREGADO'
--       AND estado <> 'CANCELADA';
--
-- 3) Resumen por modalidad y estado:
--
--    SELECT modalidad_entrega, estado_entrega, COUNT(*)
--      FROM ventas GROUP BY 1, 2 ORDER BY 1, 2;
-- =============================================================================
