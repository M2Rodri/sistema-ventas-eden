-- =============================================================================
-- 12. Alinear el esquema de Supabase con las entidades del backend
-- =============================================================================
--
-- POR QUÉ
--
-- Al migrar la base a Supabase, tres tablas quedaron con una estructura
-- distinta de la que esperan las entidades JPA. El esquema de Supabase se armó
-- por otro camino que los scripts 01 a 10 de esta carpeta, y las dos versiones
-- se mezclaron.
--
-- Síntomas concretos que esto provoca:
--
--   * alertas_inventario: la consulta falla con "column cantidad_actual does
--     not exist". Es el error que rompe la campana de notificaciones y la
--     pantalla de inventario.
--   * proveedores y envios: tienen columnas obligatorias duplicadas que las
--     entidades no conocen. No molestan al leer, pero el INSERT falla apenas
--     alguien intenta dar de alta un proveedor o un envío. Eran dos fallas
--     esperando a que las encontrara un usuario.
--
-- CRITERIO
--
-- Se alinea la base a las entidades, y no al revés, porque el backend y el
-- frontend ya coinciden entre sí: el tipo TypeScript, el DTO y la pantalla
-- usan cantidadActual / cantidadMinima. La estructura de Supabase venía del
-- ejercicio de clase del módulo 3, no de una decisión de diseño de este
-- sistema.
--
-- Las columnas tipo_alerta y mensaje se eliminan en lugar de dejarse nulas:
-- una columna que ninguna capa lee es deuda que confunde al leer el esquema.
--
-- DATOS
--
-- Hay un respaldo en CSV de las tres tablas en:
--   database/respaldo_supabase_20260913/
--
-- Las dos alertas existentes no se borran: sus cantidades se recuperan del
-- inventario y del stock mínimo reales de cada producto.
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. alertas_inventario
-- -----------------------------------------------------------------------------

-- Se agregan primero como nulas para poder rellenar las filas que ya existen.
ALTER TABLE public.alertas_inventario
    ADD COLUMN IF NOT EXISTS cantidad_actual INTEGER,
    ADD COLUMN IF NOT EXISTS cantidad_minima INTEGER;

-- Las cantidades se toman de la situación real del producto, no de un valor
-- inventado: la existencia actual sale de inventario y el umbral de productos.
UPDATE public.alertas_inventario a
SET cantidad_actual = COALESCE(i.cantidad_disponible, 0)
FROM public.inventario i
WHERE i.id_producto = a.id_producto
  AND a.cantidad_actual IS NULL;

UPDATE public.alertas_inventario a
SET cantidad_minima = COALESCE(p.stock_minimo, 0)
FROM public.productos p
WHERE p.id = a.id_producto
  AND a.cantidad_minima IS NULL;

-- Red por si alguna alerta apunta a un producto sin registro de inventario.
UPDATE public.alertas_inventario
SET cantidad_actual = COALESCE(cantidad_actual, 0),
    cantidad_minima = COALESCE(cantidad_minima, 0);

ALTER TABLE public.alertas_inventario
    ALTER COLUMN cantidad_actual SET NOT NULL,
    ALTER COLUMN cantidad_minima SET NOT NULL;

-- La entidad llama a esta fecha fechaAlerta, no fechaCreacion.
ALTER TABLE public.alertas_inventario
    RENAME COLUMN fecha_creacion TO fecha_alerta;

-- Columnas del diseño anterior que ninguna capa del sistema usa.
ALTER TABLE public.alertas_inventario
    DROP COLUMN IF EXISTS tipo_alerta,
    DROP COLUMN IF EXISTS mensaje;

-- -----------------------------------------------------------------------------
-- 2. proveedores
-- -----------------------------------------------------------------------------
-- Quedaron "nombre" y "nombre_empresa", las dos obligatorias. La entidad solo
-- mapea nombre_empresa, así que todo INSERT desde el backend fallaba por no
-- enviar la otra. La tabla está vacía: no se pierde información.
ALTER TABLE public.proveedores
    DROP COLUMN IF EXISTS nombre;

-- -----------------------------------------------------------------------------
-- 3. envios
-- -----------------------------------------------------------------------------
-- Mismo caso: "direccion_destino" (la que mapea la entidad) y
-- "direccion_envio" (obligatoria y desconocida para el backend).
ALTER TABLE public.envios
    DROP COLUMN IF EXISTS direccion_envio;

COMMIT;

-- =============================================================================
-- Verificación
-- =============================================================================
SELECT 'alertas_inventario' AS tabla, string_agg(column_name, ', ' ORDER BY ordinal_position) AS columnas
FROM information_schema.columns WHERE table_schema='public' AND table_name='alertas_inventario'
UNION ALL
SELECT 'proveedores', string_agg(column_name, ', ' ORDER BY ordinal_position)
FROM information_schema.columns WHERE table_schema='public' AND table_name='proveedores'
UNION ALL
SELECT 'envios', string_agg(column_name, ', ' ORDER BY ordinal_position)
FROM information_schema.columns WHERE table_schema='public' AND table_name='envios';
