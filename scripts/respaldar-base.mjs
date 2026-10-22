#!/usr/bin/env node
// Respaldo automático de la base de datos de producción (Supabase).
//
// Hace un pg_dump comprimido, comprueba que el archivo sirva (que no esté vacío y que
// traiga las tablas principales), conserva solo los últimos respaldos y deja un registro.
// Solo LEE la base: no cambia nada en Supabase.
//
//   node scripts/respaldar-base.mjs
//   node scripts/respaldar-base.mjs --carpeta D:/respaldos --conservar 30
//   node scripts/respaldar-base.mjs --credenciales D:/ProyectoFinal/credenciales-supabase.env
//
// Credenciales: nunca van en el repositorio ni en la línea de comandos. Se leen de un archivo
// FUERA del proyecto (por defecto ../credenciales-supabase.env, junto a la carpeta del proyecto),
// con este formato (hay una plantilla en scripts/respaldar-base.env.example):
//
//   PGHOST=aws-0-us-east-1.pooler.supabase.com
//   PGUSER=postgres.xxxxxxxxxxxxxxxxxxxx
//   PGPASSWORD=la_contraseña_de_la_base
//   PGPORT=5432            (opcional)
//   PGDATABASE=postgres    (opcional)
//   PGSSLMODE=require      (opcional; Supabase lo exige, una base local no usa SSL)
//   RESPALDO_COPIA=E:/copias   (opcional: segunda carpeta, por ejemplo un pendrive o OneDrive)
//
// Para que corra solo todos los días en Windows: scripts/programar-respaldo.ps1.
//
// Salida: 0 = respaldo hecho y verificado; 1 = falló; 2 = faltan las credenciales.

import { spawn } from 'node:child_process';
import { existsSync, readdirSync, readFileSync } from 'node:fs';
import { appendFile, copyFile, mkdir, readdir, rename, rm, stat } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

import { leerEnv } from './respaldar-storage.mjs';

const aquí = path.dirname(fileURLToPath(import.meta.url));
const RAIZ_PROYECTO = path.resolve(aquí, '..');

export const PREFIJO = 'respaldo-muebleria-eden-produccion-';
export const EXTENSION = '.dump';
export const CONSERVAR_POR_DEFECTO = 30;
/** Un respaldo de esta base pesa cientos de KB: si pesa menos que esto, algo salió mal. */
export const MINIMO_BYTES = 50_000;
/** Tablas que no pueden faltar en un respaldo bueno. */
export const TABLAS_OBLIGATORIAS = ['usuarios', 'ventas', 'productos', 'clientes'];

const PATRON_NOMBRE = new RegExp(`^${PREFIJO}(\\d{4})-(\\d{2})-(\\d{2})${EXTENSION.replace('.', '\\.')}$`);

const dos = (n) => String(n).padStart(2, '0');

/** AAAA-MM-DD con la fecha LOCAL (no UTC: de noche, UTC ya es el día siguiente). */
export function fechaLocal(fecha) {
  return `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}`;
}

export function nombreDelRespaldo(fecha) {
  return `${PREFIJO}${fechaLocal(fecha)}${EXTENSION}`;
}

/** La fecha (AAAA-MM-DD) de un nombre de respaldo, o null si el archivo no es un respaldo. */
export function fechaDelNombre(nombre) {
  const m = PATRON_NOMBRE.exec(nombre);
  return m ? `${m[1]}-${m[2]}-${m[3]}` : null;
}

/**
 * Qué respaldos borrar. Se conservan los `conservar` más recientes y, además, el más antiguo
 * de cada mes (así queda una foto mensual aunque pasen meses). Solo toca archivos con el
 * nombre de un respaldo: cualquier otra cosa en la carpeta se ignora.
 */
export function elegirParaBorrar(nombres, conservar = CONSERVAR_POR_DEFECTO) {
  const respaldos = nombres
    .map((nombre) => ({ nombre, fecha: fechaDelNombre(nombre) }))
    .filter((r) => r.fecha)
    .sort((a, b) => (a.fecha < b.fecha ? 1 : a.fecha > b.fecha ? -1 : 0)); // del más nuevo al más viejo

  const guardar = new Set(respaldos.slice(0, conservar).map((r) => r.nombre));
  const primeroDelMes = new Map();
  for (const r of [...respaldos].reverse()) {
    const mes = r.fecha.slice(0, 7);
    if (!primeroDelMes.has(mes)) primeroDelMes.set(mes, r.nombre);
  }
  for (const nombre of primeroDelMes.values()) guardar.add(nombre);

  return respaldos.filter((r) => !guardar.has(r.nombre)).map((r) => r.nombre);
}

