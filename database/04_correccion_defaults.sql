-- =====================================================================
--  FASE 4 - CORRECCION DE VALORES POR OMISION
--  Proyecto : Sistema de Ventas - Muebleria Eden
--  Base     : muebleria_eden_db
--  Fecha    : 2026-08-20
-- =====================================================================
--
--  QUE CORRIGE
--  -----------
--  El script 02_integridad_indices.sql agrego CHECK constraints con los
--  valores de los enums del backend (en MAYUSCULA), pero no reviso los
--  DEFAULT que ya tenian esas columnas, que estaban en minuscula.
--
--  Como las tablas estaban vacias, los CHECK se crearon sin error. Pero
--  el primer INSERT que omitiera cualquiera de estas columnas habria
--  tomado el DEFAULT en minuscula y violado el CHECK:
--
--    ventas.estado                  'pendiente'   -> viola chk_ventas_estado
--    pagos.estado                   'completado'  -> viola chk_pagos_estado
--    envios.estado_seguimiento      'pendiente'   -> viola chk_envios_estado_seguimiento
--    alertas_inventario.estado      'pendiente'   -> viola chk_alertas_inv_estado
--    comprobantes.tipo_comprobante  'recibo'      -> viola chk_comprobantes_tipo
--    clientes.tipo                  'regular'     -> viola chk_clientes_tipo
--
--  El caso de clientes.tipo es el peor: 'regular' no existe en el enum
--  TipoCliente (REGISTRADO / INVITADO), o sea que el valor por omision
--  nunca fue valido, ni antes ni ahora.
--
--  CRITERIO
--  --------
--  Cada DEFAULT se alinea con el valor por omision que ya usa la
--  entidad correspondiente del backend, para que base y aplicacion
--  coincidan:
--
--    Venta.estado    = EstadoVenta.PENDIENTE_PAGO
--    Envio.estado    = EstadoEnvio.PENDIENTE
--    Cliente.tipo    = TipoCliente.INVITADO
--
--  IMPACTO EN BACKEND
--  ------------------
--  Ninguno. Solo cambia el valor que la base asume cuando el INSERT
--  omite la columna.
--
-- =====================================================================

BEGIN;

-- ventas.estado : 'pendiente' -> 'PENDIENTE_PAGO'
-- (el enum EstadoVenta no tiene un simple PENDIENTE; una venta nueva
--  nace pendiente de pago)
ALTER TABLE ventas
    ALTER COLUMN estado SET DEFAULT 'PENDIENTE_PAGO';

-- pagos.estado : 'completado' -> 'COMPLETADO'
ALTER TABLE pagos
    ALTER COLUMN estado SET DEFAULT 'COMPLETADO';

-- envios.estado_seguimiento : 'pendiente' -> 'PENDIENTE'
ALTER TABLE envios
    ALTER COLUMN estado_seguimiento SET DEFAULT 'PENDIENTE';

-- alertas_inventario.estado : 'pendiente' -> 'PENDIENTE'
ALTER TABLE alertas_inventario
    ALTER COLUMN estado SET DEFAULT 'PENDIENTE';

-- comprobantes.tipo_comprobante : 'recibo' -> 'RECIBO'
ALTER TABLE comprobantes
    ALTER COLUMN tipo_comprobante SET DEFAULT 'RECIBO';

-- clientes.tipo : 'regular' -> 'INVITADO'
-- Un cliente creado sin especificar tipo es una venta de mostrador.
-- Pasa a REGISTRADO cuando se le cargan los datos completos.
ALTER TABLE clientes
    ALTER COLUMN tipo SET DEFAULT 'INVITADO';

COMMIT;

-- =====================================================================
--  FIN
--  Siguiente: 05_compras.sql
-- =====================================================================
