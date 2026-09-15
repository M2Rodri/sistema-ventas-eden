-- =============================================================================
-- 15. Renombrar las claves empresa_* a negocio_*
-- =============================================================================
--
-- POR QUÉ
--
-- Las claves de configuración se llamaban empresa_telefono, empresa_direccion,
-- etc. El nombre de la clave se ve tal cual en la pantalla de Configuración, y
-- "empresa" no describe lo que es: una mueblería familiar.
--
-- El código del backend y del frontend ya usa negocio_*, así que sin este
-- cambio la tienda no encuentra los datos y las páginas de Contacto y Nosotros
-- salen vacías.
--
-- DÓNDE APLICARLO
--
-- En Supabase ya se resolvió con el script 14, que insertó las filas
-- directamente con el prefijo nuevo porque la tabla estaba vacía.
--
-- Este script es para la base local, que sí conserva las filas originales con
-- el prefijo viejo. Es idempotente: si se ejecuta dos veces, la segunda no
-- hace nada.
-- =============================================================================

BEGIN;

-- Solo renombra si la clave nueva no existe ya, para no chocar con la
-- restricción de unicidad si alguien mezcló los dos prefijos.
UPDATE configuracion_sistema c
SET clave = 'negocio_' || substring(c.clave from 9),
    fecha_actualizacion = NOW()
WHERE c.clave LIKE 'empresa\_%'
  AND NOT EXISTS (
      SELECT 1 FROM configuracion_sistema x
      WHERE x.clave = 'negocio_' || substring(c.clave from 9)
  );

-- La clave del nombre además cambia de forma: el código lee
-- negocio_razon_social, no negocio_nombre.
UPDATE configuracion_sistema
SET clave = 'negocio_razon_social', fecha_actualizacion = NOW()
WHERE clave = 'negocio_nombre'
  AND NOT EXISTS (SELECT 1 FROM configuracion_sistema WHERE clave = 'negocio_razon_social');

COMMIT;

-- Verificación: no debe quedar ninguna clave empresa_*
SELECT clave, valor FROM configuracion_sistema ORDER BY clave;
