-- =====================================================================
--  00 - ESQUEMA COMPLETO DE LA BASE (crea las 26 tablas desde cero)
--  Proyecto : Sistema de Ventas - Muebleria Eden
-- =====================================================================
--
--  QUE ES
--  ------
--  Volcado del esquema (solo estructura, sin datos) de la base de produccion
--  del 2026-10-07, ya con los scripts 01 a 35 aplicados. Sirve para crear la
--  base desde cero en otra maquina; los scripts 01 a 35 son los cambios que
--  llevaron a este estado y NO se corren encima de este archivo.
--
--  Generado con:
--    pg_dump --schema-only -n public --no-owner --no-privileges
--  contra PostgreSQL 17.6 (cliente pg_dump 18.3). Se corre en PostgreSQL 17 o
--  superior.
--
--  QUE INCLUYE
--  -----------
--  Las 26 tablas del esquema public con sus claves, restricciones, indices y
--  valores por defecto, y la funcion sincronizar_contadores_id().
--
--  QUE NO INCLUYE
--  --------------
--  - Datos. Los roles ADMIN y EMPLEADO los crea el backend al arrancar. El
--    primer administrador tambien: si la tabla usuarios esta vacia y estan
--    definidas ADMIN_INICIAL_USUARIO y ADMIN_INICIAL_CLAVE (ver .env.example),
--    el backend lo crea al arrancar. Sin esas variables no hay ningun usuario.
--  - Los esquemas internos de Supabase (auth, storage, realtime...): la
--    aplicacion no los usa.
--  - Permisos (GRANT/REVOKE) ni duenios: dependen del servidor donde se cree.
--
--  COMO CORRERLO (en una base nueva y vacia)
--  -----------------------------------------
--    createdb -U postgres muebleria_eden_db
--    psql -U postgres -d muebleria_eden_db -v ON_ERROR_STOP=1 -f backend/database/00_esquema.sql
--
--  Al final se inicializan los contadores de ID sin huecos (contadores_id):
--  sin esas filas el backend no puede numerar el primer registro.
-- =====================================================================

--
-- PostgreSQL database dump
--


-- Dumped from database version 17.6
-- Dumped by pg_dump version 18.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA IF NOT EXISTS public;


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS 'standard public schema';


--
-- Name: sincronizar_contadores_id(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.sincronizar_contadores_id() RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['ventas', 'compras', 'productos', 'clientes', 'proveedores', 'categorias',
                             'inventario', 'pagos', 'comprobantes', 'movimientos_inventario', 'usuarios']
    LOOP
        EXECUTE format(
            'INSERT INTO contadores_id (tabla, ultimo) SELECT %L, COALESCE(MAX(id), 0) FROM %I '
            'ON CONFLICT (tabla) DO UPDATE SET ultimo = EXCLUDED.ultimo', t, t);
    END LOOP;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: alertas_inventario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.alertas_inventario (
    id bigint NOT NULL,
    id_producto bigint NOT NULL,
    cantidad_minima integer NOT NULL,
    cantidad_actual integer NOT NULL,
    estado character varying(20) DEFAULT 'PENDIENTE'::character varying,
    fecha_alerta timestamp without time zone DEFAULT now(),
    CONSTRAINT chk_alertas_inv_estado CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'ATENDIDA'::character varying, 'ATENDIDA_MANUAL'::character varying])::text[])))
);


--
-- Name: alertas_inventario_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.alertas_inventario_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: alertas_inventario_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.alertas_inventario_id_seq OWNED BY public.alertas_inventario.id;


--
-- Name: auditorias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auditorias (
    id bigint NOT NULL,
    id_usuario bigint,
    accion character varying(100) NOT NULL,
    tabla_afectada character varying(50) NOT NULL,
    id_registro character varying(20),
    detalles character varying(1000),
    ip_dispositivo character varying(50),
    fecha_hora timestamp without time zone DEFAULT now()
);


--
-- Name: auditorias_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.auditorias_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: auditorias_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.auditorias_id_seq OWNED BY public.auditorias.id;


--
-- Name: categorias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.categorias (
    id bigint NOT NULL,
    nombre character varying(100) NOT NULL,
    descripcion character varying(500),
    activo boolean DEFAULT true,
    fecha_creacion timestamp without time zone DEFAULT now(),
    fecha_actualizacion timestamp without time zone DEFAULT now(),
    tipo_producto character varying(20) NOT NULL,
    CONSTRAINT chk_categorias_tipo_producto CHECK (((tipo_producto)::text = ANY ((ARRAY['CAMA'::character varying, 'COLCHON'::character varying, 'ALMOHADA'::character varying, 'ACCESORIO'::character varying, 'MUEBLE'::character varying])::text[])))
);


--
-- Name: categorias_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.categorias_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: categorias_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.categorias_id_seq OWNED BY public.categorias.id;


