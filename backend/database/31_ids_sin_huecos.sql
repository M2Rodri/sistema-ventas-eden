-- =====================================================================
--  31 - IDs sin huecos (contadores propios para las tablas principales)
-- =====================================================================
--  POR QUE
--  -------
--  La secuencia de PostgreSQL no devuelve el numero si una operacion falla
--  y se revierte (por ejemplo, una venta sin stock): el siguiente registro
--  salta un numero y queda un hueco (1, 2, 4...). Con un contador propio,
--  que se actualiza DENTRO de la misma transaccion, si la operacion se
--  revierte el contador tambien, y el numero lo usa el siguiente.
--
--  QUE HACE
--  --------
--  - Crea la tabla contadores_id con una fila por tabla: ventas, compras,
--    productos, clientes, proveedores, categorias, inventario, pagos,
--    comprobantes, movimientos_inventario y usuarios.
--  - La inicializa con el ID mas alto que ya existe en cada tabla.
--  - Crea la funcion sincronizar_contadores_id(): vuelve a poner cada
--    contador en el ID mas alto de su tabla. Hay que llamarla al terminar
--    cualquier carga masiva por SQL (como la carga inicial de productos), que
--    inserta con IDs propios sin pasar por el sistema.
--
--  El backend (IdSinHuecos / GeneradorIdSinHuecos) toma los IDs de estos
--  contadores. SIN este script, el backend nuevo no puede guardar registros:
--  hay que correrlo ANTES de publicar el backend.
--
--  QUE NO HACE
--  -----------
--  No renumera nada de lo que ya existe y no cambia las secuencias de la base.
--
--  EJECUCION
--  ---------
--  Dentro de una transaccion. Se puede correr mas de una vez sin problema.
-- =====================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS contadores_id (
    tabla  VARCHAR(60) PRIMARY KEY,
    ultimo BIGINT      NOT NULL DEFAULT 0 CHECK (ultimo >= 0)
);

CREATE OR REPLACE FUNCTION sincronizar_contadores_id() RETURNS void AS $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['ventas', 'compras', 'productos', 'clientes', 'proveedores', 'categorias',
                             'inventario', 'pagos', 'comprobantes', 'movimientos_inventario', 'usuarios']
    LOOP
        EXECUTE format(
            'INSERT INTO contadores_id (tabla, ultimo) SELECT %L, COALESCE(MAX(id), 0) FROM %I '
            'ON CONFLICT (tabla) DO UPDATE SET ultimo = EXCLUDED.ultimo', t, t);
    END LOOP;
END;
$$ LANGUAGE plpgsql;

SELECT sincronizar_contadores_id();

COMMIT;
