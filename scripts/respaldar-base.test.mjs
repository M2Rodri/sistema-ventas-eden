// Pruebas del respaldo automático de la base, sin base de datos real: un "pg_dump" falso escribe un archivo.
// Se corre con:  node --test scripts/*.test.mjs

import assert from 'node:assert/strict';
import { mkdtemp, readdir, readFile, rm, stat, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { after, before, test } from 'node:test';

import {
  argumentosDeLinea,
  buscarHerramienta,
  elegirParaBorrar,
  fechaDelNombre,
  fechaLocal,
  leerCredencialesDeBase,
  MINIMO_BYTES,
  motivoDeRespaldoInvalido,
  nombreDelRespaldo,
  respaldar,
} from './respaldar-base.mjs';

const CREDENCIALES = {
  PGHOST: 'servidor.falso',
  PGUSER: 'usuario.falso',
  PGPASSWORD: 'clave-secreta-de-prueba',
  PGPORT: '5432',
  PGDATABASE: 'postgres',
  copia: null,
};
const LISTADO_BUENO = [
  '123; 0 16386 TABLE DATA public usuarios postgres',
  '124; 0 16390 TABLE DATA public ventas postgres',
  '125; 0 16394 TABLE DATA public productos postgres',
  '126; 0 16398 TABLE DATA public clientes postgres',
].join('\n');

let carpeta;
before(async () => {
  carpeta = await mkdtemp(path.join(tmpdir(), 'respaldo-base-'));
});
after(async () => {
  await rm(carpeta, { recursive: true, force: true });
});

/** Un pg_dump / pg_restore falsos: escriben el archivo que se les pide y devuelven lo configurado. */
function ejecutorFalso({ bytes = MINIMO_BYTES + 10, listado = LISTADO_BUENO, codigoDump = 0, errorDump = '' } = {}) {
  const llamadas = [];
  const ejecutar = async (comando, args, entorno) => {
    llamadas.push({ comando, args, entorno });
    if (comando === 'pg_dump') {
      if (codigoDump === 0) await writeFile(args[args.indexOf('-f') + 1], Buffer.alloc(bytes, 1));
      return { codigo: codigoDump, salida: '', error: errorDump };
    }
    return { codigo: 0, salida: listado, error: '' };
  };
  return { ejecutar, llamadas };
}

const opcionesBase = (extra = {}) => ({
  carpeta,
  credenciales: CREDENCIALES,
  ahora: () => new Date(2026, 9, 22, 21, 0, 0),
  pgDump: () => 'pg_dump',
  pgRestore: () => 'pg_restore',
  ...extra,
});

// ---------- nombres y fechas ----------

test('el nombre lleva la fecha LOCAL, no la UTC', () => {
  // 22/10/2026 a las 23:30 hora local: en UTC ya sería el 23, el nombre debe seguir diciendo 22.
  const casiMedianoche = new Date(2026, 9, 22, 23, 30, 0);
  assert.equal(fechaLocal(casiMedianoche), '2026-10-22');
  assert.equal(nombreDelRespaldo(casiMedianoche), 'respaldo-muebleria-eden-produccion-2026-10-22.dump');
});

test('la fecha se lee del nombre, y lo que no es un respaldo se ignora', () => {
  assert.equal(fechaDelNombre('respaldo-muebleria-eden-produccion-2026-10-07.dump'), '2026-10-07');
  assert.equal(fechaDelNombre('respaldo.log'), null);
  assert.equal(fechaDelNombre('respaldo-muebleria-eden-produccion-2026-10-07.dump.parcial'), null);
  assert.equal(fechaDelNombre('otra-cosa.dump'), null);
});

// ---------- cuáles se borran ----------

const respaldoDe = (fecha) => `respaldo-muebleria-eden-produccion-${fecha}.dump`;

test('con pocos respaldos no se borra nada', () => {
  const nombres = ['2026-10-20', '2026-10-21', '2026-10-22'].map(respaldoDe);
  assert.deepEqual(elegirParaBorrar(nombres, 30), []);
});

test('se conservan los más recientes y se borran los más viejos', () => {
  // 5 respaldos seguidos, se conservan 3: se borran los 2 más viejos (que además no son los primeros del mes... salvo el 1.º)
  const nombres = ['2026-10-02', '2026-10-03', '2026-10-04', '2026-10-05', '2026-10-06'].map(respaldoDe);
  const borrar = elegirParaBorrar(nombres, 3);
  // El 02 es el primero del mes y se conserva como foto mensual; se borra solo el 03.
  assert.deepEqual(borrar, [respaldoDe('2026-10-03')]);
});

test('queda siempre una foto por mes: el primer respaldo de cada mes', () => {
  const nombres = ['2026-07-05', '2026-07-20', '2026-08-01', '2026-08-15', '2026-09-03', '2026-10-21', '2026-10-22'].map(respaldoDe);
  const borrar = elegirParaBorrar(nombres, 2); // los 2 más nuevos: 21 y 22 de octubre
  assert.deepEqual(
    borrar.sort(),
    ['2026-07-20', '2026-08-15'].map(respaldoDe), // se van los que no son el primero de su mes
  );
});

test('elegirParaBorrar no toca archivos que no son respaldos', () => {
  const nombres = ['respaldo.log', 'notas.txt', respaldoDe('2026-10-22')];
  assert.deepEqual(elegirParaBorrar(nombres, 1), []);
});

// ---------- verificación del respaldo ----------

test('un respaldo chico o sin las tablas principales no sirve', () => {
  assert.equal(motivoDeRespaldoInvalido({ bytes: 500_000, listado: LISTADO_BUENO }), null);
  assert.match(motivoDeRespaldoInvalido({ bytes: 100, listado: LISTADO_BUENO }), /pesa solo 100 bytes/);
  const sinVentas = LISTADO_BUENO.replace('TABLE DATA public ventas', 'TABLE public ventas');
  assert.match(motivoDeRespaldoInvalido({ bytes: 500_000, listado: sinVentas }), /ventas/);
});

// ---------- credenciales ----------

test('las credenciales se leen del archivo y se exigen las obligatorias', () => {
  const ok = leerCredencialesDeBase('PGHOST=h\nPGUSER=u\nPGPASSWORD=p\n');
  assert.deepEqual([ok.PGHOST, ok.PGUSER, ok.PGPASSWORD, ok.PGPORT, ok.PGDATABASE, ok.copia], ['h', 'u', 'p', '5432', 'postgres', null]);
  assert.equal(leerCredencialesDeBase('PGHOST=h\nPGUSER=u\nPGPASSWORD=p\nRESPALDO_COPIA=E:/x').copia, 'E:/x');
  assert.throws(() => leerCredencialesDeBase('PGHOST=h\nPGUSER=u\n'), /PGPASSWORD/);
});

test('los argumentos de línea', () => {
  assert.deepEqual(argumentosDeLinea(['--carpeta', 'D:/r', '--conservar', '10']), { carpeta: 'D:/r', credenciales: null, conservar: 10 });
  assert.deepEqual(argumentosDeLinea([]), { carpeta: null, credenciales: null, conservar: null });
});

test('buscarHerramienta usa el PATH y, si no está, las carpetas de PostgreSQL (la más nueva primero)', () => {
  const existen = new Set([path.join('C:/herramientas', 'pg_dump.exe'), path.join('C:/Program Files/PostgreSQL', '18', 'bin', 'pg_dump.exe')]);
  const existe = (p) => existen.has(path.normalize(p)) || existen.has(p);
  if (process.platform === 'win32') {
    assert.equal(buscarHerramienta('pg_dump', { entorno: { PATH: 'C:/herramientas;C:/otra' }, existe, listar: () => [] }), path.join('C:/herramientas', 'pg_dump.exe'));
    assert.equal(
      buscarHerramienta('pg_dump', { entorno: { PATH: 'C:/nada' }, existe, listar: (r) => (r.startsWith('C:') ? ['16', '18'] : []) }),
      path.join('C:/Program Files/PostgreSQL', '18', 'bin', 'pg_dump.exe'),
    );
  }
  assert.throws(() => buscarHerramienta('pg_dump', { entorno: { PATH: '' }, existe: () => false, listar: () => [] }), /No encontré/);
  assert.equal(buscarHerramienta('pg_dump', { entorno: { RESPALDO_PG_DUMP: 'X:/mi/pg_dump' } }), 'X:/mi/pg_dump');
});

// ---------- el respaldo completo ----------

test('un respaldo bueno queda con su nombre, verificado, y sin archivos parciales', async () => {
  const { ejecutar, llamadas } = ejecutorFalso();
  const r = await respaldar(opcionesBase({ ejecutar }));

  assert.equal(path.basename(r.archivo), 'respaldo-muebleria-eden-produccion-2026-10-22.dump');
  assert.equal((await stat(r.archivo)).size, MINIMO_BYTES + 10);
  assert.deepEqual((await readdir(carpeta)).filter((n) => n.endsWith('.parcial')), []);
  // pg_dump con los argumentos esperados, y la clave solo en el entorno, nunca en los argumentos
  const dump = llamadas.find((l) => l.comando === 'pg_dump');
  assert.deepEqual(dump.args.slice(0, 3), ['-Fc', '--no-owner', '--no-privileges']);
  assert.equal(dump.entorno.PGPASSWORD, CREDENCIALES.PGPASSWORD);
  assert.ok(!dump.args.join(' ').includes(CREDENCIALES.PGPASSWORD), 'la clave no puede ir en los argumentos');
});

test('si pg_dump falla no queda ningún archivo y el error no revela la clave', async () => {
  const antes = await readdir(carpeta);
  const { ejecutar } = ejecutorFalso({ codigoDump: 1, errorDump: `conexión rechazada con clave-secreta-de-prueba` });

  await assert.rejects(
    () => respaldar(opcionesBase({ ejecutar, ahora: () => new Date(2026, 9, 23, 21, 0, 0) })),
    (error) => {
      assert.match(error.message, /pg_dump falló/);
      assert.ok(!error.message.includes('clave-secreta-de-prueba'), 'el mensaje no debe traer la clave');
      return true;
    },
  );
  assert.deepEqual(await readdir(carpeta), antes);
});

test('un respaldo vacío o incompleto se rechaza y se descarta', async () => {
  const antes = await readdir(carpeta);

  await assert.rejects(
    () => respaldar(opcionesBase({ ejecutar: ejecutorFalso({ bytes: 100 }).ejecutar, ahora: () => new Date(2026, 9, 24, 21, 0, 0) })),
    /el respaldo no sirve/,
  );
  await assert.rejects(
    () => respaldar(opcionesBase({ ejecutar: ejecutorFalso({ listado: 'sin tablas' }).ejecutar, ahora: () => new Date(2026, 9, 24, 21, 0, 0) })),
    /el respaldo no sirve/,
  );
  assert.deepEqual(await readdir(carpeta), antes, 'un respaldo malo no debe quedar en la carpeta');
});

test('al terminar borra los respaldos que sobran, pero no otros archivos', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'respaldo-rotar-'));
  try {
    for (const f of ['2026-09-01', '2026-09-10', '2026-09-20', '2026-10-01']) await writeFile(path.join(dir, respaldoDe(f)), 'x');
    await writeFile(path.join(dir, 'respaldo.log'), 'registro');

    const r = await respaldar({ ...opcionesBase({ ejecutar: ejecutorFalso().ejecutar }), carpeta: dir, conservar: 2 });

    const quedan = (await readdir(dir)).sort();
    assert.ok(quedan.includes('respaldo.log'), 'no se debe tocar el registro');
    assert.ok(quedan.includes(respaldoDe('2026-09-01')), 'queda la foto de septiembre (el primero del mes)');
    assert.ok(quedan.includes(respaldoDe('2026-10-01')) && quedan.includes('respaldo-muebleria-eden-produccion-2026-10-22.dump'));
    assert.deepEqual(r.borrados.sort(), [respaldoDe('2026-09-10'), respaldoDe('2026-09-20')]);
  } finally {
    await rm(dir, { recursive: true, force: true });
  }
});

