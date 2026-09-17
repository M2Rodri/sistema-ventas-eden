-- =============================================================================
-- 18. Pagos: agrega el usuario que registró el cobro
-- =============================================================================
--
-- POR QUÉ
--
-- Venta, Compra y movimientos_inventario ya guardan quién hizo la operación.
-- Pago era la excepción: no había forma de saber quién cobró un pago
-- (incluidos los pagos a cuenta registrados aparte de la venta inicial).
--
-- Esto va junto con un cambio de backend: el usuario ya no lo va a mandar
-- el cliente (idUsuario por parámetro), sino que se saca del JWT en el
-- servidor. Ver services/UsuarioActualService.java.
--
-- QUE HACE
--
-- Agrega pagos.id_usuario, nullable porque los pagos ya existentes no tienen
-- ese dato y no se puede reconstruir.
--
-- IMPACTO EN BACKEND
--
--   models/Pago.java          -> campo usuario (ManyToOne, nullable)
--   dto/PagoDTO.java          -> idUsuario, nombreUsuario
--   services/PagoService.java -> setUsuario(usuarioActualService.obtenerRequerido())
-- =============================================================================

BEGIN;

ALTER TABLE pagos
    ADD COLUMN IF NOT EXISTS id_usuario BIGINT NULL;

ALTER TABLE pagos
    ADD CONSTRAINT fk_pagos_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios (id);

CREATE INDEX IF NOT EXISTS idx_pagos_usuario ON pagos (id_usuario);

COMMIT;