--
-- Name: clientes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.clientes (
    id bigint NOT NULL,
    nombre character varying(100) NOT NULL,
    apellido character varying(100),
    telefono character varying(15),
    email character varying(100),
    nit_ci character varying(20),
    activo boolean DEFAULT true,
    fecha_registro timestamp without time zone DEFAULT now(),
    fecha_actualizacion timestamp without time zone DEFAULT now()
);


--
-- Name: clientes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.clientes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: clientes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.clientes_id_seq OWNED BY public.clientes.id;


--
-- Name: compras; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.compras (
    id bigint NOT NULL,
    id_proveedor bigint,
    id_usuario bigint,
    numero_factura character varying(50),
    fecha_compra timestamp without time zone DEFAULT now() NOT NULL,
    subtotal numeric(10,2) NOT NULL,
    descuento numeric(10,2) DEFAULT 0,
    monto_total numeric(10,2) NOT NULL,
    estado character varying(20) DEFAULT 'CONFIRMADA'::character varying NOT NULL,
    notas character varying(500),
    fecha_actualizacion timestamp without time zone DEFAULT now(),
    CONSTRAINT chk_compras_estado CHECK (((estado)::text = ANY ((ARRAY['CONFIRMADA'::character varying, 'CANCELADA'::character varying])::text[]))),
    CONSTRAINT chk_compras_importes CHECK (((subtotal >= (0)::numeric) AND (monto_total >= (0)::numeric) AND (COALESCE(descuento, (0)::numeric) >= (0)::numeric)))
);


--
-- Name: compras_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.compras_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: compras_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.compras_id_seq OWNED BY public.compras.id;


--
-- Name: comprobantes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.comprobantes (
    id bigint NOT NULL,
    id_venta bigint NOT NULL,
    numero_comprobante character varying(50) NOT NULL,
    tipo_comprobante character varying(20) DEFAULT 'RECIBO'::character varying,
    nombre_cliente character varying(200),
    monto_total numeric(10,2) NOT NULL,
    observaciones character varying(500),
    anulado boolean DEFAULT false,
    fecha_emision timestamp without time zone DEFAULT now(),
    CONSTRAINT chk_comprobantes_tipo CHECK (((tipo_comprobante)::text = ANY (ARRAY[('RECIBO'::character varying)::text, ('COMPROBANTE'::character varying)::text, ('NOTA_VENTA'::character varying)::text])))
);


--
-- Name: comprobantes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.comprobantes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: comprobantes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.comprobantes_id_seq OWNED BY public.comprobantes.id;


--
-- Name: configuracion_sistema; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.configuracion_sistema (
    id bigint NOT NULL,
    clave character varying(100) NOT NULL,
    valor character varying(500) NOT NULL,
    descripcion character varying(200),
    fecha_actualizacion timestamp without time zone DEFAULT now()
);


--
-- Name: configuracion_sistema_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.configuracion_sistema_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: configuracion_sistema_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.configuracion_sistema_id_seq OWNED BY public.configuracion_sistema.id;


--
-- Name: contadores_id; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contadores_id (
    tabla character varying(60) NOT NULL,
    ultimo bigint DEFAULT 0 NOT NULL,
    CONSTRAINT contadores_id_ultimo_check CHECK ((ultimo >= 0))
);


--
-- Name: detalle_compra; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.detalle_compra (
    id bigint NOT NULL,
    id_compra bigint NOT NULL,
    id_producto bigint NOT NULL,
    cantidad integer NOT NULL,
    precio_unitario numeric(10,2) NOT NULL,
    subtotal numeric(10,2) NOT NULL,
    CONSTRAINT chk_detalle_compra_cantidad CHECK ((cantidad > 0)),
    CONSTRAINT chk_detalle_compra_importes CHECK (((precio_unitario >= (0)::numeric) AND (subtotal >= (0)::numeric)))
);


--
-- Name: detalle_compra_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.detalle_compra_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: detalle_compra_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.detalle_compra_id_seq OWNED BY public.detalle_compra.id;


--
-- Name: detalle_venta; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.detalle_venta (
    id bigint NOT NULL,
    id_venta bigint NOT NULL,
    id_producto bigint NOT NULL,
    cantidad integer NOT NULL,
    precio_unitario_original numeric(10,2),
    precio_unitario numeric(10,2) NOT NULL,
    descuento_porcentaje numeric(5,2) DEFAULT 0,
    descuento_unitario numeric(10,2) DEFAULT 0,
    subtotal numeric(10,2) NOT NULL,
    costo_unitario numeric(10,2) NOT NULL,
    CONSTRAINT chk_detalle_venta_costo CHECK ((costo_unitario >= (0)::numeric))
);


--
-- Name: detalle_venta_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.detalle_venta_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: detalle_venta_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.detalle_venta_id_seq OWNED BY public.detalle_venta.id;


