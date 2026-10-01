-- =============================================================================
-- 25. Tipo de producto MUEBLE (categoría "Muebles de dormitorio")
-- =============================================================================
--
-- POR QUÉ
--
-- Las categorías del sistema son fijas y cada una lleva un tipo de producto:
-- Camas (CAMA), Colchones (COLCHON), Almohadas (ALMOHADA) y Accesorios
-- (ACCESORIO). Se agrega una quinta, "Muebles de dormitorio" (closets,
-- roperos, veladores...), con su propio tipo MUEBLE.
--
-- La restricción de productos.tipo_producto (script 02) solo admite los cuatro
-- tipos anteriores, así que un producto de tipo MUEBLE no se podría guardar.
--
-- QUE HACE
--
-- A) Reemplaza la restricción de productos.tipo_producto para admitir MUEBLE.
-- B) Deja la misma restricción en categorias.tipo_producto. Esa columna se
--    agregó directamente en la base (sin script), así que acá se documenta que
--    existe y que admite los mismos cinco tipos.
--
-- No toca datos: todas las filas actuales ya cumplen la restricción nueva.
-- Es seguro correrlo más de una vez.
--
-- VERIFICACION (antes y después)
--
--   SELECT conrelid::regclass AS tabla, conname, pg_get_constraintdef(oid)
--   FROM pg_constraint
--   WHERE conname IN ('chk_productos_tipo_producto', 'chk_categorias_tipo_producto');
--
-- =============================================================================

BEGIN;

ALTER TABLE productos DROP CONSTRAINT IF EXISTS chk_productos_tipo_producto;
ALTER TABLE productos
    ADD CONSTRAINT chk_productos_tipo_producto
    CHECK (tipo_producto IN ('CAMA', 'COLCHON', 'ALMOHADA', 'ACCESORIO', 'MUEBLE'));

ALTER TABLE categorias DROP CONSTRAINT IF EXISTS chk_categorias_tipo_producto;
ALTER TABLE categorias
    ADD CONSTRAINT chk_categorias_tipo_producto
    CHECK (tipo_producto IN ('CAMA', 'COLCHON', 'ALMOHADA', 'ACCESORIO', 'MUEBLE'));

COMMIT;
