# Rutas de la API: anteriores y nuevas

Todas las rutas de la API van con el prefijo `/api/v1`. Una ruta con `/api/` sin la versión responde 404.

- **versionada**: sigue existiendo, ahora con `/api/v1`.
- **eliminada**: se quitó del sistema (no la usaba ninguna pantalla o era una ruta de prueba).
- **apagada (404)**: módulo fuera de alcance; su código sigue en el repositorio pero la ruta no responde.

## Auditoria

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/auditorias` | — | apagada (404) |
| GET | `/api/auditorias/{id}` | — | apagada (404) |
| GET | `/api/auditorias/usuario/{idUsuario}` | — | apagada (404) |
| GET | `/api/auditorias/tabla/{tablaAfectada}` | — | apagada (404) |
| GET | `/api/auditorias/accion/{accion}` | — | apagada (404) |
| GET | `/api/auditorias/fechas` | — | apagada (404) |
| GET | `/api/auditorias/ultimas` | — | apagada (404) |
| GET | `/api/auditorias/del-dia` | — | apagada (404) |

## Auth

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| POST | `/api/auth/login` | `/api/v1/auth/login` | versionada |
| GET | `/api/auth/test` | — | eliminada |

## Categoria

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/categorias` | `/api/v1/categorias` | versionada |
| GET | `/api/categorias/activas` | `/api/v1/categorias/activas` | versionada |
| GET | `/api/categorias/{id}` | `/api/v1/categorias/{id}` | versionada |
| POST | `/api/categorias` | `/api/v1/categorias` | versionada |
| PUT | `/api/categorias/{id}` | `/api/v1/categorias/{id}` | versionada |
| DELETE | `/api/categorias/{id}` | `/api/v1/categorias/{id}` | versionada |
| PATCH | `/api/categorias/{id}/toggle-status` | `/api/v1/categorias/{id}/toggle-status` | versionada |
| GET | `/api/categorias/estadisticas` | `/api/v1/categorias/estadisticas` | versionada |

## Cliente

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/clientes` | `/api/v1/clientes` | versionada |
| GET | `/api/clientes/con-estadisticas` | `/api/v1/clientes/con-estadisticas` | versionada |
| GET | `/api/clientes/{id}` | `/api/v1/clientes/{id}` | versionada |
| POST | `/api/clientes` | `/api/v1/clientes` | versionada |
| PUT | `/api/clientes/{id}` | `/api/v1/clientes/{id}` | versionada |
| GET | `/api/clientes/buscar` | `/api/v1/clientes/buscar` | versionada |
| GET | `/api/clientes/{id}/historial-compras` | `/api/v1/clientes/{id}/historial-compras` | versionada |
| GET | `/api/clientes/{id}/historial-compras/filtrado` | `/api/v1/clientes/{id}/historial-compras/filtrado` | versionada |

## Compra

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/compras` | `/api/v1/compras` | versionada |
| GET | `/api/compras/{id}` | `/api/v1/compras/{id}` | versionada |
| POST | `/api/compras` | `/api/v1/compras` | versionada |
| PUT | `/api/compras/{id}` | `/api/v1/compras/{id}` | versionada |
| PATCH | `/api/compras/{id}/estado` | `/api/v1/compras/{id}/estado` | versionada |
| PATCH | `/api/compras/{id}/recibir` | `/api/v1/compras/{id}/recibir` | versionada |
| PATCH | `/api/compras/{id}/cancelar` | `/api/v1/compras/{id}/cancelar` | versionada |