--
-- Name: envios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.envios (
    id bigint NOT NULL,
    id_venta bigint NOT NULL,
    id_usuario_responsable bigint,
    id_transportadora bigint,
    direccion_destino character varying(300) NOT NULL,
    ciudad character varying(50),
    departamento character varying(50),
    costo_envio numeric(10,2),
    guia_remision character varying(100),
    estado_seguimiento character varying(20) DEFAULT 'PENDIENTE'::character varying,
    fecha_entrega_estimada date,
    fecha_entrega_real date,
    notas character varying(500),
    fecha_creacion timestamp without time zone DEFAULT now(),
    fecha_actualizacion timestamp without time zone DEFAULT now(),
    CONSTRAINT chk_envios_estado_seguimiento CHECK (((estado_seguimiento)::text = ANY (ARRAY[('PENDIENTE'::character varying)::text, ('EN_PREPARACION'::character varying)::text, ('EN_CAMINO'::character varying)::text, ('ENTREGADO'::character varying)::text, ('DEVUELTO'::character varying)::text, ('CANCELADO'::character varying)::text])))
);


--
-- Name: envios_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.envios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: envios_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.envios_id_seq OWNED BY public.envios.id;


--
-- Name: imagenes_producto; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.imagenes_producto (
    id bigint NOT NULL,
    id_producto bigint NOT NULL,
    orden integer DEFAULT 1,
    es_principal boolean DEFAULT false,
    fecha_creacion timestamp without time zone DEFAULT now(),
    url_imagen character varying(500) NOT NULL
);


--
-- Name: imagenes_producto_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.imagenes_producto_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: imagenes_producto_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.imagenes_producto_id_seq OWNED BY public.imagenes_producto.id;


--
-- Name: inventario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inventario (
    id bigint NOT NULL,
    id_producto bigint NOT NULL,
    cantidad_disponible integer DEFAULT 0 NOT NULL,
    fecha_actualizacion timestamp without time zone DEFAULT now()
);


--
-- Name: inventario_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.inventario_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: inventario_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.inventario_id_seq OWNED BY public.inventario.id;


--
-- Name: mensajes_contacto; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mensajes_contacto (
    id bigint NOT NULL,
    nombre character varying(100) NOT NULL,
    email character varying(100) NOT NULL,
    telefono character varying(20),
    asunto character varying(150) NOT NULL,
    mensaje text NOT NULL,
    atendido boolean DEFAULT false NOT NULL,
    fecha_atencion timestamp without time zone,
    id_usuario_atiende bigint,
    ip_origen character varying(50),
    fecha_envio timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_mensajes_contacto_atencion CHECK (((atendido = false) OR (fecha_atencion IS NOT NULL)))
);


--
-- Name: mensajes_contacto_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.mensajes_contacto_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: mensajes_contacto_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.mensajes_contacto_id_seq OWNED BY public.mensajes_contacto.id;


--
-- Name: movimientos_inventario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.movimientos_inventario (
    id bigint NOT NULL,
    id_producto bigint NOT NULL,
    id_usuario bigint,
    tipo_movimiento character varying(20) NOT NULL,
    cantidad integer NOT NULL,
    cantidad_anterior integer,
    cantidad_nueva integer,
    motivo character varying(200),
    observacion text,
    fecha timestamp without time zone DEFAULT now(),
    CONSTRAINT chk_movimientos_inv_tipo CHECK (((tipo_movimiento)::text = ANY (ARRAY[('ENTRADA'::character varying)::text, ('SALIDA'::character varying)::text, ('COMPRA'::character varying)::text, ('VENTA'::character varying)::text, ('DEVOLUCION'::character varying)::text, ('MERMA'::character varying)::text, ('AJUSTE_INICIAL'::character varying)::text])))
);


--
-- Name: movimientos_inventario_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.movimientos_inventario_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: movimientos_inventario_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.movimientos_inventario_id_seq OWNED BY public.movimientos_inventario.id;


--
-- Name: multimedia_producto; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.multimedia_producto (
    id bigint NOT NULL,
    id_producto bigint NOT NULL,
    url_modelo3d character varying(500),
    url_vista_previa character varying(500),
    habilitado_ra boolean DEFAULT false,
    activo boolean DEFAULT true,
    fecha_creacion timestamp without time zone DEFAULT now(),
    fecha_actualizacion timestamp without time zone DEFAULT now()
);


--
-- Name: multimedia_producto_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.multimedia_producto_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: multimedia_producto_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.multimedia_producto_id_seq OWNED BY public.multimedia_producto.id;


--
-- Name: pagos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pagos (
    id bigint NOT NULL,
    id_venta bigint NOT NULL,
    metodo_pago character varying(20) NOT NULL,
    monto numeric(10,2) NOT NULL,
    referencia character varying(100),
    estado character varying(20) DEFAULT 'COMPLETADO'::character varying,
    observacion text,
    fecha_pago timestamp without time zone DEFAULT now(),
    url_comprobante character varying(500),
    id_usuario bigint,
    CONSTRAINT chk_pagos_estado CHECK (((estado)::text = ANY (ARRAY[('PENDIENTE'::character varying)::text, ('COMPLETADO'::character varying)::text, ('RECHAZADO'::character varying)::text]))),
    CONSTRAINT chk_pagos_metodo_pago CHECK (((metodo_pago)::text = ANY (ARRAY[('EFECTIVO'::character varying)::text, ('TRANSFERENCIA'::character varying)::text, ('QR'::character varying)::text])))
);


