-- =============================================================================
-- 14. Restaurar los datos del negocio en Supabase
-- =============================================================================
--
-- POR QUÉ
--
-- La tabla configuracion_sistema quedó vacía al migrar a Supabase. De ahí leen
-- las páginas públicas de la tienda (Contacto y Nosotros) la dirección, los
-- teléfonos, el horario y el correo, así que Contacto se veía en blanco.
--
-- Las claves llevan el prefijo negocio_ y no empresa_: es una mueblería
-- familiar, no una empresa, y el nombre de la clave se ve en la pantalla de
-- Configuración.
--
-- IMPORTANTE: DATOS A VERIFICAR
--
-- La dirección, los teléfonos y el correo son valores de relleno para que la
-- tienda no se vea vacía. NO son los datos reales del negocio. Cada uno lleva
-- la marca "VERIFICAR" en su descripción, y se corrigen desde la pantalla de
-- Configuración sin tocar código ni volver a ejecutar esto.
--
-- Lo único confirmado por el dueño es el nombre y la ciudad.
--
-- NOTA TÉCNICA
--
-- fecha_actualizacion es obligatoria y no tiene valor por defecto en la base.
-- Normalmente la pone Hibernate con @UpdateTimestamp, pero este script inserta
-- por SQL directo, así que hay que darle el valor a mano.
-- =============================================================================

BEGIN;

INSERT INTO configuracion_sistema (clave, valor, descripcion, fecha_actualizacion)
VALUES
    ('negocio_razon_social', 'Mueblería Edén',
     'Nombre del negocio tal como aparece en la tienda y los comprobantes', NOW()),

    ('negocio_ciudad', 'Santa Cruz de la Sierra',
     'Ciudad donde atiende el local', NOW()),

    ('negocio_direccion', 'Av. Banzer 3er anillo, Santa Cruz de la Sierra',
     'VERIFICAR con el dueño: dirección real del local', NOW()),

    ('negocio_telefono', '+591 3 345-6789',
     'VERIFICAR con el dueño: teléfono fijo del local', NOW()),

    ('negocio_whatsapp', '+591 7 000-0000',
     'VERIFICAR con el dueño: número de WhatsApp para consultas', NOW()),

    ('negocio_email', 'contacto@eden.com',
     'VERIFICAR con el dueño: correo de contacto. Provisorio hasta que el negocio tenga dominio propio', NOW()),

    ('negocio_horario', 'Lunes a viernes de 08:30 a 18:30 · Sábados de 09:00 a 13:00',
     'VERIFICAR con el dueño: horario de atención al público', NOW()),

    ('negocio_descripcion',
     'Venta de camas, colchones y accesorios de descanso en Santa Cruz de la Sierra.',
     'Texto breve que describe al negocio en la tienda', NOW()),

    ('negocio_nit', '',
     'VERIFICAR con el dueño: NIT del negocio. Vacío hasta tenerlo', NOW()),

    ('negocio_sitio_web', '',
     'Sitio web propio. Vacío hasta que exista', NOW())

ON CONFLICT (clave) DO UPDATE
    SET valor = EXCLUDED.valor,
        descripcion = EXCLUDED.descripcion,
        fecha_actualizacion = NOW();

COMMIT;

-- Verificación
SELECT clave, valor FROM configuracion_sistema WHERE clave LIKE 'negocio_%' ORDER BY clave;
