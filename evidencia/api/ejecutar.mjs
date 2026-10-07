// Ejecuta las pruebas de la API contra producción y mide el rendimiento (RNF-01).
//
//   Instalar una vez:   cd evidencia/api && npm install
//   Todo:               node ejecutar.mjs
//   Solo la colección:  node ejecutar.mjs --api
//   Solo rendimiento:   node ejecutar.mjs --rendimiento
//
// Las credenciales NUNCA van en archivos: se toman de las variables de entorno
// ADMIN_USER, ADMIN_PASS, EMP_USER y EMP_PASS. Opcional: BASE_URL (por defecto
// producción), por ejemplo http://localhost:8080 para un ensayo en local.
//
// Los reportes que se guardan no llevan cabeceras, cuerpos de petición ni
// tokens: solo caso, código HTTP, código de error y resultado de cada verificación.

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import newman from 'newman';

const aquí = path.dirname(fileURLToPath(import.meta.url));
const BASE_URL = (process.env.BASE_URL || 'https://sistema-ventas-eden-1.onrender.com').replace(/\/$/, '');
const requeridas = ['ADMIN_USER', 'ADMIN_PASS', 'EMP_USER', 'EMP_PASS'];
const faltan = requeridas.filter((v) => !process.env[v]);
if (faltan.length) {
  console.error(`Faltan variables de entorno: ${faltan.join(', ')}`);
  process.exit(2);
}

const args = process.argv.slice(2);
const soloApi = args.includes('--api');
const soloRendimiento = args.includes('--rendimiento');
const fecha = new Date().toISOString().slice(0, 10);
const hora = new Date().toTimeString().slice(0, 8);

const secretos = requeridas.map((v) => process.env[v]).filter((v) => v && v.length >= 3);
function limpiar(texto) {
  let t = String(texto);
  for (const s of secretos) t = t.split(s).join('***');
  return t.replace(/eyJ[\w-]+\.[\w-]+\.[\w-]+/g, '[token]');
}
const esc = (t) => limpiar(t).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

// ------------------------------------------------------------------ colección
function correrColeccion() {
  return new Promise((resolve, reject) => {
    newman.run(
      {
        collection: path.join(aquí, 'Muebleria-Eden-Pruebas.postman_collection.json'),
        envVar: [
          { key: 'baseUrl', value: BASE_URL },
          { key: 'adminUser', value: process.env.ADMIN_USER },
          { key: 'adminPass', value: process.env.ADMIN_PASS },
          { key: 'empUser', value: process.env.EMP_USER },
          { key: 'empPass', value: process.env.EMP_PASS },
        ],
        reporters: ['cli'],
        reporter: { cli: { noBanner: true } },
        timeoutRequest: 90000,
        delayRequest: 150,
      },
      (err, summary) => (err ? reject(err) : resolve(summary)),
    );
  });
}

function resumir(summary) {
  const casos = new Map(); // id -> { http: [], codigos: [], asertos: [] }
  const otros = [];
  for (const ej of summary.run.executions) {
    const nombre = ej.item.name;
    const id = (/^CP-\d+/.exec(nombre) || [])[0];
    const http = ej.response ? ej.response.code : null;
    let codigo = null;
    try {
      const j = JSON.parse(ej.response.stream.toString());
      codigo = j && j.error ? j.error.codigo : null;
    } catch { /* la respuesta no es JSON */ }
    for (const a of ej.assertions || []) {
      const idAserto = (/^CP-\d+/.exec(a.assertion) || [])[0];
      const registro = { texto: a.assertion, ok: !a.error, motivo: a.error ? a.error.message : '' };
      if (idAserto) {
        if (!casos.has(idAserto)) casos.set(idAserto, { http: [], codigos: [], peticiones: [], asertos: [] });
        casos.get(idAserto).asertos.push(registro);
      } else {
        otros.push({ peticion: nombre, ...registro });
      }
    }
    if (id) {
      if (!casos.has(id)) casos.set(id, { http: [], codigos: [], peticiones: [], asertos: [] });
      const c = casos.get(id);
      c.peticiones.push(nombre);
      c.http.push(http);
      c.codigos.push(codigo);
    }
  }
  const lista = [...casos.entries()].sort().map(([id, c]) => {
    const ok = c.asertos.length > 0 && c.asertos.every((a) => a.ok);
    const ultimoError = [...c.codigos].reverse().find((x) => x);
    return { id, estado: ok ? 'Aprobado' : 'Fallido', http: c.http, codigoError: ultimoError || null, peticiones: c.peticiones, asertos: c.asertos };
  });
  return { lista, otros, fallos: summary.run.failures.length };
}

