-- =====================================================================
--  37 - Quitar los datos de prueba de Newman y dejar los IDs sin huecos
-- =====================================================================
--  POR QUE
--  -------
--  Cada corrida de las pruebas de la API (Newman) crea clientes, productos,
--  ventas y compras de prueba y los anula, pero no los borra. Con el tiempo
--  quedan mezclados con los datos reales y dejan huecos en la numeracion
--  cuando se quitan.
--
--  QUE HACE (todo en una sola transaccion: si algo falla, no cambia nada)
--  -----------------------------------------------------------------------
--  1. Marca como "de prueba":
--       - usuarios  prueba_* y evaluador_e2
--       - clientes  "Cliente Prueba A/B" (nombre Cliente, apellido Prueba ...) con NIT/CI que empieza con PR
--       - productos "Producto Prueba A/B" dados de baja
--       - ventas    de esos clientes (todas deben estar CANCELADA)
--       - compras   con numero de factura PRUEBA-* (todas deben estar CANCELADA)
--     y se detiene si algo no cuadra (una venta de prueba que no esta anulada,
--     un dato real que usa un producto de prueba, un pago con comprobante subido...).
--  2. Borra lo marcado, con todo lo que cuelga de ello (detalles, pagos,
--     comprobantes, movimientos de inventario, alertas, inventario) y las
--     auditorias que hablan de ello.
--  3. Renumera lo que queda de cada tabla: 1, 2, 3... sin huecos, en el mismo
--     orden que tenia. Actualiza las claves foraneas, los ids que las
--     auditorias guardan como texto (id_registro y los "#N" de los detalles)
--     y los numeros de comprobante (REC-AAAA-00001...).
--  4. Deja los contadores de IDs y las secuencias en el ultimo numero.
--  5. Comprueba que todas las tablas quedaron sin huecos; si no, deshace todo.
--
--  QUE NO HACE
--  -----------
--  No toca usuarios, clientes, productos, ventas ni compras reales (ni los de
--  demostracion del dueño), categorias, proveedores ni configuracion. No cambia
--  nombres, claves, roles ni estados. No borra archivos del almacenamiento
--  (se comprueba que ningun dato borrado tenga un archivo subido).
--
--  DESPUES
--  -------
--  Quien tenga la sesion abierta debe volver a iniciarla (cambian los IDs de usuario).
--
--  EJECUCION (hacer antes un respaldo: node scripts/respaldar-base.mjs)
--  --------------------------------------------------------------------
--     PGOPTIONS="-c eden.confirmar_limpieza=si" psql ... -f 37_limpiar_pruebas_y_renumerar.sql
-- =====================================================================

BEGIN;

DO $$
BEGIN
    IF coalesce(current_setting('eden.confirmar_limpieza', true), '') <> 'si' THEN
        RAISE EXCEPTION 'Este script BORRA datos. Para correrlo: PGOPTIONS="-c eden.confirmar_limpieza=si"';
    END IF;
END $$;

-- ---------------------------------------------------------------------
-- 1. Que es de prueba
-- ---------------------------------------------------------------------
CREATE TEMP TABLE u_prueba  ON COMMIT DROP AS
    SELECT id FROM usuarios WHERE usuario LIKE 'prueba\_%' ESCAPE '\' OR usuario = 'evaluador_e2';
CREATE TEMP TABLE c_prueba  ON COMMIT DROP AS
    SELECT id FROM clientes WHERE nombre = 'Cliente' AND apellido LIKE 'Prueba%' AND nit_ci LIKE 'PR%';
CREATE TEMP TABLE p_prueba  ON COMMIT DROP AS
    SELECT id FROM productos WHERE nombre IN ('Producto Prueba A', 'Producto Prueba B') AND NOT activo;
CREATE TEMP TABLE v_prueba  ON COMMIT DROP AS
    SELECT id FROM ventas WHERE id_cliente IN (SELECT id FROM c_prueba);
