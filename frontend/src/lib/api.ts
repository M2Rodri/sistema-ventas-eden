// Servicio de API para conectar con el backend Spring Boot
import { ImagenProducto } from '@/types/imagenProducto';
export const BACKEND_URL =
  process.env.NEXT_PUBLIC_BACKEND_URL ?? 'https://backend-sistema-ventas-production-d0a4.up.railway.app';
const API_URL = `${BACKEND_URL}/api`;

export interface RegisterData {
  nombre: string;
  apellido: string;
  email: string;
  password: string;
  telefono?: string;
  direccion?: string;
}

export interface LoginData {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tipo: string;
  id: number;
  nombre: string;
  apellido: string;
  email: string;
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


// ============================================
// USUARIOS 
// ============================================

export interface User {
  id: number;
  nombre: string;
  apellido: string;
  nombreCompleto: string;
  email: string;
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
  email: string;
  password?: string;
  telefono?: string;
  direccion?: string;
  role: 'ADMIN' | 'EMPLEADO' | 'CLIENTE';
  activo: boolean;
}

// IMPORTAR TIPOS DE PRODUCTO Y CATEGORIA
import { Producto, ProductoRequest, Categoria, TipoProducto } from '@/types/producto';

// Helper para obtener el token
const getAuthHeaders = () => {
  const token = document.cookie
    .split('; ')
    .find(row => row.startsWith('token='))
    ?.split('=')[1];
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };
};

const getAuthHeadersForFormData = () => {
  const token = document.cookie
    .split('; ')
    .find(row => row.startsWith('token='))
    ?.split('=')[1];
  return {
    'Authorization': `Bearer ${token}`,
    // NO incluir Content-Type - el navegador lo establece automáticamente
  };
};

// Listar todos los usuarios
export const getAllUsers = async (): Promise<User[]> => {
  const response = await fetch(`${API_URL}/usuarios`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener usuarios');
  }

  return response.json();
};

// Obtener usuario por ID
export const getUserById = async (id: number): Promise<User> => {
  const response = await fetch(`${API_URL}/usuarios/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener usuario');
  }

  return response.json();
};

// Crear usuario
export const createUser = async (data: UserRequest): Promise<User> => {
  const response = await fetch(`${API_URL}/usuarios`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al crear usuario');
  }

  return response.json();
};

// Actualizar usuario
export const updateUser = async (id: number, data: UserRequest): Promise<User> => {
  const response = await fetch(`${API_URL}/usuarios/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al actualizar usuario');
  }

  return response.json();
};

// Eliminar usuario (desactivar)
export const deleteUser = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/usuarios/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al eliminar usuario');
  }
};

// Activar/Desactivar usuario
export const toggleUserStatus = async (id: number): Promise<User> => {
  const response = await fetch(`${API_URL}/usuarios/${id}/toggle-status`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al cambiar estado del usuario');
  }

  return response.json();
};

// Filtrar por rol
export const getUsersByRole = async (role: string): Promise<User[]> => {
  const response = await fetch(`${API_URL}/usuarios/rol/${role}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al filtrar usuarios');
  }

  return response.json();
};

// Obtener estadísticas
export const getUserStatistics = async () => {
  const response = await fetch(`${API_URL}/usuarios/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener estadísticas');
  }

  return response.json();
};


// ============================================
// CATEGORÍAS 
// ============================================

// Listar todas las categorías
export const getAllCategorias = async (): Promise<Categoria[]> => {
  const response = await fetch(`${API_URL}/categorias`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener categorías');
  }

  return response.json();
};

// Listar categorías activas
export const getActiveCategorias = async (): Promise<Categoria[]> => {
  const response = await fetch(`${API_URL}/categorias/activas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener categorías activas');
  }

  return response.json();
};

// Crear categoría
export const createCategoria = async (data: { nombre: string; descripcion?: string; tipoProducto: TipoProducto; activo: boolean }): Promise<Categoria> => {
  const response = await fetch(`${API_URL}/categorias`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al crear categoría');
  }

  return response.json();
};

// Actualizar categoría
export const updateCategoria = async (id: number, data: { nombre: string; descripcion?: string; tipoProducto: TipoProducto; activo: boolean }): Promise<Categoria> => {
  const response = await fetch(`${API_URL}/categorias/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al actualizar categoría');
  }

  return response.json();
};

// Eliminar categoría
export const deleteCategoria = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/categorias/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al eliminar categoría');
  }
};

// Activar/Desactivar categoría
export const toggleCategoriaStatus = async (id: number): Promise<Categoria> => {
  const response = await fetch(`${API_URL}/categorias/${id}/toggle-status`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al cambiar estado de la categoría');
  }

  return response.json();
};


// ============================================
// PRODUCTOS 
// ============================================