--
-- Name: pagos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pagos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pagos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pagos_id_seq OWNED BY public.pagos.id;


--
-- Name: productos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.productos (
    id bigint NOT NULL,
    sku character varying(50) NOT NULL,
    nombre character varying(200) NOT NULL,
    descripcion character varying(2000),
    marca character varying(100),
    modelo character varying(100),
    dimensiones character varying(100),
    firmeza character varying(50),
    material_nucleo character varying(100),
    tipo_producto character varying(20) NOT NULL,
    precio_venta numeric(10,2) NOT NULL,
    stock_minimo integer DEFAULT 3,
    id_categoria bigint NOT NULL,
    activo boolean DEFAULT true,
    fecha_creacion timestamp without time zone DEFAULT now(),
    fecha_actualizacion timestamp without time zone DEFAULT now(),
    calidad character varying(50),
    precio_compra numeric(10,2),
    color character varying(50),
    material_armazon character varying(50),
    CONSTRAINT chk_productos_tipo_producto CHECK (((tipo_producto)::text = ANY ((ARRAY['CAMA'::character varying, 'COLCHON'::character varying, 'ALMOHADA'::character varying, 'ACCESORIO'::character varying, 'MUEBLE'::character varying])::text[])))
);


--
-- Name: productos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.productos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: productos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.productos_id_seq OWNED BY public.productos.id;


--
-- Name: productos_proveedor; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.productos_proveedor (
    id bigint NOT NULL,
    id_producto bigint NOT NULL,
    id_proveedor bigint NOT NULL,
    precio_compra numeric(10,2) NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date
);


--
-- Name: productos_proveedor_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.productos_proveedor_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: productos_proveedor_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.productos_proveedor_id_seq OWNED BY public.productos_proveedor.id;


--
-- Name: promociones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.promociones (
    id bigint NOT NULL,
    nombre character varying(150) NOT NULL,
    descripcion text,
    descuento numeric(5,2) NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    activo boolean DEFAULT true,
    fecha_creacion timestamp without time zone DEFAULT now(),
    fecha_actualizacion timestamp without time zone DEFAULT now()
);


--
-- Name: promociones_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.promociones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: promociones_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.promociones_id_seq OWNED BY public.promociones.id;


--
-- Name: promociones_producto; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.promociones_producto (
    id_promocion bigint NOT NULL,
    id_producto bigint NOT NULL
);


--
-- Name: proveedores; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.proveedores (
    id bigint NOT NULL,
    nombre_empresa character varying(200) NOT NULL,
    contacto character varying(100),
    telefono character varying(15) NOT NULL,
    email character varying(100),
    direccion character varying(300),
    nit character varying(20),
    notas character varying(500),
    activo boolean DEFAULT true,
    fecha_registro timestamp without time zone DEFAULT now(),
    fecha_actualizacion timestamp without time zone DEFAULT now()
);


--
-- Name: proveedores_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.proveedores_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: proveedores_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.proveedores_id_seq OWNED BY public.proveedores.id;


--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    id bigint NOT NULL,
    nombre character varying(50) NOT NULL,
    descripcion character varying(200)
);


--
-- Name: roles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.roles_id_seq OWNED BY public.roles.id;


--
-- Name: transportadoras; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transportadoras (
    id bigint NOT NULL,
    nombre character varying(100) NOT NULL,
    telefono character varying(15),
    email character varying(100),
    tarifa_base numeric(10,2),
    tiempo_estimado_dias integer,
    activo boolean DEFAULT true,
    fecha_registro timestamp without time zone DEFAULT now(),
    fecha_actualizacion timestamp without time zone DEFAULT now()
);


--
-- Name: transportadoras_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.transportadoras_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: transportadoras_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.transportadoras_id_seq OWNED BY public.transportadoras.id;


--
-- Name: usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuarios (
    id bigint NOT NULL,
    activo boolean NOT NULL,
    apellido character varying(50) NOT NULL,
    direccion character varying(200),
    usuario character varying(30) NOT NULL,
    fecha_actualizacion timestamp(6) without time zone NOT NULL,
    fecha_creacion timestamp(6) without time zone NOT NULL,
    nombre character varying(50) NOT NULL,
    password character varying(255) NOT NULL,
    telefono character varying(15),
    id_rol bigint NOT NULL
);