CREATE TEMP TABLE co_prueba ON COMMIT DROP AS
    SELECT id FROM compras WHERE numero_factura LIKE 'PRUEBA-%';

DO $$
DECLARE
    n BIGINT;
BEGIN
    SELECT COUNT(*) INTO n FROM ventas WHERE id IN (SELECT id FROM v_prueba) AND estado <> 'CANCELADA';
    IF n > 0 THEN RAISE EXCEPTION 'Hay % ventas de prueba que no estan anuladas', n; END IF;

    SELECT COUNT(*) INTO n FROM compras WHERE id IN (SELECT id FROM co_prueba) AND estado <> 'CANCELADA';
    IF n > 0 THEN RAISE EXCEPTION 'Hay % compras de prueba que no estan anuladas', n; END IF;

    SELECT COUNT(*) INTO n FROM detalle_venta d
    WHERE d.id_venta IN (SELECT id FROM v_prueba) AND d.id_producto NOT IN (SELECT id FROM p_prueba);
    IF n > 0 THEN RAISE EXCEPTION 'Una venta de prueba incluye % productos que no son de prueba', n; END IF;

    SELECT COUNT(*) INTO n FROM detalle_venta d
    WHERE d.id_producto IN (SELECT id FROM p_prueba) AND d.id_venta NOT IN (SELECT id FROM v_prueba);
    IF n > 0 THEN RAISE EXCEPTION 'Hay % lineas de venta reales que usan productos de prueba', n; END IF;

    SELECT COUNT(*) INTO n FROM detalle_compra d
    WHERE d.id_compra IN (SELECT id FROM co_prueba) AND d.id_producto NOT IN (SELECT id FROM p_prueba);
    IF n > 0 THEN RAISE EXCEPTION 'Una compra de prueba incluye % productos que no son de prueba', n; END IF;

    SELECT COUNT(*) INTO n FROM detalle_compra d
    WHERE d.id_producto IN (SELECT id FROM p_prueba) AND d.id_compra NOT IN (SELECT id FROM co_prueba);
    IF n > 0 THEN RAISE EXCEPTION 'Hay % lineas de compra reales que usan productos de prueba', n; END IF;

    SELECT COUNT(*) INTO n FROM pagos WHERE id_venta IN (SELECT id FROM v_prueba) AND url_comprobante IS NOT NULL;
    IF n > 0 THEN RAISE EXCEPTION 'Hay % pagos de prueba con comprobante subido (borrar el archivo aparte)', n; END IF;

    SELECT COUNT(*) INTO n FROM imagenes_producto WHERE id_producto IN (SELECT id FROM p_prueba);
    IF n > 0 THEN RAISE EXCEPTION 'Hay % imagenes de productos de prueba (borrar el archivo aparte)', n; END IF;

    SELECT (SELECT COUNT(*) FROM ventas WHERE id_usuario IN (SELECT id FROM u_prueba))
         + (SELECT COUNT(*) FROM compras WHERE id_usuario IN (SELECT id FROM u_prueba))
         + (SELECT COUNT(*) FROM pagos WHERE id_usuario IN (SELECT id FROM u_prueba))
         + (SELECT COUNT(*) FROM movimientos_inventario WHERE id_usuario IN (SELECT id FROM u_prueba))
         + (SELECT COUNT(*) FROM envios WHERE id_usuario_responsable IN (SELECT id FROM u_prueba))
         + (SELECT COUNT(*) FROM mensajes_contacto WHERE id_usuario_atiende IN (SELECT id FROM u_prueba))
      INTO n;
    IF n > 0 THEN RAISE EXCEPTION 'Los usuarios de prueba tienen % registros propios', n; END IF;

    RAISE NOTICE 'De prueba: % usuarios, % clientes, % productos, % ventas, % compras',
        (SELECT COUNT(*) FROM u_prueba), (SELECT COUNT(*) FROM c_prueba), (SELECT COUNT(*) FROM p_prueba),
        (SELECT COUNT(*) FROM v_prueba), (SELECT COUNT(*) FROM co_prueba);
