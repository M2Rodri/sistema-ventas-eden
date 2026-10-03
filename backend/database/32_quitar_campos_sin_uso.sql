-- =====================================================================
--  32 - Quitar cuatro campos que no hacen nada
-- =====================================================================
--  QUE HACE
--  --------
--  Elimina columnas que el sistema ya no usa:
--    ventas.requiere_envio             nada la usaba; los envios son otro modulo
--    clientes.tipo_cliente             siempre valia REGISTRADO, no distinguia nada
--    comprobantes.motivo_anulacion     la anulacion de comprobantes ya no existe
--    comprobantes.fecha_anulacion      idem
--
--  Al borrar tipo_cliente se va tambien su CHECK (REGISTRADO / INVITADO).
--
--  DATOS EXISTENTES
--  ----------------
--  Se pierde el contenido de esas columnas, que no tiene ningun valor.
--
--  ORDEN
--  -----
--  Es seguro correrlo antes o despues de publicar el backend: las columnas
--  eran opcionales y el backend nuevo ya no las lee ni las escribe.
-- =====================================================================

BEGIN;

ALTER TABLE ventas       DROP COLUMN IF EXISTS requiere_envio;
ALTER TABLE clientes     DROP COLUMN IF EXISTS tipo_cliente;
ALTER TABLE comprobantes DROP COLUMN IF EXISTS motivo_anulacion;
ALTER TABLE comprobantes DROP COLUMN IF EXISTS fecha_anulacion;

COMMIT;
