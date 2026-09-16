-- =============================================================================
-- 16. La entrega pasa a ser parte de la venta
-- =============================================================================
--
-- POR QUÉ
--
-- Hasta ahora "por cobrar" y "por entregar" no tenían dato real detrás:
-- ventas.saldo_pendiente siempre quedaba en 0 (toda venta se creaba ya
-- cobrada) y la entrega vivía en una tabla aparte (envios), que la app móvil
-- no va a usar. El sistema necesita que ambos números salgan de 'ventas'
-- directamente.
--
-- QUE HACE
--
-- A) Agrega a 'ventas' las columnas de entrega: modalidad_entrega,
--    estado_entrega, direccion_destino, ciudad, transportadora,
--    guia_remision.
-- B) Restringe modalidad_entrega y estado_entrega con CHECK, mismo criterio
--    que el resto de las columnas de estado del esquema (ver script 02).
--
-- IMPACTO EN BACKEND
--
--   models/ModalidadEntrega.java, models/EstadoEntrega.java (nuevos)
--   models/Venta.java              -> 6 campos nuevos
--   dto/VentaRequest.java          -> montoPagado, modalidadEntrega,
--                                     direccionDestino, ciudad,
--                                     transportadora, guiaRemision
--   dto/VentaResponse.java         -> expone los mismos campos
--   services/VentaService.java     -> createVentaDirecta calcula saldo y
--                                     estado según lo pagado; nuevo método
--                                     marcarEntregado
--   services/PagoService.java      -> registrarPago recalcula el saldo
--   dto/DashboardResponse.java     -> ventasPorEntregar
--
-- Las tablas 'envios' y 'transportadoras' NO se tocan ni se eliminan: quedan
-- sin usar desde las ventas nuevas, pero el módulo web que las administra
-- sigue funcionando igual.
--
-- SEGURIDAD
--
-- Todas las columnas nuevas son NULL o tienen un default, así que las filas
-- existentes de 'ventas' quedan válidas sin necesidad de backfill:
-- modalidad_entrega y estado_entrega se completan con RETIRO/PENDIENTE.
-- =============================================================================

BEGIN;

ALTER TABLE ventas
    ADD COLUMN IF NOT EXISTS modalidad_entrega VARCHAR(20) NOT NULL DEFAULT 'RETIRO',
    ADD COLUMN IF NOT EXISTS estado_entrega     VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    ADD COLUMN IF NOT EXISTS direccion_destino  VARCHAR(300),
    ADD COLUMN IF NOT EXISTS ciudad             VARCHAR(50),
    ADD COLUMN IF NOT EXISTS transportadora     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS guia_remision      VARCHAR(100);

ALTER TABLE ventas
    DROP CONSTRAINT IF EXISTS ventas_modalidad_entrega_check,
    ADD CONSTRAINT ventas_modalidad_entrega_check
        CHECK (modalidad_entrega IN ('RETIRO', 'DOMICILIO', 'TRANSPORTADORA'));

ALTER TABLE ventas
    DROP CONSTRAINT IF EXISTS ventas_estado_entrega_check,
    ADD CONSTRAINT ventas_estado_entrega_check
        CHECK (estado_entrega IN ('PENDIENTE', 'ENTREGADO'));

COMMIT;
