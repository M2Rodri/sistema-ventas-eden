-- =====================================================================
--  36 - Cortar las sesiones de un usuario ("cerrar todas las sesiones")
-- =====================================================================
--  QUE HACE
--  --------
--  Agrega usuarios.sesiones_validas_desde (BIGINT, opcional): un instante,
--  en milisegundos desde 1970 (hora UTC), a partir del cual valen las
--  sesiones del usuario. Todo token cuya sesión empezó ANTES de ese instante
--  deja de servir: el backend lo rechaza con 401 y no se puede renovar.
--
--  POR QUE
--  -------
--  Hasta ahora un token ya emitido valía hasta vencer, aunque el usuario
--  cambiara su contraseña o le robaran el celular. Con esta columna el dueño
--  puede cortar de inmediato todas las sesiones abiertas (botón "Cerrar
--  sesiones" en Usuarios), y cambiar la contraseña de un usuario también las
--  corta.
--
--  POR QUE UN NUMERO Y NO UN TIMESTAMP
--  -----------------------------------
--  El token guarda el inicio de su sesión en milisegundos. Guardar el corte
--  en el mismo formato evita cualquier problema de zonas horarias entre la
--  base, el servidor y el token.
--
--  DATOS EXISTENTES
--  ----------------
--  Todos los usuarios quedan con el valor vacío (NULL): significa "sin corte",
--  y sus sesiones siguen valiendo con normalidad.
--
--  ORDEN
--  -----
--  Correrlo ANTES de publicar el backend nuevo: la entidad Usuario ya lee
--  esta columna y, con ddl-auto en validate, el backend no arrancaría si no
--  existe. Es seguro correrlo antes: el backend viejo simplemente la ignora.
-- =====================================================================

BEGIN;

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS sesiones_validas_desde BIGINT;

COMMENT ON COLUMN usuarios.sesiones_validas_desde IS
    'Instante (milisegundos desde 1970, UTC) desde el que valen las sesiones del usuario. Los tokens de sesiones anteriores se rechazan. NULL = sin corte.';

COMMIT;