/** Comprueba que un respaldo sirva. Devuelve null si está bien, o el motivo si no. */
export function motivoDeRespaldoInvalido({ bytes, listado }) {
  if (bytes < MINIMO_BYTES) return `pesa solo ${bytes} bytes (se esperan más de ${MINIMO_BYTES})`;
  const faltan = TABLAS_OBLIGATORIAS.filter((tabla) => !new RegExp(`TABLE DATA public ${tabla}\\b`).test(listado));
  if (faltan.length) return `no trae los datos de: ${faltan.join(', ')}`;
  return null;
}

/** Lee y valida las credenciales; lanza un Error si falta algo. */
export function leerCredencialesDeBase(contenido) {
  const env = leerEnv(contenido);
  const faltan = ['PGHOST', 'PGUSER', 'PGPASSWORD'].filter((clave) => !env[clave]);
  if (faltan.length) throw new Error(`Faltan en el archivo de credenciales: ${faltan.join(', ')}`);
  return {
    PGHOST: env.PGHOST,
    PGUSER: env.PGUSER,
    PGPASSWORD: env.PGPASSWORD,
    PGPORT: env.PGPORT || '5432',
    PGDATABASE: env.PGDATABASE || 'postgres',
    PGSSLMODE: env.PGSSLMODE || 'require',
    copia: env.RESPALDO_COPIA || null,
  };
}

/** Busca pg_dump: primero en el PATH y, si no está (como cuando corre el Programador de tareas), en las carpetas de PostgreSQL. */
export function buscarHerramienta(nombre, { entorno = process.env, existe = existsSync, listar = leerCarpeta } = {}) {
  const exe = process.platform === 'win32' ? `${nombre}.exe` : nombre;
  if (entorno[`RESPALDO_${nombre.toUpperCase()}`]) return entorno[`RESPALDO_${nombre.toUpperCase()}`];
  const separador = process.platform === 'win32' ? ';' : ':';
  for (const carpeta of (entorno.PATH || entorno.Path || '').split(separador).filter(Boolean)) {
    const candidato = path.join(carpeta, exe);
    if (existe(candidato)) return candidato;
  }
  for (const raiz of ['C:/Program Files/PostgreSQL', 'D:/Program Files/PostgreSQL', '/usr/lib/postgresql']) {
    const versiones = listar(raiz).sort((a, b) => Number(b) - Number(a)); // la más nueva primero
    for (const version of versiones) {
      const candidato = path.join(raiz, version, 'bin', exe);
      if (existe(candidato)) return candidato;
    }
  }
  throw new Error(`No encontré ${exe}. Instalá las herramientas de PostgreSQL o definí RESPALDO_${nombre.toUpperCase()}.`);
}

/** Los nombres de lo que hay en una carpeta, o nada si no existe. Sincrónico: son unas pocas entradas. */
function leerCarpeta(carpeta) {
  try {
    return readdirSync(carpeta);
  } catch {
    return [];
  }
}

/** Ejecuta un comando y devuelve { codigo, salida, error }. El entorno se pasa aparte: las claves nunca van en los argumentos. */
export function ejecutarComando(comando, args, entorno) {
  return new Promise((resolve) => {
    const proceso = spawn(comando, args, { env: entorno, windowsHide: true });
    let salida = '';
    let error = '';
    proceso.stdout.on('data', (d) => (salida += d));
    proceso.stderr.on('data', (d) => (error += d));
    proceso.on('error', (e) => resolve({ codigo: -1, salida, error: String(e.message) }));
    proceso.on('close', (codigo) => resolve({ codigo: codigo ?? -1, salida, error }));
  });
}

function sinClaves(texto, credenciales) {
  let limpio = String(texto);
  for (const secreto of [credenciales.PGPASSWORD]) {
    if (secreto && secreto.length >= 3) limpio = limpio.split(secreto).join('***');
  }
  return limpio;
}

/**
 * Hace el respaldo completo. Las dependencias se pueden reemplazar para probarlo sin base de datos.
 * Devuelve { archivo, bytes, borrados, copia } o lanza un Error con el motivo.
 */