## Comprobante

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/comprobantes` | — | eliminada |
| GET | `/api/comprobantes/{id}` | — | eliminada |
| GET | `/api/comprobantes/numero/{numeroComprobante}` | — | eliminada |
| GET | `/api/comprobantes/venta/{idVenta}` | `/api/v1/comprobantes/venta/{idVenta}` | versionada |
| POST | `/api/comprobantes` | `/api/v1/comprobantes` | versionada |
| PATCH | `/api/comprobantes/{id}/anular` | — | eliminada |
| GET | `/api/comprobantes/activos` | — | eliminada |
| GET | `/api/comprobantes/anulados` | — | eliminada |
| GET | `/api/comprobantes/tipo/{tipo}` | — | eliminada |
| GET | `/api/comprobantes/fechas` | — | eliminada |
| GET | `/api/comprobantes/ultimos` | — | eliminada |
| GET | `/api/comprobantes/estadisticas` | — | eliminada |

## Configuracion

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/configuracion/negocio` | — | apagada (404) |
| GET | `/api/configuracion` | — | apagada (404) |
| GET | `/api/configuracion/{id}` | — | apagada (404) |
| GET | `/api/configuracion/clave/{clave}` | — | apagada (404) |
| GET | `/api/configuracion/valor/{clave}` | — | apagada (404) |
| POST | `/api/configuracion` | — | apagada (404) |
| PUT | `/api/configuracion/{id}` | — | apagada (404) |
| PATCH | `/api/configuracion/clave/{clave}` | — | apagada (404) |
| DELETE | `/api/configuracion/{id}` | — | apagada (404) |
| POST | `/api/configuracion/inicializar` | — | apagada (404) |

## Dashboard

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/dashboard/estadisticas` | `/api/v1/dashboard/estadisticas` | versionada |
| GET | `/api/dashboard/ventas-semanal` | `/api/v1/dashboard/ventas-semanal` | versionada |

## Envio

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/envios` | — | apagada (404) |
| GET | `/api/envios/{id}` | — | apagada (404) |
| GET | `/api/envios/venta/{idVenta}` | — | apagada (404) |
| GET | `/api/envios/guia/{guiaRemision}` | — | apagada (404) |
| POST | `/api/envios` | — | apagada (404) |
| PUT | `/api/envios/{id}` | — | apagada (404) |
| PATCH | `/api/envios/{id}/estado` | — | apagada (404) |
| PATCH | `/api/envios/{id}/entregar` | — | apagada (404) |
| GET | `/api/envios/estado/{estado}` | — | apagada (404) |
| GET | `/api/envios/transportadora/{idTransportadora}` | — | apagada (404) |
| GET | `/api/envios/pendientes` | — | apagada (404) |
| GET | `/api/envios/en-camino` | — | apagada (404) |
| GET | `/api/envios/por-entregar` | — | apagada (404) |
| GET | `/api/envios/estadisticas` | — | apagada (404) |

## ImagenProducto

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/imagenes-producto/producto/{idProducto}` | `/api/v1/imagenes-producto/producto/{idProducto}` | versionada |
| POST | `/api/imagenes-producto/producto/{idProducto}` | `/api/v1/imagenes-producto/producto/{idProducto}` | versionada |
| DELETE | `/api/imagenes-producto/{idImagen}` | `/api/v1/imagenes-producto/{idImagen}` | versionada |
| PUT | `/api/imagenes-producto/{idImagen}/principal/{idProducto}` | `/api/v1/imagenes-producto/{idImagen}/principal/{idProducto}` | versionada |

## Inventario

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/inventario` | `/api/v1/inventario` | versionada |
| GET | `/api/inventario/catalogo` | `/api/v1/inventario/catalogo` | versionada |
| GET | `/api/inventario/{id}` | `/api/v1/inventario/{id}` | versionada |
| GET | `/api/inventario/producto/{idProducto}` | `/api/v1/inventario/producto/{idProducto}` | versionada |
| POST | `/api/inventario` | `/api/v1/inventario` | versionada |
| PUT | `/api/inventario/{id}` | `/api/v1/inventario/{id}` | versionada |
| POST | `/api/inventario/ajustar` | `/api/v1/inventario/ajustar` | versionada |
| GET | `/api/inventario/producto/{idProducto}/historial` | `/api/v1/inventario/producto/{idProducto}/historial` | versionada |
| GET | `/api/inventario/ajustes/ultimos` | `/api/v1/inventario/ajustes/ultimos` | versionada |
| GET | `/api/inventario/stock-bajo` | `/api/v1/inventario/stock-bajo` | versionada |
| GET | `/api/inventario/sin-stock` | `/api/v1/inventario/sin-stock` | versionada |
| GET | `/api/inventario/verificar-disponibilidad` | `/api/v1/inventario/verificar-disponibilidad` | versionada |
| GET | `/api/inventario/alertas/pendientes` | `/api/v1/inventario/alertas/pendientes` | versionada |
| GET | `/api/inventario/estadisticas` | `/api/v1/inventario/estadisticas` | versionada |

