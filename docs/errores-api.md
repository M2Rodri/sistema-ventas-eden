# Errores de la API

Todas las rutas de la API (`/api/v1/...`) responden los errores con el mismo formato:

```json
{
  "error": {
    "codigo": "VENTA_NO_ENCONTRADA",
    "mensaje": "Venta no encontrada con ID: 999",
    "campos": { "campo": "detalle" }
  }
}
```

- `codigo`: identificador estable del error, para que el programa decida qué hacer.
- `mensaje`: texto para el usuario; el frontend web y la app lo muestran tal cual.
- `campos`: solo en los errores de validación de campos (`VALIDACION`); trae el detalle de cada campo.

El formato es el mismo para los errores de los controladores, de la validación, de la seguridad (401 y 403) y de las rutas
que no existen. Todas las respuestas de error llevan las cabeceras CORS, para que el navegador pueda leer el código.

## Códigos HTTP

| HTTP | Significado |
|---|---|
| 400 | Petición mal formada o campo inválido |
| 401 | Sin sesión (o sesión vencida) |
| 403 | La sesión no tiene el rol que pide la ruta |
| 404 | El recurso (o la ruta) no existe; también al modificarlo con PUT, PATCH o DELETE |
| 405 | El método HTTP no está permitido en esa ruta |
| 409 | La operación choca con el estado actual (compra ya confirmada, venta ya cancelada, dato duplicado) |
| 422 | La petición es válida pero viola una regla de negocio (stock insuficiente, pago mayor al saldo) |
| 500 | Error interno: sin detalles internos ni traza (el detalle queda solo en el registro del servidor) |

## Tabla de errores

### Errores globales (cualquier ruta)

| HTTP | Código de error | Cuándo ocurre | Ejemplo de `mensaje` |
|---|---|---|---|
| 400 | `VALIDACION` | Un campo del cuerpo no cumple su regla. Trae `campos`. | `El nombre es obligatorio. El precio debe ser mayor a 0` |
| 400 | `PETICION_INVALIDA` | JSON mal formado, tipo de parámetro incorrecto, falta un parámetro o el archivo supera el tamaño máximo | `La petición está mal formada o le falta algún dato` |
| 401 | `NO_AUTENTICADO` | No hay sesión o el token es inválido o venció | `No autenticado o sesión inválida` |
| 401 | `CREDENCIALES_INVALIDAS` | Inicio de sesión con usuario inexistente, contraseña incorrecta o cuenta desactivada | `Credenciales inválidas` |
| 403 | `SIN_PERMISO` | Un EMPLEADO pide algo solo de ADMIN (por ejemplo `/api/v1/reportes/financiero`) | `No tenés permiso para esta acción` |
| 404 | `RUTA_NO_ENCONTRADA` | La ruta no existe: sin `/api/v1` o inexistente | `Ruta no encontrada. Las rutas de la API van con el prefijo /api/v1` |
| 405 | `METODO_NO_PERMITIDO` | Se usa un método HTTP que la ruta no acepta | `El método HTTP no está permitido en esta ruta` |
| 409 | `CONFLICTO_DATOS` | La base rechaza el dato por un duplicado o porque está en uso y ninguna regla lo detectó antes | `La operación choca con datos que ya existen o están en uso` |
| 415 | `TIPO_NO_SOPORTADO` | El cuerpo no es JSON (o el tipo de contenido no corresponde) | `El tipo de contenido enviado no está soportado` |
| 500 | `ERROR_INTERNO` | Cualquier falla no prevista | `Ocurrió un error inesperado. Intentá de nuevo en un momento.` |

### 400: dato inválido que no es de campo

| HTTP | Código de error | Cuándo ocurre | Ejemplo de `mensaje` |
|---|---|---|---|
| 400 | `CLIENTE_REQUERIDO` | Venta sin cliente registrado ni nombre de cliente de mostrador | `Debe proporcionar un cliente registrado (idCliente) o el nombre del cliente de mostrador` |
| 400 | `CIUDAD_REQUERIDA` | Venta por transportadora sin ciudad | `La ciudad es obligatoria para el envío por transportadora` |
| 400 | `MONTO_INVALIDO` | Monto pagado negativo | `El monto pagado no puede ser negativo` |
| 400 | `ESTADO_INVALIDO` | Filtro por un estado de venta o de pago que no existe | `Estado de venta inválido: XYZ` |
| 400 | `ARCHIVO_VACIO` | Se sube una imagen o comprobante sin contenido | `El archivo de imagen no puede estar vacío.` |
| 400 | `ARCHIVO_MUY_GRANDE` | La imagen supera el tamaño máximo | `El archivo excede el tamaño máximo permitido de N MB` |
| 400 | `IMAGEN_INVALIDA` | El archivo no es una imagen JPG, PNG o WebP real | `Tipo de archivo no permitido. Solo se aceptan imágenes JPG, PNG o WebP` |
| 400 | `STOCK_MINIMO_INVALIDO` | Stock mínimo negativo | `El stock mínimo no puede ser negativo` |
| 400 | `MOVIMIENTO_INVALIDO` | Tipo de movimiento de inventario que no es ENTRADA ni SALIDA | `Tipo de movimiento inválido. Use 'ENTRADA' o 'SALIDA'` |
| 400 | `ROL_REQUERIDO` | Usuario sin rol | `El rol es obligatorio` |
| 400 | `ROL_INVALIDO` | Rol que no existe | `Rol no válido: SUPERVISOR` |
| 400 | `PASSWORD_REQUERIDA` | Alta de usuario sin contraseña | `La contraseña es obligatoria al crear un usuario` |

