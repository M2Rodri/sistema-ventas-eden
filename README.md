# Sistema de Ventas e Inventario — Mueblería Edén

**Trabajo Final · Diplomado en Desarrollo Web y Aplicaciones Móviles · UAJMS 2026**
**Autor:** Rodrigo Mamani Mamani · **Docente:** M.Sc. Ing. Isaac Lange Aguilar

## 1. Descripción

Sistema de ventas e inventario para **Mueblería Edén**, un negocio familiar de camas, colchones y muebles de dormitorio ubicado en Santa Cruz de la Sierra, Bolivia. Tiene dos partes que hablan con un único backend:

- **Web administrativa:** ventas, cobros, clientes, productos, inventario, compras, proveedores, usuarios y reportes.
- **App móvil (Android):** consulta de catálogo y de ventas, registro de ventas y cobros para el día a día.

| Qué | Dónde |
|-----|-------|
| Web | https://sistema-ventas-eden.vercel.app |
| API (estado del servicio) | https://sistema-ventas-eden-1.onrender.com/api/v1/salud |
| App Android (APK) | https://github.com/M2Rodri/sistema-ventas-eden/releases/latest/download/muebleria-eden.apk |
| Página de estado | https://stats.uptimerobot.com/CLc5bJEPlk |

Ni la web ni la app se conectan a la base de datos: toda la lógica, los permisos y la auditoría viven en el backend.

## 2. Stack

| Parte | Tecnología y versión |
|---|---|
| Backend (`backend/`) | Java 17 · Spring Boot 3.5.16 (Web, Data JPA, Security, Validation) · JWT con jjwt 0.12.7 · Lombok · ModelMapper 3.2.6 · springdoc-openapi 2.8.17 · Maven (incluye `mvnw`) |
| Base de datos | PostgreSQL 17 (Supabase) · el esquema se valida al arrancar (`ddl-auto=validate`): el backend nunca modifica la estructura |
| Frontend (`frontend/`) | Next.js 16.4.1 · React 19.2.6 · TypeScript 5.9 · Tailwind CSS 3.4 · Recharts 3.8 · lucide-react |
| App móvil (`app/`) | Flutter 3.44.8 · Dart 3.12.2 · `http` · `flutter_secure_storage` · `local_auth` (huella) |
| Archivos | Supabase Storage (fotos de productos y comprobantes de pago) |
| Pruebas | JUnit 5.12 · Mockito 5.17 · Spring Boot Test · `flutter_test` · Newman 6.2 (colección de Postman) |

## 3. Requisitos previos

| Herramienta | Versión |
|---|---|
| JDK | 17 |
| Maven | no hace falta instalarlo: el backend trae `mvnw` |
| PostgreSQL | 17 o superior (el esquema usa `transaction_timeout`, de la versión 17) |
| Node.js | 20.9 o superior (Next.js 16); se usó 24 |
| Flutter | 3.44 (Dart 3.12 o superior) y Android SDK, para compilar la app |

## 4. Instalación local

Arrancar en este orden: base de datos, backend, frontend. La app necesita el backend corriendo.

### 4.1 Base de datos

Crear la base vacía y cargar el esquema completo (26 tablas):

```bash
createdb -U postgres muebleria_eden_db
psql -U postgres -d muebleria_eden_db -v ON_ERROR_STOP=1 -f backend/database/00_esquema.sql
```

`00_esquema.sql` es el estado final de la base. Los scripts numerados `01` a `37` de `backend/database/` son
los cambios históricos que llevaron hasta ahí: **no se corren encima del esquema**. Detalle en
[`backend/database/README.md`](backend/database/README.md).

### 4.2 Backend

```bash
cd backend
cp src/main/resources/application-dev.properties.example src/main/resources/application-dev.properties
cp .env.example .env        # completar al menos DB_PASSWORD, JWT_SECRET y el administrador inicial
./mvnw spring-boot:run      # en Windows: .\mvnw.cmd spring-boot:run
```

El archivo `application-dev.properties` no viene en el repositorio (está en `.gitignore`): sin copiarlo, el backend
no arranca (`Failed to configure a DataSource`). En `backend/.env` se define `SPRING_PROFILE=dev`; `JWT_SECRET` debe
tener 64 caracteres o más; `SUPABASE_URL` y `SUPABASE_SERVICE_KEY` se dejan vacías en `dev` (los archivos se guardan
en `backend/uploads/`). Las variables de entorno del sistema tienen prioridad sobre `backend/.env`.

Queda en `http://localhost:8080`. Spring lee `backend/.env` solo. Para comprobar que funciona, abrir
`http://localhost:8080/api/v1/salud`: debe responder `"estado":"OK"` y `"baseDeDatos":"DISPONIBLE"`.

