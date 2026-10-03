-- =====================================================================
--  Carga inicial 01 - Categorias del negocio
-- =====================================================================
--  PREPARADO, NO EJECUTADO. Hay que correrlo a mano cuando se decida.
--
--  Crea las categorias que faltan: Camas, Colchones, Almohadas, Veladores,
--  Tocadores, Roperos y Zapateros. Las que ya existen (sin importar
--  mayusculas) no se tocan ni se duplican, asi que se puede correr mas de
--  una vez.
--
--  tipo_producto decide que campos pide el formulario de producto:
--    CAMA, COLCHON, ALMOHADA  los de siempre
--    MUEBLE                   Veladores, Tocadores, Roperos y Zapateros
--                             (piden medida, como las camas)
--
--  El SKU de cada producto sale de las tres primeras letras de su categoria:
--  CAM-001, COL-001, ALM-001, VEL-001, TOC-001, ROP-001, ZAP-001.
--
--  Requiere el script 31 (contadores de ID). Al terminar vuelve a alinear
--  el contador de categorias.
-- =====================================================================

BEGIN;

INSERT INTO categorias (id, nombre, descripcion, activo, tipo_producto, fecha_creacion, fecha_actualizacion)
SELECT (SELECT COALESCE(MAX(id), 0) FROM categorias) + ROW_NUMBER() OVER (ORDER BY v.orden),
       v.nombre, NULL, TRUE, v.tipo, NOW(), NOW()
FROM (VALUES
        (1, 'Camas',      'CAMA'),
        (2, 'Colchones',  'COLCHON'),
        (3, 'Almohadas',  'ALMOHADA'),
        (4, 'Veladores',  'MUEBLE'),
        (5, 'Tocadores',  'MUEBLE'),
        (6, 'Roperos',    'MUEBLE'),
        (7, 'Zapateros',  'MUEBLE')
     ) AS v(orden, nombre, tipo)
WHERE NOT EXISTS (SELECT 1 FROM categorias c WHERE LOWER(c.nombre) = LOWER(v.nombre));

SELECT sincronizar_contadores_id();

COMMIT;
