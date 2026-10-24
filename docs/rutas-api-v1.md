# Rutas de la API: anteriores y nuevas

Todas las rutas de la API van con el prefijo `/api/v1`. Una ruta con `/api/` sin la versión responde 404.

- **versionada**: sigue existiendo, ahora con `/api/v1`.
- **nueva**: se creó después de versionar la API; nunca existió sin `/api/v1`, por eso no tiene ruta anterior.
- **eliminada**: se quitó del sistema (no la usaba ninguna pantalla, era de prueba, o dejó de existir al quitar el estado y la edición de las compras).
- **apagada (404)**: la ruta existe en el código pero no responde (solo la consulta de auditorías).
- Los módulos de envíos, transportadoras, promociones, configuración, mensajes de contacto y multimedia se sacaron del
  repositorio el 24/10/2026; sus rutas (/api/envios, /api/transportadoras, /api/promociones, /api/configuracion,
  /api/mensajes-contacto y /api/multimedia-productos) ya no existen y responden 404.

## Auditoria

El sistema **sí registra** la auditoría: cada acción sensible (crear, editar, cancelar, iniciar sesión, cerrar sesiones...) se guarda en la tabla `auditorias` con el usuario, la fecha y la IP. Lo que quedó fuera del alcance es la **consulta** desde la web o la app, por eso estas rutas están apagadas; los registros se pueden leer directamente en la base.

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
| POST | — | `/api/v1/auth/renovar` | nueva |
| POST | — | `/api/v1/auth/cerrar-sesiones` | nueva |
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
| PUT | `/api/compras/{id}` | — | eliminada |
| PATCH | `/api/compras/{id}/estado` | — | eliminada |
| PATCH | `/api/compras/{id}/recibir` | — | eliminada |
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

## Dashboard

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/dashboard/estadisticas` | `/api/v1/dashboard/estadisticas` | versionada |
| GET | `/api/dashboard/ventas-semanal` | `/api/v1/dashboard/ventas-semanal` | versionada |

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
| PATCH | — | `/api/v1/inventario/alertas/{id}/atender` | nueva |
| PATCH | — | `/api/v1/inventario/producto/{idProducto}/reactivar-alerta` | nueva |
| GET | `/api/inventario/estadisticas` | `/api/v1/inventario/estadisticas` | versionada |

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
| GET | — | `/api/v1/reportes/ventas-por-producto` | nueva |
| GET | `/api/reportes/ventas-por-metodo-pago` | `/api/v1/reportes/ventas-por-metodo-pago` | versionada |
| GET | `/api/reportes/cuentas-por-cobrar` | `/api/v1/reportes/cuentas-por-cobrar` | versionada |
| GET | `/api/reportes/financiero` | `/api/v1/reportes/financiero` | versionada |

## Salud

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/v1/salud` | `/api/v1/salud` | sin cambio de ruta; ahora también consulta la base de datos y responde 503 `BASE_NO_DISPONIBLE` si no contesta |

## Usuario

| Método | Ruta anterior | Ruta nueva | Estado |
|---|---|---|---|
| GET | `/api/usuarios` | `/api/v1/usuarios` | versionada |
| GET | `/api/usuarios/{id}` | `/api/v1/usuarios/{id}` | versionada |
| POST | `/api/usuarios` | `/api/v1/usuarios` | versionada |
| PUT | `/api/usuarios/{id}` | `/api/v1/usuarios/{id}` | versionada |
| DELETE | `/api/usuarios/{id}` | `/api/v1/usuarios/{id}` | versionada |
| PATCH | `/api/usuarios/{id}/toggle-status` | `/api/v1/usuarios/{id}/toggle-status` | versionada |
| POST | — | `/api/v1/usuarios/{id}/cerrar-sesiones` | nueva |
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
| PATCH | — | `/api/v1/ventas/{id}/fecha-limite-pago` | nueva |
| GET | `/api/ventas/cliente/{clienteId}` | `/api/v1/ventas/cliente/{clienteId}` | versionada |
| GET | `/api/ventas/estado/{estado}` | `/api/v1/ventas/estado/{estado}` | versionada |
| GET | `/api/ventas/del-dia` | `/api/v1/ventas/del-dia` | versionada |
| GET | `/api/ventas/ultimas` | `/api/v1/ventas/ultimas` | versionada |
| GET | `/api/ventas/fechas` | `/api/v1/ventas/fechas` | versionada |
| GET | `/api/ventas/total-fechas` | `/api/v1/ventas/total-fechas` | versionada |
| GET | `/api/ventas/estadisticas` | `/api/v1/ventas/estadisticas` | versionada |

