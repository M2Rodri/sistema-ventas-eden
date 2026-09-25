-- =====================================================================
--  FASE 23 - RENOMBRAR ESTADOS DE COMPRA
--  Proyecto : Sistema de Ventas - Muebleria Eden
--  Base     : muebleria_eden_db
--  Fecha    : 2026-09-25
-- =====================================================================
--
--  QUE HACE
--  --------
--  Renombra los valores de compras.estado:
--    PENDIENTE -> POR_CONFIRMAR
--    RECIBIDA  -> CONFIRMADA
--    CANCELADA se queda igual.
--
--  POR QUE
--  -------
--  El modulo de Compras registra compras que YA se hicieron (no pedidos a
--  futuro): cuando se carga una compra, el proveedor ya entrego la
--  factura y el costo real ya se conoce. "Pendiente" y "Recibida" daban a
--  entender que el sistema esperaba a que algo llegara del proveedor, algo
--  que no pasa en este negocio. El estado real que cumple es otro: dar una
--  ventana para revisar/corregir antes de que la compra impacte el stock
--  y el costo del producto (irreversible). Por eso el nombre nuevo describe
--  eso: "por confirmar" (todavia se puede editar o cancelar) y
--  "confirmada" (ya se aplico, sin vuelta atras).
--
--  IMPACTO EN BACKEND / FRONTEND
--  ------------------------------
--    models/EstadoCompra.java        -> enum renombrado
--    services/CompraService.java     -> todas las comparaciones y
--                                        mensajes de error actualizados
--    services/ReporteService.java    -> filtro de compras confirmadas
--                                        actualizado
--    frontend types/proveedor.ts     -> EstadoCompra renombrado
--    frontend dashboard/compras      -> badges, filtros, botones y
--                                        textos actualizados
--    frontend components/ProductoModal.tsx, DetalleCompraModal.tsx
--                                     -> etiquetas actualizadas
--
--  DATOS EXISTENTES
--  ----------------
--  Se migran las filas existentes: PENDIENTE -> POR_CONFIRMAR,
--  RECIBIDA -> CONFIRMADA. CANCELADA no se toca.
--
--  SEGURIDAD
--  ---------
--  Actualiza filas y reemplaza el CHECK y el DEFAULT de la columna.
--  Dentro de una transaccion.
--
--  PENDIENTE
--  ---------
--  Este script se corrio contra la base local (muebleria_eden_db). Falta
--  aplicarlo tambien contra la base de produccion (Supabase) -- ver
--  README.md de esta carpeta sobre mantener las dos bases sincronizadas.
-- =====================================================================

BEGIN;

ALTER TABLE compras DROP CONSTRAINT chk_compras_estado;

UPDATE compras SET estado = 'POR_CONFIRMAR' WHERE estado = 'PENDIENTE';
UPDATE compras SET estado = 'CONFIRMADA' WHERE estado = 'RECIBIDA';

ALTER TABLE compras ALTER COLUMN estado SET DEFAULT 'POR_CONFIRMAR';

ALTER TABLE compras
    ADD CONSTRAINT chk_compras_estado
    CHECK (estado IN ('POR_CONFIRMAR', 'CONFIRMADA', 'CANCELADA'));

COMMIT;
