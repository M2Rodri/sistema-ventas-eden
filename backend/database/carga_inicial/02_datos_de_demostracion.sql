-- =====================================================================
--  Carga inicial 02 - Datos de demostracion
-- =====================================================================
--  Llena el sistema con datos de ejemplo para probarlo y mostrarlo:
--    2 usuarios de demostracion (no pueden iniciar sesion)
--    3 proveedores
--    12 productos de las 7 categorias, con su inventario
--    4 clientes
--    4 compras (una sin proveedor) que dejan el stock
--    3 ventas con sus pagos y comprobantes (una con saldo pendiente y descuento)
--    movimientos de inventario de todo lo anterior
--    1 alerta de stock bajo
--
--  Todos los nombres son inventados. El stock de cada producto es exactamente lo
--  comprado menos lo vendido; al final el script lo verifica y, si algo no
--  cuadra, se cancela todo sin dejar nada.
--
--  REQUISITOS
--  ----------
--    * Script 31 (contadores de ID) y carga_inicial/01_categorias.sql ya corridos.
--    * Las tablas de datos vacias (productos, proveedores, clientes, compras,
--      ventas). Si no lo estan, el script se detiene y no toca nada.
--
--  USUARIOS DE DEMOSTRACION
--  ------------------------
--  Lucia Fernandez (vendedora) y Marco Soliz (administracion) existen solo para
--  que ventas, compras y movimientos tengan un responsable. Quedan INACTIVOS y con
--  una clave que no sirve, asi que nadie puede entrar con ellos. Para quitarlos,
--  despues de limpiar los datos con el script 30:
--      DELETE FROM usuarios WHERE usuario IN ('lfernandez', 'msoliz');
--
--  Para volver a empezar: script 30 (borra todo) y este de nuevo.
-- =====================================================================

BEGIN;

DO $$
BEGIN
    IF to_regclass('contadores_id') IS NULL THEN
        RAISE EXCEPTION 'Falta el script 31 (contadores de ID).';
    END IF;
    IF (SELECT COUNT(*) FROM categorias WHERE nombre IN
            ('Camas','Colchones','Almohadas','Veladores','Tocadores','Roperos','Zapateros')) < 7 THEN
        RAISE EXCEPTION 'Faltan categorias: correr carga_inicial/01_categorias.sql primero.';
    END IF;
    IF EXISTS (SELECT 1 FROM productos) OR EXISTS (SELECT 1 FROM proveedores) OR EXISTS (SELECT 1 FROM clientes)
       OR EXISTS (SELECT 1 FROM compras) OR EXISTS (SELECT 1 FROM ventas) THEN
        RAISE EXCEPTION 'Las tablas de datos no estan vacias: este script solo corre sobre una base limpia (script 30).';
    END IF;
    IF EXISTS (SELECT 1 FROM usuarios WHERE usuario IN ('lfernandez', 'msoliz')) THEN
        RAISE EXCEPTION 'Los usuarios de demostracion ya existen.';
    END IF;
END $$;

-- ---------------------------------------------------------------------
--  Usuarios de demostracion (inactivos, sin acceso)
-- ---------------------------------------------------------------------
INSERT INTO usuarios (id, nombre, apellido, usuario, password, telefono, direccion, id_rol, activo, fecha_creacion, fecha_actualizacion)
VALUES
    ((SELECT COALESCE(MAX(id), 0) + 1 FROM usuarios), 'Lucía', 'Fernández Arce', 'lfernandez', '!sin-acceso', NULL, NULL,
        (SELECT id FROM roles WHERE nombre = 'EMPLEADO'), FALSE, '2026-09-14 09:00', '2026-09-14 09:00'),
    ((SELECT COALESCE(MAX(id), 0) + 2 FROM usuarios), 'Marco', 'Soliz Vaca', 'msoliz', '!sin-acceso', NULL, NULL,
        (SELECT id FROM roles WHERE nombre = 'ADMIN'), FALSE, '2026-09-14 09:00', '2026-09-14 09:00');

