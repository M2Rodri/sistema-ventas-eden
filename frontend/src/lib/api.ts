// Servicio de API para conectar con el backend Spring Boot
import { ImagenProducto } from '@/types/imagenProducto';
import { esPorEntregar } from '@/lib/entrega';
import { errorDeRespuesta } from '@/lib/errores';

/** URL pública del backend, solo para urlArchivo() (fotos con ruta /uploads/...). */
export const BACKEND_URL =
  process.env.NEXT_PUBLIC_BACKEND_URL ?? 'https://backend-sistema-ventas-production-d0a4.up.railway.app';

// Las rutas van con la versión a través del proxy BFF: /api/proxy/RUTA llega a BACKEND/api/v1/RUTA con el JWT.
const API_URL = '/api/proxy';

/**
 * Caché corta de lecturas (GET): una lectura reciente se reutiliza unos segundos y las simultáneas comparten una petición.
 * Cualquier escritura la vacía, la clave incluye el token y solo se guardan respuestas JSON correctas.
 */
const CACHE_LECTURAS_MS = 60_000;
const cacheLecturas = new Map<string, { vence: number; respuesta: Promise<Response> }>();

export const vaciarCacheApi = (): void => cacheLecturas.clear();

// Tras guardar, la caché se vacía y se vuelve a llenar por detrás (varias escrituras seguidas, una sola precarga).
let temporizadorPrecarga: ReturnType<typeof setTimeout> | undefined;
const programarPrecarga = (): void => {
  if (typeof window === 'undefined') return;
  clearTimeout(temporizadorPrecarga);
  temporizadorPrecarga = setTimeout(() => { void precargarDatos(); }, 800);
};

const fetch = (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
  const metodo = (init?.method ?? 'GET').toUpperCase();
  const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url;

  if (metodo !== 'GET') {
    return globalThis.fetch(input, init).finally(() => {
      vaciarCacheApi();
      // El inicio de sesión no cuenta: todavía no hay token y el panel ya precarga al abrir.
      if (url.startsWith(API_URL) && !url.includes('/auth/')) programarPrecarga();
    });
  }
  if (!url.startsWith(API_URL)) {
    return globalThis.fetch(input, init);
  }

  const headers = new Headers(init?.headers);
  const clave = `${headers.get('Authorization') ?? ''}|${url}`;
  const guardada = cacheLecturas.get(clave);
  if (guardada && guardada.vence > Date.now()) {
    return guardada.respuesta.then((r) => r.clone());
  }

  const respuesta = globalThis.fetch(input, init).then((r) => {
    const esJson = (r.headers.get('content-type') ?? '').includes('json');
    if (!r.ok || !esJson) {
      cacheLecturas.delete(clave);
    }
    return r;
  }, (error) => {
    cacheLecturas.delete(clave);
    throw error;
  });
  cacheLecturas.set(clave, { vence: Date.now() + CACHE_LECTURAS_MS, respuesta });
  return respuesta.then((r) => r.clone());
};

/** URL para mostrar un archivo subido: las nuevas ya vienen completas y las viejas traen una ruta relativa (/uploads/...). */
export const urlArchivo = (url: string): string =>
  /^https?:\/\//i.test(url) ? url : `${BACKEND_URL}${url}`;

export interface RegisterData {
  nombre: string;
  apellido: string;
  usuario: string;
  password: string;
  telefono?: string;
  direccion?: string;
}

export interface LoginData {
  usuario: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tipo: string;
  id: number;
  nombre: string;
  apellido: string;
  usuario: string;
  role: string;
}

// Registrar usuario
export const register = async (data: RegisterData): Promise<AuthResponse> => {
  const response = await fetch(`${API_URL}/auth/register`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || 'Error al registrar usuario');
  }

  return response.json();
};

// Login
export const login = async (data: LoginData): Promise<AuthResponse> => {
  const response = await fetch(`${API_URL}/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new Error('Credenciales inválidas');
  }

  return response.json();
};

// Usuarios

export interface User {
  id: number;
  nombre: string;
  apellido: string;
  nombreCompleto: string;
  usuario: string;
  telefono?: string;
  direccion?: string;
  role: 'ADMIN' | 'EMPLEADO' | 'CLIENTE';
  activo: boolean;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface UserRequest {
  nombre: string;
  apellido: string;
  usuario: string;
  password?: string;
  telefono?: string;
  direccion?: string;
  role: 'ADMIN' | 'EMPLEADO' | 'CLIENTE';
  activo: boolean;
}

// IMPORTAR TIPOS DE PRODUCTO Y CATEGORIA
import { Producto, ProductoRequest, Categoria, TipoProducto } from '@/types/producto';

// Helper para obtener el token CSRF de la cookie (no es HttpOnly)
const getCsrfToken = (): string | undefined => {
  if (typeof document === 'undefined') return undefined;
  return document.cookie
    .split('; ')
    .find(row => row.startsWith('csrf-token='))
    ?.split('=')[1];
};

// Cabeceras JSON: el JWT lo inyecta el proxy BFF; el cliente manda el Content-Type y, en escrituras, el token CSRF.
const getAuthHeaders = () => {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };
  const csrf = getCsrfToken();
  if (csrf) headers['X-CSRF-Token'] = csrf;
  return headers;
};

const getAuthHeadersForFormData = () => {
  const headers: Record<string, string> = {};
  const csrf = getCsrfToken();
  if (csrf) headers['X-CSRF-Token'] = csrf;
  // NO incluir Content-Type - el navegador lo establece automáticamente
  return headers;
};

export const getAllUsers = async (): Promise<User[]> => {
  const response = await fetch(`${API_URL}/usuarios`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener usuarios');
  }

  return response.json();
};

export const getUserById = async (id: number): Promise<User> => {
  const response = await fetch(`${API_URL}/usuarios/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener usuario');
  }

  return response.json();
};

export const createUser = async (data: UserRequest): Promise<User> => {
  const response = await fetch(`${API_URL}/usuarios`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al crear usuario');
  }

  return response.json();
};