--
-- Name: usuarios_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.usuarios ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.usuarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: ventas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ventas (
    id bigint NOT NULL,
    id_cliente bigint,
    id_usuario bigint NOT NULL,
    fecha_venta timestamp without time zone DEFAULT now(),
    subtotal numeric(10,2) NOT NULL,
    descuento numeric(10,2) DEFAULT 0,
    monto_total numeric(10,2) NOT NULL,
    saldo_pendiente numeric(10,2) DEFAULT 0,
    estado character varying(20) DEFAULT 'PENDIENTE_PAGO'::character varying,
    fecha_actualizacion timestamp without time zone DEFAULT now(),
    modalidad_entrega character varying(20) DEFAULT 'RETIRO'::character varying NOT NULL,
    estado_entrega character varying(20) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    direccion_destino character varying(300),
    ciudad character varying(50),
    transportadora character varying(100),
    guia_remision character varying(100),
    fecha_limite_pago date,
    CONSTRAINT chk_ventas_estado CHECK (((estado)::text = ANY (ARRAY[('COMPLETADA'::character varying)::text, ('PENDIENTE_PAGO'::character varying)::text, ('CANCELADA'::character varying)::text]))),
    CONSTRAINT ventas_estado_entrega_check CHECK (((estado_entrega)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'ENTREGADO'::character varying])::text[]))),
    CONSTRAINT ventas_modalidad_entrega_check CHECK (((modalidad_entrega)::text = ANY (ARRAY[('RETIRO'::character varying)::text, ('DOMICILIO'::character varying)::text, ('TRANSPORTADORA'::character varying)::text])))
);


--
-- Name: COLUMN ventas.fecha_limite_pago; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ventas.fecha_limite_pago IS 'Fecha hasta la que el cliente prometió pagar el saldo pendiente. Opcional.';


--
-- Name: ventas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ventas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ventas_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ventas_id_seq OWNED BY public.ventas.id;


--
-- Name: alertas_inventario id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alertas_inventario ALTER COLUMN id SET DEFAULT nextval('public.alertas_inventario_id_seq'::regclass);


--
-- Name: auditorias id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditorias ALTER COLUMN id SET DEFAULT nextval('public.auditorias_id_seq'::regclass);


--
-- Name: categorias id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categorias ALTER COLUMN id SET DEFAULT nextval('public.categorias_id_seq'::regclass);


--
-- Name: clientes id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clientes ALTER COLUMN id SET DEFAULT nextval('public.clientes_id_seq'::regclass);


--
-- Name: compras id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compras ALTER COLUMN id SET DEFAULT nextval('public.compras_id_seq'::regclass);


--
-- Name: comprobantes id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comprobantes ALTER COLUMN id SET DEFAULT nextval('public.comprobantes_id_seq'::regclass);


--
-- Name: configuracion_sistema id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.configuracion_sistema ALTER COLUMN id SET DEFAULT nextval('public.configuracion_sistema_id_seq'::regclass);


--
-- Name: detalle_compra id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detalle_compra ALTER COLUMN id SET DEFAULT nextval('public.detalle_compra_id_seq'::regclass);


--
-- Name: detalle_venta id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detalle_venta ALTER COLUMN id SET DEFAULT nextval('public.detalle_venta_id_seq'::regclass);


--
-- Name: envios id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.envios ALTER COLUMN id SET DEFAULT nextval('public.envios_id_seq'::regclass);


--
-- Name: imagenes_producto id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.imagenes_producto ALTER COLUMN id SET DEFAULT nextval('public.imagenes_producto_id_seq'::regclass);


--
-- Name: inventario id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventario ALTER COLUMN id SET DEFAULT nextval('public.inventario_id_seq'::regclass);


--
-- Name: mensajes_contacto id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mensajes_contacto ALTER COLUMN id SET DEFAULT nextval('public.mensajes_contacto_id_seq'::regclass);


--
-- Name: movimientos_inventario id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.movimientos_inventario ALTER COLUMN id SET DEFAULT nextval('public.movimientos_inventario_id_seq'::regclass);


--
-- Name: multimedia_producto id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.multimedia_producto ALTER COLUMN id SET DEFAULT nextval('public.multimedia_producto_id_seq'::regclass);


--
-- Name: pagos id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos ALTER COLUMN id SET DEFAULT nextval('public.pagos_id_seq'::regclass);


--
-- Name: productos id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.productos ALTER COLUMN id SET DEFAULT nextval('public.productos_id_seq'::regclass);


--
-- Name: productos_proveedor id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.productos_proveedor ALTER COLUMN id SET DEFAULT nextval('public.productos_proveedor_id_seq'::regclass);


--
-- Name: promociones id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promociones ALTER COLUMN id SET DEFAULT nextval('public.promociones_id_seq'::regclass);


--
-- Name: proveedores id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.proveedores ALTER COLUMN id SET DEFAULT nextval('public.proveedores_id_seq'::regclass);


--
-- Name: roles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles ALTER COLUMN id SET DEFAULT nextval('public.roles_id_seq'::regclass);


--
-- Name: transportadoras id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transportadoras ALTER COLUMN id SET DEFAULT nextval('public.transportadoras_id_seq'::regclass);


