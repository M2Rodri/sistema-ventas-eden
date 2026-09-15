-- =============================================================================
-- 13. Quitar registros_demo (ejercicio del módulo 3)
-- =============================================================================
--
-- POR QUÉ
--
-- La tabla registros_demo no pertenece al sistema de la mueblería: viene del
-- ejercicio de GPS y contexto del módulo 3 del diplomado. Era la única tabla
-- que la aplicación móvil consultaba directamente contra Supabase, saltándose
-- el backend.
--
-- Esa doble vía es lo que se quiere evitar: cuando parte de la aplicación
-- entra directo a la base, los permisos hay que definirlos en dos lugares (las
-- reglas de Spring y las políticas de Supabase) y tienen que decir lo mismo.
-- Mantener dos modelos de permisos sincronizados es de donde salen los agujeros
-- como el que corrigió el script 11.
--
-- Sacándola, todo el sistema queda con una sola puerta de entrada: la API.
--
-- DATOS
--
-- Las 6 filas están respaldadas en:
--   database/respaldo_supabase_20260913/registros_demo.csv
--
-- Ninguna otra tabla la referencia con clave foránea: se verificó antes.
--
-- CONSECUENCIA
--
-- El proyecto Flutter del módulo 3 deja de funcionar en su módulo de registros.
-- Es intencional: ese trabajo ya se entregó y no forma parte de la aplicación
-- de la mueblería.
-- =============================================================================

DROP TABLE IF EXISTS public.registros_demo;

-- Verificación: debe devolver 0 filas.
SELECT tablename FROM pg_tables
WHERE schemaname = 'public' AND tablename = 'registros_demo';