export const updateUser = async (id: number, data: UserRequest): Promise<User> => {
  const response = await fetch(`${API_URL}/usuarios/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al actualizar usuario');
  }

  return response.json();
};

export const deleteUser = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/usuarios/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al eliminar usuario');
  }
};

// Activar/Desactivar usuario
export const toggleUserStatus = async (id: number): Promise<User> => {
  const response = await fetch(`${API_URL}/usuarios/${id}/toggle-status`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al cambiar estado del usuario');
  }

  return response.json();
};

// Cierra las sesiones abiertas de un usuario en todos sus dispositivos (solo ADMIN).
export const cerrarSesionesDeUsuario = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/usuarios/${id}/cerrar-sesiones`, {
    method: 'POST',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al cerrar las sesiones del usuario');
  }
};

// Filtrar por rol
export const getUsersByRole = async (role: string): Promise<User[]> => {
  const response = await fetch(`${API_URL}/usuarios/rol/${role}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al filtrar usuarios');
  }

  return response.json();
};

export const getUserStatistics = async () => {
  const response = await fetch(`${API_URL}/usuarios/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener estadísticas');
  }

  return response.json();
};

// Categorías

export const getAllCategorias = async (): Promise<Categoria[]> => {
  const response = await fetch(`${API_URL}/categorias`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener categorías');
  }

  return response.json();
};

export const getActiveCategorias = async (): Promise<Categoria[]> => {
  const response = await fetch(`${API_URL}/categorias/activas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener categorías activas');
  }

  return response.json();
};

export const createCategoria = async (data: { nombre: string; descripcion?: string; activo: boolean }): Promise<Categoria> => {
  const response = await fetch(`${API_URL}/categorias`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al crear categoría');
  }

  return response.json();
};

export const updateCategoria = async (id: number, data: { nombre: string; descripcion?: string; activo: boolean }): Promise<Categoria> => {
  const response = await fetch(`${API_URL}/categorias/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al actualizar categoría');
  }

  return response.json();
};

export const deleteCategoria = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/categorias/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al eliminar categoría');
  }
};

// Activar/Desactivar categoría
export const toggleCategoriaStatus = async (id: number): Promise<Categoria> => {
  const response = await fetch(`${API_URL}/categorias/${id}/toggle-status`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al cambiar estado de la categoría');
  }

  return response.json();
};

// Productos

// Ejemplo corregido y funcional para getAllProductos
export const getAllProductos = async (): Promise<Producto[]> => {
  const response = await fetch(`${API_URL}/productos`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener productos');
  }

  // Si la respuesta es exitosa, leemos el cuerpo como texto
  const text = await response.text();
  if (!text) {
    throw new Error("La respuesta del servidor está vacía");
  }

  try {
    const data = JSON.parse(text);
    return data;
  } catch (e) {
    console.error("Error al parsear JSON:", text);
    throw new Error("La respuesta del servidor no es un JSON válido");
  }
};

export const getActiveProductos = async (): Promise<Producto[]> => {
  const response = await fetch(`${API_URL}/productos/activos`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener productos activos');
  }

  return response.json();
};

export const getProductoById = async (id: number): Promise<Producto> => {
  const response = await fetch(`${API_URL}/productos/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener producto');
  }

  return response.json();
};

export const getProductoBySku = async (sku: string): Promise<Producto> => {
  const response = await fetch(`${API_URL}/productos/sku/${sku}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener producto por SKU');
  }

  return response.json();
};

export const createProducto = async (data: ProductoRequest): Promise<Producto> => {
  const response = await fetch(`${API_URL}/productos`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al crear producto');
  }

  return response.json();
};

export const updateProducto = async (id: number, data: ProductoRequest): Promise<Producto> => {
  const response = await fetch(`${API_URL}/productos/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al actualizar producto');
  }

  return response.json();
};

export const deleteProducto = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/productos/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al eliminar producto');
  }
};

// Activar/Desactivar producto
export const toggleProductoStatus = async (id: number): Promise<Producto> => {
  const response = await fetch(`${API_URL}/productos/${id}/toggle-status`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al cambiar estado del producto');
  }

  return response.json();
};

// Filtrar por categoría
export const getProductosByCategoria = async (categoriaId: number): Promise<Producto[]> => {
  const response = await fetch(`${API_URL}/productos/categoria/${categoriaId}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al filtrar productos por categoría');
  }

  return response.json();
};

// Filtrar por tipo
export const searchProductos = async (nombre: string): Promise<Producto[]> => {
  const response = await fetch(`${API_URL}/productos/buscar?nombre=${encodeURIComponent(nombre)}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al buscar productos');
  }

  return response.json();
};

// Inventario

import { 
  Inventario, 
  InventarioRequest, 
  AlertaInventario, 
  MovimientoInventario, 
  MovimientoInventarioRequest 
} from '@/types/inventario';

export const getAllInventario = async (): Promise<Inventario[]> => {
  const response = await fetch(`${API_URL}/inventario`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener inventario');
  }

  return response.json();
};

export const getInventarioById = async (id: number): Promise<Inventario> => {
  const response = await fetch(`${API_URL}/inventario/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener inventario');
  }

  return response.json();
};

export const getInventarioByProducto = async (idProducto: number): Promise<Inventario> => {
  const response = await fetch(`${API_URL}/inventario/producto/${idProducto}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener inventario del producto');
  }

  return response.json();
};

export const createInventario = async (data: InventarioRequest): Promise<Inventario> => {
  const response = await fetch(`${API_URL}/inventario`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al crear inventario');
  }

  return response.json();
};

export const updateInventario = async (id: number, data: InventarioRequest): Promise<Inventario> => {
  const response = await fetch(`${API_URL}/inventario/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al actualizar inventario');
  }

  return response.json();
};

// Ajustar inventario manualmente
export const ajustarInventario = async (data: MovimientoInventarioRequest): Promise<Inventario> => {
  const response = await fetch(`${API_URL}/inventario/ajustar`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al ajustar inventario');
  }

  return response.json();
};

export const getHistorialAjustes = async (idProducto: number): Promise<MovimientoInventario[]> => {
  const response = await fetch(`${API_URL}/inventario/producto/${idProducto}/historial`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener historial de ajustes');
  }

  // Los movimientos viejos de compra se muestran como ENTRADA, igual que los nuevos.
  const movimientos: MovimientoInventario[] = await response.json();
  return movimientos.map((m) => (m.tipoMovimiento === 'COMPRA' ? { ...m, tipoMovimiento: 'ENTRADA' } : m));
};

// Marcar una alerta de stock como atendida (solo ADMIN)
export const atenderAlertaStock = async (idAlerta: number): Promise<void> => {
  const response = await fetch(`${API_URL}/inventario/alertas/${idAlerta}/atender`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'No se pudo marcar la alerta como atendida');
  }
};

// Volver a avisar de un producto con la alerta atendida (solo ADMIN)
export const reactivarAlertaStock = async (idProducto: number): Promise<void> => {
  const response = await fetch(`${API_URL}/inventario/producto/${idProducto}/reactivar-alerta`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'No se pudo reactivar la alerta');
  }
};

export const getUltimosAjustes = async (): Promise<MovimientoInventario[]> => {
  const response = await fetch(`${API_URL}/inventario/ajustes/ultimos`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener últimos ajustes');
  }

  return response.json();
};

export const getProductosConStockBajo = async (): Promise<Inventario[]> => {
  const response = await fetch(`${API_URL}/inventario/stock-bajo`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener productos con stock bajo');
  }

  return response.json();
};