--
-- Name: ventas id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ventas ALTER COLUMN id SET DEFAULT nextval('public.ventas_id_seq'::regclass);


--
-- Name: alertas_inventario alertas_inventario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alertas_inventario
    ADD CONSTRAINT alertas_inventario_pkey PRIMARY KEY (id);


--
-- Name: auditorias auditorias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditorias
    ADD CONSTRAINT auditorias_pkey PRIMARY KEY (id);


--
-- Name: categorias categorias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categorias
    ADD CONSTRAINT categorias_pkey PRIMARY KEY (id);


--
-- Name: clientes clientes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clientes
    ADD CONSTRAINT clientes_pkey PRIMARY KEY (id);


--
-- Name: compras compras_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compras
    ADD CONSTRAINT compras_pkey PRIMARY KEY (id);


--
-- Name: comprobantes comprobantes_numero_comprobante_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comprobantes
    ADD CONSTRAINT comprobantes_numero_comprobante_key UNIQUE (numero_comprobante);


--
-- Name: comprobantes comprobantes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comprobantes
    ADD CONSTRAINT comprobantes_pkey PRIMARY KEY (id);


--
-- Name: configuracion_sistema configuracion_sistema_clave_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.configuracion_sistema
    ADD CONSTRAINT configuracion_sistema_clave_key UNIQUE (clave);


--
-- Name: configuracion_sistema configuracion_sistema_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.configuracion_sistema
    ADD CONSTRAINT configuracion_sistema_pkey PRIMARY KEY (id);


--
-- Name: contadores_id contadores_id_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contadores_id
    ADD CONSTRAINT contadores_id_pkey PRIMARY KEY (tabla);


--
-- Name: detalle_compra detalle_compra_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detalle_compra
    ADD CONSTRAINT detalle_compra_pkey PRIMARY KEY (id);


--
-- Name: detalle_venta detalle_venta_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detalle_venta
    ADD CONSTRAINT detalle_venta_pkey PRIMARY KEY (id);


--
-- Name: envios envios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.envios
    ADD CONSTRAINT envios_pkey PRIMARY KEY (id);


--
-- Name: imagenes_producto imagenes_producto_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.imagenes_producto
    ADD CONSTRAINT imagenes_producto_pkey PRIMARY KEY (id);


--
-- Name: inventario inventario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventario
    ADD CONSTRAINT inventario_pkey PRIMARY KEY (id);


--
-- Name: mensajes_contacto mensajes_contacto_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mensajes_contacto
    ADD CONSTRAINT mensajes_contacto_pkey PRIMARY KEY (id);


--
-- Name: movimientos_inventario movimientos_inventario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.movimientos_inventario
    ADD CONSTRAINT movimientos_inventario_pkey PRIMARY KEY (id);


--
-- Name: multimedia_producto multimedia_producto_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.multimedia_producto
    ADD CONSTRAINT multimedia_producto_pkey PRIMARY KEY (id);


--
-- Name: pagos pagos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos
    ADD CONSTRAINT pagos_pkey PRIMARY KEY (id);


--
-- Name: productos productos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.productos
    ADD CONSTRAINT productos_pkey PRIMARY KEY (id);


--
-- Name: productos_proveedor productos_proveedor_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.productos_proveedor
    ADD CONSTRAINT productos_proveedor_pkey PRIMARY KEY (id);


--
-- Name: productos productos_sku_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.productos
    ADD CONSTRAINT productos_sku_key UNIQUE (sku);


--
-- Name: promociones promociones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promociones
    ADD CONSTRAINT promociones_pkey PRIMARY KEY (id);


--
-- Name: promociones_producto promociones_producto_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promociones_producto
    ADD CONSTRAINT promociones_producto_pkey PRIMARY KEY (id_promocion, id_producto);


--
-- Name: proveedores proveedores_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.proveedores
    ADD CONSTRAINT proveedores_pkey PRIMARY KEY (id);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: transportadoras transportadoras_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transportadoras
    ADD CONSTRAINT transportadoras_pkey PRIMARY KEY (id);


--
-- Name: compras uq_compras_proveedor_factura; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compras
    ADD CONSTRAINT uq_compras_proveedor_factura UNIQUE (id_proveedor, numero_factura);


--
-- Name: detalle_compra uq_detalle_compra_producto; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detalle_compra
    ADD CONSTRAINT uq_detalle_compra_producto UNIQUE (id_compra, id_producto);


--
-- Name: inventario uq_inventario_producto; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventario
    ADD CONSTRAINT uq_inventario_producto UNIQUE (id_producto);


--
-- Name: productos_proveedor uq_productos_proveedor_vigencia; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.productos_proveedor
    ADD CONSTRAINT uq_productos_proveedor_vigencia UNIQUE (id_producto, id_proveedor, fecha_inicio);


--
-- Name: roles uq_roles_nombre; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT uq_roles_nombre UNIQUE (nombre);


