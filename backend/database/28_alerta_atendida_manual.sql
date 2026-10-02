-- =====================================================================
--  28 - Alerta de stock "atendida" por el dueño (ATENDIDA_MANUAL)
-- =====================================================================
--  QUE HACE
--  --------
--  El dueño puede marcar una alerta de stock bajo como atendida sin reponer
--  el producto. La alerta deja de aparecer y no vuelve a avisar mientras el
--  producto siga bajo el minimo; si se repone y vuelve a bajar, avisa de nuevo.
--
--  Agrega un valor nuevo al CHECK de alertas_inventario.estado:
--    PENDIENTE        hay que mirarla
--    ATENDIDA         se resolvio sola (el producto se repuso)
--    ATENDIDA_MANUAL  la marco el dueño sin reponer
--
--  DATOS EXISTENTES
--  ----------------
--  No se toca ninguna fila: las alertas que ya existen siguen igual.
--
--  EJECUCION
--  ---------
--  Dentro de una transaccion. Correr contra la base local y despues contra
--  produccion (ver README.md de esta carpeta).
-- =====================================================================

BEGIN;

ALTER TABLE alertas_inventario DROP CONSTRAINT IF EXISTS chk_alertas_inv_estado;
ALTER TABLE alertas_inventario
    ADD CONSTRAINT chk_alertas_inv_estado
    CHECK (estado IN ('PENDIENTE', 'ATENDIDA', 'ATENDIDA_MANUAL'));

COMMIT;