### 404: el recurso no existe (también en PUT, PATCH y DELETE)

| HTTP | Código de error | Cuándo ocurre | Ejemplo de `mensaje` |
|---|---|---|---|
| 404 | `VENTA_NO_ENCONTRADA` | Se pide, cancela, entrega o corrige una venta que no existe | `Venta no encontrada con ID: 999` |
| 404 | `PAGO_NO_ENCONTRADO` | Se pide un pago o se le adjunta comprobante y no existe | `Pago no encontrado con ID: 999` |
| 404 | `COMPROBANTE_NO_ENCONTRADO` | La venta no tiene comprobante o el comprobante no existe | `No existe comprobante para la venta con ID: 999` |
| 404 | `PRODUCTO_NO_ENCONTRADO` | Producto inexistente (en consulta, edición, baja, venta, compra o inventario) | `Producto no encontrado con ID: 999` |
| 404 | `CATEGORIA_NO_ENCONTRADA` | Categoría inexistente | `Categoría no encontrada con ID: 999` |
| 404 | `CLIENTE_NO_ENCONTRADO` | Cliente inexistente | `Cliente no encontrado con ID: 999` |
| 404 | `PROVEEDOR_NO_ENCONTRADO` | Proveedor inexistente | `Proveedor no encontrado con ID: 999` |
| 404 | `COMPRA_NO_ENCONTRADA` | Compra inexistente (consulta, edición, confirmación o cancelación) | `Compra no encontrada con ID: 999` |
| 404 | `INVENTARIO_NO_ENCONTRADO` | El producto no tiene inventario o el registro no existe | `No existe inventario para el producto con ID: 999` |
| 404 | `IMAGEN_NO_ENCONTRADA` | Imagen inexistente o que no es del producto indicado | `Imagen no encontrada con ID: 999` |
| 404 | `USUARIO_NO_ENCONTRADO` | Usuario inexistente | `Usuario no encontrado con ID: 999` |

### 409: choca con el estado actual

| HTTP | Código de error | Cuándo ocurre | Ejemplo de `mensaje` |
|---|---|---|---|
| 409 | `COMPRA_YA_CONFIRMADA` | Se confirma una compra que ya está confirmada | `Solo se puede confirmar una compra que esté sin confirmar. Estado actual: CONFIRMADA` |
| 409 | `COMPRA_NO_CONFIRMABLE` | Se confirma una compra cancelada | `Solo se puede confirmar una compra que esté sin confirmar. Estado actual: CANCELADA` |
| 409 | `COMPRA_NO_EDITABLE` | Se edita una compra que ya no está sin confirmar | `Solo se puede editar una compra que esté sin confirmar. Estado actual: CONFIRMADA` |
| 409 | `COMPRA_NO_CANCELABLE` | Se cancela una compra que ya no está sin confirmar | `No se puede cancelar una compra en estado: CONFIRMADA` |
| 409 | `FACTURA_DUPLICADA` | Otra compra del mismo proveedor ya tiene ese número de factura | `Ya existe una compra con esa factura para este proveedor` |
| 409 | `VENTA_YA_CANCELADA` | Se cancela una venta ya cancelada | `Esta venta ya está cancelada` |
| 409 | `VENTA_CANCELADA` | Se entrega, se corrige o se cobra una venta cancelada | `No se puede entregar una venta cancelada` |
| 409 | `VENTA_YA_ENTREGADA` | Se marca como entregada una venta ya entregada | `Esta venta ya está marcada como entregada` |
| 409 | `VENTA_ENTREGA_PENDIENTE` | Se corrige la entrega de una venta que todavía está pendiente | `No hay nada que corregir: la venta está pendiente de entrega` |
| 409 | `COMPROBANTE_YA_EXISTE` | La venta ya tiene comprobante | `Esta venta ya tiene un comprobante asociado` |
| 409 | `COMPROBANTE_YA_ANULADO` | Se anula un comprobante ya anulado | `Este comprobante ya está anulado` |
| 409 | `SKU_DUPLICADO` | Otro producto ya usa ese SKU | `Ya existe un producto con el SKU: CAM-001` |
| 409 | `CATEGORIA_DUPLICADA` | Otra categoría ya tiene ese nombre | `Ya existe una categoría con el nombre: Camas` |
| 409 | `CATEGORIA_CON_PRODUCTOS` | Se elimina una categoría que tiene productos | `No se puede eliminar una categoría con productos asociados` |
| 409 | `EMAIL_DUPLICADO` | Otro cliente ya usa ese email | `Ya existe un cliente con el email: ana@correo.com` |
| 409 | `NIT_CI_DUPLICADO` | Otro cliente ya tiene ese NIT o CI | `El NIT o CI ya está registrado para otro cliente` |
| 409 | `NIT_DUPLICADO` | Otro proveedor ya tiene ese NIT | `Ya existe un proveedor con el NIT: 1020304` |
| 409 | `USUARIO_DUPLICADO` | Ese nombre de usuario ya está registrado | `El usuario ya está registrado: ana.perez` |
| 409 | `INVENTARIO_YA_EXISTE` | Se crea inventario para un producto que ya lo tiene | `Ya existe inventario para este producto` |