// Listar todos los productos
// Ejemplo corregido y funcional para getAllProductos
export const getAllProductos = async (): Promise<Producto[]> => {
  const response = await fetch(`${API_URL}/productos`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    // Leemos el cuerpo del error como texto
    const errorBody = await response.text(); // ✅ Esperamos a que se resuelva la promesa
    let errorMessage = 'Error al obtener productos';

    if (errorBody) {
      try {
        // Intentamos parsear el cuerpo como JSON para obtener un mensaje amigable
        const errorJson = JSON.parse(errorBody);
        errorMessage = errorJson.error || errorMessage; // Usamos el mensaje del backend o el genérico
      } catch (e) {
        // Si no es JSON, usamos el texto plano como mensaje
        errorMessage = errorBody;
      }
    }

    throw new Error(errorMessage);
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

// Listar productos activos
export const getActiveProductos = async (): Promise<Producto[]> => {
  const response = await fetch(`${API_URL}/productos/activos`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener productos activos');
  }

  return response.json();
};

// Obtener producto por ID
export const getProductoById = async (id: number): Promise<Producto> => {
  const response = await fetch(`${API_URL}/productos/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener producto');
  }

  return response.json();
};

// Obtener producto por SKU
export const getProductoBySku = async (sku: string): Promise<Producto> => {
  const response = await fetch(`${API_URL}/productos/sku/${sku}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener producto por SKU');
  }

  return response.json();
};

// Crear producto
export const createProducto = async (data: ProductoRequest): Promise<Producto> => {
  const response = await fetch(`${API_URL}/productos`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al crear producto');
  }

  return response.json();
};

// Actualizar producto
export const updateProducto = async (id: number, data: ProductoRequest): Promise<Producto> => {
  const response = await fetch(`${API_URL}/productos/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al actualizar producto');
  }

  return response.json();
};

// Eliminar producto (desactivar)
export const deleteProducto = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/productos/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al eliminar producto');
  }
};

// Activar/Desactivar producto
export const toggleProductoStatus = async (id: number): Promise<Producto> => {
  const response = await fetch(`${API_URL}/productos/${id}/toggle-status`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al cambiar estado del producto');
  }

  return response.json();
};

// Filtrar por categoría
export const getProductosByCategoria = async (categoriaId: number): Promise<Producto[]> => {
  const response = await fetch(`${API_URL}/productos/categoria/${categoriaId}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al filtrar productos por categoría');
  }

  return response.json();
};

// Filtrar por tipo
// Buscar productos
export const searchProductos = async (nombre: string): Promise<Producto[]> => {
  const response = await fetch(`${API_URL}/productos/buscar?nombre=${encodeURIComponent(nombre)}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al buscar productos');
  }

  return response.json();
};

// ============================================
// INVENTARIO 
// ============================================

import { 
  Inventario, 
  InventarioRequest, 
  AlertaInventario, 
  MovimientoInventario, 
  MovimientoInventarioRequest 
} from '@/types/inventario';

// Listar todo el inventario
export const getAllInventario = async (): Promise<Inventario[]> => {
  const response = await fetch(`${API_URL}/inventario`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener inventario');
  }

  return response.json();
};

// Obtener inventario por ID
export const getInventarioById = async (id: number): Promise<Inventario> => {
  const response = await fetch(`${API_URL}/inventario/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener inventario');
  }

  return response.json();
};

// Obtener inventario por producto
export const getInventarioByProducto = async (idProducto: number): Promise<Inventario> => {
  const response = await fetch(`${API_URL}/inventario/producto/${idProducto}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener inventario del producto');
  }

  return response.json();
};

// Crear inventario
export const createInventario = async (data: InventarioRequest): Promise<Inventario> => {
  const response = await fetch(`${API_URL}/inventario`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al crear inventario');
  }

  return response.json();
};

// Actualizar inventario
export const updateInventario = async (id: number, data: InventarioRequest): Promise<Inventario> => {
  const response = await fetch(`${API_URL}/inventario/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al actualizar inventario');
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
    const error = await response.json();
    throw new Error(error.error || 'Error al ajustar inventario');
  }

  return response.json();
};

// Obtener historial de ajustes de un producto
export const getHistorialAjustes = async (idProducto: number): Promise<MovimientoInventario[]> => {
  const response = await fetch(`${API_URL}/inventario/producto/${idProducto}/historial`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener historial de ajustes');
  }

  return response.json();
};

// Obtener últimos 50 ajustes
export const getUltimosAjustes = async (): Promise<MovimientoInventario[]> => {
  const response = await fetch(`${API_URL}/inventario/ajustes/ultimos`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener últimos ajustes');
  }

  return response.json();
};

// Obtener productos con stock bajo
export const getProductosConStockBajo = async (): Promise<Inventario[]> => {
  const response = await fetch(`${API_URL}/inventario/stock-bajo`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener productos con stock bajo');
  }

  return response.json();
};

// Obtener productos sin stock
export const getProductosSinStock = async (): Promise<Inventario[]> => {
  const response = await fetch(`${API_URL}/inventario/sin-stock`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener productos sin stock');
  }

  return response.json();
};

// Verificar disponibilidad de stock
export const verificarDisponibilidad = async (idProducto: number, cantidad: number): Promise<{ disponible: boolean; idProducto: number; cantidadSolicitada: number }> => {
  const response = await fetch(`${API_URL}/inventario/verificar-disponibilidad?idProducto=${idProducto}&cantidad=${cantidad}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al verificar disponibilidad');
  }

  return response.json();
};

// Obtener alertas pendientes
export const getAlertasPendientes = async (): Promise<AlertaInventario[]> => {
  const response = await fetch(`${API_URL}/inventario/alertas/pendientes`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener alertas pendientes');
  }

  return response.json();
};

// Marcar alerta como atendida
export const marcarAlertaAtendida = async (id: number): Promise<AlertaInventario> => {
  const response = await fetch(`${API_URL}/inventario/alertas/${id}/atender`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al marcar alerta como atendida');
  }

  return response.json();
};

// Obtener estadísticas de inventario
export const getInventarioStatistics = async () => {
  const response = await fetch(`${API_URL}/inventario/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener estadísticas de inventario');
  }

  return response.json();
};

// ============================================
// ============================================
// VENTAS
// ============================================

import {
  Venta,
  VentaRequest,
  VentaEstadisticas,
  Pago as PagoDeVenta
} from '@/types/venta';

/**
 * Listar todas las ventas
 */
export const getAllVentas = async (): Promise<Venta[]> => {
  const response = await fetch(`${API_URL}/ventas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener ventas');
  }

  return response.json();
};

/**
 * Obtener venta por ID
 */
export const getVentaById = async (id: number): Promise<Venta> => {
  const response = await fetch(`${API_URL}/ventas/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener venta');
  }

  return response.json();
};

/**
 * Crear venta directa con cliente rápido
 * Soporta tanto cliente registrado (idCliente) como cliente rápido (nombreClienteDirecto)
 */
export const createVentaDirecta = async (
  request: VentaRequest
): Promise<Venta> => {
  const response = await fetch(`${API_URL}/ventas`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al crear venta');
  }

  return response.json();
};

/**
 * Cancelar venta (Solo ADMIN)
 * Restaura el stock de los productos
 */
export const cancelarVenta = async (id: number): Promise<Venta> => {
  const response = await fetch(`${API_URL}/ventas/${id}/cancelar`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al cancelar venta');
  }

  return response.json();
};

/**
 * Marcar la entrega de una venta como completada.
 * Rechaza si la venta tiene saldo pendiente.
 */
export const marcarVentaEntregada = async (id: number): Promise<Venta> => {
  const response = await fetch(`${API_URL}/ventas/${id}/entregar`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al marcar la venta como entregada');
  }

  return response.json();
};

/**
 * Adjuntar (o reemplazar) la foto de comprobante de un pago ya registrado.
 */
export const adjuntarComprobantePago = async (idPago: number, file: File): Promise<PagoDeVenta> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${API_URL}/pagos/${idPago}/comprobante`, {
    method: 'POST',
    headers: getAuthHeadersForFormData(),
    body: formData,
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al subir el comprobante');
  }

  return response.json();
};

/**
 * Obtener ventas por cliente registrado
 */
export const getVentasByCliente = async (clienteId: number): Promise<Venta[]> => {
  const response = await fetch(`${API_URL}/ventas/cliente/${clienteId}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener ventas del cliente');
  }

  return response.json();
};

