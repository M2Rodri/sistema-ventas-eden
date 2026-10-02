-- =====================================================================
--  29 - Compra sin proveedor
-- =====================================================================
--  QUE HACE
--  --------
--  A veces se compra sin un proveedor formal. compras.id_proveedor deja de
--  ser obligatorio. El UNIQUE (id_proveedor, numero_factura) sigue valiendo
--  para las compras que si tienen proveedor; las que no, quedan fuera de esa
--  regla (en PostgreSQL un NULL no choca con otro NULL).
--
--  DATOS EXISTENTES
--  ----------------
--  No se toca ninguna fila.
--
--  EJECUCION
--  ---------
--  Dentro de una transaccion. Correr contra la base local y despues contra
--  produccion (ver README.md de esta carpeta).
-- =====================================================================

BEGIN;

ALTER TABLE compras ALTER COLUMN id_proveedor DROP NOT NULL;

COMMIT;