-- ---------------------------------------------------------------------
--  Proveedores
-- ---------------------------------------------------------------------
INSERT INTO proveedores (id, nombre_empresa, nit, contacto, telefono, email, direccion, notas, activo, fecha_registro, fecha_actualizacion) VALUES
    (1, 'Maderas del Oriente S.R.L.', '1028374026', 'Ricardo Egüez Parada', '33465120', 'ventas@maderasdeloriente.example.com',
        'Av. Cristo Redentor, 4to anillo, Santa Cruz', 'Camas, roperos y muebles de dormitorio', TRUE, '2026-09-10 10:00', '2026-09-10 10:00'),
    (2, 'Espumas y Colchones Cruceños Ltda.', '1015263047', 'Nelly Barba Justiniano', '70845512', 'pedidos@espumascruceños.example.com',
        'Parque Industrial, Mz. 12, Santa Cruz', 'Colchones de espuma, resortes y viscoelásticos', TRUE, '2026-09-10 10:30', '2026-09-10 10:30'),
    (3, 'Textiles y Almohadas del Sur', '1039487021', 'Gonzalo Peredo Rojas', '69034781', NULL,
        'Calle Libertad #480, Santa Cruz', 'Almohadas y ropa de cama', TRUE, '2026-09-11 11:00', '2026-09-11 11:00');

-- ---------------------------------------------------------------------
--  Productos (12). precio_compra = costo de la ultima compra.
-- ---------------------------------------------------------------------
INSERT INTO productos (id, sku, nombre, descripcion, marca, modelo, dimensiones, firmeza, material_nucleo, material_armazon, color, calidad,
                       tipo_producto, precio_compra, precio_venta, stock_minimo, id_categoria, activo, fecha_creacion, fecha_actualizacion)
SELECT v.id, v.sku, v.nombre, NULL, NULL, NULL, v.dimensiones, v.firmeza, v.nucleo, v.armazon, v.color, v.calidad,
       v.tipo, v.compra, v.venta, v.minimo, (SELECT c.id FROM categorias c WHERE c.nombre = v.categoria), TRUE,
       v.creado::timestamp, v.creado::timestamp
FROM (VALUES
    (1,  'CAM-001', 'Cama Matrimonial Roble',                 'Queen Size (160x200 cm)',          NULL,    NULL,                  'Madera tajibo', 'Roble',  'Estándar',  'CAMA',     'Camas',     2400.00, 3200.00, 2, '2026-09-15 10:00'),
    (2,  'CAM-002', 'Cama 1 Plaza y Media Cedro',             '1 Plaza y Media (105x190 cm)',     NULL,    NULL,                  'Madera',        'Cedro',  'Estándar',  'CAMA',     'Camas',     1300.00, 1800.00, 2, '2026-09-15 10:00'),
    (3,  'COL-001', 'Colchón Espuma Alta Densidad 2 Plazas',  '2 Plazas (140x190 cm)',            'Firme', 'Espuma alta densidad', NULL,            'Blanco', 'Estándar',  'COLCHON',  'Colchones',  900.00, 1290.00, 3, '2026-09-17 15:30'),
    (4,  'COL-002', 'Colchón Resortado Queen',                'Queen Size (160x200 cm)',          'Medio', 'Resortes ensacados',  NULL,            'Blanco', 'Premium',   'COLCHON',  'Colchones', 1900.00, 2650.00, 2, '2026-09-17 15:30'),
    (5,  'COL-003', 'Colchón Viscoelástico King',             'King Size (180x200 cm)',           'Medio', 'Viscoelástica',       NULL,            'Gris',   'Alta gama', 'COLCHON',  'Colchones', 2700.00, 3690.00, 2, '2026-09-17 15:30'),
    (6,  'ALM-001', 'Almohada de Fibra Siliconada',           NULL,                               NULL,    NULL,                  NULL,            'Blanco', 'Estándar',  'ALMOHADA', 'Almohadas',   80.00,  135.00, 5, '2026-09-24 09:15'),
    (7,  'ALM-002', 'Almohada Viscoelástica Cervical',        NULL,                               NULL,    NULL,                  NULL,            'Blanco', 'Premium',   'ALMOHADA', 'Almohadas',  150.00,  245.00, 5, '2026-09-24 09:15'),
    (8,  'ROP-001', 'Ropero 3 Puertas Melamina',              'Medida especial (150x200x55 cm)',  NULL,    NULL,                  NULL,            'Blanco', 'Estándar',  'MUEBLE',   'Roperos',   2000.00, 2800.00, 1, '2026-09-15 10:00'),
    (9,  'ROP-002', 'Ropero 2 Puertas con Espejo',            'Medida especial (110x200x50 cm)',  NULL,    NULL,                  NULL,            'Roble',  'Estándar',  'MUEBLE',   'Roperos',   1700.00, 2350.00, 1, '2026-09-15 10:00'),
    (10, 'VEL-001', 'Velador 2 Cajones',                      'Medida especial (45x55x40 cm)',    NULL,    NULL,                  NULL,            'Roble',  'Estándar',  'MUEBLE',   'Veladores',  300.00,  450.00, 2, '2026-09-15 10:00'),
    (11, 'TOC-001', 'Tocador con Espejo y Banqueta',          'Medida especial (100x150x45 cm)',  NULL,    NULL,                  NULL,            'Blanco', 'Estándar',  'MUEBLE',   'Tocadores', 1050.00, 1500.00, 1, '2026-09-15 10:00'),
    (12, 'ZAP-001', 'Zapatero 5 Niveles',                     'Medida especial (60x100x30 cm)',   NULL,    NULL,                  NULL,            'Café',   'Estándar',  'MUEBLE',   'Zapateros',  420.00,  650.00, 2, '2026-09-15 10:00')
) AS v(id, sku, nombre, dimensiones, firmeza, nucleo, armazon, color, calidad, tipo, categoria, compra, venta, minimo, creado);