export const getProductosSinStock = async (): Promise<Inventario[]> => {
  const response = await fetch(`${API_URL}/inventario/sin-stock`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener productos sin stock');
  }

  return response.json();
};

export const verificarDisponibilidad = async (idProducto: number, cantidad: number): Promise<{ disponible: boolean; idProducto: number; cantidadSolicitada: number }> => {
  const response = await fetch(`${API_URL}/inventario/verificar-disponibilidad?idProducto=${idProducto}&cantidad=${cantidad}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al verificar disponibilidad');
  }

  return response.json();
};

export const getAlertasPendientes = async (): Promise<AlertaInventario[]> => {
  const response = await fetch(`${API_URL}/inventario/alertas/pendientes`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener alertas pendientes');
  }

  return response.json();
};

export const getInventarioStatistics = async () => {
  const response = await fetch(`${API_URL}/inventario/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener estadísticas de inventario');
  }

  return response.json();
};

// Ventas

import {
  Venta,
  VentaRequest,
  VentaEstadisticas,
  DatosEntrega,
  EstadoEntrega,
  Pago as PagoDeVenta
} from '@/types/venta';

/** Listar todas las ventas */
export const getAllVentas = async (): Promise<Venta[]> => {
  const response = await fetch(`${API_URL}/ventas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener ventas');
  }

  return response.json();
};

/** Obtener venta por ID */
export const getVentaById = async (id: number): Promise<Venta> => {
  const response = await fetch(`${API_URL}/ventas/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener venta');
  }

  return response.json();
};

/** Crea una venta directa, con cliente registrado o rápido. */
export const createVentaDirecta = async (
  request: VentaRequest
): Promise<Venta> => {
  const response = await fetch(`${API_URL}/ventas`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al crear venta');
  }

  return response.json();
};

/** Cancela una venta y devuelve el stock (solo ADMIN). */
export const cancelarVenta = async (id: number): Promise<Venta> => {
  const response = await fetch(`${API_URL}/ventas/${id}/cancelar`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al cancelar venta');
  }

  return response.json();
};

/** Marca la entrega como completada (ADMIN y EMPLEADO); el saldo pendiente no la bloquea. */
export const marcarVentaEntregada = async (id: number): Promise<Venta> => {
  const response = await fetch(`${API_URL}/ventas/${id}/entregar`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al marcar la venta como entregada');
  }

  return response.json();
};

/** Pone o cambia la fecha límite del pago pendiente: en N días o una fecha yyyy-MM-dd. */
export const actualizarFechaLimitePago = async (
  id: number,
  datos: { plazoDiasPago?: number; fechaLimitePago?: string },
): Promise<Venta> => {
  const response = await fetch(`${API_URL}/ventas/${id}/fecha-limite-pago`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
    body: JSON.stringify(datos),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al guardar la fecha límite');
  }

  return response.json();
};

/** Corregir una entrega marcada por error: ENTREGADO -> PENDIENTE (solo ADMIN). */
export const deshacerEntregaVenta = async (id: number): Promise<Venta> => {
  const response = await fetch(`${API_URL}/ventas/${id}/deshacer-entrega`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al corregir la entrega');
  }

  return response.json();
};

/** Completa o corrige dirección, transportadora y guía (solo ADMIN); un valor vacío borra el dato. */
export const actualizarDatosEntrega = async (
  id: number,
  datos: DatosEntrega
): Promise<Venta> => {
  const response = await fetch(`${API_URL}/ventas/${id}/datos-entrega`, {
    method: 'PATCH',
    headers: { ...getAuthHeaders(), 'Content-Type': 'application/json' },
    body: JSON.stringify(datos),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al guardar los datos de entrega');
  }

  return response.json();
};

/** Adjuntar (o reemplazar) la foto de comprobante de un pago ya registrado. */
export const adjuntarComprobantePago = async (idPago: number, file: File): Promise<PagoDeVenta> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${API_URL}/pagos/${idPago}/comprobante`, {
    method: 'POST',
    headers: getAuthHeadersForFormData(),
    body: formData,
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al subir el comprobante');
  }

  return response.json();
};

/** Obtener ventas por cliente registrado */
export const getVentasByCliente = async (clienteId: number): Promise<Venta[]> => {
  const response = await fetch(`${API_URL}/ventas/cliente/${clienteId}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener ventas del cliente');
  }

  return response.json();
};

/** Obtener ventas por estado */
export const getVentasByEstado = async (estado: string): Promise<Venta[]> => {
  const response = await fetch(`${API_URL}/ventas/estado/${estado}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al filtrar ventas por estado');
  }

  return response.json();
};

/** Obtener ventas del día actual */
export const getVentasDelDia = async (): Promise<Venta[]> => {
  const response = await fetch(`${API_URL}/ventas/del-dia`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener ventas del día');
  }

  return response.json();
};

/** Obtener últimas 10 ventas */
export const getUltimasVentas = async (): Promise<Venta[]> => {
  const response = await fetch(`${API_URL}/ventas/ultimas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener últimas ventas');
  }

  return response.json();
};

/** Obtener ventas por rango de fechas */
export const getVentasByFechas = async (inicio: string, fin: string): Promise<Venta[]> => {
  const response = await fetch(
    `${API_URL}/ventas/fechas?inicio=${encodeURIComponent(inicio)}&fin=${encodeURIComponent(fin)}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al filtrar ventas por fecha');
  }

  return response.json();
};

/** Obtener total de ventas por rango de fechas */
export const getTotalVentasByFechas = async (inicio: string, fin: string): Promise<number> => {
  const response = await fetch(
    `${API_URL}/ventas/total-fechas?inicio=${encodeURIComponent(inicio)}&fin=${encodeURIComponent(fin)}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener total de ventas');
  }

  const data = await response.json();
  return data.total;
};

