-- =============================================================================
-- 17. Pagos: sacar TARJETA y agregar comprobante
-- =============================================================================
--
-- POR QUÉ
--
-- El negocio no tiene terminal de pago, así que TARJETA nunca fue un método
-- real de cobro: se saca del catálogo de métodos.
--
-- Además, para QR y transferencia no había forma de guardar la foto del
-- comprobante. Ahora es opcional (nunca bloquea el registro del pago), pero
-- si falta, el pago queda "sin respaldo" para que el propietario lo vea y
-- pueda pedir la foto después.
--
-- QUE HACE
--
-- A) Achica el CHECK de pagos.metodo_pago a ('EFECTIVO', 'TRANSFERENCIA', 'QR').
-- B) Agrega pagos.url_comprobante.
--
-- IMPACTO EN BACKEND
--
--   models/MetodoPago.java     -> se quita TARJETA
--   models/Pago.java           -> campo urlComprobante
--   dto/PagoDTO.java           -> urlComprobante, sinRespaldo (calculado)
--   dto/VentaResponse.java     -> tienePagosSinRespaldo (calculado)
--   services/PagoService.java  -> adjuntarComprobante(idPago, file)
--   services/VentaService.java -> ya no exige referenciaPago para métodos
--                                  no efectivo (queda opcional siempre)
--
-- SEGURIDAD
--
-- Si ya existiera algún pago guardado con metodo_pago = 'TARJETA', este
-- ALTER falla (Postgres no deja agregar un CHECK que una fila existente
-- viola). Es a propósito: no se reinterpreta ese dato solo. Si pasa, hay que
-- decidir a mano qué hacer con esas filas antes de correr el script.
--
-- PENDIENTE (no se resuelve acá)
--
-- El comprobante se guarda en disco local (uploads/comprobantes-pago/,
-- mismo mecanismo que las imágenes de producto). En Railway el filesystem
-- no persiste entre despliegues: un redeploy borra los comprobantes ya
-- subidos. Falta moverlo a almacenamiento externo antes de depender de esto
-- en producción.
-- =============================================================================

BEGIN;

ALTER TABLE pagos
    DROP CONSTRAINT IF EXISTS chk_pagos_metodo_pago,
    ADD CONSTRAINT chk_pagos_metodo_pago
        CHECK (metodo_pago IN ('EFECTIVO', 'TRANSFERENCIA', 'QR'));

ALTER TABLE pagos
    ADD COLUMN IF NOT EXISTS url_comprobante VARCHAR(500);

COMMIT;
