// Genera el ícono de la app para Android a partir de app/assets/images/logo_app.png:
//   - Android 8+ (adaptativo): primer plano = la cama sin su fondo verde (transparente) sobre
//     un color verde. El lanzador del teléfono pone su propia forma, así que no queda el cuadro
//     blanco/cuadrado de un ícono normal.
//   - Android anterior: el logo con esquinas redondeadas y transparentes.
//
// Uso: node scripts/generar-icono-adaptativo.mjs
import fs from 'node:fs';
import path from 'node:path';
import sharp from '../frontend/node_modules/sharp/lib/index.js';

const raiz = path.resolve(path.dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1')), '..');
const res = path.join(raiz, 'app/android/app/src/main/res');
const logo = path.join(raiz, 'app/assets/images/logo_app.png');

// 1) Quitar el verde: tonos entre 95° y 175° (la cama es azul, amarilla y madera: no se parece).
const { data, info } = await sharp(logo).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
const { width: W, height: H } = info;
const alfa = Buffer.alloc(W * H, 255);
const flojo = new Uint8Array(W * H);   // parece fondo (verde u oscuro)
const estricto = new Uint8Array(W * H); // verde puro de fondo, nunca parte de la cama
for (let i = 0; i < W * H; i++) {
  const R = data[i * 4], G = data[i * 4 + 1], B = data[i * 4 + 2];
  const r = R / 255, g = G / 255, b = B / 255;
  const max = Math.max(r, g, b), min = Math.min(r, g, b), d = max - min;
  let h = 0;
  if (d > 0) {
    if (max === r) h = ((g - b) / d) % 6; else if (max === g) h = (b - r) / d + 2; else h = (r - g) / d + 4;
    h *= 60; if (h < 0) h += 360;
  }
  const sat = max === 0 ? 0 : d / max;
  flojo[i] = ((h >= 95 && h <= 175 && sat > 0.3 && max < 0.62) || max < 0.2) ? 1 : 0;
  estricto[i] = (G > 1.6 * R && G > 1.25 * B && max < 0.5) ? 1 : 0;
  if (data[i * 4 + 3] < 128) { flojo[i] = 1; estricto[i] = 1; }
}
// Solo se borra el fondo CONECTADO con el borde exterior (relleno por inundación). Lo que queda
// dentro de la cama (sombras, pliegues) se conserva aunque sea oscuro o verdoso.
const cola = [];
const visto = new Uint8Array(W * H);
const margen = 14;
for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
  const borde = x < margen || y < margen || x >= W - margen || y >= H - margen;
  if (borde) { visto[y * W + x] = 1; alfa[y * W + x] = 0; cola.push(y * W + x); }
}
while (cola.length) {
  const p = cola.pop();
  const x = p % W, y = (p / W) | 0;
  for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
    const nx = x + dx, ny = y + dy;
    if (nx < 0 || ny < 0 || nx >= W || ny >= H) continue;
    const q = ny * W + nx;
    if (visto[q] || !flojo[q]) continue;
    visto[q] = 1; alfa[q] = 0; cola.push(q);
  }
}
// Huecos de fondo encerrados (p. ej. entre las patas): solo si son verde puro de fondo.
for (let i = 0; i < W * H; i++) if (estricto[i]) alfa[i] = 0;
const alfaSuave = await sharp(alfa, { raw: { width: W, height: H, channels: 1 } }).blur(1.1).extractChannel(0).raw().toBuffer();
const rgba = Buffer.alloc(W * H * 4);
for (let i = 0; i < W * H; i++) {
  rgba[i * 4] = data[i * 4]; rgba[i * 4 + 1] = data[i * 4 + 1]; rgba[i * 4 + 2] = data[i * 4 + 2];
  rgba[i * 4 + 3] = alfaSuave[i];
}
// Recuadro del contenido (calculado a mano: trim() de sharp se confunde con el borde transparente).
let x0 = W, y0 = H, x1 = 0, y1 = 0;
for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
  if (alfaSuave[y * W + x] > 40) { x0 = Math.min(x0, x); y0 = Math.min(y0, y); x1 = Math.max(x1, x); y1 = Math.max(y1, y); }
}
const cama = await sharp(rgba, { raw: { width: W, height: H, channels: 4 } })
  .extract({ left: x0, top: y0, width: x1 - x0 + 1, height: y1 - y0 + 1 }).png().toBuffer();
const meta = await sharp(cama).metadata();
const camaRaw = await sharp(cama).raw().toBuffer();
let radio = 0;
for (let y = 0; y < meta.height; y++) for (let x = 0; x < meta.width; x++) {
  if (camaRaw[(y * meta.width + x) * 4 + 3] > 40) {
    radio = Math.max(radio, Math.hypot(x - meta.width / 2, y - meta.height / 2));
  }
}
console.log(`Cama recortada: ${meta.width}x${meta.height}, radio máx ${radio.toFixed(0)} px`);

// 2) Primer plano adaptativo: lienzo de 108 dp; la zona segura es un círculo de 66 dp.
const densidades = [
  { c: 'mdpi', dp: 1 }, { c: 'hdpi', dp: 1.5 }, { c: 'xhdpi', dp: 2 }, { c: 'xxhdpi', dp: 3 }, { c: 'xxxhdpi', dp: 4 },
];
const FRACCION_RADIO = 0.33; // radio del contenido / lienzo (zona segura = 0.306; el borde de la cama casi no cuenta)
for (const { c, dp } of densidades) {
  const lienzo = Math.round(108 * dp);
  const escala = (lienzo * FRACCION_RADIO) / radio;
  const w = Math.round(meta.width * escala), h = Math.round(meta.height * escala);
  const camaEsc = await sharp(cama).resize(w, h, { kernel: 'lanczos3' }).toBuffer();
  const dir = path.join(res, `mipmap-${c}`);
  fs.mkdirSync(dir, { recursive: true });
  await sharp({ create: { width: lienzo, height: lienzo, channels: 4, background: { r: 0, g: 0, b: 0, alpha: 0 } } })
    .composite([{ input: camaEsc, left: Math.round((lienzo - w) / 2), top: Math.round((lienzo - h) / 2) }])
    .png().toFile(path.join(dir, 'ic_launcher_foreground.png'));
}

// 3) Ícono clásico (Android < 8): el logo redondeado con esquinas transparentes.
const clasicos = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };
for (const [c, t] of Object.entries(clasicos)) {
  const mascara = Buffer.from(`<svg width="${t}" height="${t}"><rect width="${t}" height="${t}" rx="${Math.round(t * 0.2)}" fill="#fff"/></svg>`);
  await sharp(logo).resize(t, t, { kernel: 'lanczos3' }).composite([{ input: mascara, blend: 'dest-in' }]).png()
    .toFile(path.join(res, `mipmap-${c}`, 'ic_launcher.png'));
}

// 4) Definición del ícono adaptativo y su color de fondo.
fs.mkdirSync(path.join(res, 'mipmap-anydpi-v26'), { recursive: true });
fs.writeFileSync(path.join(res, 'mipmap-anydpi-v26/ic_launcher.xml'),
`<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
`);
const coloresRuta = path.join(res, 'values/colors.xml');
let colores = fs.readFileSync(coloresRuta, 'utf8');
if (!colores.includes('ic_launcher_background')) {
  colores = colores.replace('</resources>', '    <color name="ic_launcher_background">#145534</color>\n</resources>');
  fs.writeFileSync(coloresRuta, colores);
}
console.log('Ícono generado.');