-- ---------------------------------------------------------------------
--  Clientes
-- ---------------------------------------------------------------------
INSERT INTO clientes (id, nombre, apellido, nit_ci, telefono, email, activo, fecha_registro, fecha_actualizacion) VALUES
    (1, 'Marcela',  'Rojas Villarroel',      '4523198',    '70312845', 'marcela.rojas@example.com',  TRUE, '2026-09-25 11:20', '2026-09-25 11:20'),
    (2, 'Carlos Eduardo', 'Pinto Salvatierra', '5896127',  '76498120', NULL,                          TRUE, '2026-09-26 16:05', '2026-09-26 16:05'),
    (3, 'Silvia',   'Montaño Aguilera',      '6741205',    '62157834', NULL,                          TRUE, '2026-10-01 10:40', '2026-10-01 10:40'),
    (4, 'Hogar y Descanso El Cedro S.R.L.', NULL, '1029384756', '33891245', 'compras@elcedro.example.com', TRUE, '2026-09-28 09:00', '2026-09-28 09:00');

-- ---------------------------------------------------------------------
--  Compras (entran al inventario al registrarse)
-- ---------------------------------------------------------------------
INSERT INTO compras (id, id_proveedor, id_usuario, numero_factura, fecha_compra, subtotal, descuento, monto_total, estado, notas, fecha_actualizacion) VALUES
    (1, 1, (SELECT id FROM usuarios WHERE usuario = 'msoliz'), 'F-2026-0412', '2026-09-15 10:00', 28480.00, 0, 28480.00, 'CONFIRMADA', 'Reposición de camas y muebles', '2026-09-15 10:00'),
    (2, 2, (SELECT id FROM usuarios WHERE usuario = 'msoliz'), 'A-00231',     '2026-09-17 15:30', 21100.00, 0, 21100.00, 'CONFIRMADA', NULL,                            '2026-09-17 15:30'),
    (3, 3, (SELECT id FROM usuarios WHERE usuario = 'msoliz'), 'T-1190',      '2026-09-24 09:15',  1860.00, 0,  1860.00, 'CONFIRMADA', NULL,                            '2026-09-24 09:15'),
    (4, NULL, (SELECT id FROM usuarios WHERE usuario = 'msoliz'), NULL,       '2026-09-28 09:00',  1320.00, 0,  1320.00, 'CONFIRMADA', 'Compra de reposición sin proveedor', '2026-09-28 09:00');

INSERT INTO detalle_compra (id, id_compra, id_producto, cantidad, precio_unitario, subtotal)
SELECT ROW_NUMBER() OVER (ORDER BY v.compra, v.sku), v.compra, p.id, v.cantidad, v.precio, v.cantidad * v.precio
FROM (VALUES
    (1, 'CAM-001', 4, 2400.00), (1, 'CAM-002', 3, 1300.00), (1, 'ROP-001', 3, 2000.00), (1, 'ROP-002', 2, 1700.00),
    (1, 'VEL-001', 6,  300.00), (1, 'TOC-001', 2, 1050.00), (1, 'ZAP-001', 4,  420.00),
    (2, 'COL-001', 6,  900.00), (2, 'COL-002', 4, 1900.00), (2, 'COL-003', 3, 2700.00),
    (3, 'ALM-001', 12,  80.00), (3, 'ALM-002', 6,  150.00),
    (4, 'ALM-001', 6,   80.00), (4, 'ZAP-001', 2,  420.00)
) AS v(compra, sku, cantidad, precio)
JOIN productos p ON p.sku = v.sku;