END $$;

-- ---------------------------------------------------------------------
-- 2. Borrar (de lo que cuelga hacia lo que queda arriba)
-- ---------------------------------------------------------------------
DELETE FROM auditorias
WHERE id_usuario IN (SELECT id FROM u_prueba)
   OR (tabla_afectada = 'usuarios'  AND id_registro IN (SELECT id::text FROM u_prueba))
   OR (tabla_afectada = 'clientes'  AND id_registro IN (SELECT id::text FROM c_prueba))
   OR (tabla_afectada IN ('productos', 'inventario') AND id_registro IN (SELECT id::text FROM p_prueba))
   OR (tabla_afectada = 'ventas'    AND id_registro IN (SELECT id::text FROM v_prueba))
   OR (tabla_afectada = 'compras'   AND id_registro IN (SELECT id::text FROM co_prueba))
   OR detalles LIKE '%prueba\_%' ESCAPE '\'
   OR detalles LIKE '%evaluador_e2%'
   OR detalles LIKE '%Cliente Prueba%'
   OR detalles LIKE '%Producto Prueba%'
   OR detalles LIKE '%PRUEBA-%';

DELETE FROM movimientos_inventario WHERE id_producto IN (SELECT id FROM p_prueba);
DELETE FROM alertas_inventario     WHERE id_producto IN (SELECT id FROM p_prueba);
DELETE FROM inventario             WHERE id_producto IN (SELECT id FROM p_prueba);
DELETE FROM productos_proveedor    WHERE id_producto IN (SELECT id FROM p_prueba);
DELETE FROM promociones_producto   WHERE id_producto IN (SELECT id FROM p_prueba);
DELETE FROM multimedia_producto    WHERE id_producto IN (SELECT id FROM p_prueba);

DELETE FROM envios         WHERE id_venta IN (SELECT id FROM v_prueba);
DELETE FROM comprobantes   WHERE id_venta IN (SELECT id FROM v_prueba);
DELETE FROM pagos          WHERE id_venta IN (SELECT id FROM v_prueba);
DELETE FROM detalle_venta  WHERE id_venta IN (SELECT id FROM v_prueba);
DELETE FROM ventas         WHERE id IN (SELECT id FROM v_prueba);

DELETE FROM detalle_compra WHERE id_compra IN (SELECT id FROM co_prueba);
DELETE FROM compras        WHERE id IN (SELECT id FROM co_prueba);

DELETE FROM productos WHERE id IN (SELECT id FROM p_prueba);
DELETE FROM clientes  WHERE id IN (SELECT id FROM c_prueba);
DELETE FROM usuarios  WHERE id IN (SELECT id FROM u_prueba);

-- ---------------------------------------------------------------------
-- 3. Renumerar lo que queda: 1, 2, 3... en el mismo orden
-- ---------------------------------------------------------------------
-- Cambia el ID de cada fila de la tabla y, en todas las tablas que apuntan a ella, el numero
-- correspondiente. Quita las claves foraneas mientras tanto y las vuelve a crear (la base
-- revisa que todo siga cuadrando). Devuelve cuantas filas cambiaron de ID.
CREATE FUNCTION pg_temp.renumerar(p_tabla TEXT) RETURNS INT AS $$
DECLARE
    fk RECORD;
    mapa TEXT := 'mapa_' || p_tabla;
    mover INT;
    compuestas TEXT;