/** Estadísticas de ventas con totales por estado y montos (solo ADMIN). */
export const getVentasEstadisticas = async (): Promise<VentaEstadisticas> => {
  const response = await fetch(`${API_URL}/ventas/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener estadísticas de ventas');
  }

  const data = await response.json();

  const ventasDelDia = await getVentasDelDia();
  const montoDelDia = ventasDelDia.reduce((sum, v) => sum + v.montoTotal, 0);

  // Completadas y Monto Total son de los últimos 7 días (RF-10); Pendientes no se acota porque una deuda vieja sigue por cobrar.
  const todasVentas = await getAllVentas();
  const inicioSemana = new Date();
  inicioSemana.setDate(inicioSemana.getDate() - 7);
  const completadasSemana = todasVentas.filter(v =>
    v.estado === 'COMPLETADA' && new Date(v.fechaVenta) >= inicioSemana
  );
  const montoTotal = completadasSemana.reduce((sum, v) => sum + v.montoTotal, 0);

  // RF-10: "Por entregar" es estado de entrega distinto de ENTREGADO y venta no cancelada, igual en backend, web y app.
  const entregasPendientes = todasVentas.filter(esPorEntregar).length;

  return {
    totalVentas: todasVentas.length,
    ventasCompletadas: completadasSemana.length,
    ventasPendientes: data.pendientes || 0,
    ventasCanceladas: data.canceladas || 0,
    montoTotal: montoTotal,
    entregasPendientes: entregasPendientes,
    ventasDelDia: ventasDelDia.length,
    montoDelDia: montoDelDia
  };
};

// lib/api.ts - Agregar estas funciones al archivo existente

import {
  ClienteRequest,
  ClienteResponse,
  HistorialComprasResponse,
  ClienteConEstadisticas
} from '@/types/cliente';

// Clientes

/** Lista todos los clientes. */
export const getAllClientes = async (): Promise<ClienteResponse[]> => {
  const response = await fetch(`${API_URL}/clientes`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener clientes');
  }

  return response.json();
};

/** Obtener cliente por ID */
export const getClienteById = async (id: number): Promise<ClienteResponse> => {
  const response = await fetch(`${API_URL}/clientes/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Cliente no encontrado');
  }

  return response.json();
};

/** Crear nuevo cliente */
export const createCliente = async (request: ClienteRequest): Promise<ClienteResponse> => {
  const response = await fetch(`${API_URL}/clientes`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al crear cliente');
  }

  return response.json();
};

/** Actualiza un cliente. */
export const updateCliente = async (
  id: number,
  request: ClienteRequest
): Promise<ClienteResponse> => {
  const response = await fetch(`${API_URL}/clientes/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al actualizar cliente');
  }

  return response.json();
};

/** Busca clientes por nombre, teléfono o NIT/CI. */
export const searchClientes = async (query: string): Promise<ClienteResponse[]> => {
  const response = await fetch(
    `${API_URL}/clientes/buscar?q=${encodeURIComponent(query)}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al buscar clientes');
  }

  return response.json();
};

/** Historial completo de compras de un cliente. */
export const getHistorialCompras = async (clienteId: number): Promise<HistorialComprasResponse> => {
  const response = await fetch(`${API_URL}/clientes/${clienteId}/historial-compras`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener historial de compras');
  }

  return response.json();
};

/** Historial de compras de un cliente entre dos fechas. */
export const getHistorialComprasFiltrado = async (
  clienteId: number,
  inicio: string,
  fin: string
): Promise<HistorialComprasResponse> => {
  const response = await fetch(
    `${API_URL}/clientes/${clienteId}/historial-compras/filtrado?inicio=${encodeURIComponent(
      inicio
    )}&fin=${encodeURIComponent(fin)}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener historial filtrado');
  }

  return response.json();
};

/** Clientes con número de compras, monto total y última compra, en una sola consulta. */
export const getClientesConEstadisticas = async (): Promise<ClienteConEstadisticas[]> => {
  const response = await fetch(`${API_URL}/clientes/con-estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener clientes con estadísticas');
  }

  return response.json();
};

// Proveedores

import {
  Proveedor,
  ProveedorRequest,
  Compra,
  CompraRequest,
} from '@/types/proveedor';

export const getAllProveedores = async (): Promise<Proveedor[]> => {
  const response = await fetch(`${API_URL}/proveedores`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener proveedores');
  }

  return response.json();
};

export const getActiveProveedores = async (): Promise<Proveedor[]> => {
  const response = await fetch(`${API_URL}/proveedores/activos`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener proveedores activos');
  }

  return response.json();
};

export const getProveedorById = async (id: number): Promise<Proveedor> => {
  const response = await fetch(`${API_URL}/proveedores/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener proveedor');
  }

  return response.json();
};

export const createProveedor = async (data: ProveedorRequest): Promise<Proveedor> => {
  const response = await fetch(`${API_URL}/proveedores`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al crear proveedor');
  }

  return response.json();
};

export const updateProveedor = async (id: number, data: ProveedorRequest): Promise<Proveedor> => {
  const response = await fetch(`${API_URL}/proveedores/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al actualizar proveedor');
  }

  return response.json();
};

// Activa o desactiva un proveedor; es la única baja que existe.
export const toggleProveedorStatus = async (id: number): Promise<Proveedor> => {
  const response = await fetch(`${API_URL}/proveedores/${id}/toggle-status`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al cambiar estado del proveedor');
  }

  return response.json();
};

export const searchProveedores = async (nombre: string): Promise<Proveedor[]> => {
  const response = await fetch(`${API_URL}/proveedores/buscar?nombre=${encodeURIComponent(nombre)}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al buscar proveedores');
  }

  return response.json();
};

// Compras

export const getAllCompras = async (): Promise<Compra[]> => {
  const response = await fetch(`${API_URL}/compras`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener compras');
  }

  return response.json();
};

export const getCompraById = async (id: number): Promise<Compra> => {
  const response = await fetch(`${API_URL}/compras/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener compra');
  }

  return response.json();
};

export const createCompra = async (data: CompraRequest): Promise<Compra> => {
  const response = await fetch(`${API_URL}/compras`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al crear compra');
  }

  return response.json();
};

// Anular compra: descuenta del inventario lo que trajo, si todavía está en stock
export const cancelarCompra = async (id: number): Promise<Compra> => {
  const response = await fetch(`${API_URL}/compras/${id}/cancelar`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al anular la compra');
  }

  return response.json();
};

// Envíos

import { 
  Envio, 
  EnvioRequest, 
  EnvioEstadisticas 
} from '@/types/envio';
import { 
  Transportadora, 
  TransportadoraRequest 
} from '@/types/transportadora';

export const getAllEnvios = async (): Promise<Envio[]> => {
  const response = await fetch(`${API_URL}/envios`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener envíos');
  }

  return response.json();
};

export const getEnvioById = async (id: number): Promise<Envio> => {
  const response = await fetch(`${API_URL}/envios/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener envío');
  }

  return response.json();
};

export const getEnvioByVenta = async (idVenta: number): Promise<Envio> => {
  const response = await fetch(`${API_URL}/envios/venta/${idVenta}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener envío de la venta');
  }

  return response.json();
};

export const getEnvioByGuia = async (guiaRemision: string): Promise<Envio> => {
  const response = await fetch(`${API_URL}/envios/guia/${guiaRemision}`);

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener envío por guía');
  }

  return response.json();
};

export const createEnvio = async (data: EnvioRequest): Promise<Envio> => {
  const response = await fetch(`${API_URL}/envios`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al crear envío');
  }

  return response.json();
};

