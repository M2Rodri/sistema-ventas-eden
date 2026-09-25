-- =====================================================================
-- 21) QUITAR ubicacion DE inventario
-- =====================================================================
-- El campo nunca tuvo una forma en la app para cargarlo: se creaba
-- siempre en "Sin asignar" (ver antiguo ProductoService.crearProducto) y
-- ninguna pantalla dejaba editarlo. Las 8 filas de inventario lo
-- confirmaban: todas en "Sin asignar", sin excepcion. Un campo que nadie
-- puede llenar no es informacion, es ruido en la tabla y en el detalle
-- del producto.
-- =====================================================================

BEGIN;

ALTER TABLE inventario DROP COLUMN ubicacion;

COMMIT;