function htmlDelReporte(res, estadisticas) {
  const filas = res.lista.map((c) => `<tr class="${c.estado === 'Aprobado' ? 'ok' : 'mal'}"><td>${esc(c.id)}</td><td>${esc(c.http.join(' / '))}</td><td>${esc(c.codigoError || '—')}</td><td>${c.estado}</td><td><ul>${c.asertos.map((a) => `<li class="${a.ok ? 'ok' : 'mal'}">${a.ok ? '✔' : '✘'} ${esc(a.texto)}${a.ok ? '' : ` <em>(${esc(a.motivo)})</em>`}</li>`).join('')}</ul></td></tr>`).join('\n');
  const prep = res.otros.map((a) => `<li class="${a.ok ? 'ok' : 'mal'}">${a.ok ? '✔' : '✘'} ${esc(a.texto)}${a.ok ? '' : ` <em>(${esc(a.motivo)})</em>`}</li>`).join('');
  return `<!doctype html><html lang="es"><head><meta charset="utf-8"><title>Pruebas de la API ${fecha}</title>
<style>body{font-family:system-ui,sans-serif;margin:24px;color:#222}table{border-collapse:collapse;width:100%}td,th{border:1px solid #ccc;padding:6px 8px;vertical-align:top;font-size:14px}th{background:#f1f1f1;text-align:left}tr.ok td:nth-child(4){color:#0a7a2f;font-weight:700}tr.mal td:nth-child(4){color:#b00020;font-weight:700}li.ok{color:#0a7a2f}li.mal{color:#b00020}ul{margin:0;padding-left:18px}</style></head><body>
<h1>Mueblería Edén: pruebas de la API</h1>
<p><b>Fecha:</b> ${fecha} ${hora} &nbsp; <b>Servidor:</b> ${esc(BASE_URL)} &nbsp; <b>Herramienta:</b> Newman (colección de Postman)</p>
<p><b>Casos:</b> ${res.lista.length} &nbsp; <b>Aprobados:</b> ${res.lista.filter((c) => c.estado === 'Aprobado').length} &nbsp; <b>Fallidos:</b> ${res.lista.filter((c) => c.estado !== 'Aprobado').length} &nbsp; <b>Peticiones:</b> ${estadisticas.requests.total} &nbsp; <b>Verificaciones:</b> ${estadisticas.assertions.total} (fallidas: ${estadisticas.assertions.failed})</p>
<table><tr><th>Caso</th><th>HTTP</th><th>Código de error</th><th>Estado</th><th>Verificaciones</th></tr>
${filas}</table>
<h2>Preparación y limpieza</h2><ul>${prep}</ul>
<p><small>Este reporte no incluye cabeceras, cuerpos de petición ni tokens.</small></p></body></html>`;
}

async function parteApi() {
  console.log(`\n=== Colección de la API contra ${BASE_URL} ===`);
  const summary = await correrColeccion();
  const res = resumir(summary);
  const archivoHtml = path.join(aquí, `reporte-api-${fecha}.html`);
  const archivoJson = path.join(aquí, `resultados-api-${fecha}.json`);
  fs.writeFileSync(archivoHtml, htmlDelReporte(res, summary.run.stats));
  fs.writeFileSync(archivoJson, limpiar(JSON.stringify({ fecha, hora, servidor: BASE_URL, casos: res.lista, preparacionYLimpieza: res.otros }, null, 2)));
  console.log(`\nReporte: ${path.relative(process.cwd(), archivoHtml)}`);
  console.log(`Resultados: ${path.relative(process.cwd(), archivoJson)}`);
  console.log(`Casos aprobados: ${res.lista.filter((c) => c.estado === 'Aprobado').length} de ${res.lista.length}`);
  return res.lista.every((c) => c.estado === 'Aprobado') && res.otros.every((o) => o.ok);
}