export const updateEnvio = async (id: number, data: EnvioRequest): Promise<Envio> => {
  const response = await fetch(`${API_URL}/envios/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al actualizar envío');
  }

  return response.json();
};

// Cambiar estado del envío
export const cambiarEstadoEnvio = async (id: number, nuevoEstado: string): Promise<Envio> => {
  const response = await fetch(`${API_URL}/envios/${id}/estado?nuevoEstado=${nuevoEstado}`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al cambiar estado del envío');
  }

  return response.json();
};

// Marcar como entregado
export const marcarEnvioEntregado = async (id: number): Promise<Envio> => {
  const response = await fetch(`${API_URL}/envios/${id}/entregar`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al marcar envío como entregado');
  }

  return response.json();
};

// Filtrar por estado
export const getEnviosByEstado = async (estado: string): Promise<Envio[]> => {
  const response = await fetch(`${API_URL}/envios/estado/${estado}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al filtrar envíos por estado');
  }

  return response.json();
};

// Envíos por transportadora
export const getEnviosByTransportadora = async (idTransportadora: number): Promise<Envio[]> => {
  const response = await fetch(`${API_URL}/envios/transportadora/${idTransportadora}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener envíos de la transportadora');
  }

  return response.json();
};

// Envíos pendientes
export const getEnviosPendientes = async (): Promise<Envio[]> => {
  const response = await fetch(`${API_URL}/envios/pendientes`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener envíos pendientes');
  }

  return response.json();
};

// Envíos en camino
export const getEnviosEnCamino = async (): Promise<Envio[]> => {
  const response = await fetch(`${API_URL}/envios/en-camino`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener envíos en camino');
  }

  return response.json();
};

// Envíos por entregar
export const getEnviosPorEntregar = async (): Promise<Envio[]> => {
  const response = await fetch(`${API_URL}/envios/por-entregar`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener envíos por entregar');
  }

  return response.json();
};

export const getEnviosEstadisticas = async (): Promise<EnvioEstadisticas> => {
  const response = await fetch(`${API_URL}/envios/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener estadísticas de envíos');
  }

  return response.json();
};

// Transportadoras

export const getAllTransportadoras = async (): Promise<Transportadora[]> => {
  const response = await fetch(`${API_URL}/transportadoras`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener transportadoras');
  }

  return response.json();
};

export const getActiveTransportadoras = async (): Promise<Transportadora[]> => {
  const response = await fetch(`${API_URL}/transportadoras/activas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener transportadoras activas');
  }

  return response.json();
};

export const getTransportadoraById = async (id: number): Promise<Transportadora> => {
  const response = await fetch(`${API_URL}/transportadoras/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener transportadora');
  }

  return response.json();
};

export const createTransportadora = async (data: TransportadoraRequest): Promise<Transportadora> => {
  const response = await fetch(`${API_URL}/transportadoras`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al crear transportadora');
  }

  return response.json();
};

export const updateTransportadora = async (id: number, data: TransportadoraRequest): Promise<Transportadora> => {
  const response = await fetch(`${API_URL}/transportadoras/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al actualizar transportadora');
  }

  return response.json();
};

export const deleteTransportadora = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/transportadoras/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al eliminar transportadora');
  }
};

// Activar/Desactivar transportadora
export const toggleTransportadoraStatus = async (id: number): Promise<Transportadora> => {
  const response = await fetch(`${API_URL}/transportadoras/${id}/toggle-status`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al cambiar estado de la transportadora');
  }

  return response.json();
};

export const getTransportadoraStatistics = async () => {
  const response = await fetch(`${API_URL}/transportadoras/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener estadísticas de transportadoras');
  }

  return response.json();
};

// Pagos

import { 
  Pago, 
  PagoRequest, 
  PagoEstadisticas 
} from '@/types/pago';

export const getAllPagos = async (): Promise<Pago[]> => {
  const response = await fetch(`${API_URL}/pagos`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener pagos');
  }

  return response.json();
};

export const getPagoById = async (id: number): Promise<Pago> => {
  const response = await fetch(`${API_URL}/pagos/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener pago');
  }

  return response.json();
};

// Registrar nuevo pago
export const registrarPago = async (data: PagoRequest): Promise<Pago> => {
  const response = await fetch(`${API_URL}/pagos`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al registrar pago');
  }

  return response.json();
};

export const getPagosByVenta = async (ventaId: number): Promise<Pago[]> => {
  const response = await fetch(`${API_URL}/pagos/venta/${ventaId}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener pagos de la venta');
  }

  return response.json();
};

// Filtrar pagos por estado
export const getPagosByEstado = async (estado: string): Promise<Pago[]> => {
  const response = await fetch(`${API_URL}/pagos/estado/${estado}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al filtrar pagos por estado');
  }

  return response.json();
};

export const getPagosEstadisticas = async (): Promise<PagoEstadisticas> => {
  const response = await fetch(`${API_URL}/pagos/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener estadísticas de pagos');
  }

  return response.json();
};

export const getTotalPagosHoy = async (): Promise<number> => {
  const pagos = await getAllPagos();
  const hoy = new Date().toISOString().split('T')[0];
  
  const pagosHoy = pagos.filter(p => {
    const fechaPago = new Date(p.fechaPago).toISOString().split('T')[0];
    return fechaPago === hoy && p.estado === 'COMPLETADO';
  });

  return pagosHoy.reduce((total, pago) => total + pago.monto, 0);
};

export const getTotalPagosMes = async (): Promise<number> => {
  const pagos = await getAllPagos();
  const hoy = new Date();
  const mesActual = hoy.getMonth();
  const anioActual = hoy.getFullYear();
  
  const pagosMes = pagos.filter(p => {
    const fechaPago = new Date(p.fechaPago);
    return fechaPago.getMonth() === mesActual && 
           fechaPago.getFullYear() === anioActual && 
           p.estado === 'COMPLETADO';
  });

  return pagosMes.reduce((total, pago) => total + pago.monto, 0);
};

// Reportes

import {
  ReporteVentas,
  ReporteProductos,
  ReporteClientes,
  ReporteInventarioValorizado,
  VentaPorCategoria,
  VentaPorMetodoPago,
  VentaPorProductoFila,
  ReporteInventarioStockBajo,
  ReporteProveedores,
  ReporteTransportadoras,
  ReporteFinanciero,
  ReporteCuentasPorCobrar
} from '@/types/reporte';

// Reporte de Ventas
export const getReporteVentas = async (fechaInicio: string, fechaFin: string): Promise<ReporteVentas> => {
  const response = await fetch(
    `${API_URL}/reportes/ventas?inicio=${encodeURIComponent(fechaInicio)}&fin=${encodeURIComponent(fechaFin)}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener reporte de ventas');
  }

  return response.json();
};

// Reporte de Productos Más Vendidos
export const getReporteProductosMasVendidos = async (limite: number = 10): Promise<ReporteProductos> => {
  const response = await fetch(
    `${API_URL}/reportes/productos-mas-vendidos?limite=${limite}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener reporte de productos');
  }

  return response.json();
};