### 422: regla de negocio violada

| HTTP | Código de error | Cuándo ocurre | Ejemplo de `mensaje` |
|---|---|---|---|
| 422 | `STOCK_INSUFICIENTE` | Se vende, o se ajusta con una salida manual, más de lo que hay en inventario | `Stock insuficiente para el producto 'Cama 2 plazas'` (en el ajuste: `Stock insuficiente. Disponible: 5`) |
| 422 | `PRODUCTO_NO_DISPONIBLE` | Se vende un producto dado de baja | `El producto 'Cama 2 plazas' no está disponible` |
| 422 | `PRECIO_EXCEDE_CATALOGO` | Se vende por encima del precio de catálogo | `No se puede vender 'Cama 2 plazas' por encima del precio de catálogo (máximo Bs. 500.00)` |
| 422 | `PRECIO_BAJO_COSTO` | Se vende por debajo del costo | `No se puede vender 'Cama 2 plazas' por debajo del costo (Bs. 300.00)` |
| 422 | `PAGO_EXCEDE_TOTAL` | El monto pagado al registrar la venta supera el total | `El monto pagado no puede superar el total de la venta` |
| 422 | `PAGO_EXCEDE_SALDO` | Un cobro posterior supera el saldo pendiente | `El monto supera el saldo pendiente de la venta (Bs. 100.00)` |
| 422 | `VENTA_EN_TIENDA_SIN_ENTREGA` | Se corrigen o editan datos de entrega de una venta en tienda | `Una venta en tienda no tiene datos de entrega` |
| 422 | `CATEGORIA_INACTIVA` | Se crea un producto en una categoría inactiva | `No se puede crear un producto en una categoría inactiva` |
| 422 | `CATEGORIA_NO_PERMITIDA` | Se crea o renombra una categoría fuera de las cuatro fijas (más la oculta) | `Solo existen las categorías: Camas, Colchones, Almohadas, Accesorios y Muebles de dormitorio` |
| 422 | `ULTIMO_ADMINISTRADOR` | Se desactiva al único administrador activo | `Es el único administrador activo. No se puede desactivar.` |

## Ejemplos completos

Recurso inexistente al modificar (`PATCH /api/v1/ventas/999999/cancelar`, 404):

```json
{ "error": { "codigo": "VENTA_NO_ENCONTRADA", "mensaje": "Venta no encontrada con ID: 999999" } }
```

Validación de campos (`POST /api/v1/productos` con datos inválidos, 400):

```json
{
  "error": {
    "codigo": "VALIDACION",
    "mensaje": "El nombre es obligatorio. El precio de venta debe ser mayor a 0",
    "campos": {
      "nombre": "El nombre es obligatorio",
      "precioVenta": "El precio de venta debe ser mayor a 0"
    }
  }
}
```

Regla de negocio (`POST /api/v1/ventas` con más cantidad que stock, 422):

```json
{ "error": { "codigo": "STOCK_INSUFICIENTE", "mensaje": "Stock insuficiente para el producto 'Cama 2 plazas'" } }
```

Sin permiso (`GET /api/v1/reportes/financiero` con un EMPLEADO, 403):

```json
{ "error": { "codigo": "SIN_PERMISO", "mensaje": "No tenés permiso para esta acción" } }
```

## Cómo se implementa

- Los servicios lanzan excepciones propias (`com.mitienda.ecommerce.exception`): `RecursoNoEncontradoException` (404),
  `ConflictoEstadoException` (409), `ReglaNegocioException` (422), `PeticionInvalidaException` (400) y
  `CredencialesInvalidasException` (401). Cada una lleva su código de error.
- Los controladores no atrapan errores: lo hace `GlobalExceptionHandler`, el único lugar donde una excepción se
  convierte en respuesta.
- Los 401 y 403 de las reglas por ruta y el 404 de las rutas sin `/api/v1` los escriben `SecurityConfig` y
  `RutasSinVersionFilter` con el mismo formato (`RespuestaError`).
- Los módulos fuera de alcance (envíos, transportadoras, promociones, configuración, auditorías, mensajes de contacto y
  multimedia) están apagados y responden 404.
- Pruebas: `FormatoErrorApiTest` (cada código HTTP y CORS), `RecursoInexistenteApiTest` (404 en GET, PUT, PATCH y
  DELETE de cada módulo) y `ErroresNegocioTest` (409, 422 y 400 de cada regla).