/**
 * Obtener ventas por estado
 */
export const getVentasByEstado = async (estado: string): Promise<Venta[]> => {
  const response = await fetch(`${API_URL}/ventas/estado/${estado}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al filtrar ventas por estado');
  }

  return response.json();
};

/**
 * Obtener ventas del día actual
 */
export const getVentasDelDia = async (): Promise<Venta[]> => {
  const response = await fetch(`${API_URL}/ventas/del-dia`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener ventas del día');
  }

  return response.json();
};

/**
 * Obtener últimas 10 ventas
 */
export const getUltimasVentas = async (): Promise<Venta[]> => {
  const response = await fetch(`${API_URL}/ventas/ultimas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener últimas ventas');
  }

  return response.json();
};

/**
 * Obtener ventas por rango de fechas
 */
export const getVentasByFechas = async (inicio: string, fin: string): Promise<Venta[]> => {
  const response = await fetch(
    `${API_URL}/ventas/fechas?inicio=${encodeURIComponent(inicio)}&fin=${encodeURIComponent(fin)}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw new Error('Error al filtrar ventas por fecha');
  }

  return response.json();
};

/**
 * Obtener total de ventas por rango de fechas
 */
export const getTotalVentasByFechas = async (inicio: string, fin: string): Promise<number> => {
  const response = await fetch(
    `${API_URL}/ventas/total-fechas?inicio=${encodeURIComponent(inicio)}&fin=${encodeURIComponent(fin)}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw new Error('Error al obtener total de ventas');
  }

  const data = await response.json();
  return data.total;
};

/**
 * Obtener estadísticas de ventas (Solo ADMIN)
 * Incluye totales por estado y montos
 */
export const getVentasEstadisticas = async (): Promise<VentaEstadisticas> => {
  const response = await fetch(`${API_URL}/ventas/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener estadísticas de ventas');
  }

  const data = await response.json();

  // Obtener ventas del día para estadísticas adicionales
  const ventasDelDia = await getVentasDelDia();
  const montoDelDia = ventasDelDia.reduce((sum, v) => sum + v.montoTotal, 0);

  // Obtener todas las ventas para calcular monto total
  const todasVentas = await getAllVentas();
  const montoTotal = todasVentas
    .filter(v => v.estado === 'COMPLETADA')
    .reduce((sum, v) => sum + v.montoTotal, 0);

  return {
    totalVentas: todasVentas.length,
    ventasCompletadas: data.completadas || 0,
    ventasPendientes: data.pendientes || 0,
    ventasCanceladas: data.canceladas || 0,
    montoTotal: montoTotal,
    ventasDelDia: ventasDelDia.length,
    montoDelDia: montoDelDia
  };
};



// lib/api.ts - Agregar estas funciones al archivo existente

import {
  ClienteRequest,
  ClienteResponse,
  ClienteEstadisticas,
  HistorialComprasResponse,
  ClienteConEstadisticas
} from '@/types/cliente';

// ============================================
// CLIENTES
// ============================================

/**
 * Listar todos los clientes
 * CU: Gestionar Clientes - Interfaz P6.1
 */
export const getAllClientes = async (): Promise<ClienteResponse[]> => {
  const response = await fetch(`${API_URL}/clientes`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener clientes');
  }

  return response.json();
};

/**
 * Listar solo clientes activos
 */
export const getActiveClientes = async (): Promise<ClienteResponse[]> => {
  const response = await fetch(`${API_URL}/clientes/activos`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener clientes activos');
  }

  return response.json();
};

/**
 * Obtener cliente por ID
 */
export const getClienteById = async (id: number): Promise<ClienteResponse> => {
  const response = await fetch(`${API_URL}/clientes/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Cliente no encontrado');
  }

  return response.json();
};

/**
 * Crear nuevo cliente
 */
export const createCliente = async (request: ClienteRequest): Promise<ClienteResponse> => {
  const response = await fetch(`${API_URL}/clientes`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al crear cliente');
  }

  return response.json();
};

/**
 * Actualizar cliente existente
 * CU: Modificar Cliente - Interfaz P6.2
 */
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
    const error = await response.json();
    throw new Error(error.error || 'Error al actualizar cliente');
  }

  return response.json();
};

/**
 * Eliminar cliente (desactivar)
 */
export const deleteCliente = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/clientes/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al eliminar cliente');
  }
};

/**
 * Activar/Desactivar cliente
 */
export const toggleClienteStatus = async (id: number): Promise<ClienteResponse> => {
  const response = await fetch(`${API_URL}/clientes/${id}/toggle-status`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al cambiar estado del cliente');
  }

  return response.json();
};

/**
 * Buscar clientes por nombre, teléfono o NIT/CI
 * CU: Buscar/Consultar Cliente - Interfaz P6.1
 */