// ---------------------------------------------------------------- rendimiento
async function medir(url, token, veces) {
  const tiempos = [];
  let estado = null;
  let bytes = 0;
  for (let i = 0; i < veces; i++) {
    const t0 = performance.now();
    const r = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
    const cuerpo = await r.arrayBuffer();
    tiempos.push(performance.now() - t0);
    estado = r.status;
    bytes = cuerpo.byteLength;
  }
  const ordenados = [...tiempos].sort((a, b) => a - b);
  const prom = tiempos.reduce((a, b) => a + b, 0) / tiempos.length;
  return {
    url: url.replace(BASE_URL, ''),
    mediciones: tiempos.map((t) => Math.round(t)),
    estadoHttp: estado,
    bytesRespuesta: bytes,
    promedioMs: Math.round(prom),
    maximoMs: Math.round(ordenados[ordenados.length - 1]),
    minimoMs: Math.round(ordenados[0]),
    cumple2s: ordenados[ordenados.length - 1] <= 2000 && prom <= 2000,
  };
}

async function parteRendimiento() {
  console.log(`\n=== Rendimiento (RNF-01) contra ${BASE_URL} ===`);
  const login = await fetch(`${BASE_URL}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ usuario: process.env.ADMIN_USER, password: process.env.ADMIN_PASS }),
  });
  if (!login.ok) throw new Error(`No se pudo iniciar sesión para medir (HTTP ${login.status})`);
  const { token } = await login.json();
  await fetch(`${BASE_URL}/api/v1/salud`); // el servicio ya debe estar activo: se despierta antes de medir
  await fetch(`${BASE_URL}/api/v1/productos`, { headers: { Authorization: `Bearer ${token}` } }); // calentamiento, no cuenta

  const resultados = [];
  for (const ruta of ['/api/v1/productos', '/api/v1/inventario/catalogo']) {
    const r = await medir(`${BASE_URL}${ruta}`, token, 10);
    resultados.push(r);
    console.log(`${ruta}: promedio ${r.promedioMs} ms, máximo ${r.maximoMs} ms, mínimo ${r.minimoMs} ms → ${r.cumple2s ? 'cumple' : 'NO cumple'} (HTTP ${r.estadoHttp})`);
  }
  const carpeta = path.join(aquí, '..', 'rendimiento');
  fs.mkdirSync(carpeta, { recursive: true });
  const datos = { requisito: 'RNF-01: respuesta de las consultas en menos de 2 segundos', herramienta: `Script Node ${process.version} (fetch y performance.now), 10 mediciones secuenciales por ruta, con un calentamiento previo que no cuenta`, servidor: BASE_URL, fecha, hora, umbralMs: 2000, resultados };
  fs.writeFileSync(path.join(carpeta, `rnf-01-${fecha}.json`), JSON.stringify(datos, null, 2));
  const md = `# RNF-01: tiempos de respuesta (${fecha} ${hora})\n\n- **Herramienta:** ${datos.herramienta}\n- **Servidor:** ${BASE_URL}\n- **Umbral:** 2000 ms\n\n| Ruta | Promedio | Máximo | Mínimo | Mediciones (ms) | ¿Cumple? |\n|---|---|---|---|---|---|\n${resultados.map((r) => `| GET ${r.url} | ${r.promedioMs} ms | ${r.maximoMs} ms | ${r.minimoMs} ms | ${r.mediciones.join(', ')} | ${r.cumple2s ? 'Sí' : 'No'} |`).join('\n')}\n`;
  fs.writeFileSync(path.join(carpeta, `rnf-01-${fecha}.md`), md);
  console.log(`Guardado en evidencia/rendimiento/rnf-01-${fecha}.{json,md}`);
  return resultados.every((r) => r.cumple2s);
}

let bien = true;
if (!soloRendimiento) bien = (await parteApi()) && bien;
if (!soloApi) bien = (await parteRendimiento()) && bien;
process.exit(bien ? 0 : 1);