test('con RESPALDO_COPIA deja una copia, y si la copia falla el respaldo igual queda hecho', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'respaldo-copia-'));
  const copia = await mkdtemp(path.join(tmpdir(), 'respaldo-destino-'));
  try {
    const r = await respaldar({ ...opcionesBase({ ejecutar: ejecutorFalso().ejecutar }), carpeta: dir, credenciales: { ...CREDENCIALES, copia } });
    assert.equal(r.copia, copia);
    assert.equal((await readFile(path.join(copia, path.basename(r.archivo)))).length, MINIMO_BYTES + 10);

    // Una "carpeta" que es en realidad un archivo: no se puede crear. El respaldo principal no se pierde.
    const archivoComoCarpeta = path.join(dir, 'no-es-carpeta');
    await writeFile(archivoComoCarpeta, 'x');
    const r2 = await respaldar({
      ...opcionesBase({ ejecutar: ejecutorFalso().ejecutar, ahora: () => new Date(2026, 9, 25, 21, 0, 0) }),
      carpeta: dir,
      credenciales: { ...CREDENCIALES, copia: path.join(archivoComoCarpeta, 'dentro') },
    });
    assert.match(r2.copia, /no se pudo copiar/);
    assert.ok((await stat(r2.archivo)).size > 0);
  } finally {
    await rm(dir, { recursive: true, force: true });
    await rm(copia, { recursive: true, force: true });
  }
});
