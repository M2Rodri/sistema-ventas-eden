# Sistema de ventas — Mueblería Edén

Sistema de gestión para **Mueblería Edén**, un negocio familiar de venta de
camas, colchones y accesorios de descanso en Santa Cruz de la Sierra, Bolivia.

Proyecto final del Diplomado en Desarrollo Web y Aplicaciones Móviles,
Universidad Autónoma Juan Misael Saracho (Tarija).

## Despliegue en producción

| Parte | URL |
|---|---|
| Frontend | https://sistema-ventas-eden.vercel.app |
| API | https://sistema-ventas-eden.onrender.com |
| Ruta de salud | https://sistema-ventas-eden.onrender.com/api/v1/salud |

Backend en Render (Docker, ver `backend/Dockerfile`), base de datos en Supabase
(perfil `prod`), frontend en Vercel. El backend está en el plan gratuito de
Render: si nadie lo usó en los últimos 15 minutos, el servicio se apaga solo y
la primera petición después de eso tarda alrededor de un minuto en volver a
levantarlo.

## Versionado de la API

Todas las rutas de la API van con el prefijo **`/api/v1`**, por ejemplo
`/api/v1/productos`, `/api/v1/ventas/{id}` o `/api/v1/auth/login`. Una ruta que
empiece con `/api/` pero sin la versión **no existe y responde 404**, con o sin sesión.

- **En alcance y versionadas:** auth, usuarios, productos, categorías, imágenes de producto,
  inventario, ventas, pagos, clientes, compras, proveedores, dashboard, reportes y
  comprobantes de venta (solo `GET /comprobantes/venta/{idVenta}` y `POST /comprobantes`).
- **Fuera de alcance (apagadas, responden 404):** envíos, transportadoras, promociones,
  configuración, consulta de auditorías, mensajes de contacto y multimedia 3D. Su código sigue
  en el repositorio hasta que se eliminen. El registro interno de auditoría (quién hizo cada
  venta, compra, pago y ajuste) sigue funcionando: solo se apagó su ruta de consulta.
- **Acceso por rol:** se define en `SecurityConfig` y se verifica con pruebas automáticas
  (`RutasApiTest`): sin sesión, 401; con un rol sin permiso, 403 (por ejemplo, el EMPLEADO frente
  a `/api/v1/reportes/**`).
- **Ruta de salud:** `/api/v1/salud`, pública.

La lista completa, con la ruta anterior, la nueva y el estado de cada una, está en
[`docs/rutas-api-v1.md`](docs/rutas-api-v1.md). La web y la app móvil usan solo rutas `/api/v1`
(la web, desde una única constante en `frontend/src/lib/api.ts`; la app, en `app/lib/data/`).
No hay colección de Postman en el repositorio.

## Errores de la API

Todas las rutas responden los errores con el mismo formato, y la web y la app muestran `error.mensaje`:

```json
{ "error": { "codigo": "VENTA_NO_ENCONTRADA", "mensaje": "Venta no encontrada con ID: 999", "campos": { "campo": "detalle" } } }
```

`campos` solo viene en los errores de validación de campos. Códigos HTTP: 400 petición mal formada o campo inválido,
401 sin sesión, 403 rol sin permiso, 404 recurso inexistente (también en PUT, PATCH y DELETE), 409 choca con el estado
actual (por ejemplo, confirmar una compra ya confirmada), 422 regla de negocio violada (stock insuficiente, pago mayor
al saldo) y 500 error interno, sin detalles ni traza. Las respuestas de error llevan las cabeceras CORS.

La tabla completa de códigos de error, cuándo ocurre cada uno y un ejemplo está en
[`docs/errores-api.md`](docs/errores-api.md).

## Qué incluye

| Parte | Carpeta | Tecnología | Para qué |
|---|---|---|---|
| **Backend** | `backend/` | Java 17 · Spring Boot 3.5 · PostgreSQL | API REST: ventas, inventario, compras, envíos, usuarios, auditoría |
| **Frontend** | `frontend/` | Next.js 16 · React 19 · TypeScript · Tailwind | Panel de administración y tienda pública |
| **App móvil** | `app/` | Flutter · Dart | App para el dueño del negocio (en construcción) |

```
sistema-ventas-eden/
├── backend/
│   ├── database/        scripts SQL del esquema, numerados y comentados
│   └── src/
├── frontend/
│   └── src/
│       ├── app/dashboard/   panel de administración
│       └── app/tienda/      tienda pública, sin inicio de sesión
├── app/                 app Flutter
└── README.md
```

