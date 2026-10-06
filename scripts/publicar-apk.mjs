#!/usr/bin/env node
// Publica una versión nueva de la app móvil para que los teléfonos la ofrezcan solos.
//
// Flujo:
//   1. Crea un release en GitHub con tag v<versionName> y sube el APK como asset.
//   2. Sube version.json a Supabase Storage (bucket "app") apuntando a la URL de GitHub.
//
// Pasos para publicar una versión:
//   1. En app/pubspec.yaml subir la versión Y el número de compilación:
//        version: 1.1.0+3        (el número después del + SIEMPRE tiene que crecer)
//   2. Compilar firmado con la llave propia (app/android/key.properties):
//        cd app
//        flutter build apk --release --dart-define=API_URL=https://sistema-ventas-eden-1.onrender.com
//   3. Publicar:
//        node scripts/publicar-apk.mjs --notas "Qué cambió en esta versión"
//
// Requisitos:
//   - gh CLI autenticado (gh auth login)
//   - SUPABASE_URL y SUPABASE_SERVICE_KEY en backend/.env
//
// Se niega a publicar si la compilación no es mayor que la ya publicada (los teléfonos
// no se enterarían); --forzar lo permite.

import { createHash } from 'node:crypto';
import { copyFileSync, existsSync, readFileSync, statSync, unlinkSync } from 'node:fs';
import { execSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

import { leerCredenciales } from './respaldar-storage.mjs';

export const BUCKET = 'app';
const REPO = 'M2Rodri/sistema-ventas-eden';
const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

/** Lee "version: 1.1.0+3" de pubspec.yaml. */
export function leerVersionPubspec(contenido) {
  const m = contenido.match(/^version:\s*(\d+\.\d+\.\d+)\+(\d+)\s*$/m);
  if (!m) throw new Error('No encontré "version: X.Y.Z+N" en app/pubspec.yaml.');
  return { versionName: m[1], versionCode: Number(m[2]) };
}

/** El contenido de version.json, tal como lo lee la app. */
export function armarVersionJson({ versionName, versionCode, apkUrl, sha256, tamanoBytes, notas }) {
  return { versionCode, versionName, apkUrl, sha256, tamanoBytes, notas: notas || '' };
}

export function nombreApk({ versionName }) {
  return `muebleria-eden.apk`;
}

/** Sube version.json a Supabase (upsert). */
async function subirVersionJson({ url, clave }, http, info) {
  // Asegurar bucket
  const rb = await http(`${url}/storage/v1/bucket`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${clave}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ id: BUCKET, name: BUCKET, public: true }),
  });
  if (!rb.ok) {
    const t = await rb.text();
    if (rb.status !== 409 && !/already exists|Duplicate/i.test(t))
      throw new Error(`No pude crear el bucket "${BUCKET}": ${rb.status} ${t}`);
  }

  const r = await http(`${url}/storage/v1/object/${BUCKET}/version.json`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${clave}`,
      'Content-Type': 'application/json',
      'x-upsert': 'true',
      'cache-control': 'no-cache',
    },
    body: JSON.stringify(info, null, 2),
  });
  if (!r.ok) throw new Error(`No pude subir version.json: ${r.status} ${await r.text()}`);
}

/** Crea release en GitHub y sube el APK. Devuelve la URL de descarga directa. */
function publicarEnGitHub({ rutaApk, versionName, notas }) {
  const tag = `v${versionName}`;
  const nombreArchivo = 'muebleria-eden.apk';

  // Copiar APK con el nombre correcto antes de subir (el # no funciona bien en Windows)
  const dirTemp = path.dirname(rutaApk);
  const rutaTemp = path.join(dirTemp, nombreArchivo);
  copyFileSync(rutaApk, rutaTemp);

  // Borrar release anterior con el mismo tag si existe (para poder re-publicar)
  try {
    execSync(`gh release delete ${tag} --repo ${REPO} --yes --cleanup-tag`, { stdio: 'pipe' });
    console.log(`  Release ${tag} anterior eliminado.`);
  } catch (_) {
    // No existía, normal
  }

  try {
    // Crear nuevo release y subir APK ya con el nombre correcto
    const notasFlag = notas ? `--notes "${notas.replace(/"/g, '\\"')}"` : '--notes ""';
    console.log(`  Creando release ${tag} en GitHub y subiendo APK (~52 MB, puede tardar)...`);
    execSync(
      `gh release create ${tag} "${rutaTemp}" --repo ${REPO} --title "Versión ${versionName}" ${notasFlag}`,
      { stdio: 'inherit' }
    );
  } finally {
    // Limpiar archivo temporal
    try { unlinkSync(rutaTemp); } catch (_) {}
  }

  return `https://github.com/${REPO}/releases/download/${tag}/${nombreArchivo}`;
}

/** Flujo completo: GitHub Release + Supabase version.json. */
export async function publicar({ rutaApk, apkBytes, version, notas, credenciales, forzar = false, http = fetch }) {
  const urlPublica = `${credenciales.url}/storage/v1/object/public/${BUCKET}`;

  // Verificar que versionCode sea mayor al publicado
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

  // 1. Subir APK a GitHub Releases
  console.log('📦 Subiendo APK a GitHub Releases...');
  const apkUrl = publicarEnGitHub({ rutaApk, versionName: version.versionName, notas });
  console.log(`  ✅ APK disponible en: ${apkUrl}`);

  // 2. Actualizar version.json en Supabase
  console.log('📋 Actualizando version.json en Supabase...');
  const info = armarVersionJson({
    ...version,
    apkUrl,
    sha256: createHash('sha256').update(apkBytes).digest('hex'),
    tamanoBytes: apkBytes.length,
    notas,
  });
  await subirVersionJson(credenciales, http, info);
  console.log(`  ✅ version.json actualizado (versionCode: ${version.versionCode})`);

  return { apkUrl, info };
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
    throw new Error('app/pubspec.yaml es más nuevo que el APK: vuelve a compilar para que el APK lleve esa versión (o usa --forzar).');
  }

  const credenciales = leerCredenciales(process.env, path.join(raiz, 'backend/.env'));
  const apkBytes = readFileSync(rutaApk);

  const resultado = await publicar({
    rutaApk,
    apkBytes,
    version,
    notas: valor('--notas'),
    credenciales,
    forzar: argumentos.includes('--forzar'),
  });

  console.log('');
  console.log(`🎉 ¡Publicado! Versión ${version.versionName} (compilación ${version.versionCode})`);
  console.log(`   APK:          ${resultado.apkUrl}`);
  console.log(`   version.json: ${credenciales.url}/storage/v1/object/public/${BUCKET}/version.json`);
  console.log('');
  console.log('Los teléfonos recibirán la notificación de actualización al abrir la app.');
}

if (import.meta.url === pathToFileURL(process.argv[1] || '').href) {
  main().catch((error) => {
    console.error(`Error: ${error.message}`);
    process.exit(1);
  });
}
