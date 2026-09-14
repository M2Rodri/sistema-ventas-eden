-- =====================================================================
--  FASE 8 - ELIMINACION DE imagenes_producto.tipo_imagen
--  Proyecto : Sistema de Ventas - Muebleria Eden
--  Base     : muebleria_eden_db
--  Fecha    : 2026-08-21
-- =====================================================================
--
--  QUE HACE
--  --------
--  Elimina la columna imagenes_producto.tipo_imagen.
--
--  POR QUE
--  -------
--  Era el unico pendiente que quedo marcado durante el ajuste del
--  esquema, y se resolvio por eliminacion:
--
--    - No existe enum ni lista de valores validos para ella. Quedo fuera
--      de los CHECK de la fase de integridad justamente por eso.
--    - La entidad ImagenProducto del backend nunca la mapeo.
--    - Su unico valor cargado era 'normal', en minuscula.
--    - La funcion que aparentaba cumplir ya la cumple es_principal, que
--      distingue la imagen destacada del resto de la galeria, y 'orden',
--      que define la secuencia.
--
--  Mantener una columna sin valores definidos, sin uso y sin funcion
--  propia solo agrega ruido al esquema.
--
--  SEGURIDAD
--  ---------
--  Verificado: la tabla no tiene filas cargadas y ninguna entidad,
--  DTO ni consulta del backend referencia esta columna.
--  Dentro de una transaccion: si algo falla, no cambia nada.
--
--  IMPACTO EN BACKEND
--  ------------------
--  Ninguno. La entidad no la mapeaba.
--
-- =====================================================================

BEGIN;

ALTER TABLE imagenes_producto DROP COLUMN tipo_imagen;

COMMIT;

-- =====================================================================
--  FIN
--  El esquema queda sin pendientes marcados.
-- =====================================================================
