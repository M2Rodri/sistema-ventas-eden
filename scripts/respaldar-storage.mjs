#!/usr/bin/env node
// Respaldo a mano de los archivos de Supabase Storage.
//
// Descarga todo lo que hay en los buckets "productos" y "comprobantes" a una
// carpeta local, conservando la estructura: respaldo-storage/<bucket>/<ruta>.
//
// Usa las mismas variables de entorno que el backend: SUPABASE_URL y
// SUPABASE_SERVICE_KEY. Si no están en el entorno, las busca en backend/.env.
// Solo lee: no borra ni cambia nada en Supabase.
//
//   node scripts/respaldar-storage.mjs
//   node scripts/respaldar-storage.mjs --carpeta D:/respaldos/edén
//   node scripts/respaldar-storage.mjs --forzar      (vuelve a bajar todo)
//
// La carpeta de respaldo está en .gitignore: los comprobantes son datos de pago
// y no van al repositorio.

import { existsSync, readFileSync } from 'node:fs';
import { mkdir, stat, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

export const BUCKETS = ['productos', 'comprobantes'];
const POR_PAGINA = 100;

/** Lee un archivo .env simple (CLAVE=valor); ignora comentarios y líneas vacías. */
export function leerEnv(contenido) {
  const valores = {};
  for (const linea of contenido.split(/\r?\n/)) {
    const limpia = linea.trim();
    if (!limpia || limpia.startsWith('#')) continue;
    const igual = limpia.indexOf('=');
    if (igual < 0) continue;
    const clave = limpia.slice(0, igual).trim();
    let valor = limpia.slice(igual + 1).trim();
    if ((valor.startsWith('"') && valor.endsWith('"')) || (valor.startsWith("'") && valor.endsWith("'"))) {
      valor = valor.slice(1, -1);
    }
    valores[clave] = valor;
  }
  return valores;
}

/** Credenciales del entorno; si faltan, de backend/.env. */
export function leerCredenciales(entorno = process.env, archivoEnv = null) {
  let url = entorno.SUPABASE_URL;
  let clave = entorno.SUPABASE_SERVICE_KEY;
  if ((!url || !clave) && archivoEnv && existsSync(archivoEnv)) {
    const delArchivo = leerEnv(readFileSync(archivoEnv, 'utf8'));
    url = url || delArchivo.SUPABASE_URL;
    clave = clave || delArchivo.SUPABASE_SERVICE_KEY;
  }
  if (!url || !clave) {
    throw new Error(
      'Faltan SUPABASE_URL y SUPABASE_SERVICE_KEY. Defínalas en el entorno o en backend/.env ' +
        '(Supabase: Project Settings → API).'
    );
  }
  return { url: url.trim().replace(/\/+$/, ''), clave: clave.trim() };
}

function cabeceras(clave) {
  return { Authorization: `Bearer ${clave}`, apikey: clave };
}

/** Lista todos los archivos de un bucket, entrando a cada carpeta. */
export async function listarArchivos({ url, clave }, bucket, prefijo = '') {
  const archivos = [];
  let desplazamiento = 0;
  for (;;) {
    const respuesta = await fetch(`${url}/storage/v1/object/list/${bucket}`, {
      method: 'POST',
      headers: { ...cabeceras(clave), 'Content-Type': 'application/json' },
      body: JSON.stringify({
        prefix: prefijo,
        limit: POR_PAGINA,
        offset: desplazamiento,
        sortBy: { column: 'name', order: 'asc' },
      }),
    });
    if (!respuesta.ok) {
      throw new Error(`No se pudo listar "${bucket}" (${respuesta.status}): ${(await respuesta.text()).slice(0, 200)}`);
    }
    const elementos = await respuesta.json();
    for (const elemento of elementos) {
      const ruta = prefijo ? `${prefijo}/${elemento.name}` : elemento.name;
      // En Storage, una carpeta aparece sin id; un archivo trae id.
      if (elemento.id) {
        archivos.push({ ruta, tamano: elemento.metadata?.size ?? null });
      } else {
        archivos.push(...(await listarArchivos({ url, clave }, bucket, ruta)));
      }
    }
    if (elementos.length < POR_PAGINA) break;
    desplazamiento += POR_PAGINA;
  }
  return archivos;
}

function codificarRuta(ruta) {
  return ruta.split('/').map(encodeURIComponent).join('/');
}

/** Baja un bucket completo. Devuelve cuántos archivos se bajaron, se saltearon y fallaron. */
export async function respaldarBucket(credenciales, bucket, carpetaDestino, { forzar = false, log = console.log } = {}) {
  const resumen = { bajados: 0, salteados: 0, fallidos: 0 };
  const raiz = path.resolve(carpetaDestino, bucket);
  await mkdir(raiz, { recursive: true });

  const archivos = await listarArchivos(credenciales, bucket);
  log(`  ${bucket}: ${archivos.length} archivo(s)`);

  for (const { ruta, tamano } of archivos) {
    const destino = path.resolve(raiz, ...ruta.split('/'));
    // Nunca escribir fuera de la carpeta de respaldo, aunque el nombre sea raro.
    if (!destino.startsWith(raiz + path.sep)) {
      log(`  ✗ ${ruta}: nombre no permitido`);
      resumen.fallidos++;
      continue;
    }
    try {
      if (!forzar && tamano != null && existsSync(destino) && (await stat(destino)).size === tamano) {
        resumen.salteados++;
        continue;
      }
      const respuesta = await fetch(`${credenciales.url}/storage/v1/object/${bucket}/${codificarRuta(ruta)}`, {
        headers: cabeceras(credenciales.clave),
      });
      if (!respuesta.ok) throw new Error(`respondió ${respuesta.status}`);
      await mkdir(path.dirname(destino), { recursive: true });
      await writeFile(destino, Buffer.from(await respuesta.arrayBuffer()));
      resumen.bajados++;
    } catch (error) {
      log(`  ✗ ${ruta}: ${error.message}`);
      resumen.fallidos++;
    }
  }
  return resumen;
}

async function principal() {
  const argumentos = process.argv.slice(2);
  const forzar = argumentos.includes('--forzar');
  const indice = argumentos.indexOf('--carpeta');
  const raizProyecto = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
  const carpeta = indice >= 0 && argumentos[indice + 1] ? argumentos[indice + 1] : path.join(raizProyecto, 'respaldo-storage');

  const credenciales = leerCredenciales(process.env, path.join(raizProyecto, 'backend', '.env'));

  console.log(`Respaldo de Supabase Storage → ${path.resolve(carpeta)}`);
  const total = { bajados: 0, salteados: 0, fallidos: 0 };
  for (const bucket of BUCKETS) {
    const resumen = await respaldarBucket(credenciales, bucket, carpeta, { forzar });
    total.bajados += resumen.bajados;
    total.salteados += resumen.salteados;
    total.fallidos += resumen.fallidos;
  }
  console.log(`Listo: ${total.bajados} bajado(s), ${total.salteados} ya estaban, ${total.fallidos} con error.`);
  if (total.fallidos > 0) process.exitCode = 1;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  principal().catch((error) => {
    console.error(`Error: ${error.message}`);
    process.exit(1);
  });
}
