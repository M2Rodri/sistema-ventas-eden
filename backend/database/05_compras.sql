-- =====================================================================
--  FASE 5 - CICLO DE ABASTECIMIENTO: COMPRAS A PROVEEDORES
--  Proyecto : Sistema de Ventas - Muebleria Eden
--  Base     : muebleria_eden_db
--  Fecha    : 2026-08-20
-- =====================================================================
--
--  QUE HACE
--  --------
--  Crea las tablas compras y detalle_compra, espejo de ventas y
--  detalle_venta, para registrar las compras a proveedores.
--
--  POR QUE
--  -------
--  1. Sin esta tabla, proveedores y productos_proveedor quedan sin
--     participar de ningun flujo: son un catalogo de contactos.
--  2. Hoy la mercaderia entra al sistema solo como un movimiento de
--     inventario escrito a mano, sin proveedor, sin costo y sin factura.
--     No hay trazabilidad de a quien se le compro un lote.
--  3. Sin costo real de reposicion, el margen se calcula contra
--     productos.costo_referencial, un valor fijo que envejece.
--  4. El modulo ya esta implementado en el backend (CompraController,
--     CompraService, Compra, DetalleCompra, EstadoCompra) e integrado
--     con el inventario: al pasar una compra a RECIBIDA, CompraService
--     llama a inventarioService.aumentarStock() por cada detalle.
--     Solo faltaban las tablas.
--
--  DECISIONES DE DISEÑO
--  --------------------
--  a) Se usa monto_total (no costo_total, como se llama hoy en
--     Compra.java) por simetria con ventas.monto_total. Las dos mitades
--     del negocio se leen igual.
--  b) numero_factura es nuevo: la entidad no lo tenia. Es el respaldo
--     legal del gasto; sin el, la compra no es auditable.
--  c) UNIQUE (id_proveedor, numero_factura) evita cargar dos veces la
--     misma factura del mismo proveedor. Admite NULL porque en
--     PostgreSQL varios NULL no colisionan en un indice unico: una
--     compra puede registrarse antes de recibir la factura.
--  d) detalle_compra usa ON DELETE CASCADE: un detalle no tiene sentido
--     sin su compra. Difiere de detalle_venta, que no cascadea, porque
--     una venta es un hecho contable que no deberia poder borrarse;
--     una orden de compra mal cargada si.
--  e) Los tipos replican exactamente los de ventas / detalle_venta:
--     numeric(10,2) para importes, integer para cantidades.
--
--  IMPACTO EN BACKEND
--  ------------------
--    models/Compra.java        -> renombrar costoTotal a montoTotal,
--                                 agregar numeroFactura, subtotal y
--                                 descuento
--    models/DetalleCompra.java -> sin cambios (id, compra, producto,
--                                 cantidad, precioUnitario, subtotal)
--    dto/CompraRequest.java, CompraResponse.java -> reflejar lo anterior
--    frontend                  -> falta la pagina /dashboard/compras.
--                                 Los modales CompraModal.tsx y
--                                 DetalleCompraModal.tsx ya existen.
--
--  SEGURIDAD
--  ---------
--  Solo crea objetos nuevos. No modifica ni elimina nada existente.
--  Todo dentro de una transaccion.
--
-- =====================================================================

BEGIN;

-- =====================================================================
-- TABLA: compras
-- =====================================================================
CREATE TABLE compras (
    id                   BIGSERIAL      PRIMARY KEY,
    id_proveedor         BIGINT         NOT NULL,
    id_usuario           BIGINT,
    numero_factura       VARCHAR(50),
    fecha_compra         TIMESTAMP      NOT NULL DEFAULT now(),
    subtotal             NUMERIC(10,2)  NOT NULL,
    descuento            NUMERIC(10,2)           DEFAULT 0,
    monto_total          NUMERIC(10,2)  NOT NULL,
    estado               VARCHAR(20)    NOT NULL DEFAULT 'PENDIENTE',
    notas                VARCHAR(500),
    fecha_actualizacion  TIMESTAMP               DEFAULT now(),

    CONSTRAINT fk_compras_proveedor
        FOREIGN KEY (id_proveedor) REFERENCES proveedores (id),

    CONSTRAINT fk_compras_usuario
        FOREIGN KEY (id_usuario) REFERENCES users (id),

    -- Valores del enum EstadoCompra del backend
    CONSTRAINT chk_compras_estado
        CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'EN_TRANSITO',
                          'RECIBIDA', 'CANCELADA')),

    CONSTRAINT chk_compras_importes
        CHECK (subtotal >= 0 AND monto_total >= 0 AND COALESCE(descuento, 0) >= 0),

    -- La misma factura de un proveedor no puede cargarse dos veces.
    -- Varios NULL no colisionan: se puede registrar antes de tener factura.
    CONSTRAINT uq_compras_proveedor_factura
        UNIQUE (id_proveedor, numero_factura)
);

COMMENT ON TABLE compras IS
    'Compras a proveedores. Al pasar a estado RECIBIDA, el backend aumenta el stock en inventario.';

COMMENT ON COLUMN compras.id_usuario IS
    'Empleado que registro la compra.';

COMMENT ON COLUMN compras.numero_factura IS
    'Numero de factura del proveedor. Respaldo legal del gasto.';


-- =====================================================================
-- TABLA: detalle_compra
-- =====================================================================
CREATE TABLE detalle_compra (
    id              BIGSERIAL      PRIMARY KEY,
    id_compra       BIGINT         NOT NULL,
    id_producto     BIGINT         NOT NULL,
    cantidad        INTEGER        NOT NULL,
    precio_unitario NUMERIC(10,2)  NOT NULL,
    subtotal        NUMERIC(10,2)  NOT NULL,

    CONSTRAINT fk_detalle_compra_compra
        FOREIGN KEY (id_compra) REFERENCES compras (id) ON DELETE CASCADE,

    CONSTRAINT fk_detalle_compra_producto
        FOREIGN KEY (id_producto) REFERENCES productos (id),

    CONSTRAINT chk_detalle_compra_cantidad
        CHECK (cantidad > 0),

    CONSTRAINT chk_detalle_compra_importes
        CHECK (precio_unitario >= 0 AND subtotal >= 0),

    -- Un producto no puede repetirse dentro de la misma compra:
    -- se suma la cantidad en una sola linea.
    CONSTRAINT uq_detalle_compra_producto
        UNIQUE (id_compra, id_producto)
);

COMMENT ON COLUMN detalle_compra.precio_unitario IS
    'Costo unitario pagado al proveedor en esta compra. Es el dato historico real del costo.';


-- =====================================================================
-- INDICES SOBRE CLAVES FORANEAS
-- =====================================================================
-- Misma nomenclatura que el resto del esquema: idx_<tabla>_<columna>.
-- No se indexa detalle_compra.id_compra porque ya es la columna
-- principal de uq_detalle_compra_producto (id_compra, id_producto).

CREATE INDEX idx_compras_proveedor        ON compras        (id_proveedor);
CREATE INDEX idx_compras_usuario          ON compras        (id_usuario);
CREATE INDEX idx_compras_fecha            ON compras        (fecha_compra);
CREATE INDEX idx_detalle_compra_producto  ON detalle_compra (id_producto);

COMMIT;

-- =====================================================================
--  FIN
--  El esquema pasa de 23 a 25 tablas.
--
--  Siguiente y ultimo: 06_nomenclatura.sql
--    correo -> email (clientes, proveedores, transportadoras)
--    celular -> telefono (clientes)
--    tipo -> tipo_imagen / tipo_movimiento / tipo_cliente
-- =====================================================================