**Primer administrador.** Una base nueva no tiene usuarios. Si en `backend/.env` están definidas
`ADMIN_INICIAL_USUARIO` y `ADMIN_INICIAL_CLAVE` (usuario de 3 a 30 caracteres y clave de 6 o más), al arrancar
el backend crea con ellas un administrador, siempre que la tabla `usuarios` esté vacía; la clave se guarda con
BCrypt y nunca se escribe en el registro. Si falta alguna variable o ya hay usuarios, no hace nada. Una vez
dentro, crear el usuario Empleado desde la pantalla Usuarios, quitar `ADMIN_INICIAL_USUARIO` y `ADMIN_INICIAL_CLAVE`
del entorno (o de `backend/.env`) y reiniciar el backend.

Los roles `ADMIN` y `EMPLEADO` los crea el backend al arrancar.

### 4.3 Frontend

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Queda en `http://localhost:3000` (inicio de sesión en `/login`). `.env.local` solo necesita
`NEXT_PUBLIC_BACKEND_URL=http://localhost:8080`; `npm install` tarda unos minutos. Si el backend o la web usan otros
puertos, cambiar esa variable y agregar el origen de la web a `CORS_ALLOWED_ORIGINS` del backend (por ejemplo
`http://localhost:3001`).

### 4.4 App móvil

```bash
cd app
flutter pub get
flutter run --dart-define=API_URL=http://10.0.2.2:8080     # con un emulador Android
flutter build apk --debug --dart-define=API_URL=http://10.0.2.2:8080   # para comprobar que compila, sin emulador
```

`10.0.2.2` es la dirección del equipo vista desde el emulador; si el backend usa otro puerto, cambiarlo en `API_URL`.
En el celular, `localhost` es el propio celular. En `app/.env.example` están las formas de alcanzar el
backend (emulador, cable USB con `adb reverse` o la misma red WiFi); ese archivo también sirve con
`--dart-define-from-file=.env`.

## 5. Variables de entorno

Cada parte trae un `.env.example` con todas sus variables explicadas y **sin valores**. Los archivos reales
(`.env`, `.env.local`) están en `.gitignore` y nunca se suben.

### Backend (`backend/.env`)

| Variable | Qué hace |
|---|---|
| `SPRING_PROFILE` | Perfil de arranque: `dev` (PostgreSQL local, por defecto) o `prod` (Supabase) |
| `DB_URL` | URL JDBC de la base. En `dev` tiene valor por defecto; en `prod` es obligatoria |
| `DB_USERNAME` | Usuario de la base (en `dev` por defecto `postgres`) |
| `DB_PASSWORD` | Contraseña de la base |
| `JWT_SECRET` | Clave con la que se firman los tokens de sesión (larga y aleatoria, mínimo 64 caracteres) |
| `SUPABASE_URL` | URL del proyecto de Supabase, para Storage |
| `SUPABASE_SERVICE_KEY` | Clave de servicio de Supabase, solo para el backend; en `prod` son obligatorias las dos de Supabase |
| `PORT` | Puerto del servidor (por defecto 8080; Render lo asigna solo) |
| `CORS_ALLOWED_ORIGINS` | Dominios extra permitidos por CORS, separados por coma |
| `ADMIN_INICIAL_USUARIO` | Usuario del administrador inicial (solo se usa si la tabla `usuarios` está vacía) |
| `ADMIN_INICIAL_CLAVE` | Contraseña del administrador inicial |

### Frontend (`frontend/.env.local`)

| Variable | Qué hace |
|---|---|
| `NEXT_PUBLIC_BACKEND_URL` | Dirección del backend. Termina dentro del JavaScript del navegador: nunca poner claves aquí |
| `NEXT_DIST_DIR` | Opcional. Carpeta de compilación, para verificar una compilación sin pisar la del servidor de desarrollo |

### App (`app/.env`, se pasan con `--dart-define`)

| Variable | Qué hace |
|---|---|
| `API_URL` | Dirección del backend (por defecto `http://10.0.2.2:8080`, el `localhost` del emulador) |
| `UPDATE_URL` | Dónde se publica `version.json` para las actualizaciones (tiene un valor por defecto fijo) |

## 6. Estructura del repositorio

