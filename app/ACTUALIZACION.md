# 📦 Guía de Publicación de Actualizaciones — Mueblería Edén

> Documento que describe cómo funciona el sistema OTA (Over-The-Air) de la app,
> cómo se resolvió el problema de publicación y los pasos exactos para futuras actualizaciones.

---

## 🧠 ¿Cómo funciona el sistema de actualización?

La app tiene una sección en **Ajustes → Buscar Actualización** que sigue este flujo:

```
Celular abre la app
      ↓
Consulta version.json en Supabase Storage
      ↓
Compara versionCode local vs versionCode remoto
      ↓
Si remoto > local → muestra "Hay una nueva versión disponible"
      ↓
Usuario presiona Actualizar → descarga el APK desde GitHub Releases
      ↓
Android muestra diálogo de instalación (comportamiento normal del sistema)
      ↓
App actualizada ✅
```

### Archivos clave del sistema

| Archivo / URL | Rol |
|---|---|
| `app/pubspec.yaml` | Define `version: X.Y.Z+N` — el número después del `+` es el `versionCode` |
| `version.json` en Supabase | Lo que los celulares consultan para saber si hay versión nueva |
| APK en GitHub Releases | El archivo que los celulares descargan al actualizar |
| `scripts/publicar-apk.mjs` | Script que automatiza todo el proceso de publicación |

---

## 📌 URLs oficiales

| Propósito | URL |
|---|---|
| Siempre descarga la última versión | `https://github.com/M2Rodri/sistema-ventas-eden/releases/latest/download/muebleria-eden.apk` |
| Versión específica (ej. v1.1.0) | `https://github.com/M2Rodri/sistema-ventas-eden/releases/download/v1.1.0/muebleria-eden.apk` |
| version.json (Supabase) | `https://lcipybfksedtqrwgothl.supabase.co/storage/v1/object/public/app/version.json` |

---

## ⚠️ Problema encontrado: Supabase Storage tiene límite de 50 MB

### ¿Qué pasó?

Durante la publicación de la versión **1.1.0**, el script original intentaba subir
el APK directamente a **Supabase Storage**. El APK pesaba **51.7 MB**, superando
el límite de **50 MB** de Supabase, lo que causaba un error de `fetch failed` al subir.

### ¿Cómo se resolvió?

Se modificó el script `scripts/publicar-apk.mjs` para separar las responsabilidades:

- **GitHub Releases** → aloja el APK (sin límite de tamaño práctico)
- **Supabase Storage** → solo aloja `version.json` (unos pocos KB)

Así el flujo quedó:

```
Script publicar-apk.mjs
      ↓
1. Sube el APK a GitHub Releases (tag v1.1.0, archivo: muebleria-eden.apk)
      ↓
2. Actualiza version.json en Supabase apuntando a la URL de GitHub
      ↓
Publicación completada ✅
```

---

## ⚠️ Problema encontrado: Asset con nombre incorrecto en GitHub

> [!CAUTION]
> Este problema ocurrió **dos veces** (v1.1.0 y v1.1.1) hasta que se corrigió el script de forma permanente.

### ¿Qué pasó?

Al subir el APK con `gh release create`, el script pasaba el argumento
`"ruta/app-release.apk#muebleria-eden.apk"` para darle un nombre personalizado.
En **Windows**, la `#` es interpretada incorrectamente por PowerShell y el archivo
quedó subido como `app-release.apk` en lugar de `muebleria-eden.apk`.

Esto causaba que la app mostrara el error **"No se pudo descargar la actualización"**
porque el `version.json` apuntaba a `muebleria-eden.apk` pero ese archivo no existía en GitHub.

### Solución de emergencia (si vuelve a ocurrir)

Si en algún release futuro el asset queda mal nombrado, se puede renombrar sin re-publicar:

```bash
# 1. Obtener el ID del asset (reemplazar vX.X.X con la versión afectada)
gh api repos/M2Rodri/sistema-ventas-eden/releases/tags/vX.X.X --jq '.assets[] | {id: .id, name: .name}'

# 2. Renombrar via PATCH con el ID obtenido
gh api --method PATCH repos/M2Rodri/sistema-ventas-eden/releases/assets/{ASSET_ID} -f name="muebleria-eden.apk"

# 3. Verificar que el URL ya responde 200
# https://github.com/M2Rodri/sistema-ventas-eden/releases/download/vX.X.X/muebleria-eden.apk
```

### Corrección permanente aplicada al script

Se modificó `scripts/publicar-apk.mjs` para que en lugar de usar el `#` (que falla en Windows),
**copie el APK a un archivo temporal con el nombre correcto** antes de subirlo a GitHub:

```js
// Antes (fallaba en Windows):
gh release create v1.1.0 "app-release.apk#muebleria-eden.apk" ...

// Ahora (funciona en Windows):
copyFileSync("app-release.apk", "muebleria-eden.apk")  // copia temporal
gh release create v1.1.0 "muebleria-eden.apk" ...      // sube con nombre correcto
unlinkSync("muebleria-eden.apk")                        // limpia el temporal
```

---

## ✅ Pasos para publicar futuras actualizaciones

### Requisitos previos
- `gh` CLI instalado y autenticado (`gh auth login`) como M2Rodri
- `SUPABASE_URL` y `SUPABASE_SERVICE_KEY` en `backend/.env`
- Keystore de firma en `app/android/key.properties`

### Paso 1 — Subir la versión en `pubspec.yaml`

```yaml
# app/pubspec.yaml
version: 1.2.0+4   # ← el número después del + SIEMPRE debe ser mayor al anterior
```

> El número antes del `+` es el nombre visible (1.2.0).
> El número después del `+` es el `versionCode` que comparan los celulares (4).

### Paso 2 — Compilar el APK firmado

```bash
cd app
flutter build apk --release --dart-define=API_URL=https://sistema-ventas-eden-1.onrender.com
```

Tiempo estimado: **5-10 minutos**. El APK queda en:
`app/build/app/outputs/flutter-apk/app-release.apk`

### Paso 3 — Publicar

```bash
node scripts/publicar-apk.mjs --notas "Descripción de los cambios de esta versión"
```

El script automáticamente:
1. Crea el release en GitHub con tag `vX.Y.Z` y sube el APK como `muebleria-eden.apk`
2. Actualiza `version.json` en Supabase con el nuevo `versionCode` y la URL del APK

Desde ese momento, todos los celulares con la app verán la notificación de actualización
al presionar **Ajustes → Buscar Actualización**.

---

## 📋 Historial de versiones publicadas

| Versión | versionCode | Fecha | Cambios |
|---|---|---|---|
| 1.0.0 | 2 | Oct 2026 | Versión inicial publicada |
| 1.1.0 | 3 | 05 Oct 2026 | Nuevo ícono oficial, tarjetas naranjas, módulos café, tema oscuro |

---

## ❓ Preguntas frecuentes

**¿Por qué la actualización requiere dos pasos en el celular?**
El primer paso es la descarga del APK hecha por la app. El segundo es el diálogo
del sistema Android que pide confirmación antes de instalar cualquier APK externo.
Esto es comportamiento estándar de Android para apps fuera de Play Store y no puede
omitirse sin tener permisos especiales de sistema (que solo tiene Play Store).

**¿El enlace del documento oficial sigue funcionando?**
Sí. El enlace `/releases/latest/download/muebleria-eden.apk` siempre apunta
automáticamente al release más reciente. No necesita actualizarse con cada versión.

**¿Qué pasa si el APK supera los 50 MB en el futuro?**
No hay problema. El APK vive en GitHub Releases (sin límite práctico de tamaño).
Solo `version.json` (unos pocos KB) vive en Supabase Storage.