Las tres partes hablan con **un único backend**. Ni el frontend ni la app se
conectan directo a la base de datos: toda la lógica, los permisos y la
auditoría viven en un solo lugar.

## Requisitos

| Herramienta | Versión |
|---|---|
| Java (JDK) | 17 |
| Maven | no hace falta instalarlo: el backend trae `mvnw` |
| PostgreSQL | 15 o superior |
| Node.js | 20.9 o superior |
| Flutter | SDK con Dart 3.12 o superior |

---

## Cómo ejecutar cada parte localmente

Arrancar en este orden: base de datos, backend, frontend. La app móvil
necesita el backend corriendo.

### 1. Base de datos

El backend espera una base PostgreSQL llamada `muebleria_eden_db`.

> **Limitación actual:** el repositorio todavía no puede crear la base desde
> cero. Los scripts de `backend/database/` son **cambios sobre un esquema que
> ya existe** (del 01 al 15); no hay un script que cree las tablas base. Hoy
> hace falta partir de una copia de una base existente.
>
> Además, en una base vacía nadie puede iniciar sesión: al arrancar, el
> backend crea los roles `ADMIN` y `EMPLEADO`, pero ningún usuario.

El backend nunca modifica la estructura de la base
(`spring.jpa.hibernate.ddl-auto=validate`). Al arrancar compara todas las
entidades contra las tablas y, si algo no coincide, **se niega a arrancar** y
dice qué. Los cambios de esquema se hacen solo con los scripts; ver
[`backend/database/README.md`](backend/database/README.md).

### 2. Backend

```bash
cd backend

# Configuración: copiar las plantillas (los archivos reales no se versionan)
cp src/main/resources/application.properties.example      src/main/resources/application.properties
cp src/main/resources/application-dev.properties.example  src/main/resources/application-dev.properties
cp src/main/resources/application-prod.properties.example src/main/resources/application-prod.properties

# Variables: copiar y completar al menos DB_PASSWORD y JWT_SECRET
cp .env.example .env

# Arrancar (Windows: .\mvnw.cmd spring-boot:run)
./mvnw spring-boot:run
```

Queda en `http://localhost:8080`. Spring lee `backend/.env` solo; no hace falta
exportar las variables.

**Perfiles.** Eligen contra qué base se conecta:

| Perfil | Base | Cuándo |
|---|---|---|
| `dev` (por omisión) | PostgreSQL local | trabajo diario y demostraciones |
| `prod` | Supabase | entrega y app en un celular real |

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

**Verificar que todo cable bien** (construye el contexto completo de Spring y
valida el esquema contra la base, sin ocupar el puerto 8080):

```bash
./mvnw test
```

### 3. Frontend

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Queda en `http://localhost:3000`:

- `http://localhost:3000/login` — panel de administración
- `http://localhost:3000/tienda` — tienda pública

### 4. App móvil

```bash
cd app
cp .env.example .env
flutter pub get
flutter run --dart-define-from-file=.env
```

Para el celular, `localhost` es el propio celular y no la computadora donde
corre el backend. En `app/.env.example` están las tres formas de alcanzarlo
(emulador, cable USB con `adb reverse`, o misma red WiFi).

La app es por ahora el proyecto base de Flutter. Las pantallas se van
incorporando en commits separados.

---

## Variables de entorno

Cada parte tiene su `.env.example` con todas las variables que usa, explicadas
y sin valores reales.

| Parte | Archivo real | Variables |
|---|---|---|
| Backend | `backend/.env` | `SPRING_PROFILE`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `SUPABASE_URL`, `SUPABASE_SERVICE_KEY` |
| Frontend | `frontend/.env.local` | `NEXT_PUBLIC_BACKEND_URL` |
| App | `app/.env` | `API_URL` |

Los archivos reales están en `.gitignore`. **Nunca se suben.**

### Almacenamiento de archivos (Supabase Storage)

Las fotos que suben los usuarios no se guardan en el disco del servidor, porque en
Render el disco se borra al redesplegar o suspender el servicio. Van a Supabase Storage:

| Bucket | Acceso | Qué guarda |
|---|---|---|
| `productos` | Público | Fotos del catálogo. En la base queda la URL pública. |
| `comprobantes` | Privado | Fotos de comprobantes de pago. En la base queda el nombre del objeto y el backend entrega una URL firmada que vence a la hora. |

El backend usa la API REST de Storage con dos variables de entorno, que **solo** se leen
del entorno (o de `backend/.env` en desarrollo):