## MensajeContacto

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| POST | `/api/mensajes-contacto` | — | apagada (404) |
| GET | `/api/mensajes-contacto` | — | apagada (404) |
| GET | `/api/mensajes-contacto/pendientes` | — | apagada (404) |
| GET | `/api/mensajes-contacto/pendientes/cantidad` | — | apagada (404) |
| PATCH | `/api/mensajes-contacto/{id}/atender` | — | apagada (404) |
| DELETE | `/api/mensajes-contacto/{id}` | — | apagada (404) |

## MultimediaProducto

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| POST | `/api/multimedia-productos/subir-modelo/{productoId}` | — | apagada (404) |
| POST | `/api/multimedia-productos` | — | apagada (404) |
| GET | `/api/multimedia-productos/producto/{productoId}` | — | apagada (404) |
| PUT | `/api/multimedia-productos/{id}` | — | apagada (404) |
| DELETE | `/api/multimedia-productos/{id}` | — | apagada (404) |
| GET | `/api/multimedia-productos/ra-habilitados` | — | apagada (404) |
| GET | `/api/multimedia-productos/health` | — | apagada (404) |

## Pago

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/pagos` | `/api/v1/pagos` | versionada |
| GET | `/api/pagos/{id}` | `/api/v1/pagos/{id}` | versionada |
| POST | `/api/pagos` | `/api/v1/pagos` | versionada |
| POST | `/api/pagos/{id}/comprobante` | `/api/v1/pagos/{id}/comprobante` | versionada |
| GET | `/api/pagos/venta/{ventaId}` | `/api/v1/pagos/venta/{ventaId}` | versionada |
| GET | `/api/pagos/estado/{estado}` | `/api/v1/pagos/estado/{estado}` | versionada |
| GET | `/api/pagos/estadisticas` | `/api/v1/pagos/estadisticas` | versionada |

## Producto

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/productos` | `/api/v1/productos` | versionada |
| GET | `/api/productos/activos` | `/api/v1/productos/activos` | versionada |
| GET | `/api/productos/{id}` | `/api/v1/productos/{id}` | versionada |
| GET | `/api/productos/sku/{sku}` | `/api/v1/productos/sku/{sku}` | versionada |
| GET | `/api/productos/categoria/{categoriaId}` | `/api/v1/productos/categoria/{categoriaId}` | versionada |
| GET | `/api/productos/buscar` | `/api/v1/productos/buscar` | versionada |
| POST | `/api/productos` | `/api/v1/productos` | versionada |
| PATCH | `/api/productos/{id}/stock-minimo` | `/api/v1/productos/{id}/stock-minimo` | versionada |
| PUT | `/api/productos/{id}` | `/api/v1/productos/{id}` | versionada |
| DELETE | `/api/productos/{id}` | `/api/v1/productos/{id}` | versionada |
| PATCH | `/api/productos/{id}/toggle-status` | `/api/v1/productos/{id}/toggle-status` | versionada |
| GET | `/api/productos/estadisticas` | `/api/v1/productos/estadisticas` | versionada |

## Promocion

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/promociones` | — | apagada (404) |
| GET | `/api/promociones/activas` | — | apagada (404) |
| GET | `/api/promociones/vigentes` | — | apagada (404) |
| GET | `/api/promociones/{id}` | — | apagada (404) |
| POST | `/api/promociones` | — | apagada (404) |
| PUT | `/api/promociones/{id}` | — | apagada (404) |
| DELETE | `/api/promociones/{id}` | — | apagada (404) |
| PATCH | `/api/promociones/{id}/toggle-status` | — | apagada (404) |
| GET | `/api/promociones/estadisticas` | — | apagada (404) |

## Proveedor

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/proveedores` | `/api/v1/proveedores` | versionada |
| GET | `/api/proveedores/activos` | `/api/v1/proveedores/activos` | versionada |
| GET | `/api/proveedores/{id}` | `/api/v1/proveedores/{id}` | versionada |
| POST | `/api/proveedores` | `/api/v1/proveedores` | versionada |
| PUT | `/api/proveedores/{id}` | `/api/v1/proveedores/{id}` | versionada |
| PATCH | `/api/proveedores/{id}/toggle-status` | `/api/v1/proveedores/{id}/toggle-status` | versionada |
| GET | `/api/proveedores/buscar` | `/api/v1/proveedores/buscar` | versionada |