export const searchClientes = async (query: string): Promise<ClienteResponse[]> => {
  const response = await fetch(
    `${API_URL}/clientes/buscar?q=${encodeURIComponent(query)}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw new Error('Error al buscar clientes');
  }

  return response.json();
};

/**
 * Filtrar clientes por tipo
 */
export const getClientesByTipo = async (tipo: string): Promise<ClienteResponse[]> => {
  const response = await fetch(`${API_URL}/clientes/tipo/${tipo}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al filtrar clientes por tipo');
  }

  return response.json();
};

/**
 * Obtener estadísticas generales de clientes
 * Para Interfaz P6.1 - Indicadores superiores
 */
export const getEstadisticasGeneralesClientes = async (): Promise<ClienteEstadisticas> => {
  const response = await fetch(`${API_URL}/clientes/estadisticas-generales`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener estadísticas de clientes');
  }

  return response.json();
};

/**
 * Obtener historial completo de compras de un cliente
 * CU: Ver Historial de Compras del Cliente - Interfaz P6.3
 */
export const getHistorialCompras = async (clienteId: number): Promise<HistorialComprasResponse> => {
  const response = await fetch(`${API_URL}/clientes/${clienteId}/historial-compras`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener historial de compras');
  }

  return response.json();
};

/**
 * Obtener historial de compras filtrado por fechas
 * CU: Ver Historial de Compras del Cliente - Filtro de fechas
 */
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
    throw new Error('Error al obtener historial filtrado');
  }

  return response.json();
};

/**
 * FUNCIÓN AUXILIAR: Obtener clientes con estadísticas de compra
 * Combina datos de clientes con sus ventas para la tabla P6.1
 */
export const getClientesConEstadisticas = async (): Promise<ClienteConEstadisticas[]> => {
  const clientes = await getAllClientes();
  
  // Para cada cliente, obtener sus estadísticas
  // Promise.all es seguro aca: el map de abajo tiene su propio try/catch y
  // devuelve el cliente con estadisticas en cero si falla su historial, asi
  // que ninguna de estas promesas llega a rechazar.
  const clientesConStats = await Promise.all(
    clientes.map(async (cliente) => {
      try {
        const historial = await getHistorialCompras(cliente.id);
        return {
          ...cliente,
          numeroCompras: historial.estadisticas.totalCompras,
          montoTotalComprado: historial.estadisticas.montoTotal,
          ultimaFechaCompra: historial.estadisticas.ultimaCompra,
        };
      } catch (error) {
        // Si falla, retornar con estadísticas en cero
        return {
          ...cliente,
          numeroCompras: 0,
          montoTotalComprado: 0,
          ultimaFechaCompra: undefined,
        };
      }
    })
  );

  return clientesConStats;
};



// ============================================
// PROVEEDORES 
// ============================================

import { 
  Proveedor, 
  ProveedorRequest,
  Compra,
  CompraRequest,
  CompraEstadisticas
} from '@/types/proveedor';

// Listar todos los proveedores
export const getAllProveedores = async (): Promise<Proveedor[]> => {
  const response = await fetch(`${API_URL}/proveedores`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener proveedores');
  }

  return response.json();
};

// Listar proveedores activos
export const getActiveProveedores = async (): Promise<Proveedor[]> => {
  const response = await fetch(`${API_URL}/proveedores/activos`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener proveedores activos');
  }

  return response.json();
};

// Obtener proveedor por ID
export const getProveedorById = async (id: number): Promise<Proveedor> => {
  const response = await fetch(`${API_URL}/proveedores/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener proveedor');
  }

  return response.json();
};

// Obtener proveedor por NIT
export const getProveedorByNit = async (nit: string): Promise<Proveedor> => {
  const response = await fetch(`${API_URL}/proveedores/nit/${nit}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener proveedor por NIT');
  }

  return response.json();
};

// Crear proveedor
export const createProveedor = async (data: ProveedorRequest): Promise<Proveedor> => {
  const response = await fetch(`${API_URL}/proveedores`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al crear proveedor');
  }

  return response.json();
};

// Actualizar proveedor
export const updateProveedor = async (id: number, data: ProveedorRequest): Promise<Proveedor> => {
  const response = await fetch(`${API_URL}/proveedores/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al actualizar proveedor');
  }

  return response.json();
};

// Eliminar proveedor (desactivar)
export const deleteProveedor = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/proveedores/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al eliminar proveedor');
  }
};

// Activar/Desactivar proveedor
export const toggleProveedorStatus = async (id: number): Promise<Proveedor> => {
  const response = await fetch(`${API_URL}/proveedores/${id}/toggle-status`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al cambiar estado del proveedor');
  }

  return response.json();
};

// Buscar proveedores
export const searchProveedores = async (nombre: string): Promise<Proveedor[]> => {
  const response = await fetch(`${API_URL}/proveedores/buscar?nombre=${encodeURIComponent(nombre)}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al buscar proveedores');
  }

  return response.json();
};

// Obtener estadísticas de proveedores
export const getProveedorStatistics = async () => {
  const response = await fetch(`${API_URL}/proveedores/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener estadísticas de proveedores');
  }

  return response.json();
};

// ============================================
// COMPRAS 
// ============================================

// Listar todas las compras
export const getAllCompras = async (): Promise<Compra[]> => {
  const response = await fetch(`${API_URL}/compras`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener compras');
  }

  return response.json();
};

// Obtener compra por ID
export const getCompraById = async (id: number): Promise<Compra> => {
  const response = await fetch(`${API_URL}/compras/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener compra');
  }

  return response.json();
};

// Crear compra
export const createCompra = async (data: CompraRequest): Promise<Compra> => {
  const response = await fetch(`${API_URL}/compras`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al crear compra');
  }

  return response.json();
};

// Recibir compra
export const recibirCompra = async (id: number): Promise<Compra> => {
  const response = await fetch(`${API_URL}/compras/${id}/recibir`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al recibir compra');
  }

  return response.json();
};

// Cancelar compra
export const cancelarCompra = async (id: number): Promise<Compra> => {
  const response = await fetch(`${API_URL}/compras/${id}/cancelar`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al cancelar compra');
  }

  return response.json();
};

