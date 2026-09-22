-- =====================================================================
--  FASE 19 - SIMPLIFICAR ESTADOS DE COMPRA
--  Proyecto : Sistema de Ventas - Muebleria Eden
--  Base     : muebleria_eden_db
--  Fecha    : 2026-09-21
-- =====================================================================
--
--  QUE HACE
--  --------
--  Reduce el CHECK de compras.estado de cinco valores a tres:
--  PENDIENTE, RECIBIDA, CANCELADA. Saca CONFIRMADA y EN_TRANSITO.
--
--  POR QUE
--  -------
--  El negocio es de una sola persona: no hay un paso donde "el proveedor
--  confirma el pedido" ni un seguimiento real de "en transito" separado
--  de simplemente esperar a que llegue. Esos dos estados intermedios
--  nunca se usaron en la practica.
--
--  IMPACTO EN BACKEND / FRONTEND
--  ------------------------------
--    models/EstadoCompra.java       -> enum reducido a los tres valores
--    services/CompraService.java    -> cancelarCompra() solo permite
--                                       cancelar desde PENDIENTE
--    controllers/CompraController   -> /estadisticas ya no cuenta
--                                       confirmadas ni enTransito
--    frontend types/proveedor.ts    -> EstadoCompra y CompraEstadisticas
--                                       reducidos igual
--    frontend dashboard/compras     -> se sacan los botones y filtros de
--                                       Confirmar / Marcar en transito
--
--  DATOS EXISTENTES
--  ----------------
--  Verificado antes de escribir esto: no hay ninguna compra guardada en
--  estado CONFIRMADA ni EN_TRANSITO (solo RECIBIDA), asi que no hace
--  falta reasignar filas.
--
--  SEGURIDAD
--  ---------
--  Solo reemplaza el CHECK. No toca filas ni otras columnas.
--  Dentro de una transaccion.
-- =====================================================================

BEGIN;

ALTER TABLE compras DROP CONSTRAINT chk_compras_estado;

ALTER TABLE compras
    ADD CONSTRAINT chk_compras_estado
    CHECK (estado IN ('PENDIENTE', 'RECIBIDA', 'CANCELADA'));

COMMIT;