// Reporte de Clientes Frecuentes
export const getReporteClientesFrecuentes = async (limite: number = 10): Promise<ReporteClientes> => {
  const response = await fetch(
    `${API_URL}/reportes/clientes-frecuentes?limite=${limite}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener reporte de clientes');
  }

  return response.json();
};

// Reporte de Inventario Valorizado
export const getReporteInventarioValorizado = async (): Promise<ReporteInventarioValorizado> => {
  const response = await fetch(
    `${API_URL}/reportes/inventario-valorizado`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener reporte de inventario valorizado');
  }

  return response.json();
};

// Reporte de Ventas por Categoría
export const getReporteVentasPorCategoria = async (fechaInicio: string, fechaFin: string): Promise<VentaPorCategoria[]> => {
  const response = await fetch(
    `${API_URL}/reportes/ventas-por-categoria?inicio=${encodeURIComponent(fechaInicio)}&fin=${encodeURIComponent(fechaFin)}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener reporte de ventas por categoría');
  }

  return response.json();
};

// Reporte de Ventas por Método de Pago
export const getReporteVentasPorProducto = async (fechaInicio: string, fechaFin: string): Promise<VentaPorProductoFila[]> => {
  const params = new URLSearchParams({ inicio: fechaInicio, fin: fechaFin });
  const response = await fetch(`${API_URL}/reportes/ventas-por-producto?${params}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener el reporte de ventas por producto');
  }

  return response.json();
};

export const getReporteVentasPorMetodoPago = async (fechaInicio: string, fechaFin: string): Promise<VentaPorMetodoPago[]> => {
  const response = await fetch(
    `${API_URL}/reportes/ventas-por-metodo-pago?inicio=${encodeURIComponent(fechaInicio)}&fin=${encodeURIComponent(fechaFin)}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener reporte de ventas por método de pago');
  }

  return response.json();
};

// Reporte de Inventario Stock Bajo (construido desde datos existentes)
export const getReporteInventarioStockBajo = async (): Promise<ReporteInventarioStockBajo> => {
  const inventarios = await getProductosConStockBajo();
  
  const productos = inventarios.map(inv => ({
    idProducto: inv.idProducto,
    nombreProducto: inv.nombreProducto,
    skuProducto: inv.skuProducto,
    categoria: inv.nombreCategoria ?? undefined,
    cantidadDisponible: inv.cantidadDisponible,
    stockMinimo: inv.stockMinimo,
    diferencia: inv.stockMinimo - inv.cantidadDisponible
  }));

  return {
    totalProductos: productos.length,
    productos
  };
};

// Reporte de Proveedores (construido desde datos existentes)
export const getReporteProveedores = async (): Promise<ReporteProveedores> => {
  // Promise.all: el reporte cruza las dos listas y con datos parciales los totales saldrían mal.
  const [proveedores, compras] = await Promise.all([
    getAllProveedores(),
    getAllCompras()
  ]);

  const proveedoresReporte = proveedores.map(proveedor => {
    // Solo cuentan las compras CONFIRMADA; las canceladas o por confirmar inflarían el gasto con el proveedor.
    const comprasProveedor = compras.filter(c => c.idProveedor === proveedor.id && c.estado === 'CONFIRMADA');
    const montoTotal = comprasProveedor.reduce((sum, c) => sum + c.montoTotal, 0);
    const ultimaCompra = comprasProveedor.length > 0 
      ? comprasProveedor.sort((a, b) => new Date(b.fechaCompra).getTime() - new Date(a.fechaCompra).getTime())[0].fechaCompra
      : null;

    return {
      idProveedor: proveedor.id,
      nombreProveedor: proveedor.nombreEmpresa,
      nit: proveedor.nit,
      cantidadCompras: comprasProveedor.length,
      montoTotalCompras: montoTotal,
      ultimaCompra,
      activo: proveedor.activo
    };
  });

  const montoTotalCompras = proveedoresReporte.reduce((sum, p) => sum + p.montoTotalCompras, 0);

  return {
    totalProveedores: proveedores.length,
    proveedoresActivos: proveedores.filter(p => p.activo).length,
    montoTotalCompras,
    proveedores: proveedoresReporte.sort((a, b) => b.montoTotalCompras - a.montoTotalCompras)
  };
};

// Reporte de Transportadoras (construido desde datos existentes)
export const getReporteTransportadoras = async (): Promise<ReporteTransportadoras> => {
  // Promise.all: sin la lista completa de envíos, la tasa de entrega saldría falsa.
  const [transportadoras, envios] = await Promise.all([
    getAllTransportadoras(),
    getAllEnvios()
  ]);

  const transportadorasReporte = transportadoras.map(transportadora => {
    const enviosTransportadora = envios.filter(e => e.idTransportadora === transportadora.id);
    const enviosEntregados = enviosTransportadora.filter(e => e.estadoSeguimiento === 'ENTREGADO').length;
    const enviosPendientes = enviosTransportadora.filter(e => 
      e.estadoSeguimiento === 'PENDIENTE' || 
      e.estadoSeguimiento === 'EN_PREPARACION' || 
      e.estadoSeguimiento === 'EN_CAMINO'
    ).length;
    const tasaEntrega = enviosTransportadora.length > 0 
      ? (enviosEntregados / enviosTransportadora.length) * 100 
      : 0;

    return {
      idTransportadora: transportadora.id,
      nombreTransportadora: transportadora.nombre,
      cantidadEnvios: enviosTransportadora.length,
      enviosEntregados,
      enviosPendientes,
      tasaEntrega,
      activo: transportadora.activo
    };
  });

  return {
    totalTransportadoras: transportadoras.length,
    transportadorasActivas: transportadoras.filter(t => t.activo).length,
    totalEnvios: envios.length,
    transportadoras: transportadorasReporte.sort((a, b) => b.cantidadEnvios - a.cantidadEnvios)
  };
};

// Cuentas por cobrar: foto del momento, sin fechas; el cálculo vive en el backend.
export const getReporteCuentasPorCobrar = async (): Promise<ReporteCuentasPorCobrar> => {
  const response = await fetch(
    `${API_URL}/reportes/cuentas-por-cobrar`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener reporte de cuentas por cobrar');
  }

  return response.json();
};

// Reporte financiero (solo ADMIN): el backend calcula la ganancia y aquí solo llega el resultado.
export const getReporteFinanciero = async (fechaInicio: string, fechaFin: string): Promise<ReporteFinanciero> => {
  const response = await fetch(
    `${API_URL}/reportes/financiero?inicio=${encodeURIComponent(fechaInicio)}&fin=${encodeURIComponent(fechaFin)}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener reporte financiero');
  }

  return response.json();
};

// Promociones - promocions

import { Promocion, PromocionRequest } from '@/types/promocion';

export const getAllPromociones = async (): Promise<Promocion[]> => {
  const response = await fetch(`${API_URL}/promociones`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener promociones');
  }

  return response.json();
};

export const getActivePromociones = async (): Promise<Promocion[]> => {
  const response = await fetch(`${API_URL}/promociones/activas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener promociones activas');
  }

  return response.json();
};