// Obtener compras por proveedor
export const getComprasByProveedor = async (proveedorId: number): Promise<Compra[]> => {
  const response = await fetch(`${API_URL}/compras/proveedor/${proveedorId}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener compras del proveedor');
  }

  return response.json();
};

// Obtener compras por estado
export const getComprasByEstado = async (estado: string): Promise<Compra[]> => {
  const response = await fetch(`${API_URL}/compras/estado/${estado}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al filtrar compras por estado');
  }

  return response.json();
};

// Obtener últimas compras
export const getUltimasCompras = async (): Promise<Compra[]> => {
  const response = await fetch(`${API_URL}/compras/ultimas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener últimas compras');
  }

  return response.json();
};

// Obtener compras por fechas
export const getComprasByFechas = async (inicio: string, fin: string): Promise<Compra[]> => {
  const response = await fetch(
    `${API_URL}/compras/fechas?inicio=${encodeURIComponent(inicio)}&fin=${encodeURIComponent(fin)}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw new Error('Error al filtrar compras por fecha');
  }

  return response.json();
};

// Obtener total de compras por fechas
export const getTotalComprasByFechas = async (inicio: string, fin: string): Promise<number> => {
  const response = await fetch(
    `${API_URL}/compras/total-fechas?inicio=${encodeURIComponent(inicio)}&fin=${encodeURIComponent(fin)}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw new Error('Error al obtener total de compras');
  }

  const data = await response.json();
  return data.total;
};

// Obtener estadísticas de compras
export const getComprasEstadisticas = async (): Promise<CompraEstadisticas> => {
  const response = await fetch(`${API_URL}/compras/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener estadísticas de compras');
  }

  return response.json();
};

// ============================================
// ENVÍOS 
// ============================================

import { 
  Envio, 
  EnvioRequest, 
  EnvioEstadisticas 
} from '@/types/envio';
import { 
  Transportadora, 
  TransportadoraRequest 
} from '@/types/transportadora';

// Listar todos los envíos
export const getAllEnvios = async (): Promise<Envio[]> => {
  const response = await fetch(`${API_URL}/envios`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener envíos');
  }

  return response.json();
};

// Obtener envío por ID
export const getEnvioById = async (id: number): Promise<Envio> => {
  const response = await fetch(`${API_URL}/envios/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener envío');
  }

  return response.json();
};


// Obtener envío por venta
export const getEnvioByVenta = async (idVenta: number): Promise<Envio> => {
  const response = await fetch(`${API_URL}/envios/venta/${idVenta}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener envío de la venta');
  }

  return response.json();
};

// Obtener envío por guía (público para tracking)
export const getEnvioByGuia = async (guiaRemision: string): Promise<Envio> => {
  const response = await fetch(`${API_URL}/envios/guia/${guiaRemision}`);

  if (!response.ok) {
    throw new Error('Error al obtener envío por guía');
  }

  return response.json();
};

// Crear envío
export const createEnvio = async (data: EnvioRequest): Promise<Envio> => {
  const response = await fetch(`${API_URL}/envios`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al crear envío');
  }

  return response.json();
};

// Actualizar envío
export const updateEnvio = async (id: number, data: EnvioRequest): Promise<Envio> => {
  const response = await fetch(`${API_URL}/envios/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al actualizar envío');
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
    const error = await response.json();
    throw new Error(error.error || 'Error al cambiar estado del envío');
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
    const error = await response.json();
    throw new Error(error.error || 'Error al marcar envío como entregado');
  }

  return response.json();
};

// Filtrar por estado
export const getEnviosByEstado = async (estado: string): Promise<Envio[]> => {
  const response = await fetch(`${API_URL}/envios/estado/${estado}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al filtrar envíos por estado');
  }

  return response.json();
};

// Envíos por transportadora
export const getEnviosByTransportadora = async (idTransportadora: number): Promise<Envio[]> => {
  const response = await fetch(`${API_URL}/envios/transportadora/${idTransportadora}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener envíos de la transportadora');
  }

  return response.json();
};

// Envíos pendientes
export const getEnviosPendientes = async (): Promise<Envio[]> => {
  const response = await fetch(`${API_URL}/envios/pendientes`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener envíos pendientes');
  }

  return response.json();
};

// Envíos en camino
export const getEnviosEnCamino = async (): Promise<Envio[]> => {
  const response = await fetch(`${API_URL}/envios/en-camino`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener envíos en camino');
  }

  return response.json();
};

// Envíos por entregar
export const getEnviosPorEntregar = async (): Promise<Envio[]> => {
  const response = await fetch(`${API_URL}/envios/por-entregar`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener envíos por entregar');
  }

  return response.json();
};

// Obtener estadísticas de envíos
export const getEnviosEstadisticas = async (): Promise<EnvioEstadisticas> => {
  const response = await fetch(`${API_URL}/envios/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener estadísticas de envíos');
  }

  return response.json();
};

// ============================================
// TRANSPORTADORAS 
// ============================================

// Listar todas las transportadoras
export const getAllTransportadoras = async (): Promise<Transportadora[]> => {
  const response = await fetch(`${API_URL}/transportadoras`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener transportadoras');
  }

  return response.json();
};

// Listar transportadoras activas
export const getActiveTransportadoras = async (): Promise<Transportadora[]> => {
  const response = await fetch(`${API_URL}/transportadoras/activas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener transportadoras activas');
  }

  return response.json();
};

// Obtener transportadora por ID
export const getTransportadoraById = async (id: number): Promise<Transportadora> => {
  const response = await fetch(`${API_URL}/transportadoras/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener transportadora');
  }

  return response.json();
};

// Crear transportadora
export const createTransportadora = async (data: TransportadoraRequest): Promise<Transportadora> => {
  const response = await fetch(`${API_URL}/transportadoras`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al crear transportadora');
  }

  return response.json();
};