export async function respaldar({
  carpeta,
  conservar = CONSERVAR_POR_DEFECTO,
  credenciales,
  ahora = () => new Date(),
  ejecutar = ejecutarComando,
  pgDump = () => buscarHerramienta('pg_dump'),
  pgRestore = () => buscarHerramienta('pg_restore'),
}) {
  await mkdir(carpeta, { recursive: true });
  const nombre = nombreDelRespaldo(ahora());
  const destino = path.join(carpeta, nombre);
  const parcial = `${destino}.parcial`;
  const entorno = {
    ...process.env,
    PGHOST: credenciales.PGHOST,
    PGPORT: credenciales.PGPORT,
    PGUSER: credenciales.PGUSER,
    PGDATABASE: credenciales.PGDATABASE,
    PGPASSWORD: credenciales.PGPASSWORD,
    PGSSLMODE: credenciales.PGSSLMODE || 'require',
    PGOPTIONS: '',
  };

  try {
    const volcado = await ejecutar(pgDump(), ['-Fc', '--no-owner', '--no-privileges', '-f', parcial], entorno);
    if (volcado.codigo !== 0) {
      throw new Error(`pg_dump falló (código ${volcado.codigo}): ${sinClaves(volcado.error, credenciales).trim().split('\n').pop()}`);
    }

    const bytes = (await stat(parcial)).size;
    const lista = await ejecutar(pgRestore(), ['-l', parcial], entorno);
    if (lista.codigo !== 0) {
      throw new Error(`el respaldo no se pudo leer con pg_restore: ${sinClaves(lista.error, credenciales).trim().split('\n').pop()}`);
    }
    const motivo = motivoDeRespaldoInvalido({ bytes, listado: lista.salida });
    if (motivo) throw new Error(`el respaldo no sirve: ${motivo}`);

    await rename(parcial, destino); // recién acá existe como respaldo; antes era un archivo parcial
  } catch (error) {
    await rm(parcial, { force: true });
    throw error;
  }

  const bytes = (await stat(destino)).size;
  const borrados = elegirParaBorrar(await readdir(carpeta), conservar);
  for (const viejo of borrados) await rm(path.join(carpeta, viejo), { force: true });

  let copia = null;
  if (credenciales.copia) {
    try {
      await mkdir(credenciales.copia, { recursive: true });
      await copyFile(destino, path.join(credenciales.copia, nombre));
      copia = credenciales.copia;
    } catch (error) {
      copia = `no se pudo copiar a ${credenciales.copia}: ${error.message}`;
    }
  }
  return { archivo: destino, bytes, borrados, copia };
}

export function argumentosDeLinea(argv) {
  const valor = (bandera) => {
    const i = argv.indexOf(bandera);
    return i >= 0 && argv[i + 1] ? argv[i + 1] : null;
  };
  return {
    carpeta: valor('--carpeta'),
    credenciales: valor('--credenciales'),
    conservar: valor('--conservar') ? Number(valor('--conservar')) : null,
  };
}

async function principal() {
  const args = argumentosDeLinea(process.argv.slice(2));
  const carpeta = path.resolve(args.carpeta || process.env.RESPALDO_CARPETA || path.join(RAIZ_PROYECTO, '..', 'respaldos'));
  const archivoCredenciales = path.resolve(
    args.credenciales || process.env.RESPALDO_CREDENCIALES || path.join(RAIZ_PROYECTO, '..', 'credenciales-supabase.env'),
  );
  const registro = path.join(carpeta, 'respaldo.log');
  const sello = () => {
    const d = new Date();
    return `${fechaLocal(d)} ${dos(d.getHours())}:${dos(d.getMinutes())}:${dos(d.getSeconds())}`;
  };
  const anotar = async (texto) => {
    console.log(texto);
    try {
      await mkdir(carpeta, { recursive: true });
      await appendFile(registro, `${sello()}  ${texto}\n`);
    } catch {
      // si no se puede escribir el registro, igual se informa por consola
    }
  };

  if (!existsSync(archivoCredenciales)) {
    await anotar(`FALTAN LAS CREDENCIALES: no existe ${archivoCredenciales} (plantilla: scripts/respaldar-base.env.example)`);
    process.exit(2);
  }

  let credenciales;
  try {
    credenciales = leerCredencialesDeBase(readFileSync(archivoCredenciales, 'utf8'));
  } catch (error) {
    await anotar(`FALLO: ${error.message}`);
    process.exit(2);
  }

  try {
    const r = await respaldar({ carpeta, conservar: args.conservar || CONSERVAR_POR_DEFECTO, credenciales });
    const copia = r.copia ? ` | copia: ${r.copia}` : '';
    await anotar(`OK  ${path.basename(r.archivo)}  ${Math.round(r.bytes / 1024)} KB  | borrados por antiguos: ${r.borrados.length}${copia}`);
  } catch (error) {
    await anotar(`FALLO: ${sinClaves(error.message, credenciales)}`);
    process.exit(1);
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  principal();
}
