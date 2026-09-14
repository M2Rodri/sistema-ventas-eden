-- =====================================================================
--  FASE 6 - UNIFICACION DE NOMENCLATURA
--  Proyecto : Sistema de Ventas - Muebleria Eden
--  Base     : muebleria_eden_db
--  Fecha    : 2026-08-20
-- =====================================================================
--
--  QUE HACE
--  --------
--  Unifica nombres de columnas que representan el mismo concepto pero
--  se llamaban distinto segun la tabla, y desambigua las columnas
--  llamadas simplemente "tipo".
--
--  POR QUE
--  -------
--  1. El correo electronico se llamaba "email" en users pero "correo"
--     en clientes, proveedores y transportadoras. El telefono se
--     llamaba "telefono" en users, proveedores y transportadoras, pero
--     "celular" en clientes. El mismo dato con dos nombres obliga a
--     recordar cual corresponde a cada tabla y es fuente constante de
--     errores al escribir consultas.
--  2. Una columna llamada solo "tipo" no dice nada fuera del contexto
--     de su tabla. En una consulta con varios JOIN aparecen dos o tres
--     columnas "tipo" y hay que calificarlas todas para distinguirlas.
--
--  CRITERIO ELEGIDO
--  ----------------
--  Gana el nombre que ya usa users, por dos razones: es la tabla mas
--  central del sistema y "email" / "telefono" son los terminos que
--  tambien usa el frontend.
--
--  NOTA TECNICA
--  ------------
--  ALTER TABLE ... RENAME COLUMN conserva automaticamente indices,
--  constraints, claves foraneas y comentarios asociados a la columna.
--  En particular, chk_clientes_tipo sigue vigente sobre la columna
--  renombrada a tipo_cliente sin necesidad de recrearlo.
--
--  IMPACTO EN BACKEND
--  ------------------
--    models/Cliente.java         -> correo   a email
--                                   celular  a telefono
--                                   tipo     a tipoCliente
--    models/Proveedor.java       -> correo   a email
--    models/Transportadora.java  -> correo   a email
--    models/AjusteInventario.java-> tipo     a tipoMovimiento
--    models/ImagenProducto.java  -> no mapea "tipo"; sin cambios
--
--    DTOs afectados: ClienteRequest, ClienteResponse, ProveedorRequest,
--    ProveedorResponse, TransportadoraRequest, TransportadoraResponse,
--    AjusteInventarioRequest, AjusteInventarioResponse
--
--    Frontend: types/cliente.ts, types/proveedor.ts,
--    types/transportadora.ts, types/inventario.ts y los modales
--    correspondientes.
--
--  SEGURIDAD
--  ---------
--  Renombres puros: no se pierde ni un dato, no se elimina ni una
--  columna. Todo dentro de una transaccion.
--
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 6.1  Correo electronico: "correo" -> "email"
-- ---------------------------------------------------------------------
ALTER TABLE clientes         RENAME COLUMN correo TO email;
ALTER TABLE proveedores      RENAME COLUMN correo TO email;
ALTER TABLE transportadoras  RENAME COLUMN correo TO email;


-- ---------------------------------------------------------------------
-- 6.2  Telefono: "celular" -> "telefono"
-- ---------------------------------------------------------------------
-- Solo clientes usaba "celular". El resto del esquema ya usa "telefono".
ALTER TABLE clientes RENAME COLUMN celular TO telefono;


-- ---------------------------------------------------------------------
-- 6.3  Desambiguacion de las columnas "tipo"
-- ---------------------------------------------------------------------
-- clientes.tipo conserva su CHECK (chk_clientes_tipo) tras el renombre.
ALTER TABLE clientes               RENAME COLUMN tipo TO tipo_cliente;
ALTER TABLE movimientos_inventario RENAME COLUMN tipo TO tipo_movimiento;
ALTER TABLE imagenes_producto      RENAME COLUMN tipo TO tipo_imagen;


-- ---------------------------------------------------------------------
-- 6.4  Documentacion de la columna pendiente de definir
-- ---------------------------------------------------------------------
-- imagenes_producto.tipo_imagen quedo fuera de los CHECK de la Fase 2:
-- no existe enum para ella en el backend, la entidad ImagenProducto no
-- la mapea, y su unico valor cargado es 'normal' en minuscula.
-- Se deja el pendiente escrito en la base para que no se pierda.
COMMENT ON COLUMN imagenes_producto.tipo_imagen IS
    'PENDIENTE DE DEFINIR: sin enum en el backend y sin usar por la entidad ImagenProducto. Definir valores validos y agregar CHECK, o eliminar la columna (es_principal ya distingue la imagen principal).';

COMMIT;

-- =====================================================================
--  FIN DEL PLAN DE AJUSTE DE BASE DE DATOS
--
--  Resumen de las 7 fases:
--    0  respaldo
--    1  limpieza de restos del flujo de pedidos descartado
--    2  integridad: 20 indices sobre FK, 1 FK faltante, 10 CHECK
--    3  ambigüedades: stock unico, roles normalizados, costo_referencial
--    4  correccion de valores por omision
--    5  compras y detalle_compra
--    6  unificacion de nomenclatura
--
--  Siguiente etapa: ajuste del backend contra este esquema.
-- =====================================================================