## Reporte

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/reportes/ventas` | `/api/v1/reportes/ventas` | versionada |
| GET | `/api/reportes/productos-mas-vendidos` | `/api/v1/reportes/productos-mas-vendidos` | versionada |
| GET | `/api/reportes/clientes-frecuentes` | `/api/v1/reportes/clientes-frecuentes` | versionada |
| GET | `/api/reportes/inventario-valorizado` | `/api/v1/reportes/inventario-valorizado` | versionada |
| GET | `/api/reportes/ventas-por-categoria` | `/api/v1/reportes/ventas-por-categoria` | versionada |
| GET | `/api/reportes/ventas-por-metodo-pago` | `/api/v1/reportes/ventas-por-metodo-pago` | versionada |
| GET | `/api/reportes/cuentas-por-cobrar` | `/api/v1/reportes/cuentas-por-cobrar` | versionada |
| GET | `/api/reportes/financiero` | `/api/v1/reportes/financiero` | versionada |

## Salud

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/v1/salud` | `/api/v1/salud` | sin cambio |

## Transportadora

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/transportadoras` | — | apagada (404) |
| GET | `/api/transportadoras/activas` | — | apagada (404) |
| GET | `/api/transportadoras/{id}` | — | apagada (404) |
| POST | `/api/transportadoras` | — | apagada (404) |
| PUT | `/api/transportadoras/{id}` | — | apagada (404) |
| DELETE | `/api/transportadoras/{id}` | — | apagada (404) |
| PATCH | `/api/transportadoras/{id}/toggle-status` | — | apagada (404) |
| GET | `/api/transportadoras/estadisticas` | — | apagada (404) |

## Usuario

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/usuarios` | `/api/v1/usuarios` | versionada |
| GET | `/api/usuarios/{id}` | `/api/v1/usuarios/{id}` | versionada |
| POST | `/api/usuarios` | `/api/v1/usuarios` | versionada |
| PUT | `/api/usuarios/{id}` | `/api/v1/usuarios/{id}` | versionada |
| DELETE | `/api/usuarios/{id}` | `/api/v1/usuarios/{id}` | versionada |
| PATCH | `/api/usuarios/{id}/toggle-status` | `/api/v1/usuarios/{id}/toggle-status` | versionada |
| GET | `/api/usuarios/activos` | `/api/v1/usuarios/activos` | versionada |
| GET | `/api/usuarios/rol/{role}` | `/api/v1/usuarios/rol/{role}` | versionada |
| GET | `/api/usuarios/estadisticas` | `/api/v1/usuarios/estadisticas` | versionada |

## Venta

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/ventas` | `/api/v1/ventas` | versionada |
| GET | `/api/ventas/{id}` | `/api/v1/ventas/{id}` | versionada |
| POST | `/api/ventas` | `/api/v1/ventas` | versionada |
| PATCH | `/api/ventas/{id}/cancelar` | `/api/v1/ventas/{id}/cancelar` | versionada |
| PATCH | `/api/ventas/{id}/entregar` | `/api/v1/ventas/{id}/entregar` | versionada |
| PATCH | `/api/ventas/{id}/deshacer-entrega` | `/api/v1/ventas/{id}/deshacer-entrega` | versionada |
| PATCH | `/api/ventas/{id}/datos-entrega` | `/api/v1/ventas/{id}/datos-entrega` | versionada |
| GET | `/api/ventas/cliente/{clienteId}` | `/api/v1/ventas/cliente/{clienteId}` | versionada |
| GET | `/api/ventas/estado/{estado}` | `/api/v1/ventas/estado/{estado}` | versionada |
| GET | `/api/ventas/del-dia` | `/api/v1/ventas/del-dia` | versionada |
| GET | `/api/ventas/ultimas` | `/api/v1/ventas/ultimas` | versionada |
| GET | `/api/ventas/fechas` | `/api/v1/ventas/fechas` | versionada |
| GET | `/api/ventas/total-fechas` | `/api/v1/ventas/total-fechas` | versionada |
| GET | `/api/ventas/estadisticas` | `/api/v1/ventas/estadisticas` | versionada |