// Actualizar transportadora
export const updateTransportadora = async (id: number, data: TransportadoraRequest): Promise<Transportadora> => {
  const response = await fetch(`${API_URL}/transportadoras/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al actualizar transportadora');
  }

  return response.json();
};

// Eliminar transportadora
export const deleteTransportadora = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/transportadoras/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al eliminar transportadora');
  }
};

// Activar/Desactivar transportadora
export const toggleTransportadoraStatus = async (id: number): Promise<Transportadora> => {
  const response = await fetch(`${API_URL}/transportadoras/${id}/toggle-status`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al cambiar estado de la transportadora');
  }

  return response.json();
};

// Obtener estadísticas de transportadoras
export const getTransportadoraStatistics = async () => {
  const response = await fetch(`${API_URL}/transportadoras/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener estadísticas de transportadoras');
  }

  return response.json();
};


// ============================================
// PAGOS 
// ============================================

import { 
  Pago, 
  PagoRequest, 
  PagoEstadisticas 
} from '@/types/pago';

// Listar todos los pagos
export const getAllPagos = async (): Promise<Pago[]> => {
  const response = await fetch(`${API_URL}/pagos`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener pagos');
  }

  return response.json();
};

// Obtener pago por ID
export const getPagoById = async (id: number): Promise<Pago> => {
  const response = await fetch(`${API_URL}/pagos/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener pago');
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
    const error = await response.json();
    throw new Error(error.error || 'Error al registrar pago');
  }

  return response.json();
};

// Obtener pagos de una venta
export const getPagosByVenta = async (ventaId: number): Promise<Pago[]> => {
  const response = await fetch(`${API_URL}/pagos/venta/${ventaId}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener pagos de la venta');
  }

  return response.json();
};

// Filtrar pagos por estado
export const getPagosByEstado = async (estado: string): Promise<Pago[]> => {
  const response = await fetch(`${API_URL}/pagos/estado/${estado}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al filtrar pagos por estado');
  }

  return response.json();
};

// Obtener estadísticas de pagos
export const getPagosEstadisticas = async (): Promise<PagoEstadisticas> => {
  const response = await fetch(`${API_URL}/pagos/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener estadísticas de pagos');
  }

  return response.json();
};

// Obtener total de pagos del día
export const getTotalPagosHoy = async (): Promise<number> => {
  const pagos = await getAllPagos();
  const hoy = new Date().toISOString().split('T')[0];
  
  const pagosHoy = pagos.filter(p => {
    const fechaPago = new Date(p.fechaPago).toISOString().split('T')[0];
    return fechaPago === hoy && p.estado === 'COMPLETADO';
  });

  return pagosHoy.reduce((total, pago) => total + pago.monto, 0);
};

// Obtener total de pagos del mes
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


// ============================================
// REPORTES 
// ============================================

import { 
  ReporteVentas,
  ReporteProductos,
  ReporteClientes,
  ReporteInventarioValorizado,
  VentaPorCategoria,
  VentaPorMetodoPago,
  ReporteInventarioStockBajo,
  ReporteProveedores,
  ReporteTransportadoras,
  ReporteFinanciero
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
    throw new Error('Error al obtener reporte de ventas');
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
    throw new Error('Error al obtener reporte de productos');
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
    throw new Error('Error al obtener reporte de clientes');
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
    throw new Error('Error al obtener reporte de inventario valorizado');
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
    throw new Error('Error al obtener reporte de ventas por categoría');
  }

  return response.json();
};

// Reporte de Ventas por Método de Pago
export const getReporteVentasPorMetodoPago = async (fechaInicio: string, fechaFin: string): Promise<VentaPorMetodoPago[]> => {
  const response = await fetch(
    `${API_URL}/reportes/ventas-por-metodo-pago?inicio=${encodeURIComponent(fechaInicio)}&fin=${encodeURIComponent(fechaFin)}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw new Error('Error al obtener reporte de ventas por método de pago');
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
  // Promise.all a proposito: el reporte cruza las dos listas. Con datos
  // parciales los totales por proveedor saldrian mal, y un numero equivocado
  // es peor que un error visible.
  const [proveedores, compras] = await Promise.all([
    getAllProveedores(),
    getAllCompras()
  ]);

  const proveedoresReporte = proveedores.map(proveedor => {
    const comprasProveedor = compras.filter(c => c.idProveedor === proveedor.id);
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
  // Promise.all a proposito: sin la lista completa de envios, la tasa de
  // entrega de cada transportadora daria un porcentaje falso.
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

// Reporte Financiero.
// El cálculo vive en el backend (solo ADMIN puede pedir este endpoint): la
// ganancia real sale de sumar, por cada línea de venta, precio menos costo
// unitario por la cantidad. Ese costo por producto no debe llegarle al
// frontend en ninguna respuesta que también pueda ver un EMPLEADO, así que
// acá solo se recibe el resultado ya calculado.
export const getReporteFinanciero = async (fechaInicio: string, fechaFin: string): Promise<ReporteFinanciero> => {
  const response = await fetch(
    `${API_URL}/reportes/financiero?inicio=${encodeURIComponent(fechaInicio)}&fin=${encodeURIComponent(fechaFin)}`,
    {
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw new Error('Error al obtener reporte financiero');
  }

  return response.json();
};



// ============================================
// PROMOCIONES - PROMOCIONS 
// ============================================

import { Promocion, PromocionRequest } from '@/types/promocion';

// Listar todas las promociones (ADMIN)
export const getAllPromociones = async (): Promise<Promocion[]> => {
  const response = await fetch(`${API_URL}/promociones`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener promociones');
  }

  return response.json();
};

// Listar promociones activas (ADMIN)
export const getActivePromociones = async (): Promise<Promocion[]> => {
  const response = await fetch(`${API_URL}/promociones/activas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener promociones activas');
  }

  return response.json();
};

// Listar promociones vigentes (público - tienda)
export const getPromocionesVigentes = async (): Promise<Promocion[]> => {
  const response = await fetch(`${API_URL}/promociones/vigentes`);

  if (!response.ok) {
    throw new Error('Error al obtener promociones vigentes');
  }

  return response.json();
};

