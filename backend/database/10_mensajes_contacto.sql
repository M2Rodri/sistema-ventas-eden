-- =====================================================================
--  FASE 10 - MENSAJES DE CONTACTO DE LA TIENDA
--  Proyecto : Sistema de Ventas - Muebleria Eden
--  Base     : muebleria_eden_db
--  Fecha    : 2026-08-23
-- =====================================================================
--
--  QUE HACE
--  --------
--  Crea la tabla mensajes_contacto, donde se guardan las consultas que
--  los visitantes envian desde la tienda.
--
--  POR QUE
--  -------
--  El formulario de contacto simulaba el envio: mostraba "mensaje
--  enviado con exito" tras dos segundos y no hacia ninguna llamada al
--  backend. Como la tienda no tiene carrito ni pedidos, ese formulario
--  es la UNICA via que tiene un cliente para comunicarse con el negocio,
--  asi que era el punto mas critico de la vitrina y estaba cortado.
--
--  Se resuelve guardando el mensaje en la base y mostrandolo en el
--  sistema, en lugar de enviarlo por correo. Motivos:
--    - no depende de un servicio externo ni de credenciales SMTP
--    - queda registro consultable y se puede marcar como atendido
--    - es coherente con el resto del sistema, que administra todo desde
--      el dashboard
--
--  DECISIONES DE DISEÑO
--  --------------------
--  a) El endpoint que la alimenta es publico (la tienda no tiene login),
--     por eso los campos tienen limites de longitud y el estado no lo
--     define quien escribe.
--  b) 'atendido' + 'fecha_atencion' + 'id_usuario_atiende' permiten
--     llevar el seguimiento de quien respondio y cuando, igual que se
--     hace con las alertas de inventario.
--  c) 'ip_origen' queda registrada como en auditorias: ante un abuso del
--     formulario publico, es lo unico que permite identificar el origen.
--
--  SEGURIDAD
--  ---------
--  Solo crea una tabla nueva. No modifica nada existente.
--
-- =====================================================================

BEGIN;

CREATE TABLE mensajes_contacto (
    id                  BIGSERIAL     PRIMARY KEY,
    nombre              VARCHAR(100)  NOT NULL,
    email               VARCHAR(100)  NOT NULL,
    telefono            VARCHAR(20),
    asunto              VARCHAR(150)  NOT NULL,
    mensaje             TEXT          NOT NULL,

    atendido            BOOLEAN       NOT NULL DEFAULT false,
    fecha_atencion      TIMESTAMP,
    id_usuario_atiende  BIGINT,

    ip_origen           VARCHAR(50),
    fecha_envio         TIMESTAMP     NOT NULL DEFAULT now(),

    CONSTRAINT fk_mensajes_contacto_usuario
        FOREIGN KEY (id_usuario_atiende) REFERENCES usuarios (id),

    -- Un mensaje no puede quedar marcado como atendido sin registrar cuándo.
    CONSTRAINT chk_mensajes_contacto_atencion
        CHECK (atendido = false OR fecha_atencion IS NOT NULL)
);

CREATE INDEX idx_mensajes_contacto_usuario  ON mensajes_contacto (id_usuario_atiende);
CREATE INDEX idx_mensajes_contacto_atendido ON mensajes_contacto (atendido);
CREATE INDEX idx_mensajes_contacto_fecha    ON mensajes_contacto (fecha_envio);

COMMENT ON TABLE mensajes_contacto IS
    'Consultas enviadas desde el formulario de la tienda. Se responden por fuera del sistema (teléfono o correo) y acá se marcan como atendidas.';

COMMIT;

-- =====================================================================
--  FIN
--  El esquema pasa de 25 a 26 tablas.
-- =====================================================================