```
sistema-ventas-eden/
├── backend/
│   ├── database/        00_esquema.sql, scripts 01 a 37 y carga_inicial/
│   ├── src/main/java/com/mitienda/ecommerce/
│   │   ├── controllers/ services/ repositories/ models/ dto/
│   │   ├── security/ config/ exception/ storage/
│   ├── src/test/        pruebas del backend
│   ├── Dockerfile       imagen para Render
│   └── pom.xml
├── frontend/
│   ├── middleware.ts    protege /dashboard y redirige las rutas fuera de alcance
│   └── src/
│       ├── app/         páginas: login y dashboard (panel administrativo)
│       ├── components/ contexts/ hooks/ lib/ types/
├── app/                 app Flutter
│   ├── lib/             config/ data/ models/ screens/ theme/ widgets/
│   ├── test/            pruebas de la app
│   └── android/
├── scripts/             publicar-apk.mjs (publica el APK), respaldar-base.mjs y programar-respaldo.ps1 (respaldo diario
│                        de la base), respaldar-storage.mjs (respaldo de Storage),
│                        generar-icono-adaptativo.mjs e instalar-icono.mjs (ícono de la app) y sus pruebas
├── evidencia/           reportes de pruebas: api/ (Newman), backend/, app/, rendimiento/
├── docs/                rutas-api-v1.md y errores-api.md
├── .gitignore
└── README.md
```

El repositorio conserva código de módulos que quedaron fuera del alcance del trabajo. No forman parte del
sistema entregado: sus rutas de API responden 404 y sus páginas redirigen al inicio de sesión.

La auditoría es un caso aparte: el registro funciona (cada acción sensible queda en la tabla `auditorias` con usuario, fecha e IP), pero la consulta desde la web o la app quedó fuera del alcance y sus rutas están apagadas (ver [`docs/rutas-api-v1.md`](docs/rutas-api-v1.md)).

**Convenciones de la API**
- Todas las rutas llevan el prefijo `/api/v1`; una ruta `/api/...` sin versión responde 404. La lista completa
  está en [`docs/rutas-api-v1.md`](docs/rutas-api-v1.md).
- Los errores tienen un solo formato, `{ "error": { "codigo", "mensaje", "campos" } }`, con los códigos HTTP 400,
  401, 403, 404, 409, 422, 429 y 500. La tabla de códigos está en [`docs/errores-api.md`](docs/errores-api.md).
- Cada token dura 4 horas y se renueva solo mientras se usa el sistema (`POST /api/v1/auth/renovar`, con el
  rol vigente en la base). El tope de una sesión desde el inicio de sesión es de 8 horas en la web, 12 en la
  app con candado (huella o bloqueo de pantalla) y 4 en la app sin candado. La web cierra la sesión tras
  30 minutos sin actividad. Una cuenta dada de baja pierde el acceso en la siguiente petición, y el rol se
  lee de la base en cada una. Se pueden cortar todas las sesiones de un usuario (botón "Cerrar sesiones" en
  Usuarios, o `POST /api/v1/auth/cerrar-sesiones`), y cambiar la contraseña de alguien también las corta
  (columna `usuarios.sesiones_validas_desde`, script 36). Cinco inicios de sesión fallidos de un mismo
  usuario en 15 minutos lo bloquean 15 minutos (429 `DEMASIADOS_INTENTOS`).
- Los ID de ventas, compras, productos, clientes, proveedores, categorías, inventario, pagos, comprobantes,
  movimientos y usuarios salen de un contador propio (`contadores_id`) dentro de la misma transacción: si una
  operación se revierte, el número no se pierde y la numeración no tiene huecos. Después de una carga masiva
  por SQL, ejecutar `SELECT sincronizar_contadores_id();`.

## 7. Roles

| Rol | Quién | Qué puede hacer |
|---|---|---|
| **Administrador** (`ADMIN`) | El dueño del negocio | Todo: usuarios, productos, categorías, inventario, compras, proveedores, reportes y anulaciones |
| **Empleado** (`EMPLEADO`) | Personal de venta | Registrar ventas y cobros, gestionar clientes y marcar entregas; consultar productos e inventario. No ve costos de compra ni accede a usuarios, compras, proveedores y reportes |

El usuario `admin` es el administrador principal: solo él puede editarse, darse de baja o cerrarse las sesiones; los demás administradores reciben 403.

Los permisos se definen y se aplican en el backend (`SecurityConfig.java` y `@PreAuthorize`) y se verifican con
pruebas automáticas (`RutasApiTest`); la web y la app solo ocultan lo que el rol no puede usar.
Las credenciales de acceso **se entregan por canal privado**.

## 8. Pruebas

| Qué | Comando | Total actual |
|---|---|---|
| Backend | `cd backend && ./mvnw test` | **270** pruebas, 0 fallos |
| App móvil | `cd app && flutter test` | **74** pruebas, 0 fallos |
| API (Newman) | `cd evidencia/api && npm install && node ejecutar.mjs` | **38** casos, todos aprobados |