// Obtener promocion por ID
export const getPromocionById = async (id: number): Promise<Promocion> => {
  const response = await fetch(`${API_URL}/promociones/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener promocion');
  }

  return response.json();
};

// Crear promocion
export const createPromocion = async (data: PromocionRequest): Promise<Promocion> => {
  const response = await fetch(`${API_URL}/promociones`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al crear promocion');
  }

  return response.json();
};

// Actualizar promocion
export const updatePromocion = async (id: number, data: PromocionRequest): Promise<Promocion> => {
  const response = await fetch(`${API_URL}/promociones/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al actualizar promocion');
  }

  return response.json();
};

// Eliminar promocion (desactivar)
export const deletePromocion = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/promociones/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al eliminar promocion');
  }
};

// Activar/Desactivar promocion
export const togglePromocionStatus = async (id: number): Promise<Promocion> => {
  const response = await fetch(`${API_URL}/promociones/${id}/toggle-status`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al cambiar estado de la promocion');
  }

  return response.json();
};

// Obtener estadísticas de promociones
export const getPromocionStatistics = async () => {
  const response = await fetch(`${API_URL}/promociones/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener estadísticas de promociones');
  }

  return response.json();
};


// ============================================
// CONFIGURACIÓN 
// ============================================

import { Configuracion, ConfiguracionRequest, Auditoria } from '@/types/configuracion';

// Listar todas las configuraciones
export const getAllConfiguraciones = async (): Promise<Configuracion[]> => {
  const response = await fetch(`${API_URL}/configuracion`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener configuraciones');
  }

  return response.json();
};

// Obtener configuración por ID
export const getConfiguracionById = async (id: number): Promise<Configuracion> => {
  const response = await fetch(`${API_URL}/configuracion/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener configuración');
  }

  return response.json();
};

// Obtener configuración por clave
export const getConfiguracionByClave = async (clave: string): Promise<Configuracion> => {
  const response = await fetch(`${API_URL}/configuracion/clave/${clave}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener configuración');
  }

  return response.json();
};

// Obtener solo el valor de una configuración
export const getValorConfiguracion = async (clave: string): Promise<string> => {
  const response = await fetch(`${API_URL}/configuracion/valor/${clave}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener valor de configuración');
  }

  const data = await response.json();
  return data.valor;
};

// Crear configuración
export const createConfiguracion = async (data: ConfiguracionRequest): Promise<Configuracion> => {
  const response = await fetch(`${API_URL}/configuracion`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al crear configuración');
  }

  return response.json();
};

// Actualizar configuración
export const updateConfiguracion = async (id: number, data: ConfiguracionRequest): Promise<Configuracion> => {
  const response = await fetch(`${API_URL}/configuracion/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al actualizar configuración');
  }

  return response.json();
};

// Actualizar solo el valor por clave
export const updateValorByClave = async (clave: string, nuevoValor: string): Promise<Configuracion> => {
  const response = await fetch(`${API_URL}/configuracion/clave/${clave}?nuevoValor=${encodeURIComponent(nuevoValor)}`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al actualizar configuración');
  }

  return response.json();
};

// Eliminar configuración
export const deleteConfiguracion = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/configuracion/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al eliminar configuración');
  }
};

// Inicializar configuraciones por defecto
export const inicializarConfiguraciones = async (): Promise<void> => {
  const response = await fetch(`${API_URL}/configuracion/inicializar`, {
    method: 'POST',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al inicializar configuraciones');
  }
};

// ============================================
// AUDITORÍA 
// ============================================

// Listar todas las auditorías
export const getAllAuditorias = async (): Promise<Auditoria[]> => {
  const response = await fetch(`${API_URL}/auditorias`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener auditorías');
  }

  return response.json();
};

// Obtener auditoría por ID
export const getAuditoriaById = async (id: number): Promise<Auditoria> => {
  const response = await fetch(`${API_URL}/auditorias/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener auditoría');
  }

  return response.json();
};

// Auditorías de un usuario
export const getAuditoriasByUsuario = async (idUsuario: number): Promise<Auditoria[]> => {
  const response = await fetch(`${API_URL}/auditorias/usuario/${idUsuario}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener auditorías del usuario');
  }

  return response.json();
};

// Auditorías de una tabla
export const getAuditoriasByTabla = async (tablaAfectada: string): Promise<Auditoria[]> => {
  const response = await fetch(`${API_URL}/auditorias/tabla/${tablaAfectada}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener auditorías de la tabla');
  }

  return response.json();
};

// Auditorías por acción
export const getAuditoriasByAccion = async (accion: string): Promise<Auditoria[]> => {
  const response = await fetch(`${API_URL}/auditorias/accion/${accion}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener auditorías por acción');
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
    throw new Error('Error al obtener auditorías por fechas');
  }

  return response.json();
};

// Últimas auditorías
export const getUltimasAuditorias = async (): Promise<Auditoria[]> => {
  const response = await fetch(`${API_URL}/auditorias/ultimas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener últimas auditorías');
  }

  return response.json();
};

// Auditorías del día
export const getAuditoriasDelDia = async (): Promise<Auditoria[]> => {
  const response = await fetch(`${API_URL}/auditorias/del-dia`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener auditorías del día');
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
    const error = await response.json();
    throw new Error(error.error || 'Error al obtener imágenes del producto');
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
    headers: getAuthHeadersForFormData(), // ✅ USAR LA NUEVA FUNCIÓN
    body: formData,
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al subir la imagen');
  }
  return response.json();
};

export const deleteImagenProducto = async (idImagen: number): Promise<void> => {
  const response = await fetch(`${API_URL}/imagenes-producto/${idImagen}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al eliminar la imagen');
  }
  // No hay cuerpo de respuesta para DELETE
};

export const setImagenPrincipal = async (idImagen: number, idProducto: number): Promise<void> => {
  const response = await fetch(`${API_URL}/imagenes-producto/${idImagen}/principal/${idProducto}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al marcar imagen como principal');
  }
  // No hay cuerpo de respuesta para PUT
};