BEGIN
    EXECUTE format('CREATE TEMP TABLE %I ON COMMIT DROP AS '
                   'SELECT id AS viejo, ROW_NUMBER() OVER (ORDER BY id) AS nuevo FROM %I', mapa, p_tabla);
    EXECUTE format('SELECT COUNT(*) FROM %I WHERE viejo <> nuevo', mapa) INTO mover;
    IF mover = 0 THEN
        RETURN 0;
    END IF;

    SELECT string_agg(k.conrelid::regclass::text || '.' || k.conname, ', ') INTO compuestas
    FROM pg_constraint k
    WHERE k.contype = 'f' AND k.confrelid = format('public.%I', p_tabla)::regclass AND array_length(k.conkey, 1) <> 1;
    IF compuestas IS NOT NULL THEN
        RAISE EXCEPTION 'Clave foranea de varias columnas hacia %: %', p_tabla, compuestas;
    END IF;

    -- Los ids que las auditorias guardan como texto (no tienen clave foranea).
    IF p_tabla IN ('ventas', 'compras') THEN
        EXECUTE format('UPDATE auditorias a SET detalles = regexp_replace(a.detalles, ''#'' || m.viejo || ''\M'', ''#'' || m.nuevo) '
                       'FROM %I m WHERE a.tabla_afectada = %L AND a.id_registro = m.viejo::text '
                       'AND a.detalles ~ (''#'' || m.viejo || ''\M'')', mapa, p_tabla);
    END IF;
    IF p_tabla IN ('usuarios', 'clientes', 'productos', 'ventas', 'compras') THEN
        EXECUTE format('UPDATE auditorias a SET id_registro = m.nuevo::text '
                       'FROM %I m WHERE a.tabla_afectada = %L AND a.id_registro = m.viejo::text', mapa, p_tabla);
    END IF;
    IF p_tabla = 'productos' THEN
        -- Los ajustes de inventario guardan el id del producto.
        EXECUTE format('UPDATE auditorias a SET id_registro = m.nuevo::text '
                       'FROM %I m WHERE a.tabla_afectada = ''inventario'' AND a.id_registro = m.viejo::text', mapa);
    END IF;

    DROP TABLE IF EXISTS fks_tmp;
    CREATE TEMP TABLE fks_tmp ON COMMIT DROP AS
    SELECT k.conrelid::regclass::text AS tabla, k.conname AS nombre,
           pg_get_constraintdef(k.oid) AS definicion, a.attname AS columna
    FROM pg_constraint k
    JOIN pg_attribute a ON a.attrelid = k.conrelid AND a.attnum = k.conkey[1]
    WHERE k.contype = 'f' AND k.confrelid = format('public.%I', p_tabla)::regclass;

    FOR fk IN SELECT * FROM fks_tmp LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', fk.tabla, fk.nombre);
    END LOOP;

    FOR fk IN SELECT * FROM fks_tmp LOOP
        EXECUTE format('UPDATE %s t SET %I = m.nuevo FROM %I m WHERE t.%I = m.viejo', fk.tabla, fk.columna, mapa, fk.columna);
    END LOOP;

    -- En dos pasos para que un ID nuevo no choque con uno viejo que aun no se movio.
    EXECUTE format('UPDATE %I SET id = id + 1000000', p_tabla);
    EXECUTE format('UPDATE %I t SET id = m.nuevo FROM %I m WHERE t.id = m.viejo + 1000000', p_tabla, mapa);

    FOR fk IN SELECT * FROM fks_tmp LOOP
        EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s', fk.tabla, fk.nombre, fk.definicion);
    END LOOP;

    RETURN mover;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    t TEXT;
    cambiados INT;
BEGIN
    -- Primero las tablas a las que apuntan otras, despues las que no tienen quien las apunte.
    FOREACH t IN ARRAY ARRAY['usuarios', 'clientes', 'productos', 'ventas', 'compras', 'inventario', 'pagos',
                             'comprobantes', 'movimientos_inventario', 'alertas_inventario', 'imagenes_producto',
                             'detalle_venta', 'detalle_compra', 'auditorias']
    LOOP
        cambiados := pg_temp.renumerar(t);
        RAISE NOTICE '% : % filas cambiaron de ID', t, cambiados;
    END LOOP;
END $$;

