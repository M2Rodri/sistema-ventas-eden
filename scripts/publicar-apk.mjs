#!/usr/bin/env node
// Publica una versión nueva de la app móvil para que los teléfonos la ofrezcan solos.
//
// Sube a Supabase Storage (bucket público "app"):
//   - el APK:        muebleria-eden-<versión>-<compilación>.apk
//   - version.json:  lo que la app lee al abrirse para saber si hay algo nuevo
//
// Pasos para publicar una versión:
//   1. En app/pubspec.yaml subir la versión Y el número de compilación:
//        version: 1.1.0+2        (el número después del + SIEMPRE tiene que crecer)
//   2. Compilar firmado con la llave propia (app/android/key.properties):
//        cd app
//        flutter build apk --release --dart-define=API_URL=https://sistema-ventas-eden.onrender.com
//   3. Publicar:
//        node scripts/publicar-apk.mjs --notas "Qué cambió en esta versión"
//
// Usa SUPABASE_URL y SUPABASE_SERVICE_KEY, igual que el backend (del entorno o de backend/.env).
// Se niega a publicar si la compilación no es mayor que la ya publicada (los teléfonos no se
// enterarían); --forzar lo permite.

import { createHash } from 'node:crypto';
import { existsSync, readFileSync, statSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

import { leerCredenciales } from './respaldar-storage.mjs';

export const BUCKET = 'app';
const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

/** Lee "version: 1.1.0+2" de pubspec.yaml. */
export function leerVersionPubspec(contenido) {
  const m = contenido.match(/^version:\s*(\d+\.\d+\.\d+)\+(\d+)\s*$/m);
  if (!m) throw new Error('No encontré "version: X.Y.Z+N" en app/pubspec.yaml.');
  return { versionName: m[1], versionCode: Number(m[2]) };
}

/** El contenido de version.json, tal como lo lee la app. */
export function armarVersionJson({ versionName, versionCode, apkUrl, sha256, tamanoBytes, notas }) {
  return { versionCode, versionName, apkUrl, sha256, tamanoBytes, notas: notas || '' };
}

export function nombreApk({ versionName, versionCode }) {
  return `muebleria-eden-${versionName}-${versionCode}.apk`;
}

/** Hay que asegurar que el bucket exista y sea público; si ya existe, no pasa nada. */
async function asegurarBucket({ url, clave }, http) {
  const r = await http(`${url}/storage/v1/bucket`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${clave}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ id: BUCKET, name: BUCKET, public: true }),
  });
  if (r.ok) return 'creado';
  const texto = await r.text();
  if (r.status === 409 || /already exists|Duplicate/i.test(texto)) return 'existente';
  throw new Error(`No pude crear el bucket "${BUCKET}": ${r.status} ${texto}`);
}

async function subir({ url, clave }, http, nombre, cuerpo, tipo, cache) {
  const r = await http(`${url}/storage/v1/object/${BUCKET}/${nombre}`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${clave}`,
      'Content-Type': tipo,
      'x-upsert': 'true',
      'cache-control': cache,
    },
    body: cuerpo,
  });
  if (!r.ok) throw new Error(`No pude subir ${nombre}: ${r.status} ${await r.text()}`);
}

/** Sube el APK y después version.json (en ese orden: nadie ve la versión antes de que exista el APK). */
export async function publicar({ apk, version, notas, credenciales, forzar = false, http = fetch }) {
  const urlPublica = `${credenciales.url}/storage/v1/object/public/${BUCKET}`;

  const previo = await http(`${urlPublica}/version.json`, { headers: { 'Cache-Control': 'no-cache' } });
  if (previo.ok && !forzar) {
    const publicada = await previo.json().catch(() => null);
    if (publicada && version.versionCode <= publicada.versionCode) {
      throw new Error(
        `La compilación ${version.versionCode} no es mayor que la publicada (${publicada.versionCode}). ` +
          'Sube el número después del + en app/pubspec.yaml y vuelve a compilar (o usa --forzar).'
      );
    }
  }

  const nombre = nombreApk(version);
  const info = armarVersionJson({
    ...version,
    apkUrl: `${urlPublica}/${nombre}`,
    sha256: createHash('sha256').update(apk).digest('hex'),
    tamanoBytes: apk.length,
    notas,
  });

  const bucket = await asegurarBucket(credenciales, http);
  await subir(credenciales, http, nombre, apk, 'application/vnd.android.package-archive', 'max-age=3600');
  await subir(credenciales, http, 'version.json', JSON.stringify(info, null, 2), 'application/json', 'no-cache');
  return { bucket, nombre, info };
}

async function main() {
  const argumentos = process.argv.slice(2);
  const valor = (nombre) => {
    const i = argumentos.indexOf(nombre);
    return i >= 0 ? argumentos[i + 1] : undefined;
  };
  const rutaApk = valor('--apk') || path.join(raiz, 'app/build/app/outputs/flutter-apk/app-release.apk');
  if (!existsSync(rutaApk)) throw new Error(`No existe el APK: ${rutaApk}. Compílalo primero (ver los pasos arriba).`);

  const version = leerVersionPubspec(readFileSync(path.join(raiz, 'app/pubspec.yaml'), 'utf8'));
  const pubspecMasNuevo = statSync(path.join(raiz, 'app/pubspec.yaml')).mtimeMs > statSync(rutaApk).mtimeMs;
  if (pubspecMasNuevo && !argumentos.includes('--forzar')) {
    throw new Error('app/pubspec.yaml es más nuevo que el APK: vuelve a compilar para que el APK lleve esa versión.');
  }

  const credenciales = leerCredenciales(process.env, path.join(raiz, 'backend/.env'));
  const resultado = await publicar({
    apk: readFileSync(rutaApk),
    version,
    notas: valor('--notas'),
    credenciales,
    forzar: argumentos.includes('--forzar'),
  });

  console.log(`Publicado ${resultado.nombre} (bucket ${resultado.bucket}).`);
  console.log(`Los teléfonos ven: versión ${version.versionName} (compilación ${version.versionCode}).`);
  console.log(`version.json: ${credenciales.url}/storage/v1/object/public/${BUCKET}/version.json`);
}

if (import.meta.url === pathToFileURL(process.argv[1] || '').href) {
  main().catch((error) => {
    console.error(`Error: ${error.message}`);
    process.exit(1);
  });
}
