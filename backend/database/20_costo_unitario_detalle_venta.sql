-- =====================================================================
--  FASE 20 - COSTO UNITARIO EN DETALLE DE VENTA
--  Proyecto : Sistema de Ventas - Muebleria Eden
--  Base     : muebleria_eden_db
--  Fecha    : 2026-09-22
-- =====================================================================
--
--  QUE HACE
--  --------
--  Agrega detalle_venta.costo_unitario: el costo del producto en el
--  momento exacto en que se vendio, copiado de productos.costo_referencial
--  al registrar la venta. Backfillea las filas existentes con el costo
--  referencial actual (es lo unico que hay disponible para ventas viejas).
--
--  POR QUE
--  -------
--  El Reporte Financiero calculaba la ganancia como ventas menos compras
--  del periodo: eso es flujo de caja, no ganancia real. Un mes en que el
--  negocio compra para abastecerse aparecia como perdida aunque haya
--  vendido con margen.
--
--  La ganancia real es, por cada producto vendido, precio de venta menos
--  su costo, por la cantidad. Ese costo tiene que quedar fijo al momento
--  de la venta: si productos.costo_referencial cambia despues (el
--  proveedor subio el precio, por ejemplo), la ganancia de una venta ya
--  registrada no se puede recalcular sola.
--
--  IMPACTO EN BACKEND
--  ------------------
--    models/DetalleVenta.java    -> campo costoUnitario nuevo
--    services/VentaService.java  -> lo completa al crear cada detalle,
--                                    tomando producto.getCostoReferencial()
--    services/ReporteService.java -> nuevo getReporteFinanciero() que
--                                    calcula la ganancia real sumando
--                                    (precioUnitario - costoUnitario) *
--                                    cantidad de las ventas del periodo
--
--  NO SE TOCA
--  ----------
--  dto/DetalleVentaDTO.java NO expone este campo: el rol EMPLEADO puede
--  ver ventas, y no tiene que ver costos ni margenes. El calculo de
--  ganancia vive en el backend, en el endpoint de reporte financiero,
--  que es ADMIN unicamente.
--
--  DATOS EXISTENTES
--  -----------------
--  Verificado antes de escribir esto: 13 filas en detalle_venta, 0
--  productos con costo_referencial en cero, y costo_referencial es
--  NOT NULL en productos. El backfill no deja ninguna fila sin valor.
--
--  SEGURIDAD
--  ---------
--  Solo agrega la columna y la completa. No borra ni modifica otra
--  columna. Dentro de una transaccion.
-- =====================================================================

BEGIN;

-- 1) Columna nueva, nullable por ahora (las filas existentes todavia no
--    tienen valor).
ALTER TABLE detalle_venta ADD COLUMN costo_unitario NUMERIC(10,2);

-- 2) Backfill con el costo referencial actual del producto de cada linea.
UPDATE detalle_venta dv
SET costo_unitario = p.costo_referencial
FROM productos p
WHERE p.id = dv.id_producto;

-- 3) Ya no puede haber nulos: se exige de ahora en mas.
ALTER TABLE detalle_venta ALTER COLUMN costo_unitario SET NOT NULL;

-- 4) Mismo patron que chk_compras_importes en Compras.
ALTER TABLE detalle_venta
    ADD CONSTRAINT chk_detalle_venta_costo
    CHECK (costo_unitario >= 0);

COMMIT;
