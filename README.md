# Sistema de ventas — Mueblería Edén

Sistema de gestión para **Mueblería Edén**, un negocio familiar de venta de
camas, colchones y accesorios de descanso en Santa Cruz de la Sierra, Bolivia.

Proyecto final del Diplomado en Desarrollo Web y Aplicaciones Móviles,
Universidad Autónoma Juan Misael Saracho (Tarija).

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
| Backend | `backend/.env` | `SPRING_PROFILE`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` |
| Frontend | `frontend/.env.local` | `NEXT_PUBLIC_BACKEND_URL` |
| App | `app/.env` | `API_URL` |

Los archivos reales están en `.gitignore`. **Nunca se suben.**

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