export const getPromocionesVigentes = async (): Promise<Promocion[]> => {
  const response = await fetch(`${API_URL}/promociones/vigentes`);

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener promociones vigentes');
  }

  return response.json();
};

export const getPromocionById = async (id: number): Promise<Promocion> => {
  const response = await fetch(`${API_URL}/promociones/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener promocion');
  }

  return response.json();
};

export const createPromocion = async (data: PromocionRequest): Promise<Promocion> => {
  const response = await fetch(`${API_URL}/promociones`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al crear promocion');
  }

  return response.json();
};

export const updatePromocion = async (id: number, data: PromocionRequest): Promise<Promocion> => {
  const response = await fetch(`${API_URL}/promociones/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al actualizar promocion');
  }

  return response.json();
};

export const deletePromocion = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/promociones/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al eliminar promocion');
  }
};

// Activar/Desactivar promocion
export const togglePromocionStatus = async (id: number): Promise<Promocion> => {
  const response = await fetch(`${API_URL}/promociones/${id}/toggle-status`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al cambiar estado de la promocion');
  }

  return response.json();
};

export const getPromocionStatistics = async () => {
  const response = await fetch(`${API_URL}/promociones/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener estadísticas de promociones');
  }

  return response.json();
};

// Configuración

import { Configuracion, ConfiguracionRequest, Auditoria } from '@/types/configuracion';

export const getAllConfiguraciones = async (): Promise<Configuracion[]> => {
  const response = await fetch(`${API_URL}/configuracion`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener configuraciones');
  }

  return response.json();
};

export const getConfiguracionById = async (id: number): Promise<Configuracion> => {
  const response = await fetch(`${API_URL}/configuracion/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener configuración');
  }

  return response.json();
};

export const getConfiguracionByClave = async (clave: string): Promise<Configuracion> => {
  const response = await fetch(`${API_URL}/configuracion/clave/${clave}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener configuración');
  }

  return response.json();
};

export const getValorConfiguracion = async (clave: string): Promise<string> => {
  const response = await fetch(`${API_URL}/configuracion/valor/${clave}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener valor de configuración');
  }

  const data = await response.json();
  return data.valor;
};

export const createConfiguracion = async (data: ConfiguracionRequest): Promise<Configuracion> => {
  const response = await fetch(`${API_URL}/configuracion`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al crear configuración');
  }

  return response.json();
};

export const updateConfiguracion = async (id: number, data: ConfiguracionRequest): Promise<Configuracion> => {
  const response = await fetch(`${API_URL}/configuracion/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al actualizar configuración');
  }

  return response.json();
};

export const updateValorByClave = async (clave: string, nuevoValor: string): Promise<Configuracion> => {
  const response = await fetch(`${API_URL}/configuracion/clave/${clave}?nuevoValor=${encodeURIComponent(nuevoValor)}`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al actualizar configuración');
  }

  return response.json();
};

export const deleteConfiguracion = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/configuracion/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al eliminar configuración');
  }
};

// Inicializar configuraciones por defecto
export const inicializarConfiguraciones = async (): Promise<void> => {
  const response = await fetch(`${API_URL}/configuracion/inicializar`, {
    method: 'POST',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al inicializar configuraciones');
  }
};

// Auditoría

export const getAllAuditorias = async (): Promise<Auditoria[]> => {
  const response = await fetch(`${API_URL}/auditorias`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener auditorías');
  }

  return response.json();
};

export const getAuditoriaById = async (id: number): Promise<Auditoria> => {
  const response = await fetch(`${API_URL}/auditorias/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener auditoría');
  }

  return response.json();
};

// Auditorías de un usuario
export const getAuditoriasByUsuario = async (idUsuario: number): Promise<Auditoria[]> => {
  const response = await fetch(`${API_URL}/auditorias/usuario/${idUsuario}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener auditorías del usuario');
  }

  return response.json();
};

// Auditorías de una tabla
export const getAuditoriasByTabla = async (tablaAfectada: string): Promise<Auditoria[]> => {
  const response = await fetch(`${API_URL}/auditorias/tabla/${tablaAfectada}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener auditorías de la tabla');
  }

  return response.json();
};

// Auditorías por acción
export const getAuditoriasByAccion = async (accion: string): Promise<Auditoria[]> => {
  const response = await fetch(`${API_URL}/auditorias/accion/${accion}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener auditorías por acción');
  }

  return response.json();
};

// Auditorías entre fechas
export const getAuditoriasByFechas = async (inicio: string, fin: string): Promise<Auditoria[]> => {
  const response = await fetch(
    `${API_URL}/auditorias/fechas?inicio=${encodeURIComponent(inicio)}&fin=${encodeURIComponent(fin)}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener auditorías por fechas');
  }

  return response.json();
};

// Últimas auditorías
export const getUltimasAuditorias = async (): Promise<Auditoria[]> => {
  const response = await fetch(`${API_URL}/auditorias/ultimas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener últimas auditorías');
  }

  return response.json();
};

// Auditorías del día
export const getAuditoriasDelDia = async (): Promise<Auditoria[]> => {
  const response = await fetch(`${API_URL}/auditorias/del-dia`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener auditorías del día');
  }

  return response.json();
};

// Funciones para Imágenes de Producto
export const getImagenesProducto = async (idProducto: number): Promise<ImagenProducto[]> => {
  const response = await fetch(`${API_URL}/imagenes-producto/producto/${idProducto}`, {
    method: 'GET',
    headers: getAuthHeaders(), // Asumiendo que usas esta función para incluir el token
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener imágenes del producto');
  }
  return response.json();
};

// Función corregida para subir imágenes
export const addImagenProducto = async (idProducto: number, file: File, esPrincipal: boolean): Promise<ImagenProducto> => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('esPrincipal', esPrincipal.toString());

  const response = await fetch(`${API_URL}/imagenes-producto/producto/${idProducto}`, {
    method: 'POST',
    headers: getAuthHeadersForFormData(),
    body: formData,
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al subir la imagen');
  }
  return response.json();
};

export const deleteImagenProducto = async (idImagen: number): Promise<void> => {
  const response = await fetch(`${API_URL}/imagenes-producto/${idImagen}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al eliminar la imagen');
  }
  // No hay cuerpo de respuesta para DELETE
};