-- Numero de comprobante correlativo por prefijo (REC-AAAA-00001...), en el orden de los IDs.
CREATE TEMP TABLE orden_comprobantes ON COMMIT DROP AS
SELECT id,
       left(numero_comprobante, length(numero_comprobante) - 5) AS prefijo,
       ROW_NUMBER() OVER (PARTITION BY left(numero_comprobante, length(numero_comprobante) - 5) ORDER BY id) AS orden
FROM comprobantes;

-- En dos pasos para que un numero nuevo no choque con uno viejo (el numero es unico).
UPDATE comprobantes SET numero_comprobante = 'TMP-' || id;
UPDATE comprobantes c
SET numero_comprobante = o.prefijo || lpad(o.orden::text, 5, '0')
FROM orden_comprobantes o
WHERE c.id = o.id;

-- ---------------------------------------------------------------------
-- 4. Contadores y secuencias
-- ---------------------------------------------------------------------
SELECT sincronizar_contadores_id();

DO $$
DECLARE
    t TEXT;
    secuencia TEXT;
    maximo BIGINT;
BEGIN
    FOR t IN SELECT c.table_name FROM information_schema.columns c
                  JOIN information_schema.tables x ON x.table_schema = c.table_schema AND x.table_name = c.table_name AND x.table_type = 'BASE TABLE'
                  WHERE c.table_schema = 'public' AND c.column_name = 'id' LOOP
        secuencia := pg_get_serial_sequence(format('public.%I', t), 'id');
        IF secuencia IS NOT NULL THEN
            EXECUTE format('SELECT COALESCE(MAX(id), 0) FROM %I', t) INTO maximo;
            IF maximo = 0 THEN
                PERFORM setval(secuencia, 1, false);
            ELSE
                PERFORM setval(secuencia, maximo, true);
            END IF;
        END IF;
    END LOOP;
END $$;

-- ---------------------------------------------------------------------
-- 5. Comprobar que no quedaron huecos
-- ---------------------------------------------------------------------
DO $$
DECLARE
    t TEXT;
    filas BIGINT;
    minimo BIGINT;
    maximo BIGINT;
BEGIN
    FOREACH t IN ARRAY ARRAY['usuarios', 'clientes', 'productos', 'ventas', 'compras', 'inventario', 'pagos',
                             'comprobantes', 'movimientos_inventario', 'alertas_inventario', 'imagenes_producto',
                             'detalle_venta', 'detalle_compra', 'auditorias', 'proveedores', 'categorias']
    LOOP
        EXECUTE format('SELECT COUNT(*), COALESCE(MIN(id), 0), COALESCE(MAX(id), 0) FROM %I', t) INTO filas, minimo, maximo;
        IF filas > 0 AND (minimo <> 1 OR maximo <> filas) THEN
            RAISE EXCEPTION 'La tabla % quedo con huecos (% filas, IDs de % a %)', t, filas, minimo, maximo;
        END IF;
    END LOOP;
    RAISE NOTICE 'Todas las tablas quedaron numeradas 1, 2, 3... sin huecos.';
END $$;

-- Resumen final.
SELECT 'usuarios' AS tabla, COUNT(*) AS filas, MAX(id) AS ultimo_id FROM usuarios
UNION ALL SELECT 'clientes', COUNT(*), MAX(id) FROM clientes
UNION ALL SELECT 'productos', COUNT(*), MAX(id) FROM productos
UNION ALL SELECT 'ventas', COUNT(*), MAX(id) FROM ventas
UNION ALL SELECT 'compras', COUNT(*), MAX(id) FROM compras
UNION ALL SELECT 'pagos', COUNT(*), MAX(id) FROM pagos
UNION ALL SELECT 'comprobantes', COUNT(*), MAX(id) FROM comprobantes
UNION ALL SELECT 'movimientos_inventario', COUNT(*), MAX(id) FROM movimientos_inventario
UNION ALL SELECT 'auditorias', COUNT(*), MAX(id) FROM auditorias;

COMMIT;
