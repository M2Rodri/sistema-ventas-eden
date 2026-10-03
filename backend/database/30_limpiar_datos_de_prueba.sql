-- =====================================================================
--  30 - Limpiar los datos de prueba y reiniciar la numeracion en 1
-- =====================================================================
--  QUE HACE
--  --------
--  Borra TODO el contenido de las tablas de datos (productos, inventario,
--  clientes, proveedores, compras, ventas, pagos, comprobantes, movimientos,
--  alertas, auditoria, imagenes...) y reinicia sus numeros de ID en 1, para
--  que el sistema arranque limpio y la numeracion sea 1, 2, 3... sin huecos.
--
--  NO toca: usuarios, roles, categorias ni configuracion_sistema.
--
--  PELIGRO: BORRA DATOS DE FORMA IRREVERSIBLE. Solo para cuando todo lo que hay
--  son datos de prueba. Hacer una copia de seguridad antes (pg_dump).
--  Por eso exige una confirmacion explicita:
--
--     PGOPTIONS="-c eden.confirmar_limpieza=si" psql ... -f 30_limpiar_datos_de_prueba.sql
--
--  Tampoco borra los archivos subidos (fotos de productos, comprobantes de
--  pago): quedan huerfanos en el almacenamiento y se borran aparte.
--
--  DESPUES
--  -------
--  Cargar los productos reales con el script de carga inicial.
-- =====================================================================

BEGIN;

DO $$
BEGIN
    IF coalesce(current_setting('eden.confirmar_limpieza', true), '') <> 'si' THEN
        RAISE EXCEPTION 'Este script BORRA los datos. Para correrlo: PGOPTIONS="-c eden.confirmar_limpieza=si"';
    END IF;
END $$;

TRUNCATE TABLE
    alertas_inventario,
    auditorias,
    clientes,
    compras,
    comprobantes,
    detalle_compra,
    detalle_venta,
    envios,
    imagenes_producto,
    inventario,
    mensajes_contacto,
    movimientos_inventario,
    multimedia_producto,
    pagos,
    productos,
    productos_proveedor,
    promociones,
    promociones_producto,
    proveedores,
    transportadoras,
    ventas
RESTART IDENTITY;

-- Las tablas que se conservan siguen su numeracion desde su ultimo ID.
SELECT setval(pg_get_serial_sequence('usuarios', 'id'), (SELECT COALESCE(MAX(id), 1) FROM usuarios));
SELECT setval(pg_get_serial_sequence('categorias', 'id'), (SELECT COALESCE(MAX(id), 1) FROM categorias));

-- Los contadores de IDs (script 31) vuelven a 0 en las tablas vaciadas.
DO $
BEGIN
    IF to_regproc('sincronizar_contadores_id') IS NOT NULL THEN
        PERFORM sincronizar_contadores_id();
    END IF;
END $;

COMMIT;
