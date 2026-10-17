/**
 * URL del backend, solo para uso en el servidor (API Routes, middleware).
 * No expone la URL real al código del cliente.
 */
export const BACKEND_URL_INTERNAL =
  process.env.BACKEND_URL_INTERNAL ??
  process.env.NEXT_PUBLIC_BACKEND_URL ??
  'https://backend-sistema-ventas-production-d0a4.up.railway.app';