-- ---------------------------------------------------------------------
--  Ventas, con pagos y comprobantes
-- ---------------------------------------------------------------------
INSERT INTO ventas (id, id_cliente, id_usuario, fecha_venta, subtotal, descuento, monto_total, saldo_pendiente, estado,
                    modalidad_entrega, estado_entrega, direccion_destino, ciudad, transportadora, guia_remision, fecha_actualizacion) VALUES
    (1, 1, (SELECT id FROM usuarios WHERE usuario = 'lfernandez'), '2026-09-25 11:20', 3470.00,   0.00, 3470.00,    0.00, 'COMPLETADA',
        'RETIRO',    'ENTREGADO', NULL, NULL, NULL, NULL, '2026-09-25 11:20'),
    (2, 2, (SELECT id FROM usuarios WHERE usuario = 'lfernandez'), '2026-09-26 16:05', 5450.00, 150.00, 5300.00, 2300.00, 'PENDIENTE_PAGO',
        'DOMICILIO', 'PENDIENTE', 'Av. Banzer, 6to anillo, Equipetrol Norte', NULL, NULL, NULL, '2026-09-26 16:05'),
    (3, 3, (SELECT id FROM usuarios WHERE usuario = 'msoliz'),     '2026-10-01 10:40', 2825.00,   0.00, 2825.00,    0.00, 'COMPLETADA',
        'RETIRO',    'ENTREGADO', NULL, NULL, NULL, NULL, '2026-10-01 10:40');

INSERT INTO detalle_venta (id, id_venta, id_producto, cantidad, precio_unitario_original, precio_unitario, descuento_porcentaje, descuento_unitario, subtotal, costo_unitario)
SELECT v.id, v.venta, p.id, v.cantidad, p.precio_venta, v.precio,
       ROUND((p.precio_venta - v.precio) / p.precio_venta * 100, 2), p.precio_venta - v.precio,
       v.cantidad * v.precio, p.precio_compra
FROM (VALUES
    (1, 1, 'CAM-001', 1, 3200.00),
    (2, 1, 'ALM-001', 2,  135.00),
    (3, 2, 'COL-002', 1, 2650.00),
    (4, 2, 'ROP-001', 1, 2650.00),   -- vendido con 150 Bs de descuento sobre el precio de lista
    (5, 3, 'COL-001', 2, 1290.00),
    (6, 3, 'ALM-002', 1,  245.00)
) AS v(id, venta, sku, cantidad, precio)
JOIN productos p ON p.sku = v.sku;

INSERT INTO pagos (id, id_venta, id_usuario, metodo_pago, monto, referencia, estado, fecha_pago) VALUES
    (1, 1, (SELECT id FROM usuarios WHERE usuario = 'lfernandez'), 'EFECTIVO',      3470.00, NULL,           'COMPLETADO', '2026-09-25 11:20'),
    (2, 2, (SELECT id FROM usuarios WHERE usuario = 'lfernandez'), 'TRANSFERENCIA', 3000.00, 'TRF-4471-A',   'COMPLETADO', '2026-09-26 16:05'),
    (3, 3, (SELECT id FROM usuarios WHERE usuario = 'msoliz'),     'EFECTIVO',      2825.00, NULL,           'COMPLETADO', '2026-10-01 10:40');

INSERT INTO comprobantes (id, id_venta, numero_comprobante, tipo_comprobante, nombre_cliente, monto_total, anulado, fecha_emision) VALUES
    (1, 1, 'REC-2026-00001', 'RECIBO', 'Marcela Rojas Villarroel',     3470.00, FALSE, '2026-09-25 11:20'),
    (2, 2, 'REC-2026-00002', 'RECIBO', 'Carlos Eduardo Pinto Salvatierra', 5300.00, FALSE, '2026-09-26 16:05'),
    (3, 3, 'REC-2026-00003', 'RECIBO', 'Silvia Montaño Aguilera',      2825.00, FALSE, '2026-10-01 10:40');

