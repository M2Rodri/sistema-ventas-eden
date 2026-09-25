-- =====================================================================
-- 22) LIMPIEZA DE clientes
-- =====================================================================
-- A) direcciones_cliente: tabla creada para que un cliente tenga varias
--    direcciones con una marcada "principal", pero nunca se llegó a
--    escribir el modelo/servicio/pantalla que la use. Vacía (0 filas),
--    sin ningún código que la referencie. Infraestructura fantasma.
--
-- B) tipo_cliente DEFAULT 'INVITADO': quedó así de cuando existía la
--    distinción Cliente Rápido/INVITADO. Esa distinción se sacó (decisión
--    del negocio, ver Cliente.java) y todo cliente nuevo se crea explícito
--    en REGISTRADO desde la aplicación, así que el default de la columna
--    nunca se usa en la práctica — pero si algún día alguien inserta sin
--    pasar por la app, quedaría con el valor viejo. Se corrige para que
--    coincida con la decisión ya tomada.
-- =====================================================================

BEGIN;

DROP TABLE IF EXISTS direcciones_cliente;

ALTER TABLE clientes ALTER COLUMN tipo_cliente SET DEFAULT 'REGISTRADO';

COMMIT;
