-- 34: renumerar los usuarios existentes para que los IDs queden 1, 2, 3... sin huecos.
--
-- Por que hace falta: el generador de IDs (script 31) solo evita huecos en los
-- usuarios NUEVOS. Los huecos que ya existian (por ejemplo 1, 2, 6, 9, 10, 11)
-- siguen ahi. Este script los renumera por orden de ID actual: el que tiene el
-- ID mas bajo queda con 1, el siguiente con 2, y asi.
--
-- Que hace, todo en una sola transaccion (si algo falla, no cambia nada):
--  1. Se detiene si encuentra una columna id_usuario* que no tenga clave foranea
--     hacia usuarios (no se podria saber si hay que cambiarla).
--  2. Quita temporalmente las claves foraneas que apuntan a usuarios.
--  3. Cambia el ID de cada usuario y, en todas las tablas que lo usan (ventas,
--     pagos, compras, movimientos de inventario, auditorias, envios, mensajes
--     de contacto), el numero de usuario correspondiente.
--  4. Vuelve a crear las claves foraneas (la base revisa que todo siga cuadrando).
--  5. Deja el contador de usuarios en el ultimo ID.
--
-- Que NO hace: no cambia nombres, claves, roles ni estados de nadie.
--
-- Despues de correrlo, quien tenga la sesion abierta debe volver a iniciarla.
-- Se puede correr mas de una vez: si ya no hay huecos, no cambia nada.
--
-- Antes de correrlo conviene decidir que hacer con los usuarios de prueba
-- (evaluador_e2, jsanchez), para no renumerar usuarios que despues se borran.

BEGIN;

CREATE TEMP TABLE mapa_usuarios ON COMMIT DROP AS
SELECT id AS viejo, ROW_NUMBER() OVER (ORDER BY id) AS nuevo
FROM usuarios;

DO $$
DECLARE
    fk RECORD;
    sin_fk TEXT;
    compuestas TEXT;
    mover INT;
    secuencia TEXT;
BEGIN
    SELECT COUNT(*) INTO mover FROM mapa_usuarios WHERE viejo <> nuevo;
    IF mover = 0 THEN
        RAISE NOTICE 'Los usuarios ya estan numerados sin huecos: no se cambia nada.';
        RETURN;
    END IF;

    -- 1a. Claves foraneas de mas de una columna: no se esperan, se prefiere parar.
    SELECT string_agg(k.conrelid::regclass::text || '.' || k.conname, ', ') INTO compuestas
    FROM pg_constraint k
    WHERE k.contype = 'f' AND k.confrelid = 'public.usuarios'::regclass AND array_length(k.conkey, 1) <> 1;
    IF compuestas IS NOT NULL THEN
        RAISE EXCEPTION 'Clave foranea de varias columnas hacia usuarios: %', compuestas;
    END IF;

    -- 1b. Columnas id_usuario* sin clave foranea hacia usuarios.
    SELECT string_agg(c.table_name || '.' || c.column_name, ', ') INTO sin_fk
    FROM information_schema.columns c
    JOIN information_schema.tables t
      ON t.table_schema = c.table_schema AND t.table_name = c.table_name AND t.table_type = 'BASE TABLE'
    WHERE c.table_schema = 'public' AND c.column_name LIKE 'id\_usuario%'
      AND NOT EXISTS (
          SELECT 1
          FROM pg_constraint k
          JOIN pg_attribute a ON a.attrelid = k.conrelid AND a.attnum = k.conkey[1]
          WHERE k.contype = 'f' AND k.confrelid = 'public.usuarios'::regclass
            AND k.conrelid = format('public.%I', c.table_name)::regclass
            AND a.attname = c.column_name);
    IF sin_fk IS NOT NULL THEN
        RAISE EXCEPTION 'Columnas de usuario sin clave foranea (revisar a mano antes de renumerar): %', sin_fk;
    END IF;

    -- 2. Guardar y quitar las claves foraneas hacia usuarios.
    CREATE TEMP TABLE fks_usuarios ON COMMIT DROP AS
    SELECT k.conrelid::regclass::text AS tabla, k.conname AS nombre,
           pg_get_constraintdef(k.oid) AS definicion, a.attname AS columna
    FROM pg_constraint k
    JOIN pg_attribute a ON a.attrelid = k.conrelid AND a.attnum = k.conkey[1]
    WHERE k.contype = 'f' AND k.confrelid = 'public.usuarios'::regclass;

    FOR fk IN SELECT * FROM fks_usuarios LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', fk.tabla, fk.nombre);
    END LOOP;

    -- 3. Cambiar el usuario en cada tabla que lo usa y despues el ID del usuario.
    FOR fk IN SELECT * FROM fks_usuarios LOOP
        EXECUTE format('UPDATE %s t SET %I = m.nuevo FROM mapa_usuarios m WHERE t.%I = m.viejo',
                       fk.tabla, fk.columna, fk.columna);
    END LOOP;

    -- En dos pasos para que un ID nuevo no choque con uno viejo que aun no se movio.
    UPDATE usuarios SET id = id + 1000000;
    UPDATE usuarios u SET id = m.nuevo FROM mapa_usuarios m WHERE u.id = m.viejo + 1000000;

    -- 4. Volver a crear las claves foraneas.
    FOR fk IN SELECT * FROM fks_usuarios LOOP
        EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s', fk.tabla, fk.nombre, fk.definicion);
    END LOOP;

    -- 5. Contador de IDs (el del backend y, si existe, la secuencia de PostgreSQL).
    UPDATE contadores_id SET ultimo = (SELECT COALESCE(MAX(id), 0) FROM usuarios) WHERE tabla = 'usuarios';
    secuencia := pg_get_serial_sequence('public.usuarios', 'id');
    IF secuencia IS NOT NULL THEN
        PERFORM setval(secuencia, (SELECT COALESCE(MAX(id), 1) FROM usuarios), true);
    END IF;

    RAISE NOTICE 'Usuarios renumerados: % cambiaron de ID.', mover;
END $$;

-- Resultado: cada usuario con su ID anterior y el nuevo.
SELECT m.viejo AS id_anterior, u.id AS id_nuevo, u.usuario, u.activo
FROM usuarios u
LEFT JOIN mapa_usuarios m ON m.nuevo = u.id
ORDER BY u.id;

COMMIT;