// ============================================
// COMPROBANTES 
// ============================================

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
  fechaAnulacion?: string;
  motivoAnulacion?: string;
}

export interface ComprobanteRequest {
  idVenta: number;
  tipoComprobante: 'RECIBO' | 'COMPROBANTE' | 'NOTA_VENTA';
  nombreCliente: string;
  observaciones?: string;
}

/**
 * Obtener todos los comprobantes
 */
export const getAllComprobantes = async (): Promise<Comprobante[]> => {
  const response = await fetch(`${API_URL}/comprobantes`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener comprobantes');
  }

  return response.json();
};

/**
 * Obtener comprobante por ID
 */
export const getComprobanteById = async (id: number): Promise<Comprobante> => {
  const response = await fetch(`${API_URL}/comprobantes/${id}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Comprobante no encontrado');
  }

  return response.json();
};

/**
 * Obtener comprobante por venta
 */
export const getComprobanteByVenta = async (idVenta: number): Promise<Comprobante> => {
  const response = await fetch(`${API_URL}/comprobantes/venta/${idVenta}`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('No existe comprobante para esta venta');
  }

  return response.json();
};

/**
 * Crear comprobante manualmente (si no se creó automáticamente)
 */
export const createComprobante = async (data: ComprobanteRequest): Promise<Comprobante> => {
  const response = await fetch(`${API_URL}/comprobantes`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al crear comprobante');
  }

  return response.json();
};

/**
 * Anular comprobante (Solo ADMIN)
 */
export const anularComprobante = async (id: number, motivo: string): Promise<Comprobante> => {
  const response = await fetch(`${API_URL}/comprobantes/${id}/anular?motivo=${encodeURIComponent(motivo)}`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al anular comprobante');
  }

  return response.json();
};

/**
 * Obtener comprobantes activos (no anulados)
 */
export const getComprobantesActivos = async (): Promise<Comprobante[]> => {
  const response = await fetch(`${API_URL}/comprobantes/activos`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener comprobantes activos');
  }

  return response.json();
};

/**
 * Obtener últimos comprobantes
 */
export const getUltimosComprobantes = async (): Promise<Comprobante[]> => {
  const response = await fetch(`${API_URL}/comprobantes/ultimos`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener últimos comprobantes');
  }

  return response.json();
};


// frontend/src/lib/api.ts (agregar al final de la sección de PRODUCTOS)

// ============================================
// MULTIMEDIA PRODUCTOS (MODELOS 3D)
// ============================================

import { MultimediaProducto } from '@/types/producto';

// Obtener multimedia de un producto
export const getMultimediaProducto = async (productoId: number): Promise<MultimediaProducto | null> => {
  try {
    const response = await fetch(`${API_URL}/multimedia-productos/producto/${productoId}`, {
      headers: getAuthHeaders(),
    });

    if (response.status === 404) {
      return null; // No tiene modelo 3D
    }

    if (!response.ok) {
      throw new Error('Error al obtener multimedia del producto');
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
    const error = await response.json();
    throw new Error(error.error || 'Error al subir modelo 3D');
  }

  return response.json();
};

// Eliminar multimedia
export const eliminarMultimedia = async (multimediaId: number): Promise<void> => {
  const response = await fetch(`${API_URL}/multimedia-productos/${multimediaId}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al eliminar multimedia');
  }
};

// ============================================
// DASHBOARD
// ============================================

import { DashboardEstadisticas } from '@/types/dashboard';

/**
 * Estadísticas del panel de inicio.
 * El endpoint existía en el backend desde el principio; la pantalla de inicio
 * mostraba cifras escritas a mano en lugar de consultarlo.
 */
export const getDashboardEstadisticas = async (): Promise<DashboardEstadisticas> => {
  const response = await fetch(`${API_URL}/dashboard/estadisticas`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener las estadísticas del panel');
  }

  return response.json();
};


// ============================================
// DATOS PÚBLICOS DEL NEGOCIO
// ============================================

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

/**
 * Datos del negocio para la tienda pública (sin autenticación).
 * Se guardan en configuracion_sistema y se editan desde Configuración, así
 * que corregirlos no requiere tocar código.
 */
export const getDatosNegocio = async (): Promise<DatosNegocio> => {
  const response = await fetch(`${API_URL}/configuracion/negocio`);

  if (!response.ok) {
    throw new Error('Error al obtener los datos del negocio');
  }

  return response.json();
};


// ============================================
// MENSAJES DE CONTACTO
// ============================================

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

/**
 * Envía una consulta desde la tienda. No requiere sesión: es el único
 * endpoint de escritura público del sistema.
 */
export const enviarMensajeContacto = async (
  data: MensajeContactoRequest
): Promise<MensajeContacto> => {
  const response = await fetch(`${API_URL}/mensajes-contacto`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const detalle = await response.json().catch(() => null);
    throw new Error(detalle?.error || 'No se pudo enviar el mensaje');
  }

  return response.json();
};

export const getMensajesContacto = async (): Promise<MensajeContacto[]> => {
  const response = await fetch(`${API_URL}/mensajes-contacto`, {
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al obtener los mensajes de contacto');
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
    throw new Error('Error al marcar el mensaje como atendido');
  }

  return response.json();
};

export const eliminarMensajeContacto = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/mensajes-contacto/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Error al eliminar el mensaje');
  }
};


/**
 * Cambia el stock mínimo de un producto: el umbral que dispara las alertas.
 * Antes la pantalla de inventario solo hacía console.log y el valor nunca
 * llegaba a la base.
 */
export const actualizarStockMinimo = async (
  idProducto: number,
  stockMinimo: number
): Promise<Producto> => {
  const response = await fetch(
    `${API_URL}/productos/${idProducto}/stock-minimo?stockMinimo=${stockMinimo}`,
    { method: 'PATCH', headers: getAuthHeaders() }
  );

  if (!response.ok) {
    const detalle = await response.json().catch(() => null);
    throw new Error(detalle?.error || 'Error al actualizar el stock mínimo');
  }

  return response.json();
};
