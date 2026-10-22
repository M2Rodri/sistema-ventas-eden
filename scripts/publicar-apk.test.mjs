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

test('el nombre del APK lleva el nombre fijo', () => {
  assert.equal(nombreApk(version), 'muebleria-eden.apk');
});

test('version.json trae lo que la app necesita', () => {
  const json = armarVersionJson({ ...version, apkUrl: 'https://x/a.apk', sha256: 'abc', tamanoBytes: 5, notas: 'Mejoras' });
  assert.deepEqual(json, {
    versionCode: 2, versionName: '1.1.0', apkUrl: 'https://x/a.apk', sha256: 'abc', tamanoBytes: 5, notas: 'Mejoras',
  });
});

test('publica el release en GitHub y después version.json en Supabase', async () => {
  const { http, pedidos } = storageFalso();
  let githubLlamado = false;
  const subidorGitHubFalso = ({ rutaApk, versionName, notas }) => {
    githubLlamado = true;
    return `https://github.com/M2Rodri/sistema-ventas-eden/releases/download/v${versionName}/muebleria-eden.apk`;
  };

  const r = await publicar({
    rutaApk: 'app-release.apk',
    apkBytes: apk,
    version,
    notas: 'Nueva',
    credenciales,
    http,
    subidorGitHub: subidorGitHubFalso,
  });

  assert.ok(githubLlamado, 'debe subir a GitHub Releases');
  assert.equal(r.apkUrl, 'https://github.com/M2Rodri/sistema-ventas-eden/releases/download/v1.1.0/muebleria-eden.apk');

  const subidas = pedidos.filter((p) => p.metodo === 'POST' && p.url.includes('/object/app/'));
  assert.equal(subidas.length, 1, 'solo sube version.json a Supabase (el APK va a GitHub)');
  assert.ok(subidas[0].url.endsWith('/object/app/version.json'));
  assert.equal(subidas[0].cabeceras['cache-control'], 'no-cache');
  assert.equal(subidas[0].cabeceras['x-upsert'], 'true');

  const publicado = JSON.parse(subidas[0].cuerpo);
  assert.equal(publicado.versionCode, 2);
  assert.equal(publicado.apkUrl, 'https://github.com/M2Rodri/sistema-ventas-eden/releases/download/v1.1.0/muebleria-eden.apk');
  assert.equal(publicado.tamanoBytes, apk.length);
  assert.equal(publicado.sha256.length, 64);
});

test('si el bucket ya existe en Supabase, sigue sin problema', async () => {
  const { http } = storageFalso({ bucketExiste: true });
  const subidorGitHubFalso = () => 'https://github.com/fake/apk';
  const r = await publicar({
    rutaApk: 'app-release.apk',
    apkBytes: apk,
    version,
    credenciales,
    http,
    subidorGitHub: subidorGitHubFalso,
  });
  assert.equal(r.info.versionCode, 2);
});

test('se niega a publicar una compilación que no es mayor que la publicada', async () => {
  const { http, pedidos } = storageFalso({ publicada: { versionCode: 2 } });
  await assert.rejects(
    () => publicar({ rutaApk: 'a.apk', apkBytes: apk, version, credenciales, http }),
    /no es mayor/
  );
  assert.equal(pedidos.filter((p) => p.metodo === 'POST').length, 0, 'no subió nada');
});

test('--forzar permite repetir la compilación', async () => {
  const { http } = storageFalso({ publicada: { versionCode: 2 } });
  const subidorGitHubFalso = () => 'https://github.com/fake/apk';
  const r = await publicar({
    rutaApk: 'a.apk',
    apkBytes: apk,
    version,
    credenciales,
    http,
    forzar: true,
    subidorGitHub: subidorGitHubFalso,
  });
  assert.equal(r.info.versionCode, 2);
});