- Las pruebas del backend construyen la aplicación completa y validan las entidades contra la base, así que
  necesitan PostgreSQL local con el esquema cargado (sección 4.1).
- `ejecutar.mjs` corre la colección de Postman contra el servidor y mide el rendimiento. Las credenciales de
  los usuarios de prueba se leen de las variables de entorno `ADMIN_USER`, `ADMIN_PASS`, `EMP_USER` y `EMP_PASS`;
  `BASE_URL` es opcional (por defecto, el servidor de producción). Opciones: `--api` (solo la colección) y
  `--rendimiento` (solo los tiempos). Crea datos de prueba en el servidor y los anula al terminar (quedan anulados, no borrados: el script
  `backend/database/37_limpiar_pruebas_y_renumerar.sql` los quita y deja la numeración sin huecos).
- **Rendimiento (RNF-01, umbral 2 s)**, medido el 23/10/2026 en producción: `GET /api/v1/productos` 401 ms de
  promedio y 731 ms de máximo; `GET /api/v1/inventario/catalogo` 527 ms y 1273 ms.
- Los reportes están en [`evidencia/`](evidencia/) con la fecha en el nombre.

## 9. Despliegue

El backend (Render) y el frontend (Vercel) se despliegan solos al hacer push a `main`. El APK se publica a mano, con el procedimiento de más abajo.

| Parte | Servicio | Notas |
|---|---|---|
| Backend | **Render** (Docker, `backend/Dockerfile`: Maven 3.9 y Java 17) | Plan gratuito: si nadie lo usa un rato, el servicio se duerme y la primera petición puede tardar más de un minuto. Variables de entorno de la sección 5 |
| Frontend | **Vercel** | Variable `NEXT_PUBLIC_BACKEND_URL` apuntando al backend |
| Base de datos y archivos | **Supabase** (PostgreSQL 17, Storage) | Buckets `productos` (público), `comprobantes` (privado) y `app` (público, con `version.json`) |
| App Android | **GitHub Releases** | El APK se publica como `muebleria-eden.apk` |
| Disponibilidad | **UptimeRobot** | Página de estado: https://stats.uptimerobot.com/CLc5bJEPlk |

**Orden al cambiar la base.** Los cambios de estructura se aplican en Supabase **antes** de publicar el backend que
los usa: con `ddl-auto=validate`, un backend que espera una columna que no existe no arranca.

**Actualización de la app.** Al abrirse (y en Ajustes → "Buscar actualización"), la app lee `version.json` del
bucket `app`; si la compilación publicada es mayor que la instalada, descarga el APK, comprueba su huella
SHA-256 y abre el instalador de Android. Para publicar una versión nueva:
1. Subir la versión y el número de compilación en `app/pubspec.yaml` (por ejemplo `1.0.1+2`; el número después del
   `+` siempre crece).
2. Compilar firmado: `cd app && flutter build apk --release --dart-define=API_URL=https://sistema-ventas-eden-1.onrender.com`.
3. Publicar: `node scripts/publicar-apk.mjs --notas "Qué cambió"` (necesita `gh` autenticado y `SUPABASE_URL` y
   `SUPABASE_SERVICE_KEY`). Crea el release de GitHub y actualiza `version.json`.

La llave de firma del APK y su `key.properties` no están en el repositorio: una actualización solo se instala
sobre la app ya instalada si está firmada con la misma llave, así que hay que guardar copia fuera de esta
computadora.

**Respaldos.**
- Base de datos, automático: `node scripts/respaldar-base.mjs` hace un `pg_dump` comprimido de Supabase, comprueba que
  sirva (que pese lo esperado y traiga las tablas usuarios, ventas, productos y clientes), conserva los 30 más
  recientes y el primero de cada mes, y deja el resultado en `respaldos/respaldo.log`. Las credenciales se leen de
  `credenciales-supabase.env`, fuera del proyecto (plantilla: `scripts/respaldar-base.env.example`). Para que corra
  solo todos los días: `powershell -ExecutionPolicy Bypass -File scripts\programar-respaldo.ps1` (si la computadora
  estaba apagada, corre al prenderse). Con `RESPALDO_COPIA` deja además una copia en otra carpeta o pendrive.
- Base de datos, a mano (por ejemplo antes de un cambio de esquema): `pg_dump -Fc --no-owner --no-privileges` contra
  Supabase, guardado fuera del repositorio.
- Archivos: `node scripts/respaldar-storage.mjs` descarga los buckets `productos` y `comprobantes` a
  `respaldo-storage/` (ignorada por git: tiene comprobantes de pago).

## 10. Licencia

Uso académico. Todos los derechos reservados por el autor.