--
-- Name: usuarios uq_usuarios_usuario; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT uq_usuarios_usuario UNIQUE (usuario);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- Name: ventas ventas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ventas
    ADD CONSTRAINT ventas_pkey PRIMARY KEY (id);


--
-- Name: idx_alertas_inventario_producto; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_alertas_inventario_producto ON public.alertas_inventario USING btree (id_producto);


--
-- Name: idx_auditorias_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditorias_usuario ON public.auditorias USING btree (id_usuario);


--
-- Name: idx_compras_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_compras_fecha ON public.compras USING btree (fecha_compra);


--
-- Name: idx_compras_proveedor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_compras_proveedor ON public.compras USING btree (id_proveedor);


--
-- Name: idx_compras_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_compras_usuario ON public.compras USING btree (id_usuario);


--
-- Name: idx_comprobantes_venta; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comprobantes_venta ON public.comprobantes USING btree (id_venta);


--
-- Name: idx_detalle_compra_producto; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_detalle_compra_producto ON public.detalle_compra USING btree (id_producto);


--
-- Name: idx_detalle_venta_producto; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_detalle_venta_producto ON public.detalle_venta USING btree (id_producto);


--
-- Name: idx_detalle_venta_venta; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_detalle_venta_venta ON public.detalle_venta USING btree (id_venta);


--
-- Name: idx_envios_transportadora; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_envios_transportadora ON public.envios USING btree (id_transportadora);


--
-- Name: idx_envios_usuario_responsable; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_envios_usuario_responsable ON public.envios USING btree (id_usuario_responsable);


--
-- Name: idx_envios_venta; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_envios_venta ON public.envios USING btree (id_venta);


--
-- Name: idx_imagenes_producto_producto; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_imagenes_producto_producto ON public.imagenes_producto USING btree (id_producto);


--
-- Name: idx_mensajes_contacto_atendido; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mensajes_contacto_atendido ON public.mensajes_contacto USING btree (atendido);


--
-- Name: idx_mensajes_contacto_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mensajes_contacto_fecha ON public.mensajes_contacto USING btree (fecha_envio);


--
-- Name: idx_mensajes_contacto_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mensajes_contacto_usuario ON public.mensajes_contacto USING btree (id_usuario_atiende);


--
-- Name: idx_movimientos_inv_producto; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_movimientos_inv_producto ON public.movimientos_inventario USING btree (id_producto);


--
-- Name: idx_movimientos_inv_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_movimientos_inv_usuario ON public.movimientos_inventario USING btree (id_usuario);


--
-- Name: idx_multimedia_producto_producto; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_multimedia_producto_producto ON public.multimedia_producto USING btree (id_producto);


--
-- Name: idx_pagos_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pagos_usuario ON public.pagos USING btree (id_usuario);


--
-- Name: idx_pagos_venta; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pagos_venta ON public.pagos USING btree (id_venta);


--
-- Name: idx_productos_categoria; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_productos_categoria ON public.productos USING btree (id_categoria);


--
-- Name: idx_productos_proveedor_producto; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_productos_proveedor_producto ON public.productos_proveedor USING btree (id_producto);


--
-- Name: idx_productos_proveedor_proveedor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_productos_proveedor_proveedor ON public.productos_proveedor USING btree (id_proveedor);


--
-- Name: idx_promociones_producto_producto; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_promociones_producto_producto ON public.promociones_producto USING btree (id_producto);


--
-- Name: idx_usuarios_rol; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuarios_rol ON public.usuarios USING btree (id_rol);


--
-- Name: idx_ventas_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ventas_cliente ON public.ventas USING btree (id_cliente);


--
-- Name: idx_ventas_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ventas_usuario ON public.ventas USING btree (id_usuario);


--
-- Name: alertas_inventario alertas_inventario_id_producto_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alertas_inventario
    ADD CONSTRAINT alertas_inventario_id_producto_fkey FOREIGN KEY (id_producto) REFERENCES public.productos(id);


--
-- Name: comprobantes comprobantes_id_venta_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comprobantes
    ADD CONSTRAINT comprobantes_id_venta_fkey FOREIGN KEY (id_venta) REFERENCES public.ventas(id);


--
-- Name: detalle_venta detalle_venta_id_producto_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detalle_venta
    ADD CONSTRAINT detalle_venta_id_producto_fkey FOREIGN KEY (id_producto) REFERENCES public.productos(id);


--
-- Name: detalle_venta detalle_venta_id_venta_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detalle_venta
    ADD CONSTRAINT detalle_venta_id_venta_fkey FOREIGN KEY (id_venta) REFERENCES public.ventas(id);


--
-- Name: envios envios_id_transportadora_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.envios
    ADD CONSTRAINT envios_id_transportadora_fkey FOREIGN KEY (id_transportadora) REFERENCES public.transportadoras(id);