-- ---------------------------------------------------------------------
--  Movimientos de inventario: una fila por cada entrada (compra) y salida (venta)
-- ---------------------------------------------------------------------
INSERT INTO movimientos_inventario (id, id_producto, id_usuario, tipo_movimiento, cantidad, cantidad_anterior, cantidad_nueva, motivo, fecha)
SELECT ROW_NUMBER() OVER (ORDER BY v.fecha::timestamp, v.sku), p.id,
       (SELECT id FROM usuarios WHERE usuario = v.usuario),
       v.tipo, v.nueva - v.anterior, v.anterior, v.nueva, v.motivo, v.fecha::timestamp
FROM (VALUES
    ('2026-09-15 10:00', 'CAM-001', 'ENTRADA', 0,  4, 'Compra #1 - Factura F-2026-0412', 'msoliz'),
    ('2026-09-15 10:00', 'CAM-002', 'ENTRADA', 0,  3, 'Compra #1 - Factura F-2026-0412', 'msoliz'),
    ('2026-09-15 10:00', 'ROP-001', 'ENTRADA', 0,  3, 'Compra #1 - Factura F-2026-0412', 'msoliz'),
    ('2026-09-15 10:00', 'ROP-002', 'ENTRADA', 0,  2, 'Compra #1 - Factura F-2026-0412', 'msoliz'),
    ('2026-09-15 10:00', 'VEL-001', 'ENTRADA', 0,  6, 'Compra #1 - Factura F-2026-0412', 'msoliz'),
    ('2026-09-15 10:00', 'TOC-001', 'ENTRADA', 0,  2, 'Compra #1 - Factura F-2026-0412', 'msoliz'),
    ('2026-09-15 10:00', 'ZAP-001', 'ENTRADA', 0,  4, 'Compra #1 - Factura F-2026-0412', 'msoliz'),
    ('2026-09-17 15:30', 'COL-001', 'ENTRADA', 0,  6, 'Compra #2 - Factura A-00231',     'msoliz'),
    ('2026-09-17 15:30', 'COL-002', 'ENTRADA', 0,  4, 'Compra #2 - Factura A-00231',     'msoliz'),
    ('2026-09-17 15:30', 'COL-003', 'ENTRADA', 0,  3, 'Compra #2 - Factura A-00231',     'msoliz'),
    ('2026-09-24 09:15', 'ALM-001', 'ENTRADA', 0, 12, 'Compra #3 - Factura T-1190',      'msoliz'),
    ('2026-09-24 09:15', 'ALM-002', 'ENTRADA', 0,  6, 'Compra #3 - Factura T-1190',      'msoliz'),
    ('2026-09-25 11:20', 'CAM-001', 'SALIDA',  4,  3, 'Venta #1',                        'lfernandez'),
    ('2026-09-25 11:20', 'ALM-001', 'SALIDA', 12, 10, 'Venta #1',                        'lfernandez'),
    ('2026-09-26 16:05', 'COL-002', 'SALIDA',  4,  3, 'Venta #2',                        'lfernandez'),
    ('2026-09-26 16:05', 'ROP-001', 'SALIDA',  3,  2, 'Venta #2',                        'lfernandez'),
    ('2026-09-28 09:00', 'ALM-001', 'ENTRADA', 10, 16, 'Compra #4',                      'msoliz'),
    ('2026-09-28 09:00', 'ZAP-001', 'ENTRADA', 4,  6, 'Compra #4',                       'msoliz'),
    ('2026-10-01 10:40', 'COL-001', 'SALIDA',  6,  4, 'Venta #3',                        'msoliz'),
    ('2026-10-01 10:40', 'ALM-002', 'SALIDA',  6,  5, 'Venta #3',                        'msoliz')
) AS v(fecha, sku, tipo, anterior, nueva, motivo, usuario)
JOIN productos p ON p.sku = v.sku;

-- ---------------------------------------------------------------------
--  Inventario = el stock que dejo el ultimo movimiento de cada producto
-- ---------------------------------------------------------------------
INSERT INTO inventario (id, id_producto, cantidad_disponible, fecha_actualizacion)
SELECT ROW_NUMBER() OVER (ORDER BY p.id), p.id, u.cantidad_nueva, u.fecha
FROM productos p
JOIN LATERAL (SELECT m.cantidad_nueva, m.fecha FROM movimientos_inventario m
              WHERE m.id_producto = p.id ORDER BY m.fecha DESC, m.id DESC LIMIT 1) u ON TRUE;

