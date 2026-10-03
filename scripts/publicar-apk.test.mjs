// Prueba del script que publica el APK, contra un Storage falso. Se corre con:  node --test scripts/*.test.mjs

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { armarVersionJson, leerVersionPubspec, nombreApk, publicar } from './publicar-apk.mjs';

const credenciales = { url: 'https://proyecto.test', clave: 'clave-falsa' };
const apk = Buffer.from('contenido-del-apk');
const version = { versionName: '1.1.0', versionCode: 2 };

/** Un "fetch" falso: guarda lo que se le pide y contesta según lo que ya haya publicado. */
function storageFalso({ publicada = null, bucketExiste = false } = {}) {
  const pedidos = [];
  const http = async (url, opciones = {}) => {
    pedidos.push({ url, metodo: opciones.method || 'GET', cabeceras: opciones.headers, cuerpo: opciones.body });
    if (url.endsWith('/public/app/version.json')) {
      return publicada
        ? new Response(JSON.stringify(publicada), { status: 200 })
        : new Response('no existe', { status: 404 });
    }
    if (url.endsWith('/storage/v1/bucket')) {
      return bucketExiste
        ? new Response('{"message":"The resource already exists"}', { status: 400 })
        : new Response('{}', { status: 200 });
    }
    return new Response('{}', { status: 200 });
  };
  return { http, pedidos };
}

test('lee la versión de pubspec.yaml', () => {
  assert.deepEqual(leerVersionPubspec('name: x\nversion: 1.4.2+17\nenvironment:'), { versionName: '1.4.2', versionCode: 17 });
  assert.throws(() => leerVersionPubspec('name: x'), /version/);
});

test('el nombre del APK lleva la versión y la compilación', () => {
  assert.equal(nombreApk(version), 'muebleria-eden-1.1.0-2.apk');
});

test('version.json trae lo que la app necesita', () => {
  const json = armarVersionJson({ ...version, apkUrl: 'https://x/a.apk', sha256: 'abc', tamanoBytes: 5, notas: 'Mejoras' });
  assert.deepEqual(json, {
    versionCode: 2, versionName: '1.1.0', apkUrl: 'https://x/a.apk', sha256: 'abc', tamanoBytes: 5, notas: 'Mejoras',
  });
});

test('publica primero el APK y después version.json', async () => {
  const { http, pedidos } = storageFalso();
  const r = await publicar({ apk, version, notas: 'Nueva', credenciales, http });

  const subidas = pedidos.filter((p) => p.metodo === 'POST' && p.url.includes('/object/app/'));
  assert.equal(subidas.length, 2);
  assert.ok(subidas[0].url.endsWith('/object/app/muebleria-eden-1.1.0-2.apk'));
  assert.ok(subidas[1].url.endsWith('/object/app/version.json'));
  assert.equal(subidas[1].cabeceras['cache-control'], 'no-cache');
  assert.equal(subidas[0].cabeceras['x-upsert'], 'true');

  const publicado = JSON.parse(subidas[1].cuerpo);
  assert.equal(publicado.versionCode, 2);
  assert.equal(publicado.apkUrl, 'https://proyecto.test/storage/v1/object/public/app/muebleria-eden-1.1.0-2.apk');
  assert.equal(publicado.tamanoBytes, apk.length);
  assert.equal(publicado.sha256.length, 64);
  assert.equal(r.bucket, 'creado');
});

test('si el bucket ya existe, sigue sin problema', async () => {
  const { http } = storageFalso({ bucketExiste: true });
  const r = await publicar({ apk, version, credenciales, http });
  assert.equal(r.bucket, 'existente');
});

test('se niega a publicar una compilación que no es mayor que la publicada', async () => {
  const { http, pedidos } = storageFalso({ publicada: { versionCode: 2 } });
  await assert.rejects(() => publicar({ apk, version, credenciales, http }), /no es mayor/);
  assert.equal(pedidos.filter((p) => p.metodo === 'POST').length, 0, 'no subió nada');
});

test('--forzar permite repetir la compilación', async () => {
  const { http } = storageFalso({ publicada: { versionCode: 2 } });
  const r = await publicar({ apk, version, credenciales, http, forzar: true });
  assert.equal(r.info.versionCode, 2);
});
