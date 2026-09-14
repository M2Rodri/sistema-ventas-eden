-- =============================================================================
-- 11. Cerrar el acceso público a las tablas del negocio (Supabase)
-- =============================================================================
--
-- POR QUÉ
--
-- Al migrar la base a Supabase, las 25 tablas del sistema quedaron con permisos
-- completos (SELECT, INSERT, UPDATE, DELETE, TRUNCATE) para los roles públicos
-- 'anon' y 'authenticated', y 12 de ellas sin seguridad por filas.
--
-- Eso importa porque la clave que usa una aplicación móvil viaja dentro del
-- APK: es pública por diseño, y cualquiera la puede extraer. Supabase parte de
-- que la protección real la dan las políticas de filas. Sin políticas y con los
-- permisos abiertos, esa clave equivale a una llave maestra de la base.
--
-- El caso más grave era 'auditorias': el registro de quién hizo qué se podía
-- leer, alterar o vaciar entero desde afuera. Una auditoría que cualquiera
-- puede editar no sirve como auditoría.
--
-- Además, con los permisos abiertos toda la capa de seguridad del backend
-- (roles ADMIN/EMPLEADO, validaciones, auditoría) se puede rodear entrando
-- directo a la base.
--
-- QUÉ HACE
--
-- 1. Le quita a 'anon' y 'authenticated' todo permiso sobre las tablas del
--    sistema, y les impide recibirlos automáticamente en tablas futuras.
-- 2. Activa la seguridad por filas en las tablas que no la tenían, como
--    segunda barrera por si alguien vuelve a otorgar permisos más adelante.
--
-- POR QUÉ NO ROMPE NADA
--
-- El backend Spring Boot se conecta con el rol 'postgres', que es el DUEÑO de
-- estas tablas. Un dueño no pasa por estos permisos ni por las políticas de
-- filas, así que sigue leyendo y escribiendo igual que antes. Los roles que se
-- restringen son únicamente los que usaría un cliente conectándose directo a
-- Supabase, que es precisamente lo que no queremos que ocurra.
--
-- QUÉ NO TOCA
--
-- Las tablas 'incidents', 'profiles' y 'registros_demo' pertenecen a otros
-- proyectos que comparten esta misma base de Supabase. No se modifican.
--
-- CÓMO REVERTIR
--
-- Volver a otorgar los permisos:
--   GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated;
-- (no se recomienda: es exactamente el estado que este script corrige)
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. Quitar los permisos de los roles públicos sobre las tablas del sistema
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    t text;
    tablas text[] := ARRAY[
        'alertas_inventario', 'auditorias', 'categorias', 'clientes',
        'compras', 'comprobantes', 'configuracion_sistema', 'detalle_compra',
        'detalle_venta', 'direcciones_cliente', 'envios', 'imagenes_producto',
        'inventario', 'mensajes_contacto', 'movimientos_inventario',
        'multimedia_producto', 'pagos', 'productos', 'promociones',
        'promociones_producto', 'proveedores', 'roles', 'transportadoras',
        'usuarios', 'ventas'
    ];
BEGIN
    FOREACH t IN ARRAY tablas LOOP
        EXECUTE format('REVOKE ALL ON TABLE public.%I FROM anon, authenticated;', t);
        RAISE NOTICE 'Permisos públicos retirados de %', t;
    END LOOP;
END $$;

-- Las secuencias también: sin esto, un cliente con la clave pública podría
-- seguir consumiendo números de la secuencia aunque no pueda insertar.
DO $$
DECLARE
    s text;
BEGIN
    FOR s IN
        SELECT sequencename FROM pg_sequences
        WHERE schemaname = 'public'
          AND sequencename NOT LIKE 'incidents%'
          AND sequencename NOT LIKE 'profiles%'
          AND sequencename NOT LIKE 'registros_demo%'
    LOOP
        EXECUTE format('REVOKE ALL ON SEQUENCE public.%I FROM anon, authenticated;', s);
    END LOOP;
END $$;

-- -----------------------------------------------------------------------------
-- 2. Que las tablas nuevas no nazcan abiertas
-- -----------------------------------------------------------------------------
-- Supabase deja configurado que toda tabla creada en 'public' otorgue permisos
-- a los roles públicos automáticamente. Esto lo desactiva para lo que se cree
-- de ahora en adelante.
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    REVOKE ALL ON TABLES FROM anon, authenticated;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    REVOKE ALL ON SEQUENCES FROM anon, authenticated;

-- -----------------------------------------------------------------------------
-- 3. Seguridad por filas como segunda barrera
-- -----------------------------------------------------------------------------
-- Doce tablas la tenían desactivada. Se activa sin definir políticas: sin
-- políticas, los roles no dueños no acceden a ninguna fila. El backend no se
-- ve afectado porque entra como dueño de la tabla.
ALTER TABLE public.auditorias             ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.compras                ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.comprobantes           ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.configuracion_sistema  ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.detalle_compra         ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.imagenes_producto      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mensajes_contacto      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.movimientos_inventario ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.multimedia_producto    ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pagos                  ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.promociones            ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.promociones_producto   ENABLE ROW LEVEL SECURITY;

COMMIT;

-- =============================================================================
-- Verificación
-- =============================================================================
-- Debe devolver 0 filas: ninguna tabla del sistema con permisos públicos.
SELECT g.table_name, g.grantee, g.privilege_type
FROM information_schema.role_table_grants g
WHERE g.table_schema = 'public'
  AND g.grantee IN ('anon', 'authenticated')
  AND g.table_name NOT IN ('incidents', 'profiles', 'registros_demo')
ORDER BY g.table_name, g.grantee;