-- Una alerta de stock bajo: la almohada viscoelastica quedo en el minimo.
INSERT INTO alertas_inventario (id, id_producto, cantidad_minima, cantidad_actual, estado, fecha_alerta)
SELECT 1, p.id, p.stock_minimo, i.cantidad_disponible, 'PENDIENTE', '2026-10-01 10:40'
FROM productos p JOIN inventario i ON i.id_producto = p.id
WHERE p.sku = 'ALM-002' AND i.cantidad_disponible <= p.stock_minimo;

SELECT sincronizar_contadores_id();

-- Las tablas que el backend numera con la secuencia de PostgreSQL (por ejemplo
-- detalle_venta y detalle_compra) tambien se insertaron con ID fijo: sin esto,
-- la proxima venta recibe un ID ya usado y la API responde 409 CONFLICTO_DATOS.
DO $$
DECLARE
    fila RECORD;
    secuencia TEXT;
    mayor BIGINT;
BEGIN
    FOR fila IN
        SELECT t.table_name AS tabla
        FROM information_schema.tables t
        WHERE t.table_schema = 'public' AND t.table_type = 'BASE TABLE'
          AND EXISTS (SELECT 1 FROM information_schema.columns c
                      WHERE c.table_schema = 'public' AND c.table_name = t.table_name AND c.column_name = 'id')
    LOOP
        secuencia := pg_get_serial_sequence(format('public.%I', fila.tabla), 'id');
        CONTINUE WHEN secuencia IS NULL;
        EXECUTE format('SELECT COALESCE(MAX(id), 0) FROM public.%I', fila.tabla) INTO mayor;
        IF mayor > 0 THEN PERFORM setval(secuencia, mayor, true); END IF;
    END LOOP;
END $$;

-- ---------------------------------------------------------------------
--  Verificacion: si algo no cuadra, se cancela todo
-- ---------------------------------------------------------------------
DO $$
DECLARE
    malos TEXT;
BEGIN
    -- 1. El stock de cada producto es lo comprado menos lo vendido
    SELECT string_agg(p.sku, ', ') INTO malos
    FROM productos p
    JOIN inventario i ON i.id_producto = p.id
    WHERE i.cantidad_disponible <>
          COALESCE((SELECT SUM(d.cantidad) FROM detalle_compra d JOIN compras c ON c.id = d.id_compra
                    WHERE d.id_producto = p.id AND c.estado = 'CONFIRMADA'), 0)
        - COALESCE((SELECT SUM(d.cantidad) FROM detalle_venta d JOIN ventas v ON v.id = d.id_venta
                    WHERE d.id_producto = p.id AND v.estado <> 'CANCELADA'), 0);
    IF malos IS NOT NULL THEN RAISE EXCEPTION 'El stock no cuadra con compras y ventas: %', malos; END IF;

    -- 2. Los movimientos encadenan (lo que quedo es lo que tenia el siguiente)
    SELECT string_agg(DISTINCT p.sku, ', ') INTO malos
    FROM movimientos_inventario m
    JOIN productos p ON p.id = m.id_producto
    WHERE m.cantidad_anterior <> COALESCE((SELECT m2.cantidad_nueva FROM movimientos_inventario m2
                                           WHERE m2.id_producto = m.id_producto AND m2.id < m.id
                                           ORDER BY m2.id DESC LIMIT 1), 0);
    IF malos IS NOT NULL THEN RAISE EXCEPTION 'Los movimientos no encadenan: %', malos; END IF;

    -- 3. El total de cada compra y de cada venta es la suma de sus lineas
    IF EXISTS (SELECT 1 FROM compras c WHERE c.monto_total <> (SELECT SUM(subtotal) FROM detalle_compra WHERE id_compra = c.id)) THEN
        RAISE EXCEPTION 'El total de alguna compra no coincide con sus productos';
    END IF;
    IF EXISTS (SELECT 1 FROM ventas v WHERE v.monto_total <> (SELECT SUM(subtotal) FROM detalle_venta WHERE id_venta = v.id)) THEN
        RAISE EXCEPTION 'El total de alguna venta no coincide con sus productos';
    END IF;

    -- 4. Lo pagado mas el saldo pendiente es el total de la venta
    IF EXISTS (SELECT 1 FROM ventas v WHERE v.monto_total <> v.saldo_pendiente + COALESCE((SELECT SUM(monto) FROM pagos WHERE id_venta = v.id), 0)) THEN
        RAISE EXCEPTION 'Pagos y saldo de alguna venta no cuadran con su total';
    END IF;
END $$;

COMMIT;