| Variable | Qué es |
|---|---|
| `SUPABASE_URL` | URL del proyecto, `https://<proyecto>.supabase.co` (Project Settings → API) |
| `SUPABASE_SERVICE_KEY` | Clave `service_role` (Project Settings → API). Da acceso total al proyecto: la usa solo el backend, nunca el frontend ni la app, y no va en el repositorio. |

- **Desarrollo:** sin las dos variables, los archivos se guardan en `backend/uploads/` (disco local).
- **Producción (perfil `prod`):** si faltan, el backend **no arranca** y el mensaje dice qué variable falta.
- **Validación:** el servidor revisa el tipo real de la imagen (JPG, PNG o WebP), que no pase de 5 MB (productos) o 10 MB (comprobantes) y ignora el nombre original del archivo.
- **Registros viejos** (rutas `/uploads/...` del disco): siguen funcionando; si el archivo ya no existe, la web muestra la imagen genérica.
- **Ruta `/uploads/**`:** con Supabase activo exige sesión de ADMIN o EMPLEADO; con disco local (desarrollo) sigue abierta para que las pantallas muestren las fotos.
- **Fuera de este cambio:** los modelos 3D y sus vistas previas (tienda) siguen en el disco.

**Preparar Supabase (a mano, una sola vez):** en Storage, crear el bucket `productos` con *Public bucket* activado y el bucket `comprobantes` con *Public bucket* desactivado. No hacen falta políticas: el backend usa la clave de servicio.

**Preparar Render:** en Environment del servicio, agregar `SUPABASE_URL` y `SUPABASE_SERVICE_KEY` y volver a desplegar (*Save and Deploy*).

#### Respaldo de los archivos

Supabase guarda los archivos, pero conviene tener una copia propia. El script
`scripts/respaldar-storage.mjs` descarga todo lo que hay en los buckets `productos` y
`comprobantes` a la carpeta `respaldo-storage/`, conservando la estructura
(`respaldo-storage/<bucket>/<ruta>`). Se ejecuta a mano, necesita Node 18 o superior y solo
lee: no borra ni cambia nada en Supabase.

```bash
# Usa SUPABASE_URL y SUPABASE_SERVICE_KEY del entorno; si no están, las toma de backend/.env
node scripts/respaldar-storage.mjs

# Opcionales
node scripts/respaldar-storage.mjs --carpeta D:/respaldos/eden   # otra carpeta de destino
node scripts/respaldar-storage.mjs --forzar                      # volver a bajar todo
```

Por defecto no vuelve a bajar lo que ya está con el mismo tamaño, así que se puede correr
seguido. Si algún archivo falla, lo informa y termina con código de error. La carpeta
`respaldo-storage/` está en `.gitignore`: tiene comprobantes de pago y **no se sube al
repositorio ni se comparte**. Las pruebas del script se corren con
`node --test scripts/respaldar-storage.test.mjs`.

## Seguridad

El `.gitignore` de la raíz excluye todo lo que no debe entrar al repositorio:
archivos `.env`, la configuración del backend (lleva la contraseña de la base
y el secreto JWT), `local.json`, `google-services.json`, las credenciales de
Firebase y las llaves de firma de Android e iOS.

Si alguno se sube por error, borrarlo en un commit nuevo **no alcanza**: queda
en el historial. Hay que cambiar la credencial y reescribir el historial.

## Roles

| Rol | Quién |
|---|---|
| `ADMIN` | Dueño del negocio. Acceso completo |
| `EMPLEADO` | Personal de venta. Acceso limitado |

Los permisos se controlan en el backend. El frontend oculta lo que un rol no
puede usar, pero la regla que vale es la del backend.

## Historial

Este repositorio unifica tres repositorios que antes estaban separados. El
historial de `backend/` y `frontend/` se trajo completo con `git subtree`,
conservando autor, fecha y mensaje de cada commit. Antes de unificar se
reescribió el historial del backend para quitar una contraseña que figuraba en
el código.

Con `git subtree`, los commits anteriores a la unificación guardan las rutas
**sin** el prefijo de su carpeta. Por eso, para ver la historia de un archivo:

```bash
# Así solo aparece el commit de la unificación:
git log -- backend/src/main/java/com/mitienda/ecommerce/services/VentaService.java

# Así aparece la historia completa: la ruta anterior, sin "backend/",
# y --full-history para que Git no descarte la rama que se unió.
git log --full-history -- src/main/java/com/mitienda/ecommerce/services/VentaService.java
```

`git blame` sobre la ruta actual sí llega a los commits originales sin nada
especial.
