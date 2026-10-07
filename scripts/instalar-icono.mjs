import fs from 'node:fs';
import path from 'node:path';
import sharp from '../frontend/node_modules/sharp/lib/index.js';

const raiz = path.resolve('.');
const archivoOrigen = path.join(raiz, 'docs/icono_3d_fondo_verde.jpg');

if (!fs.existsSync(archivoOrigen)) {
  console.error(`No existe el archivo origen: ${archivoOrigen}`);
  process.exit(1);
}

const densidades = [
  { carpeta: 'mipmap-mdpi', tamano: 48 },
  { carpeta: 'mipmap-hdpi', tamano: 72 },
  { carpeta: 'mipmap-xhdpi', tamano: 96 },
  { carpeta: 'mipmap-xxhdpi', tamano: 144 },
  { carpeta: 'mipmap-xxxhdpi', tamano: 192 },
];

async function generar() {
  console.log(`Procesando ícono desde: ${archivoOrigen}`);

  for (const d of densidades) {
    const destinoDir = path.join(raiz, 'app/android/app/src/main/res', d.carpeta);
    if (!fs.existsSync(destinoDir)) {
      fs.mkdirSync(destinoDir, { recursive: true });
    }
    const destino = path.join(destinoDir, 'ic_launcher.png');
    await sharp(archivoOrigen)
      .resize(d.tamano, d.tamano, { kernel: sharp.kernel.lanczos3 })
      .png({ quality: 100 })
      .toFile(destino);
    console.log(`  ✓ Generado ${d.carpeta}/ic_launcher.png (${d.tamano}x${d.tamano})`);
  }

  // Generar copia en assets de la app Flutter
  const assetsDir = path.join(raiz, 'app/assets/images');
  if (!fs.existsSync(assetsDir)) {
    fs.mkdirSync(assetsDir, { recursive: true });
  }

  await sharp(archivoOrigen)
    .resize(1024, 1024, { kernel: sharp.kernel.lanczos3 })
    .png({ quality: 100 })
    .toFile(path.join(assetsDir, 'logo.png'));
  console.log(`  ✓ Guardado app/assets/images/logo.png (1024x1024)`);

  await sharp(archivoOrigen)
    .resize(512, 512, { kernel: sharp.kernel.lanczos3 })
    .png({ quality: 100 })
    .toFile(path.join(assetsDir, 'logo_512.png'));
  console.log(`  ✓ Guardado app/assets/images/logo_512.png (512x512)`);

  console.log('¡Ícono oficial configurado con éxito para Android y la App!');
}

generar().catch(err => {
  console.error('Error al generar íconos:', err);
  process.exit(1);
});