--
-- Name: envios envios_id_venta_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.envios
    ADD CONSTRAINT envios_id_venta_fkey FOREIGN KEY (id_venta) REFERENCES public.ventas(id);


--
-- Name: auditorias fk_auditorias_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditorias
    ADD CONSTRAINT fk_auditorias_usuario FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id);


--
-- Name: compras fk_compras_proveedor; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compras
    ADD CONSTRAINT fk_compras_proveedor FOREIGN KEY (id_proveedor) REFERENCES public.proveedores(id);


--
-- Name: compras fk_compras_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compras
    ADD CONSTRAINT fk_compras_usuario FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id);


--
-- Name: detalle_compra fk_detalle_compra_compra; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detalle_compra
    ADD CONSTRAINT fk_detalle_compra_compra FOREIGN KEY (id_compra) REFERENCES public.compras(id) ON DELETE CASCADE;


--
-- Name: detalle_compra fk_detalle_compra_producto; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detalle_compra
    ADD CONSTRAINT fk_detalle_compra_producto FOREIGN KEY (id_producto) REFERENCES public.productos(id);


--
-- Name: envios fk_envios_usuario_responsable; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.envios
    ADD CONSTRAINT fk_envios_usuario_responsable FOREIGN KEY (id_usuario_responsable) REFERENCES public.usuarios(id);


--
-- Name: mensajes_contacto fk_mensajes_contacto_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mensajes_contacto
    ADD CONSTRAINT fk_mensajes_contacto_usuario FOREIGN KEY (id_usuario_atiende) REFERENCES public.usuarios(id);


--
-- Name: movimientos_inventario fk_movimientos_inv_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.movimientos_inventario
    ADD CONSTRAINT fk_movimientos_inv_usuario FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id);


--
-- Name: pagos fk_pagos_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos
    ADD CONSTRAINT fk_pagos_usuario FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id);


--
-- Name: usuarios fk_usuarios_rol; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT fk_usuarios_rol FOREIGN KEY (id_rol) REFERENCES public.roles(id);


--
-- Name: ventas fk_ventas_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ventas
    ADD CONSTRAINT fk_ventas_usuario FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id);


--
-- Name: imagenes_producto imagenes_producto_id_producto_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.imagenes_producto
    ADD CONSTRAINT imagenes_producto_id_producto_fkey FOREIGN KEY (id_producto) REFERENCES public.productos(id);


--
-- Name: inventario inventario_id_producto_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventario
    ADD CONSTRAINT inventario_id_producto_fkey FOREIGN KEY (id_producto) REFERENCES public.productos(id);


--
-- Name: movimientos_inventario movimientos_inventario_id_producto_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.movimientos_inventario
    ADD CONSTRAINT movimientos_inventario_id_producto_fkey FOREIGN KEY (id_producto) REFERENCES public.productos(id);


--
-- Name: multimedia_producto multimedia_producto_id_producto_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.multimedia_producto
    ADD CONSTRAINT multimedia_producto_id_producto_fkey FOREIGN KEY (id_producto) REFERENCES public.productos(id);


--
-- Name: pagos pagos_id_venta_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos
    ADD CONSTRAINT pagos_id_venta_fkey FOREIGN KEY (id_venta) REFERENCES public.ventas(id);


--
-- Name: productos productos_id_categoria_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.productos
    ADD CONSTRAINT productos_id_categoria_fkey FOREIGN KEY (id_categoria) REFERENCES public.categorias(id);


--
-- Name: productos_proveedor productos_proveedor_id_producto_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.productos_proveedor
    ADD CONSTRAINT productos_proveedor_id_producto_fkey FOREIGN KEY (id_producto) REFERENCES public.productos(id);


--
-- Name: productos_proveedor productos_proveedor_id_proveedor_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.productos_proveedor
    ADD CONSTRAINT productos_proveedor_id_proveedor_fkey FOREIGN KEY (id_proveedor) REFERENCES public.proveedores(id);


--
-- Name: promociones_producto promociones_producto_id_producto_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promociones_producto
    ADD CONSTRAINT promociones_producto_id_producto_fkey FOREIGN KEY (id_producto) REFERENCES public.productos(id);


--
-- Name: promociones_producto promociones_producto_id_promocion_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.promociones_producto
    ADD CONSTRAINT promociones_producto_id_promocion_fkey FOREIGN KEY (id_promocion) REFERENCES public.promociones(id);


--
-- Name: ventas ventas_id_cliente_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ventas
    ADD CONSTRAINT ventas_id_cliente_fkey FOREIGN KEY (id_cliente) REFERENCES public.clientes(id);


--
-- PostgreSQL database dump complete
--


-- =====================================================================
--  Contadores de ID sin huecos: una fila por tabla, en 0 (las tablas estan
--  vacias). Despues de cualquier carga masiva por SQL volver a llamarla.
-- =====================================================================
-- pg_dump deja el search_path vacío; la función usa las tablas sin esquema.
SET search_path TO public;
SELECT public.sincronizar_contadores_id();
