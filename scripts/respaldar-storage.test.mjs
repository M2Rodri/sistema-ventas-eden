// Prueba del script de respaldo contra un servidor falso que imita Supabase Storage.
// Se corre con:  node --test scripts/

import assert from 'node:assert/strict';
import { mkdtemp, readFile, rm, writeFile, mkdir } from 'node:fs/promises';
import { createServer } from 'node:http';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { after, before, test } from 'node:test';

import { leerCredenciales, leerEnv, listarArchivos, respaldarBucket } from './respaldar-storage.mjs';

// Contenido falso: "productos" tiene un archivo suelto y una carpeta; "comprobantes", un archivo.
const archivos = {
  productos: { '': [{ name: 'a.png', id: '1', metadata: { size: 3 } }, { name: 'sub', id: null }],
               sub: [{ name: 'b.jpg', id: '2', metadata: { size: 2 } }] },
  comprobantes: { '': [{ name: 'pago.jpg', id: '3', metadata: { size: 4 } }] },
};
const contenido = { 'productos/a.png': 'AAA', 'productos/sub/b.jpg': 'BB', 'comprobantes/pago.jpg': 'PPPP' };

let servidor;
let credenciales;
let carpeta;
const pedidos = [];

before(async () => {
  servidor = createServer((req, res) => {
    pedidos.push(`${req.method} ${req.url} ${req.headers.authorization}`);
    const lista = req.url.match(/^\/storage\/v1\/object\/list\/(\w+)$/);
    if (req.method === 'POST' && lista) {
      let cuerpo = '';
      req.on('data', (d) => (cuerpo += d));
      req.on('end', () => {
        const { prefix } = JSON.parse(cuerpo);
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(archivos[lista[1]]?.[prefix] ?? []));
      });
      return;
    }
    const objeto = req.url.match(/^\/storage\/v1\/object\/(\w+)\/(.+)$/);
    if (req.method === 'GET' && objeto && contenido[`${objeto[1]}/${objeto[2]}`]) {
      res.end(contenido[`${objeto[1]}/${objeto[2]}`]);
      return;
    }
    res.statusCode = 404;
    res.end('no existe');
  });
  await new Promise((resolver) => servidor.listen(0, '127.0.0.1', resolver));
  credenciales = { url: `http://127.0.0.1:${servidor.address().port}`, clave: 'clave-de-prueba' };
  carpeta = await mkdtemp(path.join(tmpdir(), 'respaldo-'));
});

after(async () => {
  servidor.close();
  await rm(carpeta, { recursive: true, force: true });
});

test('lista los archivos entrando a las carpetas', async () => {
  const lista = await listarArchivos(credenciales, 'productos');
  assert.deepEqual(lista.map((a) => a.ruta).sort(), ['a.png', 'sub/b.jpg']);
});

test('baja los dos buckets conservando la estructura y manda la clave solo como cabecera', async () => {
  const productos = await respaldarBucket(credenciales, 'productos', carpeta, { log: () => {} });
  const comprobantes = await respaldarBucket(credenciales, 'comprobantes', carpeta, { log: () => {} });

  assert.equal(productos.bajados, 2);
  assert.equal(comprobantes.bajados, 1);
  assert.equal(await readFile(path.join(carpeta, 'productos', 'a.png'), 'utf8'), 'AAA');
  assert.equal(await readFile(path.join(carpeta, 'productos', 'sub', 'b.jpg'), 'utf8'), 'BB');
  assert.equal(await readFile(path.join(carpeta, 'comprobantes', 'pago.jpg'), 'utf8'), 'PPPP');
  assert.ok(pedidos.every((p) => p.includes('Bearer clave-de-prueba')));
  assert.ok(pedidos.every((p) => !p.split(' ')[1].includes('clave-de-prueba')), 'la clave no va en la URL');
});

test('una segunda corrida no vuelve a bajar lo que ya está, y --forzar sí', async () => {
  const otra = await respaldarBucket(credenciales, 'productos', carpeta, { log: () => {} });
  assert.deepEqual(otra, { bajados: 0, salteados: 2, fallidos: 0 });

  const forzada = await respaldarBucket(credenciales, 'productos', carpeta, { forzar: true, log: () => {} });
  assert.equal(forzada.bajados, 2);
});

test('un archivo que falla se cuenta y no detiene el resto', async () => {
  archivos.productos[''].push({ name: 'fantasma.png', id: '9', metadata: { size: 1 } });
  const resumen = await respaldarBucket(credenciales, 'productos', carpeta, { forzar: true, log: () => {} });
  assert.equal(resumen.fallidos, 1);
  assert.equal(resumen.bajados, 2);
  archivos.productos[''].pop();
});

test('lee las credenciales del entorno y, si faltan, de backend/.env', async () => {
  assert.deepEqual(leerCredenciales({ SUPABASE_URL: 'https://x.supabase.co/', SUPABASE_SERVICE_KEY: 'k' }),
    { url: 'https://x.supabase.co', clave: 'k' });

  const archivoEnv = path.join(carpeta, '.env');
  await mkdir(carpeta, { recursive: true });
  await writeFile(archivoEnv, '# comentario\nSUPABASE_URL="https://y.supabase.co"\nSUPABASE_SERVICE_KEY=otra\n');
  assert.deepEqual(leerCredenciales({}, archivoEnv), { url: 'https://y.supabase.co', clave: 'otra' });
  assert.throws(() => leerCredenciales({}, null), /SUPABASE_URL y SUPABASE_SERVICE_KEY/);
  assert.deepEqual(leerEnv('A=1\n\n#x\nB = dos'), { A: '1', B: 'dos' });
});