export const setImagenPrincipal = async (idImagen: number, idProducto: number): Promise<void> => {
  const response = await fetch(`${API_URL}/imagenes-producto/${idImagen}/principal/${idProducto}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al marcar imagen como principal');
  }
  // No hay cuerpo de respuesta para PUT
};

// Comprobantes

export interface Comprobante {
  id: number;
  idVenta: number;
  numeroComprobante: string;
  tipoComprobante: 'RECIBO' | 'COMPROBANTE' | 'NOTA_VENTA';
  montoTotal: number;
  nombreCliente: string;
  observaciones?: string;
  fechaEmision: string;
  anulado: boolean;
}

export interface ComprobanteRequest {
  idVenta: number;
  tipoComprobante: 'RECIBO' | 'COMPROBANTE' | 'NOTA_VENTA';
  nombreCliente: string;
  observaciones?: string;
}

/** Obtener comprobante por venta */
export const getComprobanteByVenta = async (idVenta: number): Promise<Comprobante> => {
  const response = await fetch(`${API_URL}/comprobantes/venta/${idVenta}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'No existe comprobante para esta venta');
  }

  return response.json();
};

/** Crear comprobante manualmente (si no se creó automáticamente) */
export const createComprobante = async (data: ComprobanteRequest): Promise<Comprobante> => {
  const response = await fetch(`${API_URL}/comprobantes`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al crear comprobante');
  }

  return response.json();
};

// frontend/src/lib/api.ts (agregar al final de la sección de PRODUCTOS)

// Multimedia productos (modelos 3d)

import { MultimediaProducto } from '@/types/producto';

export const getMultimediaProducto = async (productoId: number): Promise<MultimediaProducto | null> => {
  try {
    const response = await fetch(`${API_URL}/multimedia-productos/producto/${productoId}`, {
      headers: getAuthHeaders(),
    });

    if (response.status === 404) {
      return null; // No tiene modelo 3D
    }

    if (!response.ok) {
      throw await errorDeRespuesta(response, 'Error al obtener multimedia del producto');
    }

    return response.json();
  } catch (error) {
    console.error('Error en getMultimediaProducto:', error);
    return null;
  }
};

// Subir modelo 3D
export const subirModelo3D = async (
  productoId: number,
  archivoModelo: File,
  archivoPreview: File | null,
  habilitadoRa: boolean
): Promise<MultimediaProducto> => {
  const formData = new FormData();
  formData.append('archivoModelo', archivoModelo);
  if (archivoPreview) {
    formData.append('archivoPreview', archivoPreview);
  }
  formData.append('habilitadoRa', String(habilitadoRa));

  const token = document.cookie
    .split('; ')
    .find(row => row.startsWith('token='))
    ?.split('=')[1];

  const response = await fetch(`${API_URL}/multimedia-productos/subir-modelo/${productoId}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
    body: formData,
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al subir modelo 3D');
  }

  return response.json();
};

export const eliminarMultimedia = async (multimediaId: number): Promise<void> => {
  const response = await fetch(`${API_URL}/multimedia-productos/${multimediaId}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al eliminar multimedia');
  }
};

// Dashboard

import { DashboardEstadisticas, VentasSemanal } from '@/types/dashboard';

/** Estadísticas del panel de inicio. */
export const getDashboardEstadisticas = async (): Promise<DashboardEstadisticas> => {
  const response = await fetch(`${API_URL}/dashboard/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener las estadísticas del panel');
  }

  return response.json();
};

// Ventas de la semana en curso (lunes a domingo), solo completadas.
export const getVentasSemanal = async (): Promise<VentasSemanal> => {
  const response = await fetch(`${API_URL}/dashboard/ventas-semanal`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener las ventas de la semana');
  }

  return response.json();
};

// Datos públicos del negocio

export interface DatosNegocio {
  negocio_razon_social?: string;
  negocio_descripcion?: string;
  negocio_direccion?: string;
  negocio_ciudad?: string;
  negocio_telefono?: string;
  negocio_whatsapp?: string;
  negocio_email?: string;
  negocio_horario?: string;
  negocio_nit?: string;
  negocio_sitio_web?: string;
}

/** Datos del negocio para la tienda pública (sin autenticación), editables desde Configuración. */
export const getDatosNegocio = async (): Promise<DatosNegocio> => {
  const response = await fetch(`${API_URL}/configuracion/negocio`);

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener los datos del negocio');
  }

  return response.json();
};

// Mensajes de contacto

export interface MensajeContactoRequest {
  nombre: string;
  email: string;
  telefono?: string;
  asunto: string;
  mensaje: string;
}

export interface MensajeContacto {
  id: number;
  nombre: string;
  email: string;
  telefono?: string;
  asunto: string;
  mensaje: string;
  atendido: boolean;
  fechaAtencion?: string;
  idUsuarioAtiende?: number;
  nombreUsuarioAtiende?: string;
  fechaEnvio: string;
}

/** Envía una consulta desde la tienda; no requiere sesión. */
export const enviarMensajeContacto = async (
  data: MensajeContactoRequest
): Promise<MensajeContacto> => {
  const response = await fetch(`${API_URL}/mensajes-contacto`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'No se pudo enviar el mensaje');
  }

  return response.json();
};

export const getMensajesContacto = async (): Promise<MensajeContacto[]> => {
  const response = await fetch(`${API_URL}/mensajes-contacto`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al obtener los mensajes de contacto');
  }

  return response.json();
};

export const marcarMensajeAtendido = async (
  id: number,
  idUsuario: number
): Promise<MensajeContacto> => {
  const response = await fetch(
    `${API_URL}/mensajes-contacto/${id}/atender?idUsuario=${idUsuario}`,
    { method: 'PATCH', headers: getAuthHeaders() }
  );

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al marcar el mensaje como atendido');
  }

  return response.json();
};

export const eliminarMensajeContacto = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/mensajes-contacto/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al eliminar el mensaje');
  }
};

/** Cambia el stock mínimo de un producto, el umbral que dispara las alertas. */
export const actualizarStockMinimo = async (
  idProducto: number,
  stockMinimo: number
): Promise<Producto> => {
  const response = await fetch(
    `${API_URL}/productos/${idProducto}/stock-minimo?stockMinimo=${stockMinimo}`,
    { method: 'PATCH', headers: getAuthHeaders() }
  );

  if (!response.ok) {
    throw await errorDeRespuesta(response, 'Error al actualizar el stock mínimo');
  }

  return response.json();
};

/** Precarga en segundo plano las listas principales en la caché; ignora los fallos. */
export const precargarDatos = async (): Promise<void> => {
  const pedidos: Array<() => Promise<unknown>> = [
    getAllProductos,
    getActiveCategorias,
    getAllInventario,
    getAlertasPendientes,
    getAllVentas,
    getAllClientes,
    getClientesConEstadisticas,
    getAllCompras,
    getAllProveedores,
    getAllUsers,
  ];
  for (const pedir of pedidos) {
    try {
      await pedir();
    } catch {
      // se ignora: es solo una precarga
    }
  }
};
