/** @type {import('next').NextConfig} */
const nextConfig = {
  // Carpeta donde se deja lo compilado.
  //
  // Por defecto es .next, la misma que usa `npm run dev`. Si se corre
  // `next build` sin mas, la compilacion de produccion se mezcla con la del
  // servidor de desarrollo y Turbopack entra en panico ("Next.js package not
  // found"), obligando a borrar la carpeta entera.
  //
  // Con esto, para verificar una compilacion de produccion sin tocar la de
  // desarrollo alcanza con:
  //
  //   NEXT_DIST_DIR=.next-verificacion npx next build
  //   NEXT_DIST_DIR=.next-verificacion npx next start
  //
  // Sin la variable, todo sigue funcionando como siempre.
  distDir: process.env.NEXT_DIST_DIR || '.next',
}

module.exports = nextConfig
