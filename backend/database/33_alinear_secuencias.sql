-- 33: alinear los contadores de ID (secuencias) con los datos que ya hay.
--
-- El script de datos de demostración inserta filas con ID fijo (1, 2, 3...) y
-- no mueve el contador de cada tabla. La próxima venta nueva recibe entonces
-- el ID 1, que ya existe, y la base la rechaza: la API responde 409
-- CONFLICTO_DATOS ("La operación choca con datos que ya existen o están en uso").
--
-- Por cada tabla de public con columna id y contador propio, muestra (NOTICE)
-- el mayor ID, el siguiente que daría el contador y si hacía falta moverlo.
-- Solo mueve los contadores que chocarían con un ID ya usado, y los deja en el
-- mayor ID de la tabla. No cambia ni borra ningún dato. Se puede correr varias veces.

DO $$
DECLARE
    fila record;
    secuencia text;
    mayor bigint;
    ultimo bigint;
    usado boolean;
    siguiente bigint;
BEGIN
    FOR fila IN
        SELECT t.table_name AS tabla
        FROM information_schema.tables t
        WHERE t.table_schema = 'public' AND t.table_type = 'BASE TABLE'
          AND EXISTS (SELECT 1 FROM information_schema.columns c
                      WHERE c.table_schema = 'public' AND c.table_name = t.table_name AND c.column_name = 'id')
        ORDER BY t.table_name
    LOOP
        secuencia := pg_get_serial_sequence(format('public.%I', fila.tabla), 'id');
        CONTINUE WHEN secuencia IS NULL;

        EXECUTE format('SELECT COALESCE(MAX(id), 0) FROM public.%I', fila.tabla) INTO mayor;
        EXECUTE format('SELECT last_value, is_called FROM %s', secuencia) INTO ultimo, usado;
        siguiente := CASE WHEN usado THEN ultimo + 1 ELSE ultimo END;

        IF siguiente <= mayor THEN
            PERFORM setval(secuencia, mayor, true);
            RAISE NOTICE '% -> DESALINEADA (mayor id %, siguiente era %): movida a %', fila.tabla, mayor, siguiente, mayor + 1;
        ELSE
            RAISE NOTICE '% -> ok (mayor id %, siguiente %)', fila.tabla, mayor, siguiente;
        END IF;
    END LOOP;
END $$;
